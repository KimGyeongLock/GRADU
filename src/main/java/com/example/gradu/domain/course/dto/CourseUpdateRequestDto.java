package com.example.gradu.domain.course.dto;

import com.example.gradu.domain.curriculum.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseUpdateRequestDto {
    private String name;
    private Integer credit;
    private Integer designedCredit;
    private String grade;
    private Category category;
}
