package com.example.gradu.domain.ranking.dto;

import java.util.List;

public class CourseRankingDto {

    public record RankingItem(
            int rank,
            String courseName,
            long takenCount,
            int delta
    ) {}

    public record LiberalRanking(
            List<RankingItem> faithWorldview,
            List<RankingItem> generalEdu,
            List<RankingItem> bsm,
            List<RankingItem> freeElective
    ) {}

    public record MajorRanking(
            List<RankingItem> y1s2,
            List<RankingItem> y2s1,
            List<RankingItem> y2s2,
            List<RankingItem> y3s1,
            List<RankingItem> y3s2,
            List<RankingItem> y4s1,
            List<RankingItem> y4s2
    ) {}

    public record RankingResponse(
            MajorRanking major,
            LiberalRanking liberal
    ) {}
}
