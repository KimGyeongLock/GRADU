package com.example.gradu.domain.summary.util;

import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.summary.dto.SummaryRowDto;
import com.example.gradu.domain.summary.policy.SummaryPolicy;

import java.math.BigDecimal;
import java.util.*;

public final class RowAssembler {
    private RowAssembler() {}

    private static final Map<String, String> KOR = Map.ofEntries(
            Map.entry("FAITH_WORLDVIEW", "신앙및세계관"),
            Map.entry("PERSONALITY_LEADERSHIP", "인성및리더십"),
            Map.entry("PRACTICAL_ENGLISH", "실무영어"),
            Map.entry("GENERAL_EDU", "전문교양"),
            Map.entry("BSM", "BSM"),
            Map.entry("ICT_INTRO", "ICT융합기초"),
            Map.entry("FREE_ELECTIVE_BASIC", "자유선택(교양)"),
            Map.entry("FREE_ELECTIVE_MJR", "자유선택(교양또는비교양)"),
            Map.entry("MAJOR", "전공")
            // "MAJOR_DESIGNED"는 별도 행으로 표시하지 않고 전공 행의 설계학점으로 합산
    );

    private static final List<String> ORDER = List.of(
            "FAITH_WORLDVIEW","PERSONALITY_LEADERSHIP","PRACTICAL_ENGLISH",
            "GENERAL_EDU","BSM","ICT_INTRO",
            "FREE_ELECTIVE_BASIC","FREE_ELECTIVE_MJR","MAJOR"
    );

    /** F가 아니면 이수로 간주(P/F의 P도 포함) */
    private static boolean isPassGrade(String grade) {
        if (grade == null) return false;
        return !"F".equalsIgnoreCase(grade.trim());
    }

    /** 0.5 단위 학점을 유닛(int, 학점×2)으로 변환 */
    private static int toUnits(BigDecimal credit) {
        if (credit == null) return 0;
        return credit.multiply(BigDecimal.valueOf(2)).intValue(); // 0.5 단위 전제
    }

    public static List<SummaryRowDto> buildRows(List<Course> courses, SummaryPolicy policy) {
        // 카테고리별 취득 "유닛" 합계(학점×2) – 정밀한 경계 처리
        Map<String, Integer> earnedUnits = new HashMap<>();
        // 전공 설계학점 합계(정수 학점)
        int designedEarned = 0;

        for (Course c : courses) {
            if (!isPassGrade(c.getGrade())) continue;

            String cat = c.getCategory().name();
            int units = toUnits(Optional.ofNullable(c.getCredit()).orElse(BigDecimal.ZERO));
            earnedUnits.merge(cat, units, Integer::sum);

            // 설계학점은 "전공 과목의 designedCredit" 합으로 계산
            if ("MAJOR".equals(cat)) {
                int d = Optional.ofNullable(c.getDesignedCredit()).orElse(0);
                designedEarned += d;
            }
        }

        List<SummaryRowDto> rows = new ArrayList<>();

        for (String key : ORDER) {
            if ("MAJOR".equals(key)) {
                double eMajor = earnedUnits.getOrDefault("MAJOR", 0) / 2.0; // ✅ /2.0
                int reqMajor = policy.getRequired().getOrDefault("MAJOR", 0);
                int reqDesigned = policy.getMajorDesignedRequired();

                boolean majorPass = eMajor >= reqMajor;
                boolean designedPass = designedEarned >= reqDesigned;

                rows.add(SummaryRowDto.builder()
                        .key("MAJOR")
                        .name(KOR.getOrDefault("MAJOR", "전공"))
                        .grad(reqMajor + "(" + reqDesigned + ")")
                        .earned(eMajor)                         // ✅ double로 세팅
                        .designedEarned(designedEarned)        // 설계는 정수 유지
                        .status((majorPass && designedPass) ? "PASS" : "FAIL")
                        .build());

            } else {
                double earned = earnedUnits.getOrDefault(key, 0) / 2.0; // ✅ /2.0
                int req = policy.getRequired().getOrDefault(key, 0);

                rows.add(SummaryRowDto.builder()
                        .key(key)
                        .name(KOR.getOrDefault(key, key))
                        .grad(String.valueOf(req))
                        .earned(earned)                        // ✅ double
                        .status(earned >= req ? "PASS" : "FAIL")
                        .build());
            }
        }

        return rows;
    }
}
