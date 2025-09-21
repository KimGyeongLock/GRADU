package com.example.gradu.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseUpdateRequestDto {
    private String name;
    private int credit;
    private int designedCredit;
    private String grade;
}
