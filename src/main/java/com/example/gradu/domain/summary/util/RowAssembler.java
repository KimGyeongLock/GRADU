package com.example.gradu.domain.summary.util;

import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.course.service.CourseService;
import com.example.gradu.domain.summary.dto.SummaryRowDto;
import com.example.gradu.domain.summary.policy.SummaryPolicy;

import java.math.BigDecimal;
import java.util.*;

import static com.example.gradu.domain.course.service.CourseService.toUnits;

public final class RowAssembler {
    private RowAssembler() {}

    private static final String CAT_FAITH_WORLDVIEW = "FAITH_WORLDVIEW";
    private static final String CAT_PERSONALITY_LEADERSHIP = "PERSONALITY_LEADERSHIP";
    private static final String CAT_PRACTICAL_ENGLISH = "PRACTICAL_ENGLISH";
    private static final String CAT_GENERAL_EDU = "GENERAL_EDU";
    private static final String CAT_BSM = "BSM";
    private static final String CAT_ICT_INTRO = "ICT_INTRO";
    private static final String CAT_FREE_ELECTIVE_BASIC = "FREE_ELECTIVE_BASIC";
    private static final String CAT_FREE_ELECTIVE_MJR = "FREE_ELECTIVE_MJR";
    private static final String CAT_MAJOR = "MAJOR";

    private static final Map<String, String> KOR = Map.ofEntries(
            Map.entry(CAT_FAITH_WORLDVIEW, "신앙및세계관"),
            Map.entry(CAT_PERSONALITY_LEADERSHIP, "인성및리더십"),
            Map.entry(CAT_PRACTICAL_ENGLISH, "실무영어"),
            Map.entry(CAT_GENERAL_EDU, "전문교양"),
            Map.entry(CAT_BSM, "BSM"),
            Map.entry(CAT_ICT_INTRO, "ICT융합기초"),
            Map.entry(CAT_FREE_ELECTIVE_BASIC, "자유선택(교양)"),
            Map.entry(CAT_FREE_ELECTIVE_MJR, "자유선택(교양또는비교양)"),
            Map.entry(CAT_MAJOR, "전공")
    );

    private static final List<String> ORDER = List.of(
            CAT_FAITH_WORLDVIEW,
            CAT_PERSONALITY_LEADERSHIP,
            CAT_PRACTICAL_ENGLISH,
            CAT_GENERAL_EDU,
            CAT_BSM,
            CAT_ICT_INTRO,
            CAT_FREE_ELECTIVE_BASIC,
            CAT_FREE_ELECTIVE_MJR,
            CAT_MAJOR
    );

    /** F가 아니면 이수로 간주(P/F의 P도 포함) */
    public static boolean isPassGrade(String grade) {
        if (grade == null) return false;
        return !"F".equalsIgnoreCase(grade.trim());
    }

    public static List<SummaryRowDto> buildRows(List<Course> courses, SummaryPolicy policy) {
        Acc acc = accumulate(courses);

        List<SummaryRowDto> rows = new ArrayList<>();
        for (String key : ORDER) {
            rows.add(buildRowFor(key, acc, policy));
        }
        return rows;
    }

    /** buildRows()의 복잡도를 줄이기 위해 “집계” 로직 분리 */
    private static Acc accumulate(List<Course> courses) {
        Map<String, Integer> earnedUnits = new HashMap<>();
        int designedEarned = 0;

        for (Course c : courses) {
            if (!isPassGrade(c.getGrade())) continue;

            String cat = c.getCategory().name();
            int units = toUnits(Optional.ofNullable(c.getCredit()).orElse(BigDecimal.ZERO));
            earnedUnits.merge(cat, units, Integer::sum);

            if (CAT_MAJOR.equals(cat)) {
                designedEarned += Optional.ofNullable(c.getDesignedCredit()).orElse(0);
            }
        }

        return new Acc(earnedUnits, designedEarned);
    }

    /** buildRows()의 복잡도를 줄이기 위해 “행 생성” 로직 분리 */
    private static SummaryRowDto buildRowFor(String key, Acc acc, SummaryPolicy policy) {
        if (CAT_MAJOR.equals(key)) {
            return buildMajorRow(acc, policy);
        }
        return buildCommonRow(key, acc, policy);
    }

    private static SummaryRowDto buildMajorRow(Acc acc, SummaryPolicy policy) {
        double earned = acc.earnedUnits().getOrDefault(CAT_MAJOR, 0) / 2.0;
        int reqMajor = policy.getRequired().getOrDefault(CAT_MAJOR, 0);
        int reqDesigned = policy.getMajorDesignedRequired();

        boolean pass = earned >= reqMajor && acc.designedEarned() >= reqDesigned;

        return SummaryRowDto.builder()
                .key(CAT_MAJOR)
                .name(KOR.getOrDefault(CAT_MAJOR, "전공"))
                .grad(reqMajor + "(" + reqDesigned + ")")
                .earned(earned)
                .designedEarned(acc.designedEarned())
                .status(pass ? "PASS" : "FAIL")
                .build();
    }

    private static SummaryRowDto buildCommonRow(String key, Acc acc, SummaryPolicy policy) {
        double earned = acc.earnedUnits().getOrDefault(key, 0) / 2.0;
        int req = policy.getRequired().getOrDefault(key, 0);

        return SummaryRowDto.builder()
                .key(key)
                .name(KOR.getOrDefault(key, key))
                .grad(String.valueOf(req))
                .earned(earned)
                .status(earned >= req ? "PASS" : "FAIL")
                .build();
    }

    private record Acc(Map<String, Integer> earnedUnits, int designedEarned) {}
}
