package com.example.gradu.domain.ranking.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class MajorRoadmapIndex {

    public record RoadmapRow(
            int year,
            int semester,
            String courseCode,
            String nameKo,
            String nameEn
    ) {}

    public record TermKey(int year, int semester) {
        public String toBucketKey() {
            // 1-1은 없음 (파일에도 없어야 함)
            return "y" + year + "s" + semester;
        }
    }

    private final Map<String, TermKey> byNormalizedName = new HashMap<>();

    public MajorRoadmapIndex(ObjectMapper objectMapper) throws IOException {
        var res = new ClassPathResource("catalog/major_roadmap.json");
        try (var is = res.getInputStream()) {
            RoadmapRow[] rows = objectMapper.readValue(is, RoadmapRow[].class);

            for (RoadmapRow r : rows) {
                var term = new TermKey(r.year(), r.semester());

                put(r.nameKo(), term);
                put(r.nameEn(), term);
            }
        }
    }

    public Optional<TermKey> findTermByCourseName(String courseName) {
        if (courseName == null) return Optional.empty();
        return Optional.ofNullable(byNormalizedName.get(norm(courseName)));
    }

    private void put(String name, TermKey term) {
        String k = norm(name);
        if (k.isBlank()) return;
        byNormalizedName.put(k, term);
    }

    // ✅ 공백만 제거 + trim
    private static String norm(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", "");
    }
}
