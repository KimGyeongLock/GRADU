package com.example.gradu.domain.summary.util;

import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.summary.dto.SummaryDto;
import com.example.gradu.domain.summary.dto.SummaryRowDto;
import com.example.gradu.domain.summary.policy.SummaryPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.example.gradu.domain.summary.util.RowAssembler.isPassGrade;

public class SummaryCalculator {
    private SummaryCalculator(){}

    private static final Map<String, Double> GPA = Map.ofEntries(
            Map.entry("A+", 4.5), Map.entry("A0", 4.0),
            Map.entry("B+", 3.5), Map.entry("B0", 3.0),
            Map.entry("C+", 2.5), Map.entry("C0", 2.0),
            Map.entry("D+", 1.5), Map.entry("D0", 1.0),
            Map.entry("F", 0.0)
    );
    private static final Set<String> PF = Set.of("P","PD","PASS");

    /** 0.5 단위 학점을 유닛(int, 학점×2)으로 변환: 3.0→6, 3.5→7 */
    private static int toUnits(BigDecimal credit) {
        if (credit == null) return 0;
        return credit.multiply(BigDecimal.valueOf(2)).intValue(); // 0.5 단위 전제
    }

    // “기독교 세계관”만 총합/영어/GPA 1회만 반영
    private static String normName(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "").toUpperCase();
    }
    private static final Set<String> COUNT_ONCE_NAMES_NORM =
            Set.of(normName("기독교 세계관")); // "기독교세계관"
    private static final Set<String> COUNT_ONCE_ALLOWED_CATS =
            Set.of("FAITH_WORLDVIEW", "GENERAL_EDU");

    private static boolean isCountOnceTarget(Course c) {
        return COUNT_ONCE_NAMES_NORM.contains(normName(c.getName()))
                && COUNT_ONCE_ALLOWED_CATS.contains(c.getCategory().name());
    }

    /** 등급 문자열 정규화: 공백 제거/대문자, A/B/C/D → A0/B0/C0/D0 */
    private static String normalizeGrade(String g) {
        if (g == null) return "";
        g = g.trim().toUpperCase();
        if (g.matches("^[ABCD]$")) return g + "0";
        return g;
    }

    public static SummaryDto compute(List<Course> courses, SummaryPolicy policy,
                                     boolean gradEnglishPassed) {
        int totU = 0, pfU = 0, eMajorU = 0, eLibU = 0;

        // GPA는 유닛 기준으로 누적 → 분자는 BigDecimal, 분모는 유닛 int
        BigDecimal gNumU = BigDecimal.ZERO; // Σ(u * gradePoint)
        int gDenU = 0;                      // Σ(u) (성적 있는 과목만)

        // “한 번만 총합에 반영할” 과목들(정규화된 이름) 추적
        Set<String> seenCountOnce = new HashSet<>();

        for (Course c : courses) {
            int u = toUnits(Optional.ofNullable(c.getCredit()).orElse(BigDecimal.ZERO));
            String grade = normalizeGrade(Optional.ofNullable(c.getGrade()).orElse(""));
            boolean isPF = PF.contains(grade);
            boolean isGpaGrade = GPA.containsKey(grade);

            // “기독교 세계관”이 신앙/전문교양 양쪽에 있을 때 총합은 1회만
            boolean countsForTotals = true;
            if (isCountOnceTarget(c)) {
                String key = normName(c.getName()); // "기독교세계관"
                if (!seenCountOnce.add(key)) {
                    countsForTotals = false; // 두 번째 이후는 총합/GPA/영어 제외
                }
            }

            if (countsForTotals) {
                // 총 취득
                totU += u;

                // PF/GPA
                if (isPF) {
                    pfU += u;
                } else if (isGpaGrade) {
                    BigDecimal gp = BigDecimal.valueOf(GPA.get(grade)); // 4.5, 4.0, ...
                    gNumU = gNumU.add(gp.multiply(BigDecimal.valueOf(u))); // Σ(u * 점수)
                    gDenU += u;                                           // Σ(u)
                }

                // 영어강의 합(전공/교양) – 총합에서 1회만
                if (Boolean.TRUE.equals(c.getIsEnglish())) {
                    String cat = c.getCategory().name();
                    if ("MAJOR".equals(cat) || "MAJOR_DESIGNED".equals(cat)) eMajorU += u;
                    else eLibU += u;
                }
            }

        }

        // GPA = (Σ(u * 점수) / Σ(u)) 을 BigDecimal로 반올림(소수 셋째자리)
        BigDecimal gpaBD = (gDenU == 0)
                ? BigDecimal.ZERO
                : gNumU.divide(BigDecimal.valueOf(gDenU), 3, RoundingMode.HALF_UP);
        double gpa = gpaBD.doubleValue();

        // P/F 기준 및 총합 판정(유닛 비교)
        int baseU = Math.max(totU, policy.getPfMinTotalForLimit() * 2);
        int pfLimitU = (int) Math.floor(baseU * policy.getPfRatioMax());
        boolean pfPass = pfU <= pfLimitU;

        boolean totalPass = totU >= policy.getTotalCreditsMin() * 2;

        // 영어강의 패스 판정(정책이 정수 기준이므로 바닥함수)
        int engMajorCreditsD  = eMajorU / 2;
        int engLiberalCreditsD= eLibU  / 2;
        boolean englishPass = EnglishRules.check(
                policy,
                engMajorCreditsD,
                engLiberalCreditsD
        );

        // 카테고리 PASS는 RowAssembler 결과로
        List<SummaryRowDto> rows = RowAssembler.buildRows(courses, policy);
        boolean allCatPass = rows.stream().allMatch(r -> "PASS".equals(r.getStatus()));

        // 캡스톤 수강 여부 판정
        boolean cap1 = false;
        boolean cap2 = false;

        for (Course c : courses) {
            if (!isPassGrade(c.getGrade())) continue;

            String name = normName(c.getName());
            if (name.equals(normName("캡스톤디자인1")) || name.equals(normName("캡스톤디자인 1"))) {
                cap1 = true;
            }
            if (name.equals(normName("캡스톤디자인2")) || name.equals(normName("캡스톤디자인 2"))) {
                cap2 = true;
            }
        }

        boolean deptExtraPassed = cap1 && cap2;


        boolean finalPass =
                allCatPass && englishPass && pfPass && totalPass
                        && gradEnglishPassed
                        && deptExtraPassed // ← 이제 자동 계산됨
                        && gpa >= policy.getGpaMin();


        // 표시용(소수 지원)
        double pfCredits    = pfU      / 2.0;
        double pfLimit      = pfLimitU / 2.0;
        double totalCredits = totU     / 2.0;

        return SummaryDto.builder()
                .rows(rows)
                .pfCredits(pfCredits).pfLimit(pfLimit).pfPass(pfPass)
                .totalCredits(totalCredits).totalPass(totalPass)
                .gpa(gpa)
                .engMajorCredits(engMajorCreditsD).engLiberalCredits(engLiberalCreditsD).englishPass(englishPass)
                .gradEnglishPassed(gradEnglishPassed).deptExtraPassed(deptExtraPassed)
                .finalPass(finalPass)
                .build();
    }
}
