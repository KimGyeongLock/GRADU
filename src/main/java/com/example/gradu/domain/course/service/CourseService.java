package com.example.gradu.domain.course.service;

import com.example.gradu.domain.capture_ai.dto.CourseBulkRequest;
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
import com.example.gradu.domain.summary.service.SummaryCommandService;
import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.course.CourseException;
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
    private final CourseCommandService courseCommandService;
    private final SummaryCommandService summaryCommandService;


    public static int toUnits(BigDecimal credit) {
        if (credit == null) return 0;
        return credit.multiply(BigDecimal.valueOf(2)).intValue();
    }


    @Transactional
    public void addCourse(Long studentId, CourseRequestDto request, boolean overwrite) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        Optional<Course> existingOpt = courseRepository.findByStudentIdAndNameAndCategoryAndAcademicYearAndTerm(studentId, request.name(), request.category(), request.academicYear(), Term.fromCode(request.term()));

        if (existingOpt.isPresent() && !overwrite) {
            throw new CourseException(ErrorCode.COURSE_DUPLICATE_EXCEPTION);
        }

        if (existingOpt.isPresent() && overwrite) {
            Course existing = existingOpt.get();

            CourseUpdateRequestDto dto = new CourseUpdateRequestDto(
                    request.name(),
                    request.credit(),
                    request.designedCredit(),
                    request.grade(),
                    request.category(),
                    request.isEnglish(),
                    request.academicYear(),
                    request.term()
            );

            courseCommandService.updateCourse(studentId, existing.getId(), dto);
            return;
        }

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

        summaryCommandService.recomputeAndSave(studentId);
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByCategory(Long studentId, Category category) {
        return courseRepository.findByStudentIdAndCategoryOrderByCreatedAtDesc(studentId, category);
    }

    public List<Course> getCoursesAll(Long studentId) {
        return courseRepository.findByStudentId(studentId);
    }

    private String buildKey(String name, Category category, Short year, Term term) {
        return name + "|" + category.name() + "|" + year + "|" + term.name();
    }

    private String buildKey(CourseBulkRequest req) {
        return buildKey(
                req.getName(),
                req.getCategory(),
                req.getAcademicYear(),
                Term.fromCode(req.getTerm())
        );
    }

    @Transactional
    public void bulkInsert(Long studentId, List<CourseBulkRequest> courses) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        // 현재 학생이 가진 과목 이름들
        List<Course> existingCourses = courseRepository.findByStudentId(studentId);
        var existingKeys = existingCourses.stream()
                .map(c -> buildKey(c.getName(), c.getCategory(), c.getAcademicYear(), c.getTerm()))
                .collect(Collectors.toSet());

        // 이번에 넣으려는 것 중에서 이미 존재하는 이름들
        List<String> dupNames = courses.stream()
                .filter(req -> existingKeys.contains(buildKey(req)))
                .map(CourseBulkRequest::getName)
                .distinct()
                .toList();

        if (!dupNames.isEmpty()) {
            throw new CourseException(ErrorCode.COURSE_DUPLICATE_EXCEPTION, dupNames);
        }

        // 🔽 기존 bulkInsert 그대로
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
                        Collectors.mapping(CourseBulkRequest::getCredit,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
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
        summaryCommandService.recomputeAndSave(studentId);

    }


    public void removeForStudent(Long studentId) {
        courseRepository.deleteByStudentId(studentId);
    }
}
