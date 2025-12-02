package com.example.gradu.domain.summary.repository;

import com.example.gradu.domain.summary.entity.Summary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
    Optional<Summary> findByStudentId(String studentId);

    void deleteByStudentId(String studentId);
}
