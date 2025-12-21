package com.example.gradu.domain.course.controller;

import com.example.gradu.domain.course.dto.CourseRequestDto;
import com.example.gradu.domain.course.dto.CourseUpdateRequestDto;
import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.course.entity.Term;
import com.example.gradu.domain.course.service.CourseCommandService;
import com.example.gradu.domain.course.service.CourseService;
import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.global.config.JpaAuditingConfig;
import com.example.gradu.global.security.SecurityConfig;
import com.example.gradu.global.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CourseController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JpaAuditingConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean CourseService courseService;
    @MockitoBean CourseCommandService courseCommandService;

    private Course stubCourseForResponse() {
        Course c = mock(Course.class);
        when(c.getCategory()).thenReturn(Category.MAJOR);
        when(c.getTerm()).thenReturn(Term.FIRST);
        return c;
    }

    @Test
    void addCourse_ok_overwriteDefaultFalse() throws Exception {
        // given
        long studentId = 1L;
        String body = "{}";

        // when
        mockMvc.perform(post("/api/v1/students/{studentId}/courses", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // then
        verify(courseService).addCourse(eq(studentId), any(CourseRequestDto.class), eq(false));
    }

    @Test
    void addCourse_ok_overwriteTrue() throws Exception {
        // given
        long studentId = 1L;
        String body = "{}";

        // when
        mockMvc.perform(post("/api/v1/students/{studentId}/courses?overwrite=true", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // then
        verify(courseService).addCourse(eq(studentId), any(CourseRequestDto.class), eq(true));
    }

    @Test
    void getCoursesByCategory_ok() throws Exception {
        // given
        long studentId = 1L;

        Course c1 = stubCourseForResponse();
        when(courseService.getCoursesByCategory(studentId, Category.MAJOR)).thenReturn(List.of(c1));

        // when
        mockMvc.perform(get("/api/v1/students/{studentId}/courses/categories/{category}", studentId, "MAJOR"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        // then
        verify(courseService).getCoursesByCategory(studentId, Category.MAJOR);
    }

    @Test
    void getAllCourses_ok() throws Exception {
        // given
        long studentId = 1L;

        Course c1 = stubCourseForResponse();
        when(courseService.getCoursesAll(studentId)).thenReturn(List.of(c1));

        // when
        mockMvc.perform(get("/api/v1/students/{studentId}/courses/all", studentId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        // then
        verify(courseService).getCoursesAll(studentId);
    }

    @Test
    void updateCourse_ok() throws Exception {
        // given
        long studentId = 1L;
        long courseId = 10L;

        String body = "{}";

        Course updated = stubCourseForResponse();
        when(courseCommandService.updateCourse(eq(studentId), eq(courseId), any(CourseUpdateRequestDto.class)))
                .thenReturn(updated);

        // when
        mockMvc.perform(patch("/api/v1/students/{studentId}/courses/{courseId}", studentId, courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        // then
        verify(courseCommandService).updateCourse(eq(studentId), eq(courseId), any(CourseUpdateRequestDto.class));
    }

    @Test
    void deleteCourse_noContent() throws Exception {
        // given
        long studentId = 1L;
        long courseId = 10L;

        // when
        mockMvc.perform(delete("/api/v1/students/{studentId}/courses/{courseId}", studentId, courseId))
                .andExpect(status().isNoContent());

        // then
        verify(courseCommandService).deleteCourse(studentId, courseId);
    }

    @Test
    void saveBulk_ok() throws Exception {
        // given
        long studentId = 1L;

        String body = "[]";

        // when
        mockMvc.perform(post("/api/v1/students/{studentId}/courses/bulk", studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // then
        verify(courseService).bulkInsert(eq(studentId), anyList());
    }
}
