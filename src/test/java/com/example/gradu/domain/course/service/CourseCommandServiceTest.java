package com.example.gradu.domain.course.service;

import com.example.gradu.domain.course.dto.CourseUpdateRequestDto;
import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.course.repository.CourseRepository;
import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.curriculum.entity.Curriculum;
import com.example.gradu.domain.curriculum.repository.CurriculumRepository;
import com.example.gradu.domain.summary.service.SummaryCommandService;
import com.example.gradu.global.exception.course.CourseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseCommandServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock CurriculumRepository curriculumRepository;
    @Mock SummaryCommandService summaryCommandService;

    @InjectMocks CourseCommandService courseCommandService;

    private Course courseForDuplicateName(Category cat) {
        Course c = mock(Course.class);
        when(c.getCategory()).thenReturn(cat);
        when(c.getName()).thenReturn("OLD");
        return c;
    }

    private Course courseForCredit(Category cat, BigDecimal credit, Integer designed) {
        Course c = mock(Course.class);
        when(c.getCategory()).thenReturn(cat);
        when(c.getCredit()).thenReturn(credit);
        when(c.getDesignedCredit()).thenReturn(designed);
        return c;
    }

    @Test
    void updateCourse_courseNotFound_throws() {
        // given
        when(courseRepository.findByIdAndStudentId(10L, 1L)).thenReturn(Optional.empty());

        // when
        CourseUpdateRequestDto req = new CourseUpdateRequestDto(
                "NEW", BigDecimal.valueOf(3), 0, "A0", Category.GENERAL_EDU, false, (short)2024, "1"
        );

        // then: 해당하는 과목 없음
        assertThatThrownBy(() -> courseCommandService.updateCourse(1L, 10L, req))
                .isInstanceOf(CourseException.class);

        verifyNoInteractions(curriculumRepository, summaryCommandService);
    }

    @Test
    void updateCourse_duplicateName_throwsBeforeAnyChanges() {
        // given
        Course c = courseForDuplicateName(Category.GENERAL_EDU);

        when(courseRepository.findByIdAndStudentId(10L, 1L)).thenReturn(Optional.of(c));
        when(courseRepository.existsByStudentIdAndNameAndCategoryAndAcademicYearAndTermAndIdNot(
                eq(1L), eq("NEW"), any(), any(), any(), eq(10L)
        )).thenReturn(true);

        // when
        CourseUpdateRequestDto req = new CourseUpdateRequestDto(
                "NEW", null, null, null, null, false, null, null
        );

        // then: 이미 존재 하는 이름으로 변경할 경우 중복 에러
        assertThatThrownBy(() -> courseCommandService.updateCourse(1L, 10L, req))
                .isInstanceOf(CourseException.class);

        verifyNoInteractions(curriculumRepository);
        verify(summaryCommandService, never()).recomputeAndSave(anyLong());
    }

    @Test
    void updateCourse_sameCategory_creditDelta_updatesSameCurriculum() {
        // given
        Course c = courseForCredit(Category.GENERAL_EDU, BigDecimal.valueOf(3), 1);
        when(courseRepository.findByIdAndStudentId(10L, 1L)).thenReturn(Optional.of(c));

        Curriculum cur = mock(Curriculum.class);
        when(curriculumRepository.findByStudentIdAndCategory(1L, Category.GENERAL_EDU)).thenReturn(Optional.of(cur));

        // when: 3 -> 4 학점 : +1학점 = +2 units
        CourseUpdateRequestDto req = new CourseUpdateRequestDto(
                null, BigDecimal.valueOf(4), null, null, Category.GENERAL_EDU, false, null, null
        );

        courseCommandService.updateCourse(1L, 10L, req);

        // then: 학점 업데이트
        verify(cur).addEarnedCredits(2);
        verify(c).changeCredit(BigDecimal.valueOf(4));
        verify(summaryCommandService).recomputeAndSave(1L);
    }

    @Test
    void updateCourse_majorDesignedDelta_updatesMajorDesignedCurriculum() {
        // given
        Course c = courseForCredit(Category.MAJOR, BigDecimal.valueOf(3), 1);
        when(courseRepository.findByIdAndStudentId(10L, 1L)).thenReturn(Optional.of(c));

        Curriculum designedCur = mock(Curriculum.class);
        when(curriculumRepository.findByStudentIdAndCategory(1L, Category.MAJOR_DESIGNED)).thenReturn(Optional.of(designedCur));

        // when: category 그대로 MAJOR, designed 1 -> 3 (delta +2), credit 변화 없음
        CourseUpdateRequestDto req = new CourseUpdateRequestDto(
                null, BigDecimal.valueOf(3), 3, null, Category.MAJOR, false, null, null
        );

        courseCommandService.updateCourse(1L, 10L, req);

        // then: 설계학점만 변경, 총학점에는 영향이 없음
        verify(designedCur).addEarnedCredits(2);
        verify(c).changeDesignedCredit(3);
        verify(summaryCommandService).recomputeAndSave(1L);
    }

    @Test
    void updateCourse_categoryChange_movesCredits_betweenCurriculums_andHandlesDesigned() {
        // given
        Course c = courseForCredit(Category.GENERAL_EDU, BigDecimal.valueOf(3), 1);
        when(courseRepository.findByIdAndStudentId(10L, 1L)).thenReturn(Optional.of(c));

        Curriculum prev = mock(Curriculum.class);
        Curriculum next = mock(Curriculum.class);
        Curriculum designed = mock(Curriculum.class);

        when(curriculumRepository.findByStudentIdAndCategory(1L, Category.GENERAL_EDU)).thenReturn(Optional.of(prev));
        when(curriculumRepository.findByStudentIdAndCategory(1L, Category.MAJOR)).thenReturn(Optional.of(next));
        when(curriculumRepository.findByStudentIdAndCategory(1L, Category.MAJOR_DESIGNED)).thenReturn(Optional.of(designed));

        // when: GENERAL_EDU(3학점) -> MAJOR(4학점, designed 2)
        CourseUpdateRequestDto req = new CourseUpdateRequestDto(
                null, BigDecimal.valueOf(4), 2, null, Category.MAJOR, false, null, null
        );

        courseCommandService.updateCourse(1L, 10L, req);

        // then: prev에서 -3학점(-6units), next에 +4학점(+8units)
        verify(prev).addEarnedCredits(-6);
        verify(next).addEarnedCredits(8);
        // 새 카테고리가 MAJOR이므로 설계 +2
        verify(designed).addEarnedCredits(2);

        verify(c).changeCategory(Category.MAJOR);
        verify(c).changeCredit(BigDecimal.valueOf(4));
        verify(c).changeDesignedCredit(2);
        verify(summaryCommandService).recomputeAndSave(1L);
    }

    @Test
    void deleteCourse_major_removesCredits_andDesigned_thenDeletes_andRecomputes() {
        // given
        Course c = courseForCredit(Category.MAJOR, BigDecimal.valueOf(3), 2);
        when(courseRepository.findByIdAndStudentId(10L, 1L)).thenReturn(Optional.of(c));

        Curriculum majorCur = mock(Curriculum.class);
        Curriculum designedCur = mock(Curriculum.class);

        when(curriculumRepository.findByStudentIdAndCategory(1L, Category.MAJOR)).thenReturn(Optional.of(majorCur));
        when(curriculumRepository.findByStudentIdAndCategory(1L, Category.MAJOR_DESIGNED)).thenReturn(Optional.of(designedCur));

        // when
        courseCommandService.deleteCourse(1L, 10L);

        // then
        verify(majorCur).addEarnedCredits(-6);    // 3학점 -> -6 units
        verify(designedCur).addEarnedCredits(-2); // designed -2
        verify(courseRepository).delete(c);
        verify(summaryCommandService).recomputeAndSave(1L);
    }
}
