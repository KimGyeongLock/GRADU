package com.example.gradu.domain.student.entity;

import com.example.gradu.global.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String password;

//    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "email_hash", nullable = false, unique = true)
    private String emailHash;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
    public void changeEmail(String newEmail, String emailHash) { this.email = newEmail; this.emailHash = emailHash; }
}
