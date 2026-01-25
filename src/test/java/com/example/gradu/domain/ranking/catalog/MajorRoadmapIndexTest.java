package com.example.gradu.domain.ranking.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class MajorRoadmapIndexTest {

    @Test
    void findTermByCourseName_matches_andIgnoresSpaces_usingRealJsonRow() throws IOException {
        // given: 인덱스(운영과 동일하게 json 로딩)
        var index = new MajorRoadmapIndex(new ObjectMapper());

        // ✅ 테스트도 같은 json을 읽어서 "실존하는 과목명"을 하나 고른다
        var sample = pickAnyRowWithKoOrEn();
        String ko = sample.nameKo();
        String en = sample.nameEn();

        // when / then: KO가 있으면 KO로 검증
        if (ko != null && !ko.isBlank()) {
            var ko1 = index.findTermByCourseName(ko);
            var ko2 = index.findTermByCourseName(withSpacesInserted(ko));

            assertThat(ko1).as("KO 원문이 인덱스에 있어야 함: " + ko).isPresent();
            assertThat(ko2).as("KO 공백섞기 버전도 매칭돼야 함").isPresent();
            assertThat(ko1.get()).isEqualTo(ko2.get());
        }

        // EN이 있으면 EN으로 검증
        if (en != null && !en.isBlank()) {
            var en1 = index.findTermByCourseName(en);
            var en2 = index.findTermByCourseName(withSpacesInserted(en));

            assertThat(en1).as("EN 원문이 인덱스에 있어야 함: " + en).isPresent();
            assertThat(en2).as("EN 공백섞기 버전도 매칭돼야 함").isPresent();
            assertThat(en1.get()).isEqualTo(en2.get());
        }

        // KO/EN 둘 다 있으면 같은 TermKey인지도 검증(같은 row니까)
        if (ko != null && !ko.isBlank() && en != null && !en.isBlank()) {
            assertThat(index.findTermByCourseName(ko)).isPresent();
            assertThat(index.findTermByCourseName(en)).isPresent();
            assertThat(index.findTermByCourseName(ko).get())
                    .as("같은 row의 KO/EN은 같은 TermKey여야 함")
                    .isEqualTo(index.findTermByCourseName(en).get());
        }
    }

    @Test
    void findTermByCourseName_returnsEmpty_whenNotFound_orNull_orBlank() throws IOException {
        var index = new MajorRoadmapIndex(new ObjectMapper());

        assertThat(index.findTermByCourseName("없는과목_절대없음")).isEmpty();
        assertThat(index.findTermByCourseName(null)).isEmpty();
        assertThat(index.findTermByCourseName("")).isEmpty();
        assertThat(index.findTermByCourseName("   ")).isEmpty();
    }

    // ===== helpers =====

    private static MajorRoadmapIndex.RoadmapRow pickAnyRowWithKoOrEn() throws IOException {
        var res = new ClassPathResource("catalog/major_roadmap.json");
        try (var is = res.getInputStream()) {
            var mapper = new ObjectMapper();
            MajorRoadmapIndex.RoadmapRow[] rows = mapper.readValue(is, MajorRoadmapIndex.RoadmapRow[].class);

            // KO 또는 EN 중 하나라도 있는 row를 선택
            for (var r : rows) {
                boolean hasKo = r.nameKo() != null && !r.nameKo().isBlank();
                boolean hasEn = r.nameEn() != null && !r.nameEn().isBlank();
                if (hasKo || hasEn) return r;
            }
        }
        throw new IllegalStateException("major_roadmap.json에 nameKo/nameEn이 비어있지 않은 row가 없습니다.");
    }

    private static String withSpacesInserted(String s) {
        // "Data Structures" 같은 걸 " D a t a   S t r u c t u r e s " 형태로 만들어서
        // norm(trim + 모든 공백 제거) 매칭을 강제 검증
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return trimmed;

        StringBuilder sb = new StringBuilder();
        sb.append("  ");
        for (int i = 0; i < trimmed.length(); i++) {
            sb.append(trimmed.charAt(i));
            if (i % 2 == 0) sb.append(' ');
            if (i % 5 == 0) sb.append("  ");
        }
        sb.append("  ");
        return sb.toString();
    }
}
