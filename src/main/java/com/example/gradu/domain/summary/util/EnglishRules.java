package com.example.gradu.domain.summary.util;

import com.example.gradu.domain.summary.policy.SummaryPolicy;

public final class EnglishRules {
    private EnglishRules() {}
    public static boolean check(SummaryPolicy p, double major, double liberal) {
        boolean caseA = major >= p.getEngMajorMinA() && liberal >= p.getEngLiberalMinA();
        boolean caseB = major >= p.getEngMajorMinB() && liberal >= p.getEngLiberalMinB();
        return caseA || caseB;
    }
}
