package com.example.gradu.domain.summary.util;

import com.example.gradu.domain.summary.policy.SummaryPolicy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EnglishRulesTest {

    private SummaryPolicy originalPolicy() {
        return SummaryPolicy.builder()
                .engMajorMinA(24).engLiberalMinA(6)
                .engMajorMinB(21).engLiberalMinB(9)
                .pfRatioMax(0.25).pfMinTotalForLimit(12).totalCreditsMin(0).gpaMin(0.0)
                .required(Map.of()).majorDesignedRequired(0)
                .build();
    }

    @Test
    void check_shouldPass_whenTrackA_satisfied() {
        // given: A트랙 기준 (전공 24, 교양 6)
        SummaryPolicy policy = originalPolicy();

        // when
        boolean ok = EnglishRules.check(policy, 24, 6);

        // then
        assertThat(ok).isTrue();
    }

    @Test
    void check_shouldFail_whenTrackA_notSatisfied() {
        // given: A트랙 기준 (전공 24, 교양 6)
        SummaryPolicy policy = originalPolicy();

        // when
        boolean ok = EnglishRules.check(policy, 24, 5);

        // then
        assertThat(ok).isFalse();
    }

    @Test
    void check_shouldPass_whenTrackB_satisfied_evenIfTrackA_fails() {
        // given: B트랙 기준 (전공 21, 교양 9)
        SummaryPolicy policy = originalPolicy();

        // when
        boolean ok = EnglishRules.check(policy, 21, 9);

        // then
        assertThat(ok).isTrue();
    }
}
