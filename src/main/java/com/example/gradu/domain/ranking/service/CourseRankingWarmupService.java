package com.example.gradu.domain.ranking.service;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.ranking.catalog.MajorRoadmapIndex;
import com.example.gradu.domain.ranking.repository.CourseRankingRedisRepository;
import com.example.gradu.domain.ranking.repository.CourseRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class CourseRankingWarmupService {
    private final CourseRankingRepository dbRepo;
    private final CourseRankingRedisRepository redisRepo;
    private final MajorRoadmapIndex roadmapIndex;
    
    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        redisRepo.clearRankingKeys();

        loadCategory(Category.MAJOR, true, 300);
        loadCategory(Category.FAITH_WORLDVIEW, false, 300);
        loadCategory(Category.GENERAL_EDU, false, 300);
        loadCategory(Category.BSM, false, 300);
        loadCategory(Category.FREE_ELECTIVE_BASIC, false, 300);
        loadCategory(Category.FREE_ELECTIVE_MJR, false, 300);

        redisRepo.markReady();
    }

    private void loadCategory(Category cat, boolean majorBucket, int size) {
        var rows = dbRepo.findTopCoursesByCategories(EnumSet.of(cat), PageRequest.of(0, size));
        for (var r : rows) {
            String norm = norm(r.getName());
            if (norm.isBlank()) continue;

            redisRepo.zincrByCategory(cat, norm, r.getTakenCount());
            redisRepo.saveDisplay(norm, r.getName());

            if (majorBucket) {
                roadmapIndex.findTermByCourseName(r.getName())
                        .ifPresent(t -> redisRepo.zincrByMajorBucket(t.toBucketKey(), norm, r.getTakenCount()));
            }
        }
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", "");
    }
}
