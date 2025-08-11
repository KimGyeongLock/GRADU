package com.example.gradu.domain.course.service;

import com.example.gradu.domain.course.dto.CourseRequest;
import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.course.repository.CourseRepository;
import com.example.gradu.domain.curriculum.entity.Curriculum;
import com.example.gradu.domain.curriculum.repository.CurriculumRepository;
import com.example.gradu.domain.student.entity.Student;
import com.example.gradu.domain.student.repository.StudentRepository;
import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.student.StudentException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final CurriculumRepository currRepository;

    @Transactional
    public void addCourse(String studentId, CourseRequest request) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        Course course = Course.builder()
                .student(student)
                .name(request.name())
                .category(request.category())
                .credit(request.credit())
                .designedCredits(request.designedCredit())
                .grade(request.grade())
                .build();
        courseRepository.save(course);

        Curriculum cur = currRepository.findByStudentStudentIdAndCategory(studentId, request.category())
                .orElseGet(() -> {
                    Curriculum r = Curriculum.builder()
                            .student(student)
                            .category(request.category())
                            .earnedCredits(0)
                            .status(Curriculum.Status.FAIL)
                            .build();
                    return currRepository.save(r);
                });
        cur.setEarnedCredits(cur.getEarnedCredits() + request.credit());
        cur.recalcStatus();
    }
}
