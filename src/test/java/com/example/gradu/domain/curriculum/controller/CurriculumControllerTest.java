package com.example.gradu.domain.curriculum.controller;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.curriculum.entity.Curriculum;
import com.example.gradu.domain.curriculum.service.CurriculumService;
import com.example.gradu.global.config.JpaAuditingConfig;
import com.example.gradu.global.security.SecurityConfig;
import com.example.gradu.global.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CurriculumController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class),
        }
)
@AutoConfigureMockMvc(addFilters = false)
class CurriculumControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean CurriculumService curriculumService;

    private Curriculum stubCurriculum(Category category, Curriculum.Status status) {
        Curriculum c = mock(Curriculum.class);
        when(c.getCategory()).thenReturn(category);
        when(c.getStatus()).thenReturn(status);
        return c;
    }

    @Test
    void getCurriculums_ok_returnsList() throws Exception {
        // given
        long studentId = 1L;

        Curriculum c1 = stubCurriculum(Category.MAJOR, Curriculum.Status.PASS);
        Curriculum c2 = stubCurriculum(Category.GENERAL_EDU, Curriculum.Status.PASS);

        when(curriculumService.getCurriculumsByStudentId(eq(studentId)))
                .thenReturn(List.of(c1, c2));

        // when & then
        mockMvc.perform(get("/api/v1/students/{studentId}/curriculum", studentId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                // 일단 배열 길이만 확인 (from() 내부는 여기서 굳이 검증 안 해도 됨)
                .andExpect(jsonPath("$.length()").value(2));

        verify(curriculumService).getCurriculumsByStudentId(studentId);
    }
}
