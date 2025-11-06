package com.example.gradu.domain.course.dto;

import com.example.gradu.domain.curriculum.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseUpdateRequestDto {
    private String name;
    private BigDecimal credit;
    private Integer designedCredit;
    private String grade;
    private Category category;
    private boolean isEnglish;
    Short academicYear;
    String term;
}
