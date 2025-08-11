package com.example.gradu.flow;

import com.example.gradu.domain.course.dto.CourseRequest;
import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.student.dto.StudentAuthRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class StudentCurriculumFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper om;

    private String regJson(String studentId, String password) throws Exception {
        return om.writeValueAsString(new StudentAuthRequestDto(studentId, password));
    }
    private String courseJson(String name, int credit, Category category, Integer design, String grade) throws Exception {
        return om.writeValueAsString(new CourseRequest(name, credit, category, design, grade));
    }

    @Test
    void 초기화_확인_회원가입_후_커리큘럼_전부_0() throws Exception {
        String sid = "21900001";
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType("application/json")
                .content(regJson(sid, "password123!")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/students/{sid}/curriculum", sid)
                .accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[*].earnedCredits", everyItem(is(0))));
    }

    @Test
    void 과목_추가_후_해당_카테고리_누적증가() throws Exception {
        String sid = "21900002";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(regJson(sid, "password123!")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/students/{sid}/courses", sid)
                        .contentType("application/json")
                        .content(courseJson("컴퓨터그래픽스", 60, Category.MAJOR, 1, "A+")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/students/{sid}/curriculum", sid)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[?(@.category=='MAJOR')].earnedCredits", contains(60)))
                .andExpect(jsonPath("$[?(@.category=='MAJOR')].status", hasItem("PASS")));
    }

}
