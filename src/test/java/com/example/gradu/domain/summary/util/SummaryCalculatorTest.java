package com.example.gradu.domain.summary.util;

import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.summary.dto.SummaryDto;
import com.example.gradu.domain.summary.policy.SummaryPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.example.gradu.domain.summary.util.TestHelper.*;
import static org.assertj.core.api.Assertions.assertThat;

class SummaryCalculatorTest {

    private SummaryPolicy basePolicy() {
        return SummaryPolicy.builder()
                .pfRatioMax(0.25)          // 기본 25%
                .pfMinTotalForLimit(12)    // 최소 pf
                .totalCreditsMin(0)        // totalPass 영향 제거
                .gpaMin(0.0)               // finalPass 영향 제거
                .engMajorMinA(0)
                .engLiberalMinA(0)
                .engMajorMinB(0)
                .engLiberalMinB(0)
                .required(Map.of())        // RowAssembler 영향 최소화
                .majorDesignedRequired(0)
                .build();
    }

    @Test
    void compute_gpaDenominatorZero_returnsZeroGpa() {
        // given: PF만 있으면 GPA 분모 0
        SummaryPolicy policy = basePolicy();
        List<Course> courses = List.of(
                course("아무거나", "P", 3.0, false, cat("GENERAL_EDU"))
        );

        // when
        SummaryDto dto = SummaryCalculator.compute(courses, policy, true);

        // then
        assertThat(dto.gpa()).isEqualTo(0.0);
    }

    @Test
    void compute_pfLimit_useBaseU_maxOfTotalOrMinTotal() {
        // given:
        // totU = 12학점 => units=24
        // pfMinTotalForLimit=12 => units=24
        // baseU = 24, pfLimitU = floor(24*0.25)=6 => 3학점
        SummaryPolicy policy = basePolicy();
        List<Course> courses = List.of(
                course("일반", "A0", 9.0, false, cat("GENERAL_EDU")),
                course("패스", "P", 3.0, false, cat("GENERAL_EDU"))
        );

        // when
        SummaryDto dto = SummaryCalculator.compute(courses, policy, true);

        // then: pfLimit=3.0, pf=3.0 이므로 pass
        assertThat(dto.pfLimit()).isEqualTo(3.0);
        assertThat(dto.pfCredits()).isEqualTo(3.0);
        assertThat(dto.pfPass()).isTrue();
    }

    @Test
    void compute_englishCredits_excludesPracticalEnglish_fromLiberal() {
        // given: 영어 교양 3 + practical 3 => liberal은 3만 잡혀야 함
        SummaryPolicy policy = basePolicy();

        List<Course> courses = List.of(
                course("영어교양", "A0", 3.0, true, cat("GENERAL_EDU")),
                course("실무영어", "A0", 3.0, true, cat("PRACTICAL_ENGLISH"))
        );

        // when
        SummaryDto dto = SummaryCalculator.compute(courses, policy, true);

        // then
        assertThat(dto.engMajorCredits()).isZero();
        assertThat(dto.engLiberalCredits()).isEqualTo(3);
    }

    @Test
    void compute_countOnceTarget_christianWorldview_countsOnlyOnce() {
        // given: "기독교 세계관" 과목이 2개 들어와도 total에는 1번만 반영
        // (정확히 이 이름 + allowed category 일 때)
        SummaryPolicy policy = basePolicy();

        List<Course> courses = List.of(
                course("기독교 세계관", "A0", 3.0, false, cat("FAITH_WORLDVIEW")),
                course("기독교   세계관", "A0", 3.0, false, cat("FAITH_WORLDVIEW"))
        );

        // when
        SummaryDto dto = SummaryCalculator.compute(courses, policy, true);

        // then: 총 학점이 3학점만 반영돼야 함
        assertThat(dto.totalCredits()).isEqualTo(3.0);
    }

    @Test
    void compute_deptExtraPassed_true_when_capstone1_and_2_passed() {
        // given
        SummaryPolicy policy = basePolicy();
        List<Course> courses = List.of(
                course("캡스톤디자인1", "P", 3.0, false, cat("MAJOR")),
                course("캡스톤디자인 2", "P", 3.0, false, cat("MAJOR"))
        );

        // when
        SummaryDto dto = SummaryCalculator.compute(courses, policy, true);

        // then
        assertThat(dto.deptExtraPassed()).isTrue();
    }


}
