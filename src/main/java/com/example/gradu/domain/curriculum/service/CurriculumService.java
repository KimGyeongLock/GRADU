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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CurriculumService {

    private final CurriculumRepository curriculumRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public void initializeForStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentException(ErrorCode.STUDENT_NOT_FOUND));

        List<Curriculum> newCurriculums = new ArrayList<>();
        for (Category c : Category.values()) {
            Curriculum cur = Curriculum.builder()
                    .student(student)
                    .category(c)
                    .earnedCredits(0)
                    .build();
            cur.recalcStatus();
            newCurriculums.add(cur);
        }
        curriculumRepository.saveAll(newCurriculums);
    }

    @Transactional(readOnly = true)
    public List<Curriculum> getCurriculumsByStudentId(Long studentId) {
        return curriculumRepository.findByStudentId(studentId);
    }

    @Transactional
    public void removeForStudent(Long studentId) {
        curriculumRepository.deleteByStudentId(studentId);
    }
}
