package com.example.gradu.domain.course.controller;

import com.example.gradu.domain.course.dto.CourseRequestDto;
import com.example.gradu.domain.course.service.CourseService;
import com.example.gradu.domain.curriculum.entity.Category;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean CourseService courseService;

//    @Test
//    void addCourse_withValidRequest_returnsOk() throws Exception {
//        String studentId = "21900064";
//        CourseRequestDto req = new CourseRequestDto(
//                "컴퓨터그래픽스", 3, Category.MAJOR, 1, "A+"
//        );
//
//        mockMvc.perform(post("/api/v1/students/{studentId}/courses", studentId)
//                .contentType("application/json")
//                .content(objectMapper.writeValueAsString(req)))
//                .andExpect(status().isOk());
//
//        verify(courseService).addCourse(ArgumentMatchers.eq(studentId), ArgumentMatchers.any(CourseRequestDto.class));
//    }
}