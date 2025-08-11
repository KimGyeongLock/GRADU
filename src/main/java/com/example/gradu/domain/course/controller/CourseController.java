package com.example.gradu.domain.course.controller;

import com.example.gradu.domain.course.dto.CourseRequest;
import com.example.gradu.domain.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/students/{studentId}/courses")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<Void> addCourse(@PathVariable String studentId, @RequestBody CourseRequest request) {
        courseService.addCourse(studentId, request);
        return ResponseEntity.ok().build();
    }
}
