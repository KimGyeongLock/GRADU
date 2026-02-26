package com.example.gradu.domain.ranking.service;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.ranking.catalog.MajorRoadmapIndex;
import com.example.gradu.domain.ranking.repository.CourseRankingRedisRepository;
import com.example.gradu.domain.ranking.repository.CourseRankingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseRankingRebuildBatch {

    private final CourseRankingRepository dbRepo;
    private final CourseRankingRedisRepository redisRepo;
    private final MajorRoadmapIndex roadmapIndex;

    private static final int FETCH_SIZE = 300;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = "0 0 4 * * *")
    public void rebuildNightly() {
        rebuild();
    }

    // 수동 실행/테스트용
    public void rebuild() {
        if (!running.compareAndSet(false, true)) {
            log.warn("ranking rebuild already running, skip");
        }

        try {
            redisRepo.clearRankingKeys(); // ready 포함 전체 초기화

            loadCategory(Category.MAJOR, true);
            loadCategory(Category.FAITH_WORLDVIEW, false);
            loadCategory(Category.GENERAL_EDU, false);
            loadCategory(Category.BSM, false);
            loadCategory(Category.FREE_ELECTIVE_BASIC, false);
            loadCategory(Category.FREE_ELECTIVE_MJR, false);

            redisRepo.markReady();
            log.info("ranking rebuild done");
        } catch (Exception e) {
            log.error("ranking rebuild failed", e);
        } finally {
            running.set(false);
        }
    }

    private void loadCategory(Category cat, boolean includeMajorBucket) {
        List<CourseRankingRepository.CourseCountRow> rows =
                dbRepo.findTopCoursesByCategories(EnumSet.of(cat), PageRequest.of(0, FETCH_SIZE));

        for (var r : rows) {
            String norm = norm(r.getName());
            if (norm.isBlank()) continue;

            redisRepo.updateAtomically(
                    cat,
                    includeMajorBucket
                            ? roadmapIndex.findTermByCourseName(r.getName())
                                    .map(MajorRoadmapIndex.TermKey::toBucketKey)
                                    .orElse(null)
                            : null,
                    norm,
                    r.getName(),
                    r.getTakenCount()
            );
        }
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", "");
    }
}
