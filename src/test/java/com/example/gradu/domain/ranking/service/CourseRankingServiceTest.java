package com.example.gradu.domain.ranking.service;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.ranking.catalog.MajorRoadmapIndex;
import com.example.gradu.domain.ranking.dto.CourseRankingDto;
import com.example.gradu.domain.ranking.repository.CourseRankingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CourseRankingServiceTest {

    private CourseRankingRepository repository;
    private MajorRoadmapIndex roadmapIndex;
    private CourseRankingService service;

    @BeforeEach
    void setUp() {
        repository = mock(CourseRankingRepository.class);
        roadmapIndex = mock(MajorRoadmapIndex.class);
        service = new CourseRankingService(repository, roadmapIndex);
    }

    @Test
    void getCourseRanking_majorBuckets_mergeByWhitespace_and_liberalFiltersAndRanks() {
        // ===== major (repo 1st) =====
        // "웹 서비스 개발" + "웹서비스개발" -> 합산되어 한 아이템으로 나와야 함
        List<CourseRankingRepository.CourseCountRow> majorRows = List.of(
                row("웹 서비스 개발", 3),
                row("웹서비스개발", 1),
                row("자료구조", 5),
                row("   ", 999),              // blank -> 무시 분기
                row(null, 777)                // null -> 무시 분기
        );

        // term mapping (전공 버킷 분배)
        when(roadmapIndex.findTermByCourseName("웹 서비스 개발"))
                .thenReturn(Optional.of(new MajorRoadmapIndex.TermKey(2, 1))); // y2s1
        when(roadmapIndex.findTermByCourseName("웹서비스개발"))
                .thenReturn(Optional.of(new MajorRoadmapIndex.TermKey(2, 1))); // y2s1 (same bucket)
        when(roadmapIndex.findTermByCourseName("자료구조"))
                .thenReturn(Optional.of(new MajorRoadmapIndex.TermKey(2, 1))); // y2s1
        when(roadmapIndex.findTermByCourseName("   "))
                .thenReturn(Optional.empty());
        when(roadmapIndex.findTermByCourseName(null))
                .thenReturn(Optional.empty());

        // ===== liberal (repo 2~5) =====
        // faith: 채플/공동체리더십훈련은 제외되어야 함
        List<CourseRankingRepository.CourseCountRow> faithRows = List.of(
                row("채플", 100),
                row("공동체리더십훈련(1)", 99),
                row("그리스도인과 선교", 10)
        );

        // generalEdu: 공백 차이 합산 테스트
        List<CourseRankingRepository.CourseCountRow> generalRows = List.of(
                row("철학의 이해", 2),
                row("철학의이해", 5),
                row("글쓰기", 1)
        );

        List<CourseRankingRepository.CourseCountRow> bsmRows = List.of(
                row("Calculus1", 50)
        );

        List<CourseRankingRepository.CourseCountRow> freeRows = List.of(
                row("자유선택과목", 7)
        );

        when(repository.findTopCoursesByCategories(anySet(), any(Pageable.class)))
                .thenReturn(
                        majorRows,     // 1) major
                        faithRows,     // 2) faith
                        generalRows,   // 3) general
                        bsmRows,       // 4) bsm
                        freeRows       // 5) free
                );

        // when
        CourseRankingDto.RankingResponseDto res = service.getCourseRanking();

        // then - major y2s1: 합산 + 정렬(자료구조 5 vs 웹서비스개발 4)
        var y2s1 = res.major().y2s1();
        assertThat(y2s1).hasSize(2);

        assertThat(y2s1.get(0)).isEqualTo(
                new CourseRankingDto.RankingItemDto(1, "자료구조", 5, 0)
        );

        // ✅ displayName은 더 "읽기 좋은" 쪽(보통 공백 포함/긴 쪽) 우선
        // 합산 count = 3 + 1 = 4
        assertThat(y2s1.get(1).rank()).isEqualTo(2);
        assertThat(y2s1.get(1).takenCount()).isEqualTo(4);
        assertThat(y2s1.get(1).courseName()).isEqualTo("웹 서비스 개발");

        // 다른 버킷은 빈 리스트여야 함
        assertThat(res.major().y1s2()).isEmpty();
        assertThat(res.major().y2s2()).isEmpty();
        assertThat(res.major().y3s1()).isEmpty();
        assertThat(res.major().y3s2()).isEmpty();
        assertThat(res.major().y4s1()).isEmpty();
        assertThat(res.major().y4s2()).isEmpty();

        // then - liberal faith: 필터링 후 1개만 남고 rank=1
        assertThat(res.liberal().faithWorldview()).containsExactly(
                new CourseRankingDto.RankingItemDto(1, "그리스도인과 선교", 10, 0)
        );

        // then - liberal general: "철학의 이해"(2) + "철학의이해"(5) => 7로 합쳐져 1등
        var general = res.liberal().generalEdu();
        assertThat(general).hasSize(2);
        assertThat(general.get(0)).isEqualTo(
                new CourseRankingDto.RankingItemDto(1, "철학의 이해", 7, 0)
        );
        assertThat(general.get(1)).isEqualTo(
                new CourseRankingDto.RankingItemDto(2, "글쓰기", 1, 0)
        );

        assertThat(res.liberal().bsm()).containsExactly(
                new CourseRankingDto.RankingItemDto(1, "Calculus1", 50, 0)
        );

        assertThat(res.liberal().freeElective()).containsExactly(
                new CourseRankingDto.RankingItemDto(1, "자유선택과목", 7, 0)
        );

        // repo 호출 5번
        verify(repository, times(5)).findTopCoursesByCategories(anySet(), any(Pageable.class));
    }

    @Test
    void getCourseRanking_callsRepository_5times_and_majorPageSize300() {
        when(repository.findTopCoursesByCategories(anySet(), any(Pageable.class)))
                .thenReturn(List.of(), List.of(), List.of(), List.of(), List.of());

        service.getCourseRanking();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Category>> catCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(repository, times(5)).findTopCoursesByCategories(catCaptor.capture(), pageableCaptor.capture());

        var cats = catCaptor.getAllValues();
        var pages = pageableCaptor.getAllValues();

        assertThat(cats).hasSize(5);
        assertThat(pages).hasSize(5);

        // all page=0
        assertThat(pages).allSatisfy(p -> assertThat(p.getPageNumber()).isEqualTo(0));

        // 첫 호출은 major (pageSize=300)
        assertThat(pages.get(0).getPageSize()).isEqualTo(300);

        // 나머지 4개는 liberal (서비스 정책상 30 or 10)
        for (int i = 1; i < pages.size(); i++) {
            assertThat(pages.get(i).getPageSize()).isIn(10, 30);
        }

        // 첫 cats는 MAJOR 포함이어야 함(엄격히 1개만 들어있을 수도)
        assertThat(cats.get(0)).contains(Category.MAJOR);
    }

    @Test
    void getCourseRanking_handles_unmappedMajorCourse_gracefully() {
        // major에만 값 있고 roadmapIndex가 empty -> 모든 버킷 비어야 함
        List<CourseRankingRepository.CourseCountRow> majorRows = List.of(
                row("매핑안되는전공", 10)
        );
        when(roadmapIndex.findTermByCourseName("매핑안되는전공")).thenReturn(Optional.empty());

        when(repository.findTopCoursesByCategories(anySet(), any(Pageable.class)))
                .thenReturn(
                        majorRows,
                        List.of(), List.of(), List.of(), List.of()
                );

        var res = service.getCourseRanking();

        assertThat(res.major().y1s2()).isEmpty();
        assertThat(res.major().y2s1()).isEmpty();
        assertThat(res.major().y2s2()).isEmpty();
        assertThat(res.major().y3s1()).isEmpty();
        assertThat(res.major().y3s2()).isEmpty();
        assertThat(res.major().y4s1()).isEmpty();
        assertThat(res.major().y4s2()).isEmpty();
    }

    private static CourseRankingRepository.CourseCountRow row(String name, long takenCount) {
        return new CourseRankingRepository.CourseCountRow() {
            @Override public String getName() { return name; }
            @Override public long getTakenCount() { return takenCount; }
        };
    }
}
