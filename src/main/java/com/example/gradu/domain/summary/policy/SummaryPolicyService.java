package com.example.gradu.domain.summary.policy;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SummaryPolicyService {
    public SummaryPolicy getActivePolicyFor() {
        return SummaryPolicy.builder()
                .pfRatioMax(0.30)              // "이하" 규정
                .pfMinTotalForLimit(130)       // PF 한도 산정 최소 총학점
                .totalCreditsMin(130)
                .gpaMin(0.0)                   // GPA 미사용이면 0
                .engMajorMinA(21).engLiberalMinA(9)
                .engMajorMinB(24).engLiberalMinB(6)
                .majorDesignedRequired(12)
                .required(Map.of(
                        "FAITH_WORLDVIEW", 9,
                        "PERSONALITY_LEADERSHIP", 6,
                        "PRACTICAL_ENGLISH", 9,
                        "GENERAL_EDU", 5,
                        "BSM", 18,
                        "ICT_INTRO", 2,
                        "FREE_ELECTIVE_BASIC", 9,
                        "FREE_ELECTIVE_MJR", 0,
                        "MAJOR", 60
                ))
                .build();
    }
}