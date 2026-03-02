package com.example.gradu.domain.ranking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseRankingWarmupService {
    private final CourseRankingRebuildBatch rebuildBatch;
    
    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        rebuildBatch.rebuild();
    }
}
