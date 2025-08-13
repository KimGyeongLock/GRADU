package com.example.gradu.domain.course.repository;

import com.example.gradu.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByStudentStudentId(String studentId);
}
