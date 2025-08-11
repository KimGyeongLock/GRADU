package com.example.gradu.domain.course.dto;

import com.example.gradu.domain.curriculum.entity.Category;

public record CourseRequest(
   String name,
   int credit,
   Category category,
   int designedCredit,
   String grade
) {}
