package com.example.gradu.domain.ranking.service;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.ranking.dto.CourseRankingDto.RankingResponse;
import com.example.gradu.domain.ranking.repository.CourseRankingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static com.example.gradu.domain.ranking.dto.CourseRankingDto.*;

@Service
public class CourseRankingService {

    private final CourseRankingRepository repository;

    private static final EnumSet<Category> MAJOR_CATEGORIES =
            EnumSet.of(Category.MAJOR);

    private static final EnumSet<Category> LIBERAL_CATEGORIES =
            EnumSet.complementOf(MAJOR_CATEGORIES);

    // ✅ 교양 랭킹에서 제외할 키워드
    private static final List<String> LIBERAL_EXCLUDE_KEYWORDS =
            List.of("공동체리더십훈련", "채플");

    public CourseRankingService(CourseRankingRepository repository) {
        this.repository = repository;
    }

    public RankingResponse getCourseRanking() {
        return new RankingResponse(
                loadTop10(MAJOR_CATEGORIES, false),
                loadTop10(LIBERAL_CATEGORIES, true)
        );
    }

    private List<RankingItem> loadTop10(Set<Category> categories, boolean applyLiberalFilter) {
        int fetchSize = applyLiberalFilter ? 30 : 10;

        var fetchedRows = repository.findTopCoursesByCategories(
                categories,
                PageRequest.of(0, fetchSize)
        );

        // ✅ 여기서 최종 리스트를 한 번만 결정
        final List<? extends CourseRankingRepository.CourseCountRow> finalRows;

        if (applyLiberalFilter) {
            finalRows = fetchedRows.stream()
                    .filter(r -> !containsAny(r.getName(), LIBERAL_EXCLUDE_KEYWORDS))
                    .limit(10)
                    .toList();
        } else {
            finalRows = fetchedRows.stream()
                    .limit(10)
                    .toList();
        }

        // ✅ finalRows는 effectively final → 람다에서 안전
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
