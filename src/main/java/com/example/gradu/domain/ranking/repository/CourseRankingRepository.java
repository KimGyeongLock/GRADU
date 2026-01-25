package com.example.gradu.domain.ranking.repository;

import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Set;

public interface CourseRankingRepository extends JpaRepository<Course, Long> {

    interface CourseCountRow {
        String getName();
        long getTakenCount();
    }

    @Query("""
        select
           c.name as name,
           count(c.id) as takenCount
        from Course c
        where c.category in :categories
        group by c.name
        order by count(c.id) desc, c.name asc
   """)
    List<CourseCountRow> findTopCoursesByCategories(
            Set<Category> categories,
            Pageable pageable
    );
}
