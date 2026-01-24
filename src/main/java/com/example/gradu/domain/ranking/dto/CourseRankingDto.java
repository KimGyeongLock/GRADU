package com.example.gradu.domain.ranking.dto;

import java.util.List;

public class CourseRankingDto {

    public record RankingItem(
        int rank,
        String courseName,
        long takenCount,
        int delta
    ) {}

    public record RankingResponse(
        List<RankingItem> major,
        List<RankingItem> liberal
    ) {}
}
