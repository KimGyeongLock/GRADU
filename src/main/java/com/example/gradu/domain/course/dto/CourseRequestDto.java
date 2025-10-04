package com.example.gradu.domain.course.dto;

import com.example.gradu.domain.curriculum.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CourseRequestDto(
    @NotBlank(message = "과목명은 필수입니다.") String name,
    @Positive(message = "학점은 양수여야 합니다.") BigDecimal credit,
    @NotNull(message = "카테고리는 필수입니다.") Category category,
    @Positive(message = "설계 학점은 양수여야 합니다.") int designedCredit,
    boolean isEnglish,
    String grade
) {}
