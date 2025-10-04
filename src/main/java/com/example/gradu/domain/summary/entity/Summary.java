package com.example.gradu.domain.summary.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "summary", indexes = {
        @Index(name = "idx_student_id", columnList = "student_id", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Summary {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String studentId;

    // P/F
    private double pfCredits;
    private double pfLimit;
    private boolean pfPass;

    // 총학점
    private double totalCredits;
    private boolean totalPass;

    // GPA (4.5 환산)
    private double gpa;

    // 영어강의 합계
    private int engMajorCredits;
    private int engLiberalCredits;
    private boolean englishPass;

    // 사용자 토글
    private boolean gradEnglishPassed;
    private boolean deptExtraPassed;

    // 최종 판단
    private boolean finalPass;

    // ✅ MySQL JSON 컬럼이면 @Lob 불필요. JSON 유효성 체크 장점.
    @Column(columnDefinition = "JSON")
    private String rowsJson;

    // ===== 도메인 메서드들 =====

    /** 요약 계산 결과를 한 번에 반영 */
    public void applyCalc(
            double pfCredits, double pfLimit, boolean pfPass,
            double totalCredits, boolean totalPass,
            double gpa,
            int engMajorCredits, int engLiberalCredits, boolean englishPass,
            boolean gradEnglishPassed, boolean deptExtraPassed, boolean finalPass,
            String rowsJson
    ) {
        this.pfCredits = pfCredits;
        this.pfLimit = pfLimit;
        this.pfPass = pfPass;

        this.totalCredits = totalCredits;
        this.totalPass = totalPass;

        this.gpa = gpa;

        this.engMajorCredits = engMajorCredits;
        this.engLiberalCredits = engLiberalCredits;
        this.englishPass = englishPass;

        this.gradEnglishPassed = gradEnglishPassed;
        this.deptExtraPassed = deptExtraPassed;

        this.finalPass = finalPass;

        this.rowsJson = rowsJson;
    }

    /** 토글만 갱신 */
    public void updateToggles(boolean gradEnglishPassed, boolean deptExtraPassed) {
        this.gradEnglishPassed = gradEnglishPassed;
        this.deptExtraPassed = deptExtraPassed;
    }

    /** 팩토리: sid로 빈 스냅샷 생성 */
    public static Summary ofStudent(String sid) {
        return Summary.builder().studentId(sid).build();
    }
}
