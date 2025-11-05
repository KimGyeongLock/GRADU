package com.example.gradu.domain.summary.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryDto {
    private List<SummaryRowDto> rows;

    private double pfCredits;
    private double pfLimit;
    private boolean pfPass;

    private double totalCredits;
    private boolean totalPass;

    private double gpa;

    private int engMajorCredits;
    private int engLiberalCredits;
    private boolean englishPass;

    private boolean gradEnglishPassed;
    private boolean deptExtraPassed;

    private boolean finalPass;
}
