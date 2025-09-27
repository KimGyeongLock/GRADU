package com.example.gradu.domain.course.entity;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

    // 소유자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Category category;

    @Column(nullable = false)
    private int credit;

    @Column
    private Integer designedCredit; // null 가능성이 있으면 Integer가 안전

    @Column(length = 5)
    private String grade;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** 의도 기반 변경 메서드들 */
    public void rename(String newName) {
        if (newName != null && !newName.isBlank()) this.name = newName;
    }

    /** 학점 변경 시 차액을 반환해 Curriclum 누적에 반영하게 함 */
    public int changeCredit(Integer newCredit) {
        if (newCredit == null) return 0;
        int old = this.credit;
        this.credit = newCredit;
        return newCredit - old; // 차액
    }

    public void changeDesignedCredit(Integer newDesignedCredit) {
        if (newDesignedCredit != null) this.designedCredit = newDesignedCredit;
    }

    public void changeGrade(String newGrade) {
        if (newGrade != null) this.grade = newGrade;
    }

    /** 카테고리 변경 시, 외부에서 커리큘럼 이동 처리 필요 */
    public Category changeCategory(Category newCategory) {
        if (newCategory == null || newCategory == this.category) return this.category;
        Category old = this.category;
        this.category = newCategory;
        return old; // 이전 카테고리 반환
    }
}
