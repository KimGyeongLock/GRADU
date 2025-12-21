package com.example.gradu.domain.summary.util;

import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.curriculum.entity.Category;

import java.math.BigDecimal;
import java.util.Objects;

public class TestHelper {
    public static Course course(String name, String grade, double credit, boolean isEnglish, Category category) {
        return Course.builder()
                .name(name)
                .grade(grade)
                .credit(BigDecimal.valueOf(credit))
                .isEnglish(isEnglish)
                .category(category)
                .build();
    }

    public static Category cat(String name) {
        return Category.valueOf(Objects.requireNonNull(name));
    }
}
