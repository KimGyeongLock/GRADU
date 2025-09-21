package com.example.gradu.domain.course.repository;

import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.curriculum.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByStudentStudentId(String studentId);
    // 학생 + 카테고리별 과목
    List<Course> findByStudent_StudentIdAndCategoryOrderByCreatedAtDesc(
            String studentId, Category category
    );
    Optional<Course> findByIdAndStudent_StudentId(Long id, String studentId);
}
