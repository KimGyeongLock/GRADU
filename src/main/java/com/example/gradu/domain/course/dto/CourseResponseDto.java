package com.example.gradu.domain.course.dto;

import com.example.gradu.domain.course.entity.Course;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseResponseDto {
    private Long id;
    private String name;
    private String category;
    private BigDecimal credit;      // 0.5 단위 그대로 노출
    private Integer designedCredit; // 정수
    private String grade;
    @JsonProperty("isEnglish")
    private Boolean isEnglish;

    public static CourseResponseDto from(Course c) {
        return CourseResponseDto.builder()
                .id(c.getId())
                .name(c.getName())
                .category(c.getCategory().name())
                .credit(c.getCredit())
                .designedCredit(c.getDesignedCredit())
                .grade(c.getGrade())
                .isEnglish(c.getIsEnglish())
                .build();
    }
}
