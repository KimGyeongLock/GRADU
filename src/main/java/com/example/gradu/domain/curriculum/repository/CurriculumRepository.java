package com.example.gradu.domain.curriculum.repository;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.curriculum.entity.Curriculum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {
    List<Curriculum> findByStudentStudentId(String studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Curriculum> findByStudentStudentIdAndCategory(String studentId, Category category);

    void deleteByStudentStudentId(String studentId);
}
