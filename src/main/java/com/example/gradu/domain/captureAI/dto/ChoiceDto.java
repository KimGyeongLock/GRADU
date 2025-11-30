package com.example.gradu.domain.captureAI.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChoiceDto {

    private MessageDto message;
}