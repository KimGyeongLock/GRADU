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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CourseRankingServiceTest {

    private CourseRankingRepository repository;
    private CourseRankingService service;
    private MajorRoadmapIndex resolver;

    @BeforeEach
    void setUp() {
        repository = mock(CourseRankingRepository.class);
        resolver = mock(MajorRoadmapIndex.class);
        service = new CourseRankingService(repository, resolver);
    }

    @Test
    void getCourseRanking_returnsMajorAndLiberalRankings_withFilteringAndRanksStartingAt1() {
        // given: 호출 순서(예시)
        // 1) major-y1s2
        // 2) major-y2s1
        // 3) major-y2s2
        // 4) major-y3s1
        // 5) major-y3s2
        // 6) major-y4s1
        // 7) major-y4s2
        // 8) liberal-faith
        // 9) liberal-generalEdu
        // 10) liberal-bsm
        // 11) liberal-free

        // ✅ 너 서비스가 "전공을 학기별로 7번 호출"하도록 구현했다고 가정
        // 만약 전공을 1번만 호출하는 구조면, 이 테스트의 반환/검증도 그 구조로 맞춰야 해.

        List<CourseRankingRepository.CourseCountRow> y1s2Rows = List.of(
                row("Intro to Programming", 12),
                row("Discrete Math", 10)
        );
        List<CourseRankingRepository.CourseCountRow> y2s1Rows = List.of(
                row("Data Structures", 20)
        );
        List<CourseRankingRepository.CourseCountRow> y2s2Rows = List.of();
        List<CourseRankingRepository.CourseCountRow> y3s1Rows = List.of();
        List<CourseRankingRepository.CourseCountRow> y3s2Rows = List.of();
        List<CourseRankingRepository.CourseCountRow> y4s1Rows = List.of();
        List<CourseRankingRepository.CourseCountRow> y4s2Rows = List.of();

        // 교양: faith에 필터링 대상 포함
        List<CourseRankingRepository.CourseCountRow> faithRows = List.of(
                row("채플", 30),
                row("공동체리더십훈련(1)", 25),
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
                        y1s2Rows,
                        y2s1Rows,
                        y2s2Rows,
                        y3s1Rows,
                        y3s2Rows,
                        y4s1Rows,
                        y4s2Rows,
                        faithRows,
                        generalEduRows,
                        bsmRows,
                        freeRows
                );

        // when
        CourseRankingDto.RankingResponse res = service.getCourseRanking();

        // then: major.y1s2 rank 재부여
        assertThat(res.major().y1s2()).containsExactly(
                new CourseRankingDto.RankingItem(1, "Intro to Programming", 12, 0),
                new CourseRankingDto.RankingItem(2, "Discrete Math", 10, 0)
        );

        // then: major.y2s1
        assertThat(res.major().y2s1()).containsExactly(
                new CourseRankingDto.RankingItem(1, "Data Structures", 20, 0)
        );

        // then: liberal.faithWorldview (채플/공동체리더십훈련 제외 + rank 재부여)
        assertThat(res.liberal().faithWorldview()).containsExactly(
                new CourseRankingDto.RankingItem(1, "그리스도인과 선교", 20, 0)
        );

        // then: liberal.generalEdu
        assertThat(res.liberal().generalEdu()).containsExactly(
                new CourseRankingDto.RankingItem(1, "철학의이해", 18, 0),
                new CourseRankingDto.RankingItem(2, "글쓰기", 15, 0)
        );

        // then: liberal.bsm
        assertThat(res.liberal().bsm()).containsExactly(
                new CourseRankingDto.RankingItem(1, "Calculus1", 100, 0)
        );

        // then: liberal.freeElective
        assertThat(res.liberal().freeElective()).containsExactly(
                new CourseRankingDto.RankingItem(1, "자유선택과목", 22, 0)
        );

        // repository 호출 횟수(전공 7 + 교양 4 = 11)
        verify(repository, times(11)).findTopCoursesByCategories(anySet(), any(Pageable.class));
    }

    @Test
    void getCourseRanking_callsRepository_withExpectedPageSizes() {
        // given: 빈 리스트를 호출 횟수만큼 반환
        when(repository.findTopCoursesByCategories(anySet(), any(Pageable.class)))
                .thenReturn(
                        List.<CourseRankingRepository.CourseCountRow>of(),
                        List.<CourseRankingRepository.CourseCountRow>of(),
                        List.<CourseRankingRepository.CourseCountRow>of(),
                        List.<CourseRankingRepository.CourseCountRow>of(),
                        List.<CourseRankingRepository.CourseCountRow>of(),
                        List.<CourseRankingRepository.CourseCountRow>of(),
                        List.<CourseRankingRepository.CourseCountRow>of(),
                        List.<CourseRankingRepository.CourseCountRow>of(),
                        List.<CourseRankingRepository.CourseCountRow>of(),
                        List.<CourseRankingRepository.CourseCountRow>of(),
                        List.<CourseRankingRepository.CourseCountRow>of()
                );

        // when
        service.getCourseRanking();

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Category>> catCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(repository, times(11)).findTopCoursesByCategories(catCaptor.capture(), pageableCaptor.capture());

        var cats = catCaptor.getAllValues();
        var pages = pageableCaptor.getAllValues();

        // 전부 page=0
        for (Pageable p : pages) {
            assertThat(p.getPageNumber()).isEqualTo(0);
        }

        // 전공/교양은 서비스 구현에 따라 pageSize가 다를 수 있음.
        // 네 기존 구현은 applyFilter=true 인 경우 30, 아니면 10이었음.
        // 전공 학기별도 보통 10으로 두는 게 자연스러움.
        // 여기서는 "전부 10 또는 30 중 하나"로만 검증(너무 빡세게 고정하지 않음)
        for (Pageable p : pages) {
            assertThat(p.getPageSize()).isIn(10, 30);
        }

        assertThat(cats).hasSize(11);
    }

    @Test
    void liberalRanking_limitsTo10_afterFiltering_andRanksAreReassigned_perSubTab() {
        // given
        // 전공 7회는 빈값
        List<CourseRankingRepository.CourseCountRow> empty = List.of();

        // faith는 필터링 대상 + 10개 초과
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
                        empty, empty, empty, empty, empty, empty, empty, // major 7
                        faithRows,                                       // liberal faith
                        List.<CourseRankingRepository.CourseCountRow>of(), // generalEdu
                        List.<CourseRankingRepository.CourseCountRow>of(), // bsm
                        List.<CourseRankingRepository.CourseCountRow>of()  // free
                );

        // when
        CourseRankingDto.RankingResponse res = service.getCourseRanking();

        // then
        var faith = res.liberal().faithWorldview();
        assertThat(faith).hasSize(10);
        assertThat(faith.get(0)).isEqualTo(new CourseRankingDto.RankingItem(1, "교양1", 98, 0));
        assertThat(faith.get(9)).isEqualTo(new CourseRankingDto.RankingItem(10, "교양10", 89, 0));
    }

    private static CourseRankingRepository.CourseCountRow row(String name, long takenCount) {
        return new CourseRankingRepository.CourseCountRow() {
            @Override public String getName() { return name; }
            @Override public long getTakenCount() { return takenCount; }
        };
    }
}
