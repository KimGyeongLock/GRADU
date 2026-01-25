package com.example.gradu.domain.ranking.service;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.ranking.catalog.MajorRoadmapIndex;
import com.example.gradu.domain.ranking.dto.CourseRankingDto.*;
import com.example.gradu.domain.ranking.repository.CourseRankingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.IntStream;

@Service
public class CourseRankingService {

    private final CourseRankingRepository repository;
    private final MajorRoadmapIndex majorRoadmapIndex;

    private static final EnumSet<Category> MAJOR_CATEGORIES =
            EnumSet.of(Category.MAJOR);

    private static final EnumSet<Category> LIB_FAITH =
            EnumSet.of(Category.FAITH_WORLDVIEW);

    private static final EnumSet<Category> LIB_GENERAL_EDU =
            EnumSet.of(Category.GENERAL_EDU);

    private static final EnumSet<Category> LIB_BSM =
            EnumSet.of(Category.BSM);

    private static final EnumSet<Category> LIB_FREE_ELECTIVE =
            EnumSet.of(Category.FREE_ELECTIVE_BASIC, Category.FREE_ELECTIVE_MJR);

    private static final List<String> LIBERAL_EXCLUDE_KEYWORDS =
            List.of("공동체리더십훈련", "채플");

    public CourseRankingService(
            CourseRankingRepository repository,
            MajorRoadmapIndex majorRoadmapIndex
    ) {
        this.repository = repository;
        this.majorRoadmapIndex = majorRoadmapIndex;
    }

    public RankingResponse getCourseRanking() {
        var major = loadMajorByTerms();

        var liberal = new LiberalRanking(
                loadTop10Merged(LIB_FAITH, true),
                loadTop10Merged(LIB_GENERAL_EDU, true),
                loadTop10Merged(LIB_BSM, true),
                loadTop10Merged(LIB_FREE_ELECTIVE, true)
        );

        return new RankingResponse(major, liberal);
    }

    /**
     * ✅ 전공: MAJOR에서 많이 가져온 뒤 로드맵(term)으로 분류해서 각 term Top10 생성
     * - 1-1은 없음 → y1s2부터 생성
     * - 같은 과목(공백 차이)은 term 버킷 안에서 합산
     */
    private MajorRanking loadMajorByTerms() {
        Map<String, List<CourseRankingRepository.CourseCountRow>> bucket = new LinkedHashMap<>();
        bucket.put("y1s2", new ArrayList<>());
        bucket.put("y2s1", new ArrayList<>());
        bucket.put("y2s2", new ArrayList<>());
        bucket.put("y3s1", new ArrayList<>());
        bucket.put("y3s2", new ArrayList<>());
        bucket.put("y4s1", new ArrayList<>());
        bucket.put("y4s2", new ArrayList<>());

        // ✅ 전공은 term별로 쪼개야 하므로 충분히 많이 가져오기
        var fetchedRows = repository.findTopCoursesByCategories(
                MAJOR_CATEGORIES,
                PageRequest.of(0, 300)
        );

        for (var r : fetchedRows) {
            var termOpt = majorRoadmapIndex.findTermByCourseName(r.getName());
            if (termOpt.isEmpty()) continue;

            var key = termOpt.get().toBucketKey();
            var list = bucket.get(key);
            if (list != null) list.add(r);
        }

        return new MajorRanking(
                toTop10Merged(bucket.get("y1s2")),
                toTop10Merged(bucket.get("y2s1")),
                toTop10Merged(bucket.get("y2s2")),
                toTop10Merged(bucket.get("y3s1")),
                toTop10Merged(bucket.get("y3s2")),
                toTop10Merged(bucket.get("y4s1")),
                toTop10Merged(bucket.get("y4s2"))
        );
    }

    /**
     * ✅ 교양/전공 공통: rows를 "공백 제거 키"로 합산 → takenCount desc 정렬 → top10 → rank 부여
     */
    private List<RankingItem> toTop10Merged(List<CourseRankingRepository.CourseCountRow> rows) {
        if (rows == null || rows.isEmpty()) return List.of();

        // normKey -> (sumCount, displayName)
        Map<String, Agg> map = new HashMap<>();

        for (var r : rows) {
            String raw = r.getName();
            String key = norm(raw);
            if (key.isBlank()) continue;

            Agg prev = map.get(key);
            long nextCount = (prev == null ? 0L : prev.takenCount) + r.getTakenCount();

            // ✅ 대표 표기명: 사람이 읽기 편한 쪽(공백 포함, 더 긴 것) 우선
            String nextDisplay = (prev == null)
                    ? safe(raw)
                    : chooseDisplay(prev.displayName, safe(raw));

            map.put(key, new Agg(nextCount, nextDisplay));
        }

        var merged = map.values().stream()
                .sorted((a, b) -> {
                    int c = Long.compare(b.takenCount, a.takenCount);
                    if (c != 0) return c;
                    return a.displayName.compareTo(a.displayName);
                })
                .limit(10)
                .toList();

        return IntStream.range(0, merged.size())
                .mapToObj(i -> new RankingItem(
                        i + 1,
                        merged.get(i).displayName,
                        merged.get(i).takenCount,
                        0
                ))
                .toList();
    }

    /**
     * ✅ 교양: 카테고리별로 가져온 뒤(필요시 필터) → 합산 top10
     */
    private List<RankingItem> loadTop10Merged(Set<Category> categories, boolean applyFilter) {
        int fetchSize = applyFilter ? 30 : 10;

        var fetchedRows = repository.findTopCoursesByCategories(
                categories,
                PageRequest.of(0, fetchSize)
        );

        var filtered = applyFilter
                ? fetchedRows.stream()
                .filter(r -> !containsAny(r.getName(), LIBERAL_EXCLUDE_KEYWORDS))
                .toList()
                : fetchedRows;

        return toTop10Merged(filtered);
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (text == null) return false;
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    // =======================
    // normalize / display util
    // =======================

    // ✅ 공백만 제거 + trim (웹서비스개발 / 웹 서비스 개발 동일 키)
    private static String norm(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", "");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    // 더 읽기 좋은 표기(대개 공백 포함이 더 길다) 우선
    private static String chooseDisplay(String a, String b) {
        if (a == null || a.isBlank()) return safe(b);
        if (b == null || b.isBlank()) return safe(a);
        if (b.length() > a.length()) return b;
        return a;
    }

    private static final class Agg {
        final long takenCount;
        final String displayName;

        private Agg(long takenCount, String displayName) {
            this.takenCount = takenCount;
            this.displayName = displayName;
        }
    }
}
