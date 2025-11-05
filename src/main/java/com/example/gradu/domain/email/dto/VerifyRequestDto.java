package com.example.gradu.domain.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VerifyRequestDto {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String code; // 6자리라면 @Pattern(regexp="\\d{6}")도 고려
}