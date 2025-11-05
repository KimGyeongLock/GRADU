package com.example.gradu.domain.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VerifyRequestDto {
    @NotBlank @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "인증 코드는 6자리 숫자여야 합니다.")
    private String code;
}