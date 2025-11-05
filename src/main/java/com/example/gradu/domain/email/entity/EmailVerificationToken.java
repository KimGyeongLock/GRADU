package com.example.gradu.domain.email.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_otp", indexes = {
        @Index(name="idx_email", columnList="email"),
        @Index(name="idx_expires_at", columnList="expiresAt")
})
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailVerificationToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=120)
    private String email;

    @Column(nullable=false, length=88)
    private String codeHash;

    @Column(nullable=false)
    private LocalDateTime expiresAt;

    @Column(nullable=false)
    private boolean used;

    public boolean isExpired() { return LocalDateTime.now().isAfter(expiresAt); }
    public void markUsed() { this.used = true; }
}
