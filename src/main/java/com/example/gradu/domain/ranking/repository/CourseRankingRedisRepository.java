package com.example.gradu.domain.ranking.repository;

import com.example.gradu.domain.curriculum.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class CourseRankingRedisRepository {
    private static final String PREFIX = "ranking:course:v1";
    private static final String DISPLAY_KEY = PREFIX + ":display";
    private static final String READY_KEY = PREFIX + ":ready";

    private final RedisTemplate<String, String> redisTemplate;

    public void zincrByCategory(Category category, String normName, double delta) {
        redisTemplate.opsForZSet().incrementScore(PREFIX + ":cat:" + category.name(), normName, delta);
    }

    public void zincrByMajorBucket(String bucket, String normName, double delta) {
        redisTemplate.opsForZSet().incrementScore(PREFIX + ":major:" + bucket, normName, delta);
    }

    public void saveDisplay(String normName, String display) {
        if (display == null || display.isBlank()) return;
        redisTemplate.opsForHash().put(DISPLAY_KEY, normName, display);
    }

    public String displayOf(String normName) {
        Object v = redisTemplate.opsForHash().get(DISPLAY_KEY, normName);
        return v == null ? null : v.toString();
    }

    public List<ScoreRow> topByCategory(Category category, int n) {
        return top(PREFIX + ":cat:" + category.name(), n);
    }

    public List<ScoreRow> topByMajorBucket(String bucket, int n) {
        return top(PREFIX + ":major:" + bucket, n);
    }

    public boolean isReady() {
        String v = redisTemplate.opsForValue().get(READY_KEY);
        return "1".equals(v);
    }

    public void markReady() {
        redisTemplate.opsForValue().set(READY_KEY, "1");
    }

    public void clearRankingKeys() {
        Set<String> keys = redisTemplate.keys(PREFIX + ":*");
        if (keys == null || keys.isEmpty()) return;
        redisTemplate.delete(keys);
    }

    private List<ScoreRow> top(String key, int n) {
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, n - 1);

        if (tuples == null || tuples.isEmpty()) return List.of();

        List<ScoreRow> out = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> t : tuples) {
            if (t.getValue() == null || t.getScore() == null) continue;
            out.add(new ScoreRow(t.getValue(), Math.round(t.getScore())));
        }
        return out;
    }

    public record ScoreRow(String normName, long score) {}
}
