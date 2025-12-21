package com.example.gradu.domain.email.controller;

import com.example.gradu.domain.email.dto.EmailRequestDto;
import com.example.gradu.domain.email.service.EmailVerificationService;
import com.example.gradu.global.config.JpaAuditingConfig;
import com.example.gradu.global.security.SecurityConfig;
import com.example.gradu.global.security.jwt.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = EmailController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class),
        }
)
@AutoConfigureMockMvc(addFilters = false)
class EmailControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @MockitoBean EmailVerificationService service;

    @Test
    void send_ok_returns204() throws Exception {
        // given
        EmailRequestDto req = new EmailRequestDto("a@handong.ac.kr");
        String body = om.writeValueAsString(req);

        // when & then
        mockMvc.perform(post("/api/v1/auth/email/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        verify(service).sendCode("a@handong.ac.kr");
    }

    @Test
    void send_badRequest_whenValidationFails() throws Exception {
        // EmailRequestDto에 @NotBlank/@Email 등이 있어야 400이 나옴
        String body = """
                {"email":""}
                """;

        mockMvc.perform(post("/api/v1/auth/email/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(service, never()).sendCode(anyString());
    }
}
