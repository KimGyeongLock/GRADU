package com.example.gradu.global.client;

import com.example.gradu.domain.captureAI.dto.OpenAiResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiClient {
    private final String apiKey;
    private final String apiUrl;
    private final String modelName;
    private final RestTemplate restTemplate;

    public OpenAiClient(@Value("${openai.api.key}") String apiKey,
                        @Value("${openai.api.url}") String apiUrl,
                        @Value("${openai.api.model}") String modelName,
                            RestTemplate restTemplate) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.modelName = modelName;
        this.restTemplate = restTemplate;
    }

    public String analyzeCourseImages(List<String> base64Images) {

        var content = new java.util.ArrayList<Map<String, Object>>();

        // 텍스트 블록
        content.add(
                Map.of("type", "text", "text",
                        """
                        너는 졸업심사 성적표를 분석하는 OCR AI야.
        
                        입력으로는 여러 개의 성적표 캡쳐 이미지가 들어온다.
                        각 이미지는 다음과 같은 구조를 가진 표 형태일 수 있다.
        
                        - 표 맨 위에 파란색 배경의 섹션 제목이 있다. (예: "인성및리더십", "신앙및세계관", "BSM", "자유선택(교양)", "자유선택(교양또는비교양)", "전공주제" 등)
                        - 그 아래 표의 첫 번째 열에는 "구분" 이라는 헤더와 "교필", "전필", "교선필" 같은 값이 있다.
        
                        ***중요 규칙***
        
                        1) category 는 반드시 표 맨 위 파란색 섹션 제목을 기준으로 정한다.
                           - 예: 섹션 제목이 "인성및리더십" 이면, 그 표 안의 모든 행의 category 는 "PERSONALITY_LEADERSHIP" 이다.
                           - 섹션 제목이 "자유선택(교양)" 이면, 그 표 안의 모든 행의 category 는 "FREE_ELECTIVE_BASIC" 이다.
                           - 섹션 제목이 "자유선택(교양또는비교양)" 이면, 그 표 안의 모든 행의 category 는 "FREE_ELECTIVE_MJR" 이다.
                           - 섹션 제목이 "전공주제" 인 경우, 그 표 안의 모든 행의 category 는 **무조건** "MAJOR" 여야 한다.
                        2) "구분" 열(교필, 전필, 교선필, 자선 등)은 참고용일 뿐,
                           절대 category 값으로 사용하지 말고 무시한다.
                           category 는 항상 파란색 섹션 제목으로만 결정한다.
                        3) 한 이미지 안에 섹션이 여러 개 있을 수 있다.
                           이 경우 각 섹션 제목별로 구분하여, 그 섹션에 속한 행들은 같은 category 를 사용한다.
        
                        4) category 필드는 아래 ENUM 중 하나의 영문 값으로만 출력해야 한다.
        
                           - FAITH_WORLDVIEW        : "신앙및세계관", "신앙과세계관" 등
                           - PERSONALITY_LEADERSHIP : "인성및리더십"
                           - BSM                    : "BSM"
                           - ICT_INTRO              : "ICT융합기초", "ICT 기초" 등
                           - PRACTICAL_ENGLISH      : "실용영어" 계열
                           - GENERAL_EDU            : "교양필수", "교양", 일반 교양 영역
                           - MAJOR                  : 전공 영역(전공필수, 전공선택, 전공주제 등)
                           - FREE_ELECTIVE_BASIC    : "자유선택(교양)" 등 자유선택 교양/기초
                           - FREE_ELECTIVE_MJR      : "자유선택(교양또는비교양)", 전공 자유선택
        
                           섹션 제목이 위에 정확히 매칭되지 않을 경우, 가장 의미가 비슷한 ENUM 하나를 골라 사용해라.
                           ENUM 이외의 값(예: "교필", "교선필", "교양필수" 같은 한글 그대로)은 절대 category 로 사용하지 마.
        
                        5) 설계학점(designedCredit) 규칙
        
                           - 설계학점은 **전공 영역(MAJOR)** 과목에서만 인정한다.
                             category 가 MAJOR 가 아니면 designedCredit 은 항상 0 이다.
                           - 표의 "학점(설계)" 형식이 "3(2)" 처럼 나오면:
                             credit = 3, designedCredit = 2 로 저장한다.
                           - "3(0)", "3" 처럼 설계가 0 이거나 괄호가 아예 없으면:
                             별도 표시가 없으면 designedCredit = 0 으로 본다.
                           - 만약 "학점"과 별도로 "비고" 열에 "설계" 라는 단어가 있는 과목이라면,
                             설계학점은 일반 학점과 동일하게 본다.
                             예: credit = 3, 비고에 "설계" 가 있으면 designedCredit = 3.
                           - 위 규칙이 충돌할 경우, "비고"의 설계 표시 > 괄호 안 숫자 순으로 우선한다.
        
                        6) 응답은 반드시 아래 JSON 배열 형태만 출력해라. 설명, 자연어 텍스트는 포함하지 마라.
                        
                        7) 성적(grade) 표기 규칙
                    
                           - grade 필드는 반드시 아래 중 하나의 값만 사용한다.
                             ["A+", "A0", "B+", "B0", "C+", "C0", "D+", "D0", "F", "P", "PD", "PASS"]
                        
                           - 특히 "A0", "B0", "C0", "D0" 에서의 0은 **항상 숫자 0 (zero)** 이어야 한다.
                             알파벳 O(오)를 쓰면 안 된다.
                        
                           - OCR 결과가 "AO", "A O", "aO", "Bo", "B O" 처럼 나와도,
                             최종 응답에서는 반드시 다음과 같이 정규화해서 출력한다.
                               - "AO", "A O", "aO" 등  →  "A0"
                               - "BO", "B O", "bO" 등  →  "B0"
                               - "CO"                 →  "C0"
                               - "DO"                 →  "D0"
                        
                           - 공백, 소문자 등은 모두 제거/대문자로 정리한 뒤 위 리스트 중 가장 가까운 값 하나로 변환해라.
                           - 위 리스트에 없는 문자열은 그대로 사용하지 말고, 반드시 가장 비슷한 값으로 변환해서 출력해라.
                        [
                          {
                            "name": "공동체리더십훈련1",
                            "credit": 0.5,
                            "designedCredit": 0.5,
                            "category": "PERSONALITY_LEADERSHIP",
                            "grade": "P",
                            "isEnglish": false,
                            "academicYear": 2019,
                            "term": "1"
                          }
                        ]
                        """
                )
        );


        content.addAll(createImageBlocks(base64Images));

        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", content
                        )
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<?> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<OpenAiResponseDto> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                request,
                OpenAiResponseDto.class
        );

        OpenAiResponseDto body = response.getBody();

        if (body == null || body.getChoices().isEmpty()) {
            throw new IllegalStateException("OpenAI 응답이 비어 있습니다.");
        }

        return body.getChoices()
                .get(0)
                .getMessage()
                .getContent();
    }

    private List<Map<String, Object>> createImageBlocks(List<String> base64Images) {
        return base64Images.stream()
                .map(img -> Map.of(
                        "type", "image_url",
                        "image_url", Map.of(
                                "url", "data:image/png;base64," + img
                        )
                ))
                .toList();
    }
}
