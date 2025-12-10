package com.example.gradu.domain.course.controller;

import com.example.gradu.domain.captureAI.dto.CourseBulkRequest;
import com.example.gradu.domain.course.dto.CourseRequestDto;
import com.example.gradu.domain.course.dto.CourseResponseDto;
import com.example.gradu.domain.course.dto.CourseUpdateRequestDto;
import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.course.service.CourseService;
import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.global.security.CheckStudentAccess;
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
    @CheckStudentAccess
    public ResponseEntity<Void> addCourse(
            @PathVariable Long studentId,
            @RequestBody CourseRequestDto request,
            @RequestParam(name = "overwrite", defaultValue = "false") boolean overwrite
    ) {
        courseService.addCourse(studentId, request, overwrite);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/categories/{category}")
    @CheckStudentAccess
    public ResponseEntity<List<CourseResponseDto>> getCoursesByCategory(@PathVariable Long studentId, @PathVariable Category category) {
        List<Course> list = courseService.getCoursesByCategory(studentId, category);
        return ResponseEntity.ok(list.stream().map(CourseResponseDto::from).toList());
    }

    @GetMapping("/all")
    @CheckStudentAccess
    public ResponseEntity<List<CourseResponseDto>> getAllCourses(@PathVariable Long studentId) {
        List<Course> list = courseService.getCoursesAll(studentId);
        return ResponseEntity.ok(list.stream().map(CourseResponseDto::from).toList());
    }

    @PatchMapping("/{courseId}")
    @CheckStudentAccess
    public ResponseEntity<CourseResponseDto> updateCourse(@PathVariable Long studentId, @PathVariable Long courseId, @RequestBody CourseUpdateRequestDto requestDto) {
        Course updated = courseService.updateCourse(studentId, courseId, requestDto);
        return ResponseEntity.ok(CourseResponseDto.from(updated));
    }

    @DeleteMapping("/{courseId}")
    @CheckStudentAccess
    public ResponseEntity<Void> deleteCourse(@PathVariable Long studentId, @PathVariable Long courseId) {
        courseService.deleteCourse(studentId, courseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @CheckStudentAccess
    public ResponseEntity<Void> saveBulk(@PathVariable Long studentId, @RequestBody List<CourseBulkRequest> courses) {
        courseService.bulkInsert(studentId, courses);
        return ResponseEntity.ok().build();
    }
}
