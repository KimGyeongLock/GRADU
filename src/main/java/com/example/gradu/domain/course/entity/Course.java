package com.example.gradu.domain.course.entity;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course")
@EntityListeners(AuditingEntityListener.class)
public class Course {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Category category;

    // ✅ 0.5 단위 허용
    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal credit;

    // ✅ 설계학점은 정수 유지
    @Column
    private Integer designedCredit;

    @Column(length = 5)
    private String grade;

    @Builder.Default
    @Column(name = "is_english", nullable = false)
    private Boolean isEnglish = false;

    /** 예: 2025 (표시는 25로 가공) */
    @Column(nullable = false)
    private Short academicYear;

    /** FIRST/SECOND/SUMMER/WINTER 저장 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Term term;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ===== 도메인 변경 메서드 =====
    public void rename(String newName) {
        if (newName != null && !newName.isBlank()) this.name = newName;
    }

    public BigDecimal changeCredit(BigDecimal newCredit) {
        if (newCredit == null) return BigDecimal.ZERO;
        BigDecimal old = this.credit;
        this.credit = newCredit;
        return newCredit.subtract(old);
    }

    public void changeDesignedCredit(Integer newDesignedCredit) {
        this.designedCredit = newDesignedCredit;
    }

    public void changeGrade(String newGrade) {
        if (newGrade != null) this.grade = newGrade;
    }

    public Category changeCategory(Category newCategory) {
        if (newCategory == null || newCategory == this.category) return this.category;
        Category old = this.category;
        this.category = newCategory;
        return old;
    }

    public void changeEnglish(Boolean english) {
        this.isEnglish = (english != null && english);
    }

    public Boolean getIsEnglish() {
        return isEnglish != null ? isEnglish : false;
    }

    public String getDisplaySemester() {
        int yy = academicYear % 100; // 2025 → 25
        return yy + "-" + term.getCode(); // "25-1" / "25-sum"
    }

    public void changeSemester(Short year, Term term) {
        this.academicYear = year;
        this.term = term;
    }
}

