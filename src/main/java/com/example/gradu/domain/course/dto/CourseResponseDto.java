package com.example.gradu.domain.course.dto;

import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.curriculum.entity.Category;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class CourseResponseDto {
    Long id;
    String name;
    int credit;
    int designedCredit;
    Category category;
    String grade;
    LocalDateTime createdAt;

    public static CourseResponseDto from(Course c) {
        return CourseResponseDto.builder()
                .id(c.getId())
                .name(c.getName())
                .credit(c.getCredit())
                .designedCredit(c.getDesignedCredit())
                .category(c.getCategory())
                .grade(c.getGrade())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
