package com.example.gradu.domain.captureAI.dto;

import com.example.gradu.domain.curriculum.entity.Category;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseBulkRequest {
    private String name;
    private BigDecimal credit;
    private Integer designedCredit;
    private Category category;
    private String grade;
    @JsonProperty("isEnglish")
    private boolean isEnglish;
    private Short academicYear;
    private String term;
}
