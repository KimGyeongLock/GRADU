package com.example.gradu.domain.ranking.repository;

import com.example.gradu.domain.curriculum.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseRankingRedisRepositoryTest {

    private RedisTemplate<String, String> redisTemplate;
    private CourseRankingRedisRepository repository;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        repository = new CourseRankingRedisRepository(redisTemplate);
    }

    @Test
    void updateAtomically_whenMajorBucketExists_executesLuaWithCategoryMajorDisplayKeys() {
        repository.updateAtomically(
                Category.MAJOR,
                "y2s1",
                "웹서비스개발",
                "웹 서비스 개발",
                1
        );

        verify(redisTemplate).execute(
                any(),
                eq(List.of(
                        "ranking:course:v1:cat:MAJOR",
                        "ranking:course:v1:major:y2s1",
                        "ranking:course:v1:display"
                )),
                eq("1"),
                eq("웹서비스개발"),
                eq("웹 서비스 개발")
        );
    }

    @Test
    void updateAtomically_whenMajorBucketIsNull_usesEmptyMajorKey() {
        repository.updateAtomically(
                Category.GENERAL_EDU,
                null,
                "철학의이해",
                "철학의 이해",
                -1
        );

        verify(redisTemplate).execute(
                any(),
                eq(List.of(
                        "ranking:course:v1:cat:GENERAL_EDU",
                        "",
                        "ranking:course:v1:display"
                )),
                eq("-1"),
                eq("철학의이해"),
                eq("철학의 이해")
        );
    }
}
