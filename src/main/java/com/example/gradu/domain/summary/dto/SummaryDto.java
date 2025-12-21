package com.example.gradu.domain.summary.dto;

import java.util.List;

public record SummaryDto(
        List<SummaryRowDto> rows,
        double pfCredits,
        double pfLimit,
        boolean pfPass,
        double totalCredits,
        boolean totalPass,
        double gpa,
        int engMajorCredits,
        int engLiberalCredits,
        boolean englishPass,
        boolean gradEnglishPassed,
        boolean deptExtraPassed,
        boolean finalPass
) {
    public SummaryDto {
        rows = (rows == null) ? List.of() : List.copyOf(rows);
    }
}
