package com.example.gradu.domain.summary.util;

import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.summary.dto.SummaryDto;
import com.example.gradu.domain.summary.dto.SummaryRowDto;
import com.example.gradu.domain.summary.policy.SummaryPolicy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.example.gradu.domain.course.service.CourseService.toUnits;

public class SummaryCalculator {
    private SummaryCalculator() {}
    private static final String CAT_MAJOR = "MAJOR";
    private static final String CAT_PRACTICAL_ENGLISH = "PRACTICAL_ENGLISH";

    private static final Map<String, Double> GPA = Map.ofEntries(
            Map.entry("A+", 4.5), Map.entry("A0", 4.0),
            Map.entry("B+", 3.5), Map.entry("B0", 3.0),
            Map.entry("C+", 2.5), Map.entry("C0", 2.0),
            Map.entry("D+", 1.5), Map.entry("D0", 1.0),
            Map.entry("F", 0.0)
    );
    private static final Set<String> PF = Set.of("P","PD","PASS");

    private static String normName(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "").toUpperCase();
    }

    private static String normalizeGrade(String g) {
        if (g == null) return "";
        String t = g.trim().toUpperCase();
        return t.matches("^[ABCD]$") ? (t + "0") : t;
    }

    // --------- Agg DTO ----------
    private static class Agg {
        int totU;
        int pfU;
        int eMajorU;
        int eLibU;
        BigDecimal gNumU = BigDecimal.ZERO;
        int gDenU;
    }

    public static SummaryDto compute(List<Course> courses, SummaryPolicy policy, boolean gradEnglishPassed) {
        Agg agg = aggregateCourses(courses);
        double gpa = computeGpa(agg);

        int baseU = Math.max(agg.totU, policy.getPfMinTotalForLimit() * 2);
        int pfLimitU = (int) Math.floor(baseU * policy.getPfRatioMax());

        boolean pfPass = agg.pfU <= pfLimitU;
        boolean totalPass = agg.totU >= policy.getTotalCreditsMin() * 2;

        int engMajorCredits = agg.eMajorU / 2;
        int engLiberalCredits = agg.eLibU / 2;
        boolean englishPass = EnglishRules.check(policy, engMajorCredits, engLiberalCredits);

        List<SummaryRowDto> rows = RowAssembler.buildRows(courses, policy);
        boolean allCatPass = rows.stream().allMatch(r -> "PASS".equals(r.getStatus()));

        boolean deptExtraPassed = computeDeptExtraPassed(courses);

        boolean finalPass = allCatPass
                && englishPass
                && pfPass
                && totalPass
                && gradEnglishPassed
                && deptExtraPassed
                && gpa >= policy.getGpaMin();

        return SummaryDto.builder()
                .rows(rows)
                .pfCredits(agg.pfU / 2.0)
                .pfLimit(pfLimitU / 2.0)
                .pfPass(pfPass)
                .totalCredits(agg.totU / 2.0)
                .totalPass(totalPass)
                .gpa(gpa)
                .engMajorCredits(engMajorCredits)
                .engLiberalCredits(engLiberalCredits)
                .englishPass(englishPass)
                .gradEnglishPassed(gradEnglishPassed)
                .deptExtraPassed(deptExtraPassed)
                .finalPass(finalPass)
                .build();
    }

    private static Agg aggregateCourses(List<Course> courses) {
        Agg agg = new Agg();
        Set<String> seenCountOnce = new HashSet<>();

        for (Course c : courses) {
            int u = toUnits(Optional.ofNullable(c.getCredit()).orElse(BigDecimal.ZERO));
            String grade = normalizeGrade(Optional.ofNullable(c.getGrade()).orElse(""));
            boolean isPF = PF.contains(grade);
            boolean isGpaGrade = GPA.containsKey(grade);

            boolean countsForTotals = isCountsForTotals(c, seenCountOnce);
            if (!countsForTotals) continue;

            agg.totU += u;
            applyPfAndGpa(agg, u, grade, isPF, isGpaGrade);
            applyEnglishCredits(agg, c, u);
        }

        return agg;
    }

    private static boolean isCountsForTotals(Course c, Set<String> seenCountOnce) {
        if (!isCountOnceTarget(c)) return true;
        return seenCountOnce.add(normName(c.getName()));
    }

    private static void applyPfAndGpa(Agg agg, int u, String grade, boolean isPF, boolean isGpaGrade) {
        if (isPF) {
            agg.pfU += u;
            return;
        }
        if (isGpaGrade) {
            BigDecimal gp = BigDecimal.valueOf(GPA.get(grade));
            agg.gNumU = agg.gNumU.add(gp.multiply(BigDecimal.valueOf(u)));
            agg.gDenU += u;
        }
    }

    private static void applyEnglishCredits(Agg agg, Course c, int u) {
        if (!Boolean.TRUE.equals(c.getIsEnglish())) return;

        String cat = c.getCategory().name();
        if (CAT_MAJOR.equals(cat)) {
            agg.eMajorU += u;
            return;
        }
        if (!CAT_PRACTICAL_ENGLISH.equals(cat)) {
            agg.eLibU += u;
        }
    }

    private static double computeGpa(Agg agg) {
        if (agg.gDenU == 0) return 0.0;
        return agg.gNumU
                .divide(BigDecimal.valueOf(agg.gDenU), 3, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static boolean computeDeptExtraPassed(List<Course> courses) {
        boolean cap1 = false;
        boolean cap2 = false;
        String cap1a = normName("캡스톤디자인1");
        String cap1b = normName("캡스톤디자인 1");
        String cap2a = normName("캡스톤디자인2");
        String cap2b = normName("캡스톤디자인 2");

        for (Course c : courses) {
            if (!RowAssembler.isPassGrade(c.getGrade())) continue;
            String name = normName(c.getName());
            if (name.equals(cap1a) || name.equals(cap1b)) cap1 = true;
            if (name.equals(cap2a) || name.equals(cap2b)) cap2 = true;
            if (cap1 && cap2) return true;
        }
        return false;
    }

    private static final Set<String> COUNT_ONCE_NAMES_NORM = Set.of(normName("기독교 세계관"));
    private static final Set<String> COUNT_ONCE_ALLOWED_CATS = Set.of("FAITH_WORLDVIEW", "GENERAL_EDU");

    private static boolean isCountOnceTarget(Course c) {
        return COUNT_ONCE_NAMES_NORM.contains(normName(c.getName()))
                && COUNT_ONCE_ALLOWED_CATS.contains(c.getCategory().name());
    }

}
