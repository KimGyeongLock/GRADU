package com.example.gradu.domain.course.dto;

import com.example.gradu.domain.course.entity.Course;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Getter @AllArgsConstructor @Builder
public class CourseResponseDto {
    private final Long id;
    private final String name;
    private final String category;
    private final BigDecimal credit;      // 0.5 단위 그대로 노출
    private final Integer designedCredit; // 정수
    private final String grade;
    @JsonProperty("isEnglish")
    private final Boolean isEnglish;
    Short academicYear;
    String term;
    String displaySemester;

    public static CourseResponseDto from(Course c) {
        return CourseResponseDto.builder()
                .id(c.getId())
                .name(c.getName())
                .category(c.getCategory().name())
                .credit(c.getCredit())
                .designedCredit(c.getDesignedCredit())
                .grade(c.getGrade())
                .isEnglish(c.getIsEnglish())
                .academicYear(c.getAcademicYear())
                .term(c.getTerm().getCode())
                .displaySemester(c.getDisplaySemester())
                .build();
    }
}
