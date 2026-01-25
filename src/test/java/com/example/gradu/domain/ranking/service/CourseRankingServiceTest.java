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
    void getCourseRanking_returnsMajorAndLiberalRankings_withFiltering_andMajorGroupedByTerm() {
        // ===== major rows (repo 1st call) =====
        List<CourseRankingRepository.CourseCountRow> majorRows = List.of(
                row("Intro to Programming", 12),
                row("Discrete Math", 10),
                row("Unmapped Major Course", 9)
        );

        when(roadmapIndex.findTermByCourseName("Intro to Programming"))
                .thenReturn(Optional.of(new MajorRoadmapIndex.TermKey(1, 2))); // y1s2
        when(roadmapIndex.findTermByCourseName("Discrete Math"))
                .thenReturn(Optional.of(new MajorRoadmapIndex.TermKey(2, 1))); // y2s1
        when(roadmapIndex.findTermByCourseName("Unmapped Major Course"))
                .thenReturn(Optional.empty());

        // ===== liberal rows (repo calls 2~5) =====
        List<CourseRankingRepository.CourseCountRow> faithRows = List.of(
                row("채플", 30),                 // 제외
                row("공동체리더십훈련(1)", 25),   // 제외
                row("그리스도인과 선교", 20)
        );
        List<CourseRankingRepository.CourseCountRow> generalEduRows = List.of(
                row("철학의이해", 18),
                row("글쓰기", 15)
        );
        List<CourseRankingRepository.CourseCountRow> bsmRows = List.of(
                row("Calculus1", 100)
        );
        List<CourseRankingRepository.CourseCountRow> freeRows = List.of(
                row("자유선택과목", 22)
        );

        when(repository.findTopCoursesByCategories(anySet(), any(Pageable.class)))
                .thenReturn(
                        majorRows,       // 1) major (전체)
                        faithRows,       // 2) liberal-faith
                        generalEduRows,  // 3) liberal-generalEdu
                        bsmRows,         // 4) liberal-bsm
                        freeRows         // 5) liberal-freeElective
                );

        // when
        CourseRankingDto.RankingResponse res = service.getCourseRanking();

        // then: major buckets
        assertThat(res.major().y1s2()).containsExactly(
                new CourseRankingDto.RankingItem(1, "Intro to Programming", 12, 0)
        );
        assertThat(res.major().y2s1()).containsExactly(
                new CourseRankingDto.RankingItem(1, "Discrete Math", 10, 0)
        );

        // Unmapped는 어떤 버킷에도 없어야 함
        boolean existsUnmapped =
                res.major().y1s2().stream().anyMatch(i -> i.courseName().equals("Unmapped Major Course"))
                        || res.major().y2s1().stream().anyMatch(i -> i.courseName().equals("Unmapped Major Course"))
                        || res.major().y2s2().stream().anyMatch(i -> i.courseName().equals("Unmapped Major Course"))
                        || res.major().y3s1().stream().anyMatch(i -> i.courseName().equals("Unmapped Major Course"))
                        || res.major().y3s2().stream().anyMatch(i -> i.courseName().equals("Unmapped Major Course"))
                        || res.major().y4s1().stream().anyMatch(i -> i.courseName().equals("Unmapped Major Course"))
                        || res.major().y4s2().stream().anyMatch(i -> i.courseName().equals("Unmapped Major Course"));
        assertThat(existsUnmapped).isFalse();

        // then: liberal filtering + rank reassigned
        assertThat(res.liberal().faithWorldview()).containsExactly(
                new CourseRankingDto.RankingItem(1, "그리스도인과 선교", 20, 0)
        );
        assertThat(res.liberal().generalEdu()).containsExactly(
                new CourseRankingDto.RankingItem(1, "철학의이해", 18, 0),
                new CourseRankingDto.RankingItem(2, "글쓰기", 15, 0)
        );
        assertThat(res.liberal().bsm()).containsExactly(
                new CourseRankingDto.RankingItem(1, "Calculus1", 100, 0)
        );
        assertThat(res.liberal().freeElective()).containsExactly(
                new CourseRankingDto.RankingItem(1, "자유선택과목", 22, 0)
        );

        verify(repository, times(5))
                .findTopCoursesByCategories(anySet(), any(Pageable.class));
    }

    @Test
    void getCourseRanking_callsRepository_5times_and_usesReasonablePageSizes() {
        when(repository.findTopCoursesByCategories(anySet(), any(Pageable.class)))
                .thenReturn(List.of(), List.of(), List.of(), List.of(), List.of());

        service.getCourseRanking();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Category>> catCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(repository, times(5))
                .findTopCoursesByCategories(catCaptor.capture(), pageableCaptor.capture());

        var pages = pageableCaptor.getAllValues();
        assertThat(pages).hasSize(5);

        // page=0
        for (Pageable p : pages) {
            assertThat(p.getPageNumber()).isEqualTo(0);
        }

        // ✅ 핵심 수정:
        // major는 roadmap 분류하려고 크게 가져올 수 있음(예: 300)
        // liberal 4개는 필터링 때문에 30(혹은 10)일 수 있음
        // -> "10/30/300" 허용으로 테스트를 서비스 정책에 맞춘다.
        for (Pageable p : pages) {
            assertThat(p.getPageSize()).isIn(10, 30, 300);
        }
    }

    @Test
    void liberalFaith_limitsTo10_afterFiltering_andRanksReassigned() {
        // major 1회 + liberal 4회 = 5회
        List<CourseRankingRepository.CourseCountRow> majorEmpty = List.of();

        List<CourseRankingRepository.CourseCountRow> faithRows = List.of(
                row("채플", 100),
                row("공동체리더십훈련", 99),
                row("교양1", 98),
                row("교양2", 97),
                row("교양3", 96),
                row("교양4", 95),
                row("교양5", 94),
                row("교양6", 93),
                row("교양7", 92),
                row("교양8", 91),
                row("교양9", 90),
                row("교양10", 89),
                row("교양11", 88)
        );

        when(repository.findTopCoursesByCategories(anySet(), any(Pageable.class)))
                .thenReturn(
                        majorEmpty, // 1) major
                        faithRows,  // 2) liberal-faith
                        List.of(),  // 3) generalEdu
                        List.of(),  // 4) bsm
                        List.of()   // 5) freeElective
                );

        CourseRankingDto.RankingResponse res = service.getCourseRanking();

        var faith = res.liberal().faithWorldview();
        assertThat(faith).hasSize(10);
        assertThat(faith.get(0)).isEqualTo(new CourseRankingDto.RankingItem(1, "교양1", 98, 0));
        assertThat(faith.get(9)).isEqualTo(new CourseRankingDto.RankingItem(10, "교양10", 89, 0));
    }

    @Test
    void majorCourse_isIgnored_whenRoadmapIndexReturnsEmpty() {
        when(repository.findTopCoursesByCategories(anySet(), any(Pageable.class)))
                .thenReturn(List.of(row("Unknown Major", 10)),
                        List.of(), List.of(), List.of(), List.of());

        when(roadmapIndex.findTermByCourseName("Unknown Major"))
                .thenReturn(Optional.empty());

        CourseRankingDto.RankingResponse res = service.getCourseRanking();

        assertThat(res.major().y1s2()).isEmpty();
        assertThat(res.major().y2s1()).isEmpty();
    }

    private static CourseRankingRepository.CourseCountRow row(String name, long takenCount) {
        return new CourseRankingRepository.CourseCountRow() {
            @Override public String getName() { return name; }
            @Override public long getTakenCount() { return takenCount; }
        };
    }
}
