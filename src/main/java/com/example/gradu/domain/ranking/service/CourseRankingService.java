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

    public CourseRankingService(CourseRankingRepository repository, MajorRoadmapIndex majorRoadmapIndex) {
        this.repository = repository;
        this.majorRoadmapIndex = majorRoadmapIndex;
    }

    public RankingResponse getCourseRanking() {
        var major = loadMajorByTerms(); // ✅ 변경

        var liberal = new LiberalRanking(
                loadTop10(LIB_FAITH, true),
                loadTop10(LIB_GENERAL_EDU, true),
                loadTop10(LIB_BSM, true),
                loadTop10(LIB_FREE_ELECTIVE, true)
        );

        return new RankingResponse(major, liberal);
    }

    /**
     * ✅ 전공: MAJOR에서 많이 가져온 뒤 로드맵(term)으로 분류해서 각 term Top10 생성
     * 1-1은 없음 → y1s2부터 생성
     */
    private MajorRanking loadMajorByTerms() {
        // term별 버킷 (순서 고정)
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
            if (list != null) list.add(r); // 혹시 1-1 같은게 들어오면 null이라 무시
        }

        return new MajorRanking(
                toTop10(bucket.get("y1s2")),
                toTop10(bucket.get("y2s1")),
                toTop10(bucket.get("y2s2")),
                toTop10(bucket.get("y3s1")),
                toTop10(bucket.get("y3s2")),
                toTop10(bucket.get("y4s1")),
                toTop10(bucket.get("y4s2"))
        );
    }

    private List<RankingItem> toTop10(List<CourseRankingRepository.CourseCountRow> rows) {
        if (rows == null) return List.of();
        var finalRows = rows.stream().limit(10).toList();

        return IntStream.range(0, finalRows.size())
                .mapToObj(i -> new RankingItem(
                        i + 1,
                        finalRows.get(i).getName(),
                        finalRows.get(i).getTakenCount(),
                        0
                ))
                .toList();
    }

    private List<RankingItem> loadTop10(Set<Category> categories, boolean applyFilter) {
        int fetchSize = applyFilter ? 30 : 10;

        var fetchedRows = repository.findTopCoursesByCategories(
                categories,
                PageRequest.of(0, fetchSize)
        );

        final var finalRows = applyFilter
                ? fetchedRows.stream()
                .filter(r -> !containsAny(r.getName(), LIBERAL_EXCLUDE_KEYWORDS))
                .limit(10)
                .toList()
                : fetchedRows.stream()
                .limit(10)
                .toList();

        return IntStream.range(0, finalRows.size())
                .mapToObj(i -> new RankingItem(
                        i + 1,
                        finalRows.get(i).getName(),
                        finalRows.get(i).getTakenCount(),
                        0
                ))
                .toList();
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (text == null) return false;
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
