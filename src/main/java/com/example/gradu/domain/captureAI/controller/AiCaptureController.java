package com.example.gradu.domain.captureAI.controller;

import com.example.gradu.domain.captureAI.dto.CourseBulkRequest;
import com.example.gradu.domain.captureAI.service.AiCaptureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiCaptureController {

    private final AiCaptureService aiCaptureService;

    @PostMapping(value = "/course-capture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<CourseBulkRequest>> captureCoursesByImage(
            @RequestPart("images") List<MultipartFile> images
    ) {
        List<CourseBulkRequest> courses = aiCaptureService.analyzeCapture(images);
        return ResponseEntity.ok(courses);
    }
}
