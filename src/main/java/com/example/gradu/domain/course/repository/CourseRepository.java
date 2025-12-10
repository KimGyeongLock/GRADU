package com.example.gradu.domain.course.repository;

import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.course.entity.Term;
import com.example.gradu.domain.curriculum.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByStudentId(Long studentId);
    List<Course> findByStudentIdAndCategoryOrderByCreatedAtDesc(
            Long studentId, Category category
    );
    Optional<Course> findByIdAndStudentId(Long id, Long studentId);

    void deleteByStudentId(Long studentId);

    Optional<Course> findByStudentIdAndNameAndCategoryAndAcademicYearAndTerm(
            Long studentId,
            String name,
            Category category,
            Short academicYear,
            Term term
    );

    boolean existsByStudentIdAndNameAndCategoryAndAcademicYearAndTermAndIdNot(
            Long studentId,
            String name,
            Category category,
            Short academicYear,
            Term term,
            Long id
    );
}
