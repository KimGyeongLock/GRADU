package com.example.gradu.domain.email.controller;

import com.example.gradu.domain.email.service.EmailVerificationService;
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
    public ResponseEntity<?> send(@RequestBody EmailReq req) {
        service.sendCode(req.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyReq req) {
        boolean ok = service.verifyCode(req.getEmail(), req.getCode());
        return ResponseEntity.ok(new Result(ok));
    }

    @Data
    static class EmailReq { private String email; }
    @Data static class VerifyReq { private String email; private String code; }
    @Data static class Result { private final boolean ok; }
}
