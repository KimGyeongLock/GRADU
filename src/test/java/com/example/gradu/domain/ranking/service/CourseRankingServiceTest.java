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
    void getCourseRanking_returnsMajorAndLiberalTop10_withRanksStartingAt1() {
        // given: repo가 2번 호출되므로 thenReturn을 2개 준비
        var majorRows = List.of(
                row("OS", 12),
                row("DB", 10),
                row("DS", 7)
        );

        var liberalRows = List.of(
                row("채플", 30),
                row("실용영어", 20)
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

        // then: liberal 랭킹 변환
        assertThat(res.liberal()).hasSize(2);
        assertThat(res.liberal().get(0)).isEqualTo(new CourseRankingDto.RankingItem(1, "채플", 30, 0));
        assertThat(res.liberal().get(1)).isEqualTo(new CourseRankingDto.RankingItem(2, "실용영어", 20, 0));

        // repo 호출 검증(2번)
        verify(repository, times(2)).findTopCoursesByCategories(anySet(), any(Pageable.class));
    }

    @Test
    void getCourseRanking_callsRepositoryWithMajorAndNonMajorCategories_andPageSize10() {
        // given
        when(repository.findTopCoursesByCategories(anySet(), any(Pageable.class)))
                .thenReturn(List.of(), List.of()); // major/libral 둘 다 빈 결과

        // when
        service.getCourseRanking();

        // then: 인자 캡쳐해서 major/libral 분기 확인
        ArgumentCaptor<Set<Category>> catCaptor = ArgumentCaptor.forClass(Set.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(repository, times(2)).findTopCoursesByCategories(catCaptor.capture(), pageableCaptor.capture());

        List<Set<Category>> capturedCats = catCaptor.getAllValues();
        List<Pageable> capturedPageables = pageableCaptor.getAllValues();

        // pageable: page=0, size=10 확인
        for (Pageable p : capturedPageables) {
            assertThat(p.getPageNumber()).isEqualTo(0);
            assertThat(p.getPageSize()).isEqualTo(10);
        }

        // categories: 한 번은 MAJOR 포함(전공), 한 번은 MAJOR 미포함(교양)
        boolean sawMajorSet = capturedCats.stream().anyMatch(s -> s.contains(Category.MAJOR));
        boolean sawNonMajorSet = capturedCats.stream().anyMatch(s -> !s.contains(Category.MAJOR));

        assertThat(sawMajorSet).isTrue();
        assertThat(sawNonMajorSet).isTrue();
    }

    private static CourseRankingRepository.CourseCountRow row(String name, long takenCount) {
        return new CourseRankingRepository.CourseCountRow() {
            @Override public String getName() { return name; }
            @Override public long getTakenCount() { return takenCount; }
        };
    }
}
