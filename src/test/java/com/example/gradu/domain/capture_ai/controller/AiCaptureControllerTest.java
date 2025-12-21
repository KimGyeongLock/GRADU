package com.example.gradu.domain.capture_ai.controller;

import com.example.gradu.domain.capture_ai.dto.CourseBulkRequest;
import com.example.gradu.domain.capture_ai.service.AiCaptureService;
import com.example.gradu.global.config.JpaAuditingConfig;
import com.example.gradu.global.security.SecurityConfig;
import com.example.gradu.global.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AiCaptureController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class),
        }
)
@AutoConfigureMockMvc(addFilters = false)
class AiCaptureControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AiCaptureService aiCaptureService;

    private static final String BASE = "/api/v1/ai";

    @Test
    void captureCoursesByImage_ok_returnsList() throws Exception {
        // given: multipart files (RequestPart name = "images")
        MockMultipartFile img1 = new MockMultipartFile(
                "images",
                "a.png",
                MediaType.IMAGE_PNG_VALUE,
                "dummy".getBytes()
        );
        MockMultipartFile img2 = new MockMultipartFile(
                "images",
                "b.png",
                MediaType.IMAGE_PNG_VALUE,
                "dummy2".getBytes()
        );

        // 서비스 반환값 (DTO 구조를 모르면 mock으로도 가능)
        CourseBulkRequest dto = mock(CourseBulkRequest.class);
        when(aiCaptureService.analyzeCapture(anyList())).thenReturn(List.of(dto));

        // when & then
        mockMvc.perform(multipart(BASE + "/course-capture")
                        .file(img1)
                        .file(img2)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(aiCaptureService).analyzeCapture(anyList());
    }

    @Test
    void captureCoursesByImage_badRequest_whenNoImages() throws Exception {
        // "images" 파트가 없으면 400이 나는지 확인 (기본 동작)
        mockMvc.perform(multipart(BASE + "/course-capture")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        verify(aiCaptureService, never()).analyzeCapture(anyList());
    }
}
