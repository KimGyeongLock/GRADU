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
        for (Term t : values()) if (t.code.equalsIgnoreCase(code)) return t;
        throw new IllegalArgumentException("Unknown term code: " + code);
    }

    /** 직렬화/표시용 코드 */
    @JsonValue
    public String getCode() { return code; }
}
