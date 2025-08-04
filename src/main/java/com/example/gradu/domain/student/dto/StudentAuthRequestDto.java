package com.example.gradu.domain.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class StudentAuthRequestDto {
    @NotBlank(message = "학번은 필수 입력값입니다")
    @Pattern(regexp = "^[0-9]{8}$", message = "학번은 8자리 숫자여야 합니다")
    private String studentId;

    @NotBlank(message = "비밀번호는 필수 입력값입니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "비밀번호는 8자 이상이며, 최소 하나의 문자, 숫자, 특수문자를 포함해야 합니다")
    private String password;
}
