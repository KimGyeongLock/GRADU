package com.example.gradu.domain.course.service;

import com.example.gradu.domain.course.dto.CourseRequestDto;
import com.example.gradu.domain.course.dto.CourseResponseDto;
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

    public List<Course> getCoursesByCategory(String studentId, Category category) {
        return courseRepository.findByStudent_StudentIdAndCategoryOrderByCreatedAtDesc(studentId, category);
    }

    @Transactional
    public Course updateCourse(String studentId, Long courseId, CourseRequestDto request) {
        Course course = courseRepository.findByIdAndStudent_StudentId(courseId, studentId)
                .orElseThrow(() -> new CurriculumException(ErrorCode.COURSE_NOT_FOUND));

        // --- 기존 값 백업 ---
        Category oldCat = course.getCategory();
        int oldCredit = course.getCredit();
        int oldDesigned = Optional.ofNullable(course.getDesignedCredit()).orElse(0);

        // --- 신규 값 계산(요청값 없으면 기존 유지) ---
        Category newCat = (request.category() != null) ? request.category() : oldCat;

        // credit이 null 이 아닐 것 같지만(요청 DTO가 primitive면) 방어적으로 처리
        int newCredit = (request.credit() != 0) ? request.credit() : oldCredit;

        // 설계학점: 전공에서만 의미. 요청이 null이면 기존값 유지.
        int reqDesigned = Optional.ofNullable(request.designedCredit()).orElse(oldDesigned);
        int newDesigned = (newCat == Category.MAJOR) ? reqDesigned : 0; // 비전공이면 0으로 간주

        boolean categoryChanged = (newCat != oldCat);
        int deltaCredit = newCredit - oldCredit;
        int deltaDesigned = 0;

        // --- 카테고리별 학점 반영 ---
        if (categoryChanged) {
            // 1) 이전 카테고리에서 전량 차감
            Curriculum prevCur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, oldCat)
                    .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
            prevCur.addEarnedCredits(-oldCredit);

            // 2) 새 카테고리에 전량 가산
            Curriculum newCur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, newCat)
                    .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
            newCur.addEarnedCredits(newCredit);

            // 3) 전공 설계 누계 반영
            Curriculum majorDesignedCur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, Category.MAJOR_DESIGNED)
                    .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));

            if (oldCat == Category.MAJOR) {
                // 전공 → 타 카테고리: 기존 설계학점 전량 차감
                majorDesignedCur.addEarnedCredits(-oldDesigned);
            }
            if (newCat == Category.MAJOR) {
                // 타 카테고리 → 전공: 새 설계학점 전량 가산
                majorDesignedCur.addEarnedCredits(newDesigned);
            }
        } else {
            // 카테고리 동일: 학점/설계학점의 차액만 반영
            if (deltaCredit != 0) {
                Curriculum cur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, oldCat)
                        .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
                cur.addEarnedCredits(deltaCredit);
            }

            if (oldCat == Category.MAJOR) {
                // 전공일 때만 설계학점 차액 반영
                deltaDesigned = newDesigned - oldDesigned;
                if (deltaDesigned != 0) {
                    Curriculum majorDesignedCur = curriculumRepository.findByStudentStudentIdAndCategory(studentId, Category.MAJOR_DESIGNED)
                            .orElseThrow(() -> new CurriculumException(ErrorCode.CURRICULUM_NOT_FOUND));
                    majorDesignedCur.addEarnedCredits(deltaDesigned);
                }
            }
        }

        // --- 엔티티 필드 최종 변경 (변경감지) ---
        if (request.name() != null) course.rename(request.name());
        if (request.grade() != null) course.changeGrade(request.grade());

        // 카테고리/학점/설계학점 적용
        if (categoryChanged) course.changeCategory(newCat);
        if (deltaCredit != 0) course.changeCredit(newCredit);
        // 비전공이면 설계학점은 0(또는 null)로 정규화
        course.changeDesignedCredit((newCat == Category.MAJOR) ? newDesigned : 0);

        return course;
    }


    @Transactional
    public void deleteCourse(String studentId, Long courseId) {
        Course course = courseRepository.findByIdAndStudent_StudentId(courseId, studentId)
                .orElseThrow(() -> new CurriculumException(ErrorCode.COURSE_NOT_FOUND));

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
}
