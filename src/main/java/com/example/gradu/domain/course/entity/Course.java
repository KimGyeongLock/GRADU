package com.example.gradu.domain.course.entity;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "course")
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

    @Column(nullable = false)
    private int credit;

    @Column
    private int designedCredits;

    @Column(length = 5)
    private String grade;
}
