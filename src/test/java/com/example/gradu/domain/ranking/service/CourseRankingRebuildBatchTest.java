package com.example.gradu.domain.ranking.service;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.ranking.catalog.MajorRoadmapIndex;
import com.example.gradu.domain.ranking.repository.CourseRankingRedisRepository;
import com.example.gradu.domain.ranking.repository.CourseRankingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CourseRankingRebuildBatchTest {

    private CourseRankingRepository dbRepo;
    private CourseRankingRedisRepository redisRepo;
    private MajorRoadmapIndex roadmapIndex;
    private CourseRankingRebuildBatch batch;

    @BeforeEach
    void setUp() {
        dbRepo = mock(CourseRankingRepository.class);
        redisRepo = mock(CourseRankingRedisRepository.class);
        roadmapIndex = mock(MajorRoadmapIndex.class);
        batch = new CourseRankingRebuildBatch(dbRepo, redisRepo, roadmapIndex);
    }

    @Test
    void rebuild_success_clearsThenLoadsThenMarksReady() {
        // given
        when(dbRepo.findTopCoursesByCategories(anySet(), any(Pageable.class)))
                .thenReturn(
                        List.of(row("자료구조", 10)), // MAJOR
                        List.of(), // FAITH
                        List.of(), // GENERAL
                        List.of(), // BSM
                        List.of(), // FREE_BASIC
                        List.of()  // FREE_MJR
                );

        when(roadmapIndex.findTermByCourseName("자료구조"))
                .thenReturn(Optional.of(new MajorRoadmapIndex.TermKey(2, 1)));

        // when
        batch.rebuild();

        // then
        InOrder inOrder = Mockito.inOrder(redisRepo);
        inOrder.verify(redisRepo).clearRankingKeys();
        inOrder.verify(redisRepo).updateAtomically(
                eq(Category.MAJOR),
                eq("y2s1"),
                eq("자료구조"),
                eq("자료구조"),
                eq(10L)
        );
        inOrder.verify(redisRepo).markReady();
    }

    @Test
    void rebuild_whenDbFails_doesNotMarkReady() {
        // given
        when(dbRepo.findTopCoursesByCategories(anySet(), any(Pageable.class)))
                .thenThrow(new RuntimeException("db error"));

        // when
        batch.rebuild();

        // then
        verify(redisRepo).clearRankingKeys();
        verify(redisRepo, never()).markReady();
    }

    @Test
    void rebuildNightly_callsRebuild() {
        // spy로 스케줄 엔트리 검증
        CourseRankingRebuildBatch spyBatch = spy(batch);

        doNothing().when(spyBatch).rebuild();

        spyBatch.rebuildNightly();

        verify(spyBatch, times(1)).rebuild();
    }

    private static CourseRankingRepository.CourseCountRow row(String name, long takenCount) {
        return new CourseRankingRepository.CourseCountRow() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public long getTakenCount() {
                return takenCount;
            }
        };
    }
}