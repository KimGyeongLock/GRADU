package com.example.gradu.domain.summary.policy;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter @Builder
public class SummaryPolicy {
    private final double pfRatioMax;          // 예: 0.3 (이하)
    private final int pfMinTotalForLimit;     // 한도 산정 최소총학점(예: 130)
    private final int totalCreditsMin;        // 예: 130
    private final double gpaMin;              // 필요 시 사용 (예: 2.0)

    // 영어강의 규정: (major>=A & liberal>=B) OR (major>=C & liberal>=D)
    private final int engMajorMinA;
    private final int engLiberalMinA;
    private final int engMajorMinB;
    private final int engLiberalMinB;

    // 카테고리별 요구 학점(전공설계: designedRequired 로 별도)
    private final Map<String, Integer> required;          // key: "FAITH_WORLDVIEW" ...
    private final int majorDesignedRequired;              // 전공설계 요구(예: 12)
}
