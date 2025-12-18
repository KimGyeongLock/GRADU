package com.example.gradu.domain.course.controller;

import com.example.gradu.domain.course.service.CourseCommandService;
import com.example.gradu.domain.course.service.CourseService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CourseControllerTest {

    @Test
    void sanity() {
        CourseService courseService = mock(CourseService.class);
        CourseCommandService courseCommandService = mock(CourseCommandService.class);
        CourseController controller = new CourseController(courseService, courseCommandService);

        assertNotNull(controller);
    }
}
