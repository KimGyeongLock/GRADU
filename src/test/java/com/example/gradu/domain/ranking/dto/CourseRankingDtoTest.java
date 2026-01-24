package com.example.gradu.domain.ranking.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseRankingDtoTest {

    @Test
    void dtoRecords_canBeConstructedAndRead() {
        var item = new CourseRankingDto.RankingItem(1, "자료구조", 1234, 0);
        var resp = new CourseRankingDto.RankingResponse(List.of(item), List.of());

        assertThat(item.rank()).isEqualTo(1);
        assertThat(item.courseName()).isEqualTo("자료구조");
        assertThat(item.takenCount()).isEqualTo(1234);
        assertThat(item.delta()).isEqualTo(0);

        assertThat(resp.major()).hasSize(1);
        assertThat(resp.liberal()).isEmpty();
    }
}
