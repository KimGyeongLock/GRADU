package com.example.gradu.domain.ranking.service;

import com.example.gradu.domain.curriculum.entity.Category;
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

    @BeforeEach
    void setUp() {
        repository = mock(CourseRankingRepository.class);
        service = new CourseRankingService(repository);
    }

    @Test
    void getCourseRanking_returnsMajorAndLiberalRankings_withFilteringAndRanksStartingAt1() {
        // given: repo가 2번 호출되므로 thenReturn을 2개 준비(전공 -> 교양)
        List<CourseRankingRepository.CourseCountRow> majorRows = List.of(
                row("OS", 12),
                row("DB", 10),
                row("DS", 7)
        );

        // 교양: "채플", "공동체리더십훈련" 포함 과목은 제외되어야 함
        List<CourseRankingRepository.CourseCountRow> liberalRows = List.of(
                row("채플", 30),
                row("공동체리더십훈련(1)", 25),
                row("실용영어", 20),
                row("철학의이해", 18),
                row("채플 실습", 17),              // "채플" 포함 → 제외
                row("공동체리더십훈련", 16),         // 포함 → 제외
                row("글쓰기", 15)
        );

        when(repository.findTopCoursesByCategories(anySet(), any(Pageable.class)))
                .thenReturn(majorRows, liberalRows);

        // when
        CourseRankingDto.RankingResponse res = service.getCourseRanking();

        // then: major 랭킹 변환
        assertThat(res.major()).hasSize(3);
        assertThat(res.major().get(0)).isEqualTo(new CourseRankingDto.RankingItem(1, "OS", 12, 0));
        assertThat(res.major().get(1)).isEqualTo(new CourseRankingDto.RankingItem(2, "DB", 10, 0));
        assertThat(res.major().get(2)).isEqualTo(new CourseRankingDto.RankingItem(3, "DS", 7, 0));

        // then: liberal 랭킹 변환 (필터링 + rank 재부여)
        assertThat(res.liberal()).containsExactly(
                new CourseRankingDto.RankingItem(1, "실용영어", 20, 0),
                new CourseRankingDto.RankingItem(2, "철학의이해", 18, 0),
                new CourseRankingDto.RankingItem(3, "글쓰기", 15, 0)
        );

        verify(repository, times(2)).findTopCoursesByCategories(anySet(), any(Pageable.class));
    }

    @Test
    void getCourseRanking_callsRepository_withMajorPageSize10_andLiberalPageSize30() {
        // given
        when(repository.findTopCoursesByCategories(anySet(), any(Pageable.class)))
                .thenReturn(List.<CourseRankingRepository.CourseCountRow>of(),
                        List.<CourseRankingRepository.CourseCountRow>of());

        // when
        service.getCourseRanking();

        // then: 인자 캡쳐
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Category>> catCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(repository, times(2)).findTopCoursesByCategories(catCaptor.capture(), pageableCaptor.capture());

        List<Set<Category>> capturedCats = catCaptor.getAllValues();
        List<Pageable> capturedPageables = pageableCaptor.getAllValues();

        int majorIdx = -1;
        int liberalIdx = -1;
        for (int i = 0; i < capturedCats.size(); i++) {
            if (capturedCats.get(i).contains(Category.MAJOR)) majorIdx = i;
            else liberalIdx = i;
        }

        assertThat(majorIdx).isNotEqualTo(-1);
        assertThat(liberalIdx).isNotEqualTo(-1);

        Pageable majorPageable = capturedPageables.get(majorIdx);
        Pageable liberalPageable = capturedPageables.get(liberalIdx);

        assertThat(majorPageable.getPageNumber()).isEqualTo(0);
        assertThat(majorPageable.getPageSize()).isEqualTo(10);

        assertThat(liberalPageable.getPageNumber()).isEqualTo(0);
        assertThat(liberalPageable.getPageSize()).isEqualTo(30);
    }

    @Test
    void liberalRanking_limitsTo10_afterFiltering_andRanksAreReassigned() {
        // given
        List<CourseRankingRepository.CourseCountRow> majorRows = List.of();

        List<CourseRankingRepository.CourseCountRow> liberalRows = List.of(
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
                .thenReturn(majorRows, liberalRows);

        // when
        CourseRankingDto.RankingResponse res = service.getCourseRanking();

        // then
        assertThat(res.liberal()).hasSize(10);
        assertThat(res.liberal().get(0)).isEqualTo(new CourseRankingDto.RankingItem(1, "교양1", 98, 0));
        assertThat(res.liberal().get(9)).isEqualTo(new CourseRankingDto.RankingItem(10, "교양10", 89, 0));
    }

    private static CourseRankingRepository.CourseCountRow row(String name, long takenCount) {
        return new CourseRankingRepository.CourseCountRow() {
            @Override public String getName() { return name; }
            @Override public long getTakenCount() { return takenCount; }
        };
    }
}
