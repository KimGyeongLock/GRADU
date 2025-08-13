package com.example.gradu.domain.curriculum.controller;

import com.example.gradu.domain.curriculum.dto.CurriculumResponseDto;
import com.example.gradu.domain.curriculum.entity.Curriculum;
import com.example.gradu.domain.curriculum.service.CurriculumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/students/{studentId}/curriculum")
public class CurriculumController {

    private final CurriculumService curriculumService;

    @GetMapping
    public ResponseEntity<List<CurriculumResponseDto>> getCurriculums(@PathVariable String studentId){
        List<Curriculum> curriculums = curriculumService.getCurriculumsByStudentId(studentId);
        List<CurriculumResponseDto> body = curriculums.stream()
                .map(CurriculumResponseDto::from)
                .toList();
        return ResponseEntity.ok(body);

    }
}
