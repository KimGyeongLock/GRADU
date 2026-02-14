package com.example.gradu.domain.ranking.dto;

import java.util.List;

public class CourseRankingDto {

    public record RankingItemDto(
            int rank,
            String courseName,
            long takenCount,
            int delta
    ) {}

    public record LiberalRankingDto(
            List<RankingItemDto> faithWorldview,
            List<RankingItemDto> generalEdu,
            List<RankingItemDto> bsm,
            List<RankingItemDto> freeElective
    ) {}

    public record MajorRankingDto(
            List<RankingItemDto> y1s2,
            List<RankingItemDto> y2s1,
            List<RankingItemDto> y2s2,
            List<RankingItemDto> y3s1,
            List<RankingItemDto> y3s2,
            List<RankingItemDto> y4s1,
            List<RankingItemDto> y4s2
    ) {}

    public record RankingResponseDto(
            MajorRankingDto major,
            LiberalRankingDto liberal
    ) {}
}
