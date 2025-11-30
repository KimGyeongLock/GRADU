package com.example.gradu.domain.course.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Term {
    FIRST("1"),   // 1학기
    SECOND("2"),  // 2학기
    SUMMER("sum"),
    WINTER("win");

    private final String code;
    Term(String code) { this.code = code; }

    /** "1" / "2" / "sum" / "win" → Term */
    public static Term fromCode(String code) {
        return switch (code.toLowerCase()) {
            case "1" -> FIRST;
            case "2" -> SECOND;
            case "3", "sum" -> SUMMER;
            case "4", "win" -> WINTER;
            default -> throw new IllegalArgumentException("Unknown term code: " + code);
        };

    }

    /** 직렬화/표시용 코드 */
    @JsonValue
    public String getCode() { return code; }
}
