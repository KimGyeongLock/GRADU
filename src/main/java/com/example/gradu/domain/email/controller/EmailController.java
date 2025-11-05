package com.example.gradu.domain.email.controller;

import com.example.gradu.domain.email.dto.EmailRequestDto;
import com.example.gradu.domain.email.dto.VerificationResponseDto;
import com.example.gradu.domain.email.dto.VerifyRequestDto;
import com.example.gradu.domain.email.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/email/otp")
public class EmailController {
    private final EmailVerificationService service;

    @PostMapping("/send")
    public ResponseEntity<Void> send(@Valid @RequestBody EmailRequestDto req) {
        service.sendCode(req.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<VerificationResponseDto> verify(@Valid @RequestBody VerifyRequestDto req) {
        boolean ok = service.verifyCode(req.getEmail(), req.getCode());
        return ResponseEntity.ok(new VerificationResponseDto(ok));
    }
}