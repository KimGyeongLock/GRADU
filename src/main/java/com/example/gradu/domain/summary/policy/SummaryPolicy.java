package com.example.gradu.domain.summary.policy;

import lombok.Builder;
import lombok.Getter;
import lombok.AccessLevel;

import java.util.Map;

@Getter
@Builder
public class SummaryPolicy {

    private final double pfRatioMax;
    private final int pfMinTotalForLimit;
    private final int totalCreditsMin;
    private final double gpaMin;

    private final int engMajorMinA;
    private final int engLiberalMinA;
    private final int engMajorMinB;
    private final int engLiberalMinB;

    @Getter(AccessLevel.NONE) // ✅ Lombok이 getRequired() 생성 못 하게 막음
    private final Map<String, Integer> required;

    private final int majorDesignedRequired;

    // ✅ 우리가 직접 안전한 getter 제공
    public Map<String, Integer> getRequired() {
        return (required == null) ? Map.of() : Map.copyOf(required);
    }

    public static class SummaryPolicyBuilder {
        public SummaryPolicyBuilder required(Map<String, Integer> required) {
            this.required = (required == null) ? Map.of() : Map.copyOf(required);
            return this;
        }
    }
}
