package com.example.gradu.domain.email.repository;

import com.example.gradu.domain.email.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findTopByEmailOrderByIdDesc(String email);
    void deleteByEmail(String email);
}
