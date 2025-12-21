package com.example.gradu.domain.summary.entity;

import com.example.gradu.domain.student.entity.Student;
import com.example.gradu.domain.summary.dto.SummaryCalcResult;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

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

    @Column(columnDefinition = "JSON")
    private String rowsJson;

    // ===== 도메인 메서드들 =====

    /** 요약 계산 결과를 한 번에 반영 */
    public void applyCalc(SummaryCalcResult r) {
        this.pfCredits = r.pfCredits();
        this.pfLimit = r.pfLimit();
        this.pfPass = r.pfPass();
        this.totalCredits = r.totalCredits();
        this.totalPass = r.totalPass();
        this.gpa = r.gpa();
        this.engMajorCredits = r.engMajorCredits();
        this.engLiberalCredits = r.engLiberalCredits();
        this.englishPass = r.englishPass();
        this.gradEnglishPassed = r.gradEnglishPassed();
        this.deptExtraPassed = r.deptExtraPassed();
        this.finalPass = r.finalPass();
        this.rowsJson = r.rowsJson();
    }


    /** 토글만 갱신 */
    public void updateToggles(boolean gradEnglishPassed) {
        this.gradEnglishPassed = gradEnglishPassed;
    }

    /** 팩토리: sid로 빈 스냅샷 생성 */
    public static Summary ofStudent(Student student) {
        return Summary.builder()
                .student(student)
                .pfCredits(0)
                .pfLimit(0)
                .pfPass(false)
                .totalCredits(0)
                .totalPass(false)
                .gpa(0)
                .engMajorCredits(0)
                .engLiberalCredits(0)
                .englishPass(false)
                .gradEnglishPassed(false)
                .deptExtraPassed(false)
                .finalPass(false)
                .rowsJson("[]")
                .build();
    }
}
