package com.example.gradu.domain.ranking.service;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.ranking.catalog.MajorRoadmapIndex;
import com.example.gradu.domain.ranking.repository.CourseRankingRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseRankingUpdateService {
    private final CourseRankingRedisRepository redisRepo;
    private final MajorRoadmapIndex roadmapIndex;

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
        if (norm.isBlank() || category == null) return;
        redisRepo.zincrByCategory(category, norm, delta);
        redisRepo.saveDisplay(norm, name);

        if (category == Category.MAJOR) {
            roadmapIndex.findTermByCourseName(name)
                    .ifPresent(t -> redisRepo.zincrByMajorBucket(t.toBucketKey(), norm, delta));
        }
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", "");
    }
}
