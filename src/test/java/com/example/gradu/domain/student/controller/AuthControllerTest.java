package com.example.gradu.domain.student.controller;

import com.example.gradu.domain.student.dto.StudentAuthRequestDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("회원가입 성공 테스트")
    void registerSuccessTeest() throws Exception {
        // given
        StudentAuthRequestDto request = StudentAuthRequestDto.builder()
                .studentId("21900064")
                .password("password123!")
                .build();

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원가입 성공"));
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void loginSuccessTest() throws Exception {
        // given
        StudentAuthRequestDto request = StudentAuthRequestDto.builder()
                .studentId("21900064")
                .password("password123!")
                .build();

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        MvcResult mvcResult = result.andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(cookie().exists("refreshToken"))
                .andReturn();

        String responseBody = mvcResult.getResponse().getContentAsString();
        System.out.println("응답 내용: " + responseBody);

        // JSON 파싱하여 accessToken 값만 출력
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(responseBody);
        System.out.println("Access Token: " + jsonNode.get("accessToken").asText());

    }

    @Test
    @DisplayName("토큰 재발급 테스트")
    void reissueSuccessTest() throws Exception {
        // given
        String accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyMTkwMDA2NCIsImlhdCI6MTc1NDQwMzg3MiwiZXhwIjoxNzU0NDA0NzcyfQ.wZ_mlj7hfdejDMwoGOYPY8h2Rrb6qcknoWxpz9wonRg";
        String refreshToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyMTkwMDA2NCIsImlhdCI6MTc1NDQwMzg3MiwiZXhwIjoxNzU1MDA4NjcyfQ.2zFGOY6BQvlKSeKa0tGGL08Nh2TbNsk9b1LFyZu-OGk";

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/auth/reissue")
                .cookie(new Cookie("refreshToken", refreshToken))
                .header("Authorization", accessToken));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("로그아웃 성공 테스트")
    void logoutSuccessTeset() throws Exception {
        // given
        String accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyMTkwMDA2NCIsImlhdCI6MTc1NDQwMzg3MiwiZXhwIjoxNzU0NDA0NzcyfQ.wZ_mlj7hfdejDMwoGOYPY8h2Rrb6qcknoWxpz9wonRg";
        String refreshToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyMTkwMDA2NCIsImlhdCI6MTc1NDQwMzg3MiwiZXhwIjoxNzU1MDA4NjcyfQ.2zFGOY6BQvlKSeKa0tGGL08Nh2TbNsk9b1LFyZu-OGk";

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/auth/logout")
                .cookie(new Cookie("refreshToken", refreshToken))
                .header("Authorization", "Bearer " + accessToken));


        // then
        result.andExpect(status().isOk())
                .andExpect(cookie().value("refreshToken", ""));
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 실패 테스트")
    void loginFailTest() throws Exception {
        // given
        StudentAuthRequestDto request = StudentAuthRequestDto.builder()
                .studentId("21900064")
                .password("wrongPassword12!")
                .build();

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        result.andExpect(status().isUnauthorized());
    }
}