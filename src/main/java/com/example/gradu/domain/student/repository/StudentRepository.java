package com.example.gradu.domain.student.repository;

import com.example.gradu.domain.student.entity.Student;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentId(String studentId);

    Optional<Student> findByStudentIdAndEmail(@NotBlank String studentId, @Email String email);
}
