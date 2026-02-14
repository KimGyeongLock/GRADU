package com.example.gradu.domain.ranking.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseRankingDtoTest {

    @Test
    void dtoRecords_canBeConstructedAndRead() {
        var item = new CourseRankingDto.RankingItemDto(1, "자료구조", 1234, 0);

        var major = new CourseRankingDto.MajorRankingDto(
                List.of(item), // y1s2
                List.of(),     // y2s1
                List.of(),     // y2s2
                List.of(),     // y3s1
                List.of(),     // y3s2
                List.of(),     // y4s1
                List.of()      // y4s2
        );

        var liberal = new CourseRankingDto.LiberalRankingDto(
                List.of(), // faithWorldview
                List.of(), // generalEdu
                List.of(), // bsm
                List.of()  // freeElective
        );

        var resp = new CourseRankingDto.RankingResponseDto(major, liberal);

        // item
        assertThat(item.rank()).isEqualTo(1);
        assertThat(item.courseName()).isEqualTo("자료구조");
        assertThat(item.takenCount()).isEqualTo(1234);
        assertThat(item.delta()).isEqualTo(0);

        // major
        assertThat(resp.major().y1s2()).hasSize(1);
        assertThat(resp.major().y1s2().get(0).courseName()).isEqualTo("자료구조");
        assertThat(resp.major().y2s1()).isEmpty();

        // liberal
        assertThat(resp.liberal().faithWorldview()).isEmpty();
        assertThat(resp.liberal().generalEdu()).isEmpty();
        assertThat(resp.liberal().bsm()).isEmpty();
        assertThat(resp.liberal().freeElective()).isEmpty();
    }
}
