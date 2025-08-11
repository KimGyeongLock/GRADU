package com.example.gradu.domain.curriculum.service;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.curriculum.entity.Curriculum;
import com.example.gradu.domain.curriculum.repository.CurriculumRepository;
import com.example.gradu.domain.student.entity.Student;
import com.example.gradu.domain.student.repository.StudentRepository;
import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.student.StudentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurriculumService {

    private final CurriculumRepository curriculumRepository;
    private final StudentRepository studentRepository;

    public void initializeForStudent(String studentId) {
        Student student = studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        for (Category c : Category.values()) {
            Curriculum cur = Curriculum.builder()
                    .student(student)
                    .category(c)
                    .earnedCredits(0)
                    .status(Curriculum.Status.FAIL)
                    .build();
            cur.recalcStatus();
            curriculumRepository.save(cur);
        }
    }

    public List<Curriculum> findBoard(String studentId) {
        return curriculumRepository.findByStudentStudentId(studentId);
    }
}
