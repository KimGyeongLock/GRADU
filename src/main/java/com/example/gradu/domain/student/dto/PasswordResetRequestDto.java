package com.example.gradu.domain.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;


public record PasswordResetRequestDto(
        @Email(message = "유효한 이메일 주소여야 합니다.") String email,
        @NotBlank String code,
        @NotBlank(message = "비밀번호는 필수 입력값입니다")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
        message = "비밀번호는 8자 이상이며, 최소 하나의 문자, 숫자, 특수문자를 포함해야 합니다")
        String newPassword
) { }
