package com.example.gradu.domain.curriculum.dto;

import com.example.gradu.domain.curriculum.entity.Curriculum;

public record CurriculumResponseDto(
        String category,
        int earnedCredits,
        String status
) {
    public static CurriculumResponseDto from(Curriculum c){
        return new CurriculumResponseDto(
                c.getCategory().name(),
                c.getEarnedCredits(),
                c.getStatus().name()
        );
    }
}
