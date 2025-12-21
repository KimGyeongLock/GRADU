package com.example.gradu.domain.capture_ai.service;

import com.example.gradu.domain.capture_ai.dto.CourseBulkRequest;
import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.global.client.OpenAiClient;
import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.ai.AIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiCaptureServiceTest {

    @Mock OpenAiClient openAiClient;

    @InjectMocks AiCaptureService service;

    @Test
    void analyzeCapture_success_parsesAndSetsEnglishFlag() {
        // given: 2장 이미지
        MultipartFile img1 = new MockMultipartFile("f1", "a.png", "image/png", "HELLO".getBytes());
        MultipartFile img2 = new MockMultipartFile("f2", "b.png", "image/png", "WORLD".getBytes());

        // 영어 판별 케이스 포함:
        // 1) 일반 카테고리 + 영문명 => english=true
        // 2) PRACTICAL_ENGLISH면 무조건 false
        String aiJson = """
                [
                  {"name":"Data Structures","category":"MAJOR"},
                  {"name":"Business English 1","category":"PRACTICAL_ENGLISH"}
                ]
                """;

        when(openAiClient.analyzeCourseImages(anyList())).thenReturn(aiJson);

        // when
        List<CourseBulkRequest> result = service.analyzeCapture(List.of(img1, img2));

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Data Structures");
        assertThat(result.get(0).isEnglish()).isTrue();

        assertThat(result.get(1).getCategory()).isEqualTo(Category.PRACTICAL_ENGLISH);
        assertThat(result.get(1).isEnglish()).isFalse();

        verify(openAiClient, times(1)).analyzeCourseImages(anyList());
    }

    @Test
    void analyzeCapture_whenAiReturnsInvalidJson_throwsParsingFailed() {
        // given
        MultipartFile img = new MockMultipartFile("f", "a.png", "image/png", "X".getBytes());
        when(openAiClient.analyzeCourseImages(anyList())).thenReturn("not-json");

        // when & then
        assertThatThrownBy(() -> service.analyzeCapture(List.of(img)))
                .isInstanceOf(AIException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_RESPONSE_PARSING_FAILED);
    }

    @Test
    void analyzeCapture_whenJsonHasIllegalCategory_throwsParsingFailed() {
        // given: Category enum 매핑 실패(IllegalArgumentException 유발 가능)
        MultipartFile img = new MockMultipartFile("f", "a.png", "image/png", "X".getBytes());

        String aiJson = """
                [
                  {"name":"Some Course","category":"NOT_A_CATEGORY"}
                ]
                """;
        when(openAiClient.analyzeCourseImages(anyList())).thenReturn(aiJson);

        // when & then
        assertThatThrownBy(() -> service.analyzeCapture(List.of(img)))
                .isInstanceOf(AIException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_RESPONSE_PARSING_FAILED);
    }

    @Test
    void analyzeCapture_englishDetection_false_whenNameNull() {
        // given
        MultipartFile img = new MockMultipartFile("f", "a.png", "image/png", "X".getBytes());
        String aiJson = """
                [
                  {"name":null,"category":"MAJOR"}
                ]
                """;
        when(openAiClient.analyzeCourseImages(anyList())).thenReturn(aiJson);

        // when
        List<CourseBulkRequest> result = service.analyzeCapture(List.of(img));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isNull();
        assertThat(result.get(0).isEnglish()).isFalse();
    }

    @Test
    void analyzeCapture_englishDetection_false_whenTrimmedEmpty() {
        // given: 숫자/기호만 있으면 trimmed empty => false
        MultipartFile img = new MockMultipartFile("f", "a.png", "image/png", "X".getBytes());
        String aiJson = """
                [
                  {"name":" 1234-() ","category":"MAJOR"}
                ]
                """;
        when(openAiClient.analyzeCourseImages(anyList())).thenReturn(aiJson);

        // when
        List<CourseBulkRequest> result = service.analyzeCapture(List.of(img));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isEnglish()).isFalse();
    }

    @Test
    void analyzeCapture_englishDetection_false_whenContainsKorean() {
        // given: 한글 섞이면 ^[a-zA-Z]+$ 실패 => false
        MultipartFile img = new MockMultipartFile("f", "a.png", "image/png", "X".getBytes());
        String aiJson = """
                [
                  {"name":"자료구조 Data","category":"MAJOR"}
                ]
                """;
        when(openAiClient.analyzeCourseImages(anyList())).thenReturn(aiJson);

        // when
        List<CourseBulkRequest> result = service.analyzeCapture(List.of(img));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).isEnglish()).isFalse();
    }
}
