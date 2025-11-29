package com.example.gradu.domain.captureAI.dto;

import lombok.Data;

import java.util.List;

@Data
public class CourseBulkSaveRequest {
    private String sid;
    private List<CourseBulkRequest> courses;
}