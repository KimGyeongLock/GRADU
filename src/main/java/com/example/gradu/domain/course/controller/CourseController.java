package com.example.gradu.domain.course.controller;

import com.example.gradu.domain.captureAI.dto.CourseBulkSaveRequest;
import com.example.gradu.domain.course.dto.CourseRequestDto;
import com.example.gradu.domain.course.dto.CourseResponseDto;
import com.example.gradu.domain.course.dto.CourseUpdateRequestDto;
import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.course.service.CourseService;
import com.example.gradu.domain.curriculum.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/students/{studentId}/courses")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<Void> addCourse(@PathVariable String studentId, @RequestBody CourseRequestDto request) {
        courseService.addCourse(studentId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/categories/{category}")
    public ResponseEntity<List<CourseResponseDto>> getCoursesByCategory(@PathVariable String studentId, @PathVariable Category category) {
        List<Course> list = courseService.getCoursesByCategory(studentId, category);
        return ResponseEntity.ok(list.stream().map(CourseResponseDto::from).toList());
    }

    @GetMapping("/all")
    public ResponseEntity<List<CourseResponseDto>> getAllCourses(@PathVariable String studentId) {
        List<Course> list = courseService.getCoursesAll(studentId);
        return ResponseEntity.ok(list.stream().map(CourseResponseDto::from).toList());
    }

    @PatchMapping("/{courseId}")
    public ResponseEntity<CourseResponseDto> updateCourse(@PathVariable String studentId, @PathVariable Long courseId, @RequestBody CourseUpdateRequestDto requestDto) {
        Course updated = courseService.updateCourse(studentId, courseId, requestDto);
        return ResponseEntity.ok(CourseResponseDto.from(updated));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String studentId, @PathVariable Long courseId) {
        courseService.deleteCourse(studentId, courseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    public ResponseEntity<Void> saveBulk(@RequestBody CourseBulkSaveRequest request) {
        courseService.bulkInsert(request.getSid(), request.getCourses());
        return ResponseEntity.ok().build();
    }
}
