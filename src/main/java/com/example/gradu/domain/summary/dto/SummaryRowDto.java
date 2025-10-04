package com.example.gradu.domain.summary.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryRowDto {
    private String key;
    private String name;
    private String grad;
    private double earned;
    private Integer designedEarned;
    private String status;
}
