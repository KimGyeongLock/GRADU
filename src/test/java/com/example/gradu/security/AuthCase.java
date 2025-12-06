package com.example.gradu.security;

import org.springframework.http.HttpMethod;

public record AuthCase (
    String name,
    String actorStudentId,
    String targetStudentId,
    HttpMethod method,
    String urlTemplate,
    int expectedStatus,
    boolean withBody
) {}
