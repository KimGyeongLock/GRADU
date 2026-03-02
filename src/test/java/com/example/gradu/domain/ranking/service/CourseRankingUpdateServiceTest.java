package com.example.gradu.domain.ranking.service;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.ranking.catalog.MajorRoadmapIndex;
import com.example.gradu.domain.ranking.repository.CourseRankingRedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

class CourseRankingUpdateServiceTest {

    private CourseRankingRedisRepository redisRepository;
    private MajorRoadmapIndex roadmapIndex;
    private CourseRankingUpdateService service;

    @BeforeEach
    void setUp() {
        redisRepository = mock(CourseRankingRedisRepository.class);
        roadmapIndex = mock(MajorRoadmapIndex.class);
        service = new CourseRankingUpdateService(redisRepository, roadmapIndex);
    }

    @Test
    void onCreated_majorCourse_updatesCategoryAndMajorBucketAtomically() {
        // given
        when(roadmapIndex.findTermByCourseName("웹 서비스 개발"))
                .thenReturn(Optional.of(new MajorRoadmapIndex.TermKey(2, 1)));

        // when
        service.onCreated("웹 서비스 개발", Category.MAJOR);

        // then
        verify(redisRepository).updateAtomically(
                Category.MAJOR,
                "y2s1",
                "웹서비스개발",
                "웹 서비스 개발",
                1
        );
    }

    @Test
    void onUpdated_whenNameAndCategoryChanged_decrementsOldAndIncrementsNew() {
        // given
        when(roadmapIndex.findTermByCourseName("자료 구조"))
                .thenReturn(Optional.of(new MajorRoadmapIndex.TermKey(2, 2)));

        // when
        service.onUpdated(
                "철학의 이해", Category.GENERAL_EDU,
                "자료 구조", Category.MAJOR
        );

        // then
        verify(redisRepository).updateAtomically(
                Category.GENERAL_EDU,
                null,
                "철학의이해",
                "철학의 이해",
                -1
        );

        verify(redisRepository).updateAtomically(
                Category.MAJOR,
                "y2s2",
                "자료구조",
                "자료 구조",
                1
        );
    }

    @Test
    void onCreated_untrackedCategory_skipsRedisUpdate() {
        // when
        service.onCreated("실무영어", Category.PRACTICAL_ENGLISH);

        // then
        verifyNoInteractions(redisRepository);
    }

    @Test
    void onCreated_whenRedisFails_doesNotThrow() {
        // given
        doThrow(new RuntimeException("redis down"))
                .when(redisRepository)
                .updateAtomically(any(), any(), any(), any(), anyLong());

        // when
        service.onCreated("철학의 이해", Category.GENERAL_EDU);

        // then
        verify(redisRepository).updateAtomically(
                Category.GENERAL_EDU,
                null,
                "철학의이해",
                "철학의 이해",
                1
        );
    }
}
