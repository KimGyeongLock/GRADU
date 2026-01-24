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

    public CourseRankingService(CourseRankingRepository repository) {
        this.repository = repository;
    }

    public RankingResponse getCourseRanking() {
        return new RankingResponse(
                loadTop10(MAJOR_CATEGORIES),
                loadTop10(LIBERAL_CATEGORIES)
        );
    }

    private List<RankingItem> loadTop10(Set<Category> categories) {
        var rows = repository.findTopCoursesByCategories(
                categories,
                PageRequest.of(0, 10)
        );

        return IntStream.range(0, rows.size())
                .mapToObj(i -> new RankingItem(
                        i + 1,
                        rows.get(i).getName(),
                        rows.get(i).getTakenCount(),
                        0
                ))
                .toList();
    }
}
