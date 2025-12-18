package com.example.gradu.domain.capture_ai.dto;

// OpenAiResponseDto.java
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiResponseDto {

    private List<ChoiceDto> choices;
}