package com.example.gradu.domain.ranking.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class CourseRankingWarmupServiceTest {

    @Test
    void warmup_callsRebuildOnce() {
        CourseRankingRebuildBatch rebuildBatch = mock(CourseRankingRebuildBatch.class);
        CourseRankingWarmupService warmupService = new CourseRankingWarmupService(rebuildBatch);

        warmupService.warmup();

        verify(rebuildBatch, times(1)).rebuild();
    }
}
