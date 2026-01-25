package com.example.gradu.domain.ranking.controller;

import com.example.gradu.domain.ranking.dto.CourseRankingDto;
import com.example.gradu.domain.ranking.service.CourseRankingService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CourseRankingController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class),
        }
)
@AutoConfigureMockMvc(addFilters = false)
class CourseRankingControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CourseRankingService rankingService;

    @Test
    void getCourses_returnsRankingResponseJson() throws Exception {
        // given
        var item = new CourseRankingDto.RankingItem(1, "DB", 10, 0);

        var major = new CourseRankingDto.MajorRanking(
                List.of(item), // y1s2
                List.of(),     // y2s1
                List.of(),     // y2s2
                List.of(),     // y3s1
                List.of(),     // y3s2
                List.of(),     // y4s1
                List.of()      // y4s2
        );

        var liberal = new CourseRankingDto.LiberalRanking(
                List.of(new CourseRankingDto.RankingItem(1, "채플", 20, 0)), // faithWorldview
                List.of(), // generalEdu
                List.of(), // bsm
                List.of()  // freeElective
        );

        var resp = new CourseRankingDto.RankingResponse(major, liberal);

        when(rankingService.getCourseRanking()).thenReturn(resp);

        // when & then
        mvc.perform(get("/api/v1/rankings/courses"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))

                // major.y1s2[0]
                .andExpect(jsonPath("$.major.y1s2[0].rank").value(1))
                .andExpect(jsonPath("$.major.y1s2[0].courseName").value("DB"))
                .andExpect(jsonPath("$.major.y1s2[0].takenCount").value(10))
                .andExpect(jsonPath("$.major.y1s2[0].delta").value(0))

                // liberal.faithWorldview[0]
                .andExpect(jsonPath("$.liberal.faithWorldview[0].courseName").value("채플"));
    }
}
