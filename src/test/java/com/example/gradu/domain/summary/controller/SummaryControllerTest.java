package com.example.gradu.domain.summary.controller;

import com.example.gradu.domain.summary.dto.SummaryDto;
import com.example.gradu.domain.summary.dto.TogglesDto;
import com.example.gradu.domain.summary.service.SummaryCommandService;
import com.example.gradu.domain.summary.service.SummaryService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = SummaryController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class),
        }
)
@AutoConfigureMockMvc(addFilters = false)
class SummaryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @MockitoBean SummaryService summaryService;
    @MockitoBean SummaryCommandService summaryCommandService;

    private static final String BASE = "/api/v1/students/{sid}/summary";

    @Test
    void get_ok_returnsDto() throws Exception {
        // given
        long sid = 1L;
        SummaryDto dto = mock(SummaryDto.class);
        when(summaryService.getSummary(sid)).thenReturn(dto);

        // when & then
        mockMvc.perform(get(BASE, sid))
                .andExpect(status().isOk());

        verify(summaryService).getSummary(sid);
    }

    @Test
    void patchToggles_ok_callsService() throws Exception {
        // given
        long sid = 1L;
        TogglesDto toggles = mock(TogglesDto.class);
        String body = om.writeValueAsString(toggles);

        // when & then
        mockMvc.perform(patch(BASE + "/toggles", sid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                // void 메서드 기본 응답은 200 OK
                .andExpect(status().isOk());

        verify(summaryService).updateTogglesAndRecompute(eq(sid), any(TogglesDto.class));
    }

    @Test
    void rebuild_ok_callsCommandService() throws Exception {
        // given
        long sid = 1L;

        // when & then
        mockMvc.perform(post(BASE + "/rebuild", sid))
                .andExpect(status().isOk());

        verify(summaryCommandService).recomputeAndSave(sid);
    }
}
