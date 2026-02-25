package com.example.gradu.domain.ranking.service;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.ranking.catalog.MajorRoadmapIndex;
import com.example.gradu.domain.ranking.repository.CourseRankingRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseRankingUpdateService {
    private final CourseRankingRedisRepository redisRepo;
    private final MajorRoadmapIndex roadmapIndex;
    private static final EnumSet<Category> TRACKED_CATEGORIES = EnumSet.of(
            Category.MAJOR,
            Category.FAITH_WORLDVIEW,
            Category.GENERAL_EDU,
            Category.BSM,
            Category.FREE_ELECTIVE_BASIC,
            Category.FREE_ELECTIVE_MJR
    );

    public void onCreated(String name, Category category) {
        try {
            apply(name, category, 1);
        } catch (RuntimeException ex) {
            log.warn("ranking update failed on create. name={}, category={}", name, category, ex);
        }
    }

    public void onDeleted(String name, Category category) {
        try {
            apply(name, category, -1);
        } catch (RuntimeException ex) {
            log.warn("ranking update failed on delete. name={}, category={}", name, category, ex);
        }
    }

    public void onUpdated(String oldName, Category oldCat, String newName, Category newCat) {
        try {
            String o = norm(oldName), n = norm(newName);
            if (!o.equals(n) || oldCat != newCat) {
                if (!o.isBlank()) apply(oldName, oldCat, -1);
                if (!n.isBlank()) apply(newName, newCat, 1);
            }
        } catch (RuntimeException ex) {
            log.warn("ranking update failed on update. oldName={}, oldCat={}, newName={}, newCat={}",
                    oldName, oldCat, newName, newCat, ex);
        }
    }

    private void apply(String name, Category category, int delta) {
        String norm = norm(name);
        if (norm.isBlank() || category == null || !TRACKED_CATEGORIES.contains(category)) return;

        String majorBucket = null;
        if (category == Category.MAJOR) {
            majorBucket = roadmapIndex.findTermByCourseName(name)
                    .map(MajorRoadmapIndex.TermKey::toBucketKey)
                    .orElse(null);
        }
        redisRepo.updateAtomically(category, majorBucket, norm, name, delta);
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", "");
    }
}
