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
import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.curriculum.CurriculumException;
import com.example.gradu.global.exception.student.StudentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final CurriculumRepository curriculumRepository;

    @Transactional
    public void addCourse(String studentId, CourseRequestDto request) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        Course course = Course.builder()
                .student(student)
                .name(request.name())
                .category(request.category())
                .credit(request.credit())
                .designedCredit(request.designedCredit())
                .grade(request.grade())
                .build();
        courseRepository.save(course);

        Curriculum cur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, request.category())
                .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
        Curriculum designedCur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, Category.MAJOR_DESIGNED)
                .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
        cur.addEarnedCredits(request.credit());
        designedCur.addEarnedCredits(request.designedCredit());
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

        return course;
    }

    private Course loadCourse(String studentId, Long courseId) {
        return courseRepository.findByIdAndStudent_StudentId(courseId, studentId)
                .orElseThrow(() -> new CurriculumException(ErrorCode.COURSE_NOT_FOUND));
    }

    private UpdateContext computedNewValues(Course course, CourseUpdateRequestDto request) {
        UpdateContext ctx = new UpdateContext();

        ctx.oldCat = course.getCategory();
        ctx.oldCredit = course.getCredit();
        ctx.oldDesigned = Optional.ofNullable(course.getDesignedCredit()).orElse(0);

        ctx.newCat = (request.getCategory() != null) ? request.getCategory() : ctx.oldCat;

        ctx.newCredit = (request.getCredit() != 0) ? request.getCredit() : ctx.oldCredit;

        int regDesigned = Optional.ofNullable(request.getDesignedCredit()).orElse(ctx.oldDesigned);
        ctx.newDesigned = (ctx.newCat == Category.MAJOR) ? regDesigned : 0;

        ctx.categoryChanged = (ctx.newCat != ctx.oldCat);
        ctx.deltaCredit = ctx.newCredit - ctx.oldCredit;
        ctx.deltaDesigned = (ctx.oldCat == Category.MAJOR) ? (ctx.newDesigned - ctx.oldDesigned) : 0;
        return ctx;
    }

    private void applyCategoryChange(String studentId, UpdateContext ctx) {
        Curriculum prevCur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, ctx.oldCat)
                .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
        Curriculum newCur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, ctx.newCat)
                .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));

        Curriculum majorDesignedCur = getCurriculum(studentId, Category.MAJOR_DESIGNED);

        if (ctx.oldCat == Category.MAJOR) {
            majorDesignedCur.addEarnedCredits(-ctx.oldDesigned);
        }
        if (ctx.newCat == Category.MAJOR) {
            majorDesignedCur.addEarnedCredits(ctx.newDesigned);
        }
    }

    private void applySameCategoryAdjustments(String studentId, UpdateContext ctx) {
        if (ctx.deltaCredit != 0) {
            Curriculum cur = getCurriculum(studentId, ctx.oldCat);
            cur.addEarnedCredits(ctx.deltaCredit);
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
        if (ctx.deltaCredit != 0) course.changeCredit(ctx.newCredit);

        course.changeDesignedCredit((ctx.newCat == Category.MAJOR) ? ctx.newDesigned : 0);
    }

    private Curriculum getCurriculum(String studentId, Category category) {
        return curriculumRepository.findByStudentStudentIdAndCategory(studentId, category)
                .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
    }

    @Transactional
    public void deleteCourse(String studentId, Long courseId) {
        Course course = loadCourse(studentId, courseId);

        Curriculum cur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, course.getCategory())
                .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
        cur.addEarnedCredits(-course.getCredit());

        if (course.getCategory() == Category.MAJOR) {
            Curriculum majorDesignedCur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, Category.MAJOR_DESIGNED)
                    .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
            majorDesignedCur.addEarnedCredits(-Optional.ofNullable(course.getDesignedCredit()).orElse(0));
        }

        courseRepository.delete(course);
    }

    private static class UpdateContext {
        Category oldCat;
        int oldCredit;
        int oldDesigned;

        Category newCat;
        int newCredit;
        int newDesigned;

        boolean categoryChanged;
        int deltaCredit;
        int deltaDesigned;
    }
}

