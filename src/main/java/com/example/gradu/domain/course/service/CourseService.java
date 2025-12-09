package com.example.gradu.domain.course.service;

import com.example.gradu.domain.captureAI.dto.CourseBulkRequest;
import com.example.gradu.domain.course.dto.CourseRequestDto;
import com.example.gradu.domain.course.dto.CourseUpdateRequestDto;
import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.course.entity.Term;
import com.example.gradu.domain.course.repository.CourseRepository;
import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.curriculum.entity.Curriculum;
import com.example.gradu.domain.curriculum.repository.CurriculumRepository;
import com.example.gradu.domain.student.entity.Student;
import com.example.gradu.domain.student.repository.StudentRepository;
import com.example.gradu.domain.summary.service.SummaryService;
import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.curriculum.CurriculumException;
import com.example.gradu.global.exception.student.StudentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final CurriculumRepository curriculumRepository;
    private final SummaryService summaryService;

    private static int toUnits(BigDecimal credit) {
        if (credit == null) return 0;
        return credit.multiply(BigDecimal.valueOf(2)).intValue();
    }

    @Transactional
    public void addCourse(Long studentId, CourseRequestDto request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        Course course = Course.builder()
                .student(student)
                .name(request.name())
                .category(request.category())
                .credit(request.credit())
                .designedCredit(request.designedCredit())
                .grade(request.grade())
                .isEnglish(request.isEnglish())
                .academicYear(request.academicYear())
                .term(Term.fromCode(request.term()))
                .build();
        courseRepository.save(course);

        Curriculum cur = curriculumRepository.findByStudentIdAndCategory(studentId, request.category())
                .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
        cur.addEarnedCredits(toUnits(request.credit()));

        if (request.category() == Category.MAJOR) {
            Curriculum designedCur = curriculumRepository.findByStudentIdAndCategory(studentId, Category.MAJOR_DESIGNED)
                    .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
            designedCur.addEarnedCredits(Optional.of(request.designedCredit()).orElse(0));
        }

        summaryService.recomputeAndSave(studentId);
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByCategory(Long studentId, Category category) {
        return courseRepository.findByStudentIdAndCategoryOrderByCreatedAtDesc(studentId, category);
    }

    @Transactional
    public Course updateCourse(Long studentId, Long courseId, CourseUpdateRequestDto request) {
        Course course = loadCourse(studentId, courseId);

        UpdateContext ctx = computedNewValues(course, request);
        if (ctx.categoryChanged) {
            applyCategoryChange(studentId, ctx);
        } else {
            applySameCategoryAdjustments(studentId, ctx);
        }
        applyEntityFieldUpdates(course, request, ctx);

        summaryService.recomputeAndSave(studentId);
        return course;
    }

    private Course loadCourse(Long studentId, Long courseId) {
        return courseRepository.findByIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new CurriculumException(ErrorCode.COURSE_NOT_FOUND));
    }

    /** 변경 전/후 값을 계산 (credit은 BigDecimal, 커리큘럼 반영은 유닛 int) */
    private UpdateContext computedNewValues(Course course, CourseUpdateRequestDto request) {
        UpdateContext ctx = new UpdateContext();

        ctx.oldCat = course.getCategory();
        ctx.oldCredit = Optional.ofNullable(course.getCredit()).orElse(BigDecimal.ZERO);
        ctx.oldDesigned = Optional.ofNullable(course.getDesignedCredit()).orElse(0);

        ctx.newCat = (request.getCategory() != null) ? request.getCategory() : ctx.oldCat;
        ctx.newCredit = (request.getCredit() != null) ? request.getCredit() : ctx.oldCredit;

        // 전공이 아니면 설계 0
        int requestedDesigned = Optional.ofNullable(request.getDesignedCredit()).orElse(ctx.oldDesigned);
        ctx.newDesigned = (ctx.newCat == Category.MAJOR) ? requestedDesigned : 0;

        ctx.categoryChanged = (ctx.newCat != ctx.oldCat);

        // 커리큘럼에 반영할 학점 변화량(유닛)
        ctx.deltaUnits = toUnits(ctx.newCredit.subtract(ctx.oldCredit));

        ctx.oldYear = course.getAcademicYear();
        ctx.oldTerm = course.getTerm();

        ctx.newYear = Optional.ofNullable(request.getAcademicYear()).orElse(ctx.oldYear);
        ctx.newTerm = Optional.ofNullable(request.getTerm())
                .map(Term::fromCode)
                .orElse(ctx.oldTerm);

        ctx.semesterChanged = !ctx.newYear.equals(ctx.oldYear) || ctx.newTerm != ctx.oldTerm;

        // 전공 설계 변화량(정수)
        if (ctx.oldCat == Category.MAJOR && ctx.newCat == Category.MAJOR) {
            ctx.deltaDesigned = ctx.newDesigned - ctx.oldDesigned;
        } else {
            ctx.deltaDesigned = 0; // 카테고리 이동은 별도 처리
        }

        return ctx;
    }

    private void applyCategoryChange(Long studentId, UpdateContext ctx) {
        Curriculum prevCur = curriculumRepository.findByStudentIdAndCategory(studentId, ctx.oldCat)
                .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
        Curriculum newCur = curriculumRepository.findByStudentIdAndCategory(studentId, ctx.newCat)
                .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));

        // 이전 카테고리에서 oldCredit 제거, 새 카테고리에 newCredit 추가 (유닛)
        prevCur.addEarnedCredits(-toUnits(ctx.oldCredit));
        newCur.addEarnedCredits(toUnits(ctx.newCredit));

        // 전공 설계: 전공→타카테고리면 제거, 타→전공이면 추가
        Curriculum majorDesignedCur = getCurriculum(studentId, Category.MAJOR_DESIGNED);
        if (ctx.oldCat == Category.MAJOR) {
            majorDesignedCur.addEarnedCredits(-ctx.oldDesigned);
        }
        if (ctx.newCat == Category.MAJOR) {
            majorDesignedCur.addEarnedCredits(ctx.newDesigned);
        }
    }

    private void applySameCategoryAdjustments(Long studentId, UpdateContext ctx) {
        if (ctx.deltaUnits != 0) {
            Curriculum cur = getCurriculum(studentId, ctx.oldCat);
            cur.addEarnedCredits(ctx.deltaUnits);
        }
        if (ctx.oldCat == Category.MAJOR && ctx.deltaDesigned != 0) {
            Curriculum majorDesignedCur = getCurriculum(studentId, Category.MAJOR_DESIGNED);
            majorDesignedCur.addEarnedCredits(ctx.deltaDesigned);
        }
    }

    private void applyEntityFieldUpdates(Course course, CourseUpdateRequestDto request, UpdateContext ctx) {
        if (request.getName() != null) course.rename(request.getName());
        if (request.getGrade() != null) course.changeGrade(request.getGrade());
        if (ctx.categoryChanged) course.changeCategory(ctx.newCat);
        if (ctx.deltaUnits != 0) course.changeCredit(ctx.newCredit);
        course.changeDesignedCredit((ctx.newCat == Category.MAJOR) ? ctx.newDesigned : 0);

        if (ctx.semesterChanged) course.changeSemester(ctx.newYear, ctx.newTerm);

        course.changeEnglish(request.isEnglish());
    }

    private Curriculum getCurriculum(Long studentId, Category category) {
        return curriculumRepository.findByStudentIdAndCategory(studentId, category)
                .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
    }

    @Transactional
    public void deleteCourse(Long studentId, Long courseId) {
        Course course = loadCourse(studentId, courseId);

        // 해당 카테고리에서 학점 제거(유닛)
        Curriculum cur = curriculumRepository.findByStudentIdAndCategory(studentId, course.getCategory())
                .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
        cur.addEarnedCredits(-toUnits(course.getCredit()));

        // 전공 설계 제거(정수)
        if (course.getCategory() == Category.MAJOR) {
            Curriculum majorDesignedCur = curriculumRepository.findByStudentIdAndCategory(studentId, Category.MAJOR_DESIGNED)
                    .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
            majorDesignedCur.addEarnedCredits(-Optional.ofNullable(course.getDesignedCredit()).orElse(0));
        }

        courseRepository.delete(course);
        summaryService.recomputeAndSave(studentId);
    }

    public List<Course> getCoursesAll(Long studentId) {
        return courseRepository.findByStudentId(studentId);
    }

    @Transactional
    public void bulkInsert(Long studentId, List<CourseBulkRequest> courses) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        List<Course> entities = courses.stream()
                .map(req -> Course.builder()
                        .student(student)
                        .name(req.getName())
                        .category(req.getCategory())
                        .credit(req.getCredit())
                        .designedCredit(req.getDesignedCredit())
                        .grade(req.getGrade())
                        .isEnglish(req.isEnglish())
                        .academicYear(req.getAcademicYear())
                        .term(Term.fromCode(req.getTerm()))
                        .build()
                ).toList();
        courseRepository.saveAll(entities);

        courses.stream()
                .collect(Collectors.groupingBy(CourseBulkRequest::getCategory,
                        Collectors.mapping(CourseBulkRequest::getCredit, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
                .forEach((category, totalCredit) -> {
                    Curriculum cur = curriculumRepository.findByStudentIdAndCategory(studentId, category)
                            .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
                    cur.addEarnedCredits(toUnits(totalCredit));
                });

        int totalDesignedCredit = courses.stream()
                .filter(c -> c.getCategory() == Category.MAJOR && c.getDesignedCredit() != null)
                .mapToInt(CourseBulkRequest::getDesignedCredit)
                .sum();

        if (totalDesignedCredit > 0) {
            Curriculum designedCur = curriculumRepository.findByStudentIdAndCategory(studentId, Category.MAJOR_DESIGNED)
                    .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
            designedCur.addEarnedCredits(totalDesignedCredit);
        }

        summaryService.recomputeAndSave(studentId);
    }

    public void removeForStudent(Long studentId) {
        courseRepository.deleteByStudentId(studentId);
    }

    private static class UpdateContext {
        Category oldCat;
        BigDecimal oldCredit;
        int oldDesigned;

        Category newCat;
        BigDecimal newCredit;
        int newDesigned;

        boolean categoryChanged;
        int deltaUnits;
        int deltaDesigned;

        Short oldYear; Term oldTerm;
        Short newYear; Term newTerm;
        boolean semesterChanged;
    }
}
