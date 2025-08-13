package com.example.gradu.domain.curriculum.entity;

import lombok.Getter;

@Getter
public enum Category {
    FAITH_WORLDVIEW(9),       // 신앙및세계관
    PERSONALITY_LEADERSHIP(6),// 인성및리더십
    PRACTICAL_ENGLISH(9),     // 실무영어
    GENERAL_EDU(5),           // 전문교양
    BSM(18),                  // BSM
    ICT_INTRO(2),             // ICT융합기초
    FREE_ELECTIVE_BASIC(9),   // 자유선택(교양)
    FREE_ELECTIVE_MJR(0),     // 자유선택(교양또는비교양)
    MAJOR(60),                // 전공주제
    MAJOR_DESIGNED(12);       // 설계

    private final int requiredCredits;
    Category(int requiredCredits) {
        this.requiredCredits = requiredCredits;
    }
}
