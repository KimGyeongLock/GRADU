package com.example.gradu.domain.curriculum.entity;

import com.example.gradu.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "curriculum")
public class Curriculum {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Category category;

    @Column(nullable = false)
    @Builder.Default
    private int earnedCredits = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status;

    public enum Status { PASS, FAIL }

    public void recalcStatus() {
        this.status = (earnedCredits >= category.getRequiredCredits())
                ? Status.PASS : Status.FAIL;
    }

    public void updateEarnedCredits(Integer credits) {
        this.earnedCredits += credits;
        recalcStatus();
    }
}
