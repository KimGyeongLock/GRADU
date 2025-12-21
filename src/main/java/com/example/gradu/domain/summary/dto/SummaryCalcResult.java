package com.example.gradu.domain.summary.dto;

public record SummaryCalcResult(
        double pfCredits, double pfLimit, boolean pfPass,
        double totalCredits, boolean totalPass,
        double gpa,
        int engMajorCredits, int engLiberalCredits, boolean englishPass,
        boolean gradEnglishPassed, boolean deptExtraPassed, boolean finalPass,
        String rowsJson
) {}
