package com.example.gradu.domain.curriculum.dto;

import com.example.gradu.domain.curriculum.entity.Curriculum;

public record CurriculumResponse(
        String category,
        int earnedCredits,
        String status
) {
    public static CurriculumResponse from(Curriculum c){
        return new CurriculumResponse(
                c.getCategory().name(),
                c.getEarnedCredits(),
                c.getStatus().name()
        );
    }
}
