package com.example.gradu.domain.course.service;

import com.example.gradu.domain.course.dto.CourseRequestDto;
import com.example.gradu.domain.course.dto.CourseUpdateRequestDto;
import com.example.gradu.domain.course.entity.Course;
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

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final CurriculumRepository curriculumRepository;
    private final SummaryService summaryService;

    /** 0.5 단위 학점을 유닛(int, 학점×2)으로 변환 */
    private static int toUnits(BigDecimal credit) {
        if (credit == null) return 0;
        // 0.5 단위 전제 → ×2가 항상 정수
        return credit.multiply(BigDecimal.valueOf(2)).intValue();
    }

    @Transactional
    public void addCourse(String studentId, CourseRequestDto request) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        Course course = Course.builder()
                .student(student)
                .name(request.name())
                .category(request.category())
                .credit(request.credit())                 // BigDecimal
                .designedCredit(request.designedCredit()) // Integer (정수)
                .grade(request.grade())
                .isEnglish(request.isEnglish())
                .build();
        courseRepository.save(course);

        // 커리큘럼 누적(유닛 기준)
        Curriculum cur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, request.category())
                .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
        cur.addEarnedCredits(toUnits(request.credit())); // ✅ int(유닛) 전달

        // 전공 설계학점 누적(정수), 전공일 때만
        if (request.category() == Category.MAJOR) {
            Curriculum designedCur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, Category.MAJOR_DESIGNED)
                    .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
            designedCur.addEarnedCredits(Optional.ofNullable(request.designedCredit()).orElse(0));
        }

        // 스냅샷 재계산
        summaryService.recomputeAndSave(studentId);
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByCategory(String studentId, Category category) {
        return courseRepository.findByStudent_StudentIdAndCategoryOrderByCreatedAtDesc(studentId, category);
    }

    @Transactional
    public Course updateCourse(String studentId, Long courseId, CourseUpdateRequestDto request) {
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

    private Course loadCourse(String studentId, Long courseId) {
        return courseRepository.findByIdAndStudent_StudentId(courseId, studentId)
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

        // 전공 설계 변화량(정수)
        if (ctx.oldCat == Category.MAJOR && ctx.newCat == Category.MAJOR) {
            ctx.deltaDesigned = ctx.newDesigned - ctx.oldDesigned;
        } else {
            ctx.deltaDesigned = 0; // 카테고리 이동은 별도 처리
        }

        return ctx;
    }

    /** 카테고리 이동 시: 이전 카테고리에서 빼고, 새 카테고리에 더함 (유닛) + 전공 설계 보정 */
    private void applyCategoryChange(String studentId, UpdateContext ctx) {
        Curriculum prevCur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, ctx.oldCat)
                .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
        Curriculum newCur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, ctx.newCat)
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

    /** 카테고리 동일 시: 해당 카테고리 유닛 증감 + 전공 설계 증감 */
    private void applySameCategoryAdjustments(String studentId, UpdateContext ctx) {
        if (ctx.deltaUnits != 0) {
            Curriculum cur = getCurriculum(studentId, ctx.oldCat);
            cur.addEarnedCredits(ctx.deltaUnits); // 유닛 증감
        }
        if (ctx.oldCat == Category.MAJOR && ctx.deltaDesigned != 0) {
            Curriculum majorDesignedCur = getCurriculum(studentId, Category.MAJOR_DESIGNED);
            majorDesignedCur.addEarnedCredits(ctx.deltaDesigned); // 정수 증감
        }
    }

    private void applyEntityFieldUpdates(Course course, CourseUpdateRequestDto request, UpdateContext ctx) {
        if (request.getName() != null) course.rename(request.getName());
        if (request.getGrade() != null) course.changeGrade(request.getGrade());

        if (ctx.categoryChanged) course.changeCategory(ctx.newCat);
        // credit(BigDecimal) 변경
        if (ctx.deltaUnits != 0) course.changeCredit(ctx.newCredit);

        // 설계학점: 전공만 유지, 그 외 0
        course.changeDesignedCredit((ctx.newCat == Category.MAJOR) ? ctx.newDesigned : 0);

        // 영어강의 여부 변경이 들어왔다면 반영 (옵션)
        course.changeEnglish(request.isEnglish());
    }

    private Curriculum getCurriculum(String studentId, Category category) {
        return curriculumRepository.findByStudentStudentIdAndCategory(studentId, category)
                .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
    }

    @Transactional
    public void deleteCourse(String studentId, Long courseId) {
        Course course = loadCourse(studentId, courseId);

        // 해당 카테고리에서 학점 제거(유닛)
        Curriculum cur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, course.getCategory())
                .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
        cur.addEarnedCredits(-toUnits(course.getCredit())); // ✅ 유닛으로 제거

        // 전공 설계 제거(정수)
        if (course.getCategory() == Category.MAJOR) {
            Curriculum majorDesignedCur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, Category.MAJOR_DESIGNED)
                    .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
            majorDesignedCur.addEarnedCredits(-Optional.ofNullable(course.getDesignedCredit()).orElse(0));
        }

        courseRepository.delete(course);
        summaryService.recomputeAndSave(studentId);
    }

    /** 변경 계산 컨텍스트 */
    private static class UpdateContext {
        Category oldCat;
        BigDecimal oldCredit; // BigDecimal
        int oldDesigned;

        Category newCat;
        BigDecimal newCredit; // BigDecimal
        int newDesigned;

        boolean categoryChanged;
        int deltaUnits;     // 학점 변화량(유닛: ×2)
        int deltaDesigned;  // 전공 설계 변화량(정수)
    }
}
