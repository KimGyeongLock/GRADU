package com.example.gradu.domain.captureAI.service;

import com.example.gradu.domain.captureAI.dto.CourseBulkRequest;
import com.example.gradu.domain.course.service.CourseService;
import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.global.client.OpenAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiCaptureService {

    private final OpenAiClient openAiClient;

    public List<CourseBulkRequest> analyzeCapture(List<MultipartFile> images) {

        List<String> base64Images = images.stream()
                .map(this::convertToBase64)
                .toList();

        String aiResponse = openAiClient.analyzeCourseImages(base64Images);

        return parseAiResponse(aiResponse);
    }

    private String convertToBase64(MultipartFile image) {
        try {
            return Base64.getEncoder().encodeToString(image.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("이미지 변환 실패" + e);
        }
    }

    private List<CourseBulkRequest> parseAiResponse(String aiResponse) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            CourseBulkRequest[] arr = objectMapper.readValue(aiResponse, CourseBulkRequest[].class);
            List<CourseBulkRequest> list = Arrays.asList(arr);

            for (CourseBulkRequest c : list) {
                boolean english = isEnglishCourseName(c.getName(), c.getCategory());
                c.setEnglish(english);   // 필드가 isEnglish지만 setter 이름은 setEnglish
            }

            return list;
        } catch (Exception e) {
            throw new RuntimeException("AI 응답 파싱 실패" + e);
        }
    }

    private boolean isEnglishCourseName(String name, Category category) {
        if (name == null) return false;
        if (category == Category.PRACTICAL_ENGLISH) return false;

        String trimmed = name.replaceAll("[\\s\\d.,()\\-+/]", "");
        if (trimmed.isEmpty()) return false;

        for (char ch : trimmed.toCharArray()) {
            if (!((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z'))) {
                // 한글이나 다른 문자 섞여 있으면 false
                return false;
            }
        }
        return true;
    }
}
