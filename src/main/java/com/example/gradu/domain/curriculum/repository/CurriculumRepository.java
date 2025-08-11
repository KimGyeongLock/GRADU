package com.example.gradu.domain.curriculum.repository;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.curriculum.entity.Curriculum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {
    List<Curriculum> findByStudentStudentId(String studentId);
    Optional<Curriculum> findByStudentStudentIdAndCategory(String studentId, Category category);
    boolean existsByStudentStudentIdAndCategory(String studentId, Category category);
}
