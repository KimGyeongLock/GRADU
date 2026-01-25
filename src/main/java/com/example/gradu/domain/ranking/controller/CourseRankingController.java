package com.example.gradu.domain.ranking.controller;

import com.example.gradu.domain.ranking.service.CourseRankingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.gradu.domain.ranking.dto.CourseRankingDto.*;

@RestController
@RequestMapping("/api/v1/rankings")
public class CourseRankingController {

    private final CourseRankingService rankingService;

    public CourseRankingController(CourseRankingService rankingService) {
        this.rankingService = rankingService;
    }

    @GetMapping("/courses")
    public RankingResponse courseRanking() {
        return rankingService.getCourseRanking();
    }
}
