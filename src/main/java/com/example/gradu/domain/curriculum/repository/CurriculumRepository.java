package com.example.gradu.domain.curriculum.repository;

import com.example.gradu.domain.curriculum.entity.Curriculum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {
    List<Curriculum> findByStudentStudentId(String studentId);
}
