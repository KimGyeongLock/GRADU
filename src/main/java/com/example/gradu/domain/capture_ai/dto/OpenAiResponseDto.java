package com.example.gradu.domain.capture_ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "choices is defensively copied to an unmodifiable list via List.copyOf()"
)
public record OpenAiResponseDto(List<ChoiceDto> choices) {
    public OpenAiResponseDto {
        choices = (choices == null) ? List.of() : List.copyOf(choices);
    }
}