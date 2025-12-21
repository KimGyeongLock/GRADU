package com.example.gradu.domain.summary.util;

import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.summary.dto.SummaryRowDto;
import com.example.gradu.domain.summary.policy.SummaryPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static com.example.gradu.domain.summary.util.TestHelper.*;
import static org.assertj.core.api.Assertions.assertThat;

public class RowAssemblerTest {

    private SummaryPolicy policyWithRequired(Map<String, Integer> requiredCreditsByCategory) {
        return SummaryPolicy.builder()
                .pfRatioMax(0.25)
                .pfMinTotalForLimit(12)
                .totalCreditsMin(0)
                .gpaMin(0.0)
                .engMajorMinA(0)
                .engLiberalMinA(0)
                .engMajorMinB(0)
                .engLiberalMinB(0)
                .required(requiredCreditsByCategory)
                .majorDesignedRequired(0)
                .build();
    }

    @ParameterizedTest
    @CsvSource({
            "P, true",
            "p, true",
            "PD, true",
            "pass, true",
            "A0, true",
            "B+, true",
            "C, true",
            "F, false",
            "f, false",
            "'', true",
            "' ', true",
            "NULL, true"
    })
    void isPassGrade_blankOrNull_isTreatedAsPass(String grade, boolean expected) {
        boolean actual = RowAssembler.isPassGrade(grade);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void buildRows_requiredCategory_shouldPass_whenCreditsMeetRequirement() {
        // given: FAITH_WORLDVIEW 3학점 요구
        SummaryPolicy policy = policyWithRequired(Map.of(
                "FAITH_WORLDVIEW", 3
        ));

        List<Course> courses = List.of(
                course("기독교 세계관", "A0", 3.0, false, cat("FAITH_WORLDVIEW"))
        );

        // when
        List<SummaryRowDto> rows = RowAssembler.buildRows(courses, policy);

        // then: 해당 카테고리가 PASS인지(대소문자/표기 방식은 너 DTO에 맞춰 조정)
        assertThat(rows).anySatisfy(r -> {
           assertThat(r.getKey()).isEqualTo("FAITH_WORLDVIEW");
           assertThat(r.getStatus()).isEqualTo("PASS");
        });
    }

    @Test
    void buildRows_requiredCategory_shouldFail_whenCreditsBelowRequirement() {
        // given: GENERAL_EDU 6학점 요구
        SummaryPolicy policy = policyWithRequired(Map.of(
                "GENERAL_EDU", 6
        ));

        // 3학점만 채움 => FAIL이어야 함
        List<Course> courses = List.of(
                course("아무교양", "A0", 3.0, false, cat("GENERAL_EDU"))
        );

        // when
        List<SummaryRowDto> rows = RowAssembler.buildRows(courses, policy);

        // then
        assertThat(rows).anySatisfy(r -> {
            assertThat(r.getKey()).isEqualTo("GENERAL_EDU");
            assertThat(r.getStatus()).isEqualTo("FAIL");
        });
    }
}
