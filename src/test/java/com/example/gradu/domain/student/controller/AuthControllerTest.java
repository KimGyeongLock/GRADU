package com.example.gradu.domain.student.controller;

import com.example.gradu.domain.student.dto.LoginResponseDto;
import com.example.gradu.domain.student.dto.PasswordResetRequestDto;
import com.example.gradu.domain.student.dto.StudentAuthRequestDto;
import com.example.gradu.domain.student.service.StudentService;
import com.example.gradu.global.config.JpaAuditingConfig;
import com.example.gradu.global.security.SecurityConfig;
import com.example.gradu.global.security.jwt.JwtAuthenticationFilter;
import com.example.gradu.global.security.jwt.JwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "app.frontend-domain=example.com",
        "app.cookie.secure=true",
        "app.cookie.same-site=None"
})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @MockitoBean StudentService studentService;
    @MockitoBean JwtProperties jwtProperties;

    private static final String BASE = "/api/v1/auth";

    @Test
    void register_ok_returnsMessage() throws Exception {
        // given
        StudentAuthRequestDto req = new StudentAuthRequestDto("a@handong.ac.kr", "pw1234!!", "123456");
        String body = om.writeValueAsString(req);

        // when
        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원가입 성공"));

        // then
        verify(studentService).register("a@handong.ac.kr", "pw1234!!", "123456");
    }

    @Test
    void login_ok_setsRefreshCookie_andReturnsAccessToken() throws Exception {
        // given
        when(jwtProperties.getRefreshExpiration()).thenReturn(120_000L);
        when(studentService.login("a@handong.ac.kr", "pw1234!!"))
                .thenReturn(new LoginResponseDto("ACCESS", "REFRESH"));

        StudentAuthRequestDto req = new StudentAuthRequestDto("a@handong.ac.kr", "pw1234!!", "123456");
        String body = om.writeValueAsString(req);

        // when
        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("ACCESS"))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=REFRESH")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Secure")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite=None")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Domain=example.com")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Path=/")));

        // then
        verify(studentService).login("a@handong.ac.kr", "pw1234!!");
    }

    @Test
    void reissue_ok_whenCookieHasRefreshToken() throws Exception {
        // given
        when(studentService.reissue("REFRESH")).thenReturn("NEW_ACCESS");

        // when
        mockMvc.perform(post(BASE + "/reissue")
                        .cookie(new Cookie(AuthController.REFRESH_TOKEN, "REFRESH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("NEW_ACCESS"));

        // then
        verify(studentService).reissue("REFRESH");
    }

    @Test
    void reissue_unauthorized_whenAuthorizationHeaderUsed() throws Exception {
        // when
        mockMvc.perform(post(BASE + "/reissue")
                        .header("Authorization", "Bearer REFRESH"))
                .andExpect(status().isUnauthorized());

        // then
        verify(studentService, never()).reissue(any());
    }

    @Test
    void reissue_unauthorized_whenNoToken() throws Exception {
        // when
        mockMvc.perform(post(BASE + "/reissue"))
                .andExpect(status().isUnauthorized());

        // then
        verify(studentService, never()).reissue(anyString());
    }

    @Test
    void logout_noCookie_returns204_andDeletesCookie_withoutCallingService() throws Exception {
        // when
        mockMvc.perform(post(BASE + "/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        // then
        verify(studentService, never()).logout(anyString());
    }

    @Test
    void logout_withCookie_callsService_andDeletesCookie() throws Exception {
        // when
        mockMvc.perform(post(BASE + "/logout")
                        .cookie(new Cookie(AuthController.REFRESH_TOKEN, "REFRESH")))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("refreshToken=")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        // then
        verify(studentService).logout("REFRESH");
    }

    @Test
    void withdraw_ok_passesStudentIdAndRefreshToken_andDeletesCookie() throws Exception {
        // given
        Authentication auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());

        // when
        mockMvc.perform(delete(BASE + "/withdraw")
                        .cookie(new Cookie(AuthController.REFRESH_TOKEN, "REFRESH"))
                        .principal(auth))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        // then
        verify(studentService).withdraw(1L, "REFRESH");
    }

    @Test
    void resetPassword_ok_returns204() throws Exception {
        // given
        PasswordResetRequestDto req = new PasswordResetRequestDto("a@handong.ac.kr", "123456", "Newpw123!!");
        String body = om.writeValueAsString(req);

        // when
        mockMvc.perform(post(BASE + "/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        // then
        verify(studentService).resetPassword(any(PasswordResetRequestDto.class));
    }

    @Test
    void register_badRequest_whenValidationFails() throws Exception {
        // given
        String body = """
                {"email":"","password":"","code":""}
                """;

        // when
        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        // then
        verify(studentService, never()).register(anyString(), anyString(), anyString());
    }
}
