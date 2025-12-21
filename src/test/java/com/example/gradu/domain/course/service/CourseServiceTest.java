package com.example.gradu.domain.course.service;

import com.example.gradu.domain.capture_ai.dto.CourseBulkRequest;
import com.example.gradu.domain.course.dto.CourseRequestDto;
import com.example.gradu.domain.course.dto.CourseUpdateRequestDto;
import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.course.entity.Term;
import com.example.gradu.domain.course.repository.CourseRepository;
import com.example.gradu.domain.curriculum.entity.Category;
import com.example.gradu.domain.curriculum.entity.Curriculum;
import com.example.gradu.domain.curriculum.repository.CurriculumRepository;
import com.example.gradu.domain.student.entity.Student;
import com.example.gradu.domain.student.repository.StudentRepository;
import com.example.gradu.domain.summary.service.SummaryCommandService;
import com.example.gradu.global.exception.course.CourseException;
import com.example.gradu.global.exception.student.StudentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock StudentRepository studentRepository;
    @Mock CurriculumRepository curriculumRepository;
    @Mock CourseCommandService courseCommandService;
    @Mock SummaryCommandService summaryCommandService;

    @InjectMocks CourseService courseService;

    private Student student() {
        return mock(Student.class);
    }

    private CourseRequestDto req(String name, BigDecimal credit, Category category, int designed, boolean isEnglish, String grade, short year, String term) {
        return new CourseRequestDto(name, credit, category, designed, isEnglish, grade, year, term);
    }

    @Test
    void addCourse_studentNotFound_throws() {
        // given: 항상 학생 없음을 반환
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        Throwable thrown = catchThrowable(() ->
                courseService.addCourse(
                        1L,
                        req("A", BigDecimal.valueOf(3), Category.GENERAL_EDU, 0, false, "A+", (short) 2024, "1"),
                        false
                )
        );

        // then
        assertThat(thrown).isInstanceOf(StudentException.class);
        verifyNoInteractions(courseRepository, curriculumRepository, courseCommandService, summaryCommandService);
    }

    @Test
    void addCourse_duplicateWithoutOverwrite_throws() {
        // given
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student()));
        when(courseRepository.findByStudentIdAndNameAndCategoryAndAcademicYearAndTerm(
                1L, "DB", Category.MAJOR, (short)2024, Term.fromCode("1")
        )).thenReturn(Optional.of(mock(Course.class)));

        // when
        var dto = req("DB", BigDecimal.valueOf(3), Category.MAJOR, 0, false, "A+", (short) 2024, "1");

        // then
        assertThatThrownBy(() -> courseService.addCourse(1L, dto, false))
                .isInstanceOf(CourseException.class);

        verify(courseRepository, never()).save(any());
        verify(courseCommandService, never()).updateCourse(anyLong(), anyLong(), any(CourseUpdateRequestDto.class));
        verify(summaryCommandService, never()).recomputeAndSave(anyLong());
    }


    @Test
    void addCourse_duplicateWithOverwrite_callsUpdateAndReturns() {
        // given
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student()));

        Course existing = mock(Course.class);
        when(existing.getId()).thenReturn(99L);

        when(courseRepository.findByStudentIdAndNameAndCategoryAndAcademicYearAndTerm(
                1L,
                "DB",
                Category.MAJOR,
                (short) 2024,
                Term.fromCode("1")
        )).thenReturn(Optional.of(existing));

        // when
        courseService.addCourse(
                1L,
                req("DB", BigDecimal.valueOf(3), Category.MAJOR, 0, false, "A+", (short) 2024, "1"),
                true
        );

        // then
        verify(courseCommandService)
                .updateCourse(eq(1L), eq(99L), any(CourseUpdateRequestDto.class));

        verify(courseRepository, never()).save(any());
        verify(curriculumRepository, never()).findByStudentIdAndCategory(anyLong(), any());
        verify(summaryCommandService, never()).recomputeAndSave(anyLong());
    }


    @Test
    void addCourse_newCourse_updatesCurriculum_andRecomputes() {
        // given
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student()));
        when(courseRepository.findByStudentIdAndNameAndCategoryAndAcademicYearAndTerm(anyLong(), anyString(), any(), any(), any()))
                .thenReturn(Optional.empty());

        Curriculum cur = mock(Curriculum.class);
        when(curriculumRepository.findByStudentIdAndCategory(1L, Category.GENERAL_EDU)).thenReturn(Optional.of(cur));

        // when: 3학점 -> units 6
        courseService.addCourse(1L, req("교양", BigDecimal.valueOf(3), Category.GENERAL_EDU, 0, false, "A+", (short) 2024, "1"), false);

        // then
        verify(courseRepository).save(any(Course.class));
        verify(cur).addEarnedCredits(6);
        verify(summaryCommandService).recomputeAndSave(1L);
    }

    @Test
    void addCourse_majorAlsoUpdatesMajorDesigned() {
        // given
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student()));
        when(courseRepository.findByStudentIdAndNameAndCategoryAndAcademicYearAndTerm(anyLong(), anyString(), any(), any(), any()))
                .thenReturn(Optional.empty());

        Curriculum majorCur = mock(Curriculum.class);
        Curriculum designedCur = mock(Curriculum.class);

        when(curriculumRepository.findByStudentIdAndCategory(1L, Category.MAJOR)).thenReturn(Optional.of(majorCur));
        when(curriculumRepository.findByStudentIdAndCategory(1L, Category.MAJOR_DESIGNED)).thenReturn(Optional.of(designedCur));

        // when: 전공 3학점 + 설계 2
        courseService.addCourse(1L,
                req("전공", BigDecimal.valueOf(3), Category.MAJOR, 2, false, "A+", (short) 2024, "1"), false);

        // then: MAJOR는 credit units(+6), MAJOR_DESIGNED는 designedCredit(+2)
        verify(majorCur).addEarnedCredits(6);
        verify(designedCur).addEarnedCredits(2);
        verify(summaryCommandService).recomputeAndSave(1L);
    }

    @Test
    void bulkInsert_duplicateDetected_throws_andDoesNotSave() {
        // given
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student()));

        Course existing = mock(Course.class);
        when(existing.getName()).thenReturn("DB");
        when(existing.getCategory()).thenReturn(Category.MAJOR);
        when(existing.getAcademicYear()).thenReturn((short)2024);
        when(existing.getTerm()).thenReturn(Term.fromCode("1"));

        when(courseRepository.findByStudentId(1L)).thenReturn(List.of(existing));

        CourseBulkRequest dupReq = mock(CourseBulkRequest.class);
        when(dupReq.getName()).thenReturn("DB");
        when(dupReq.getCategory()).thenReturn(Category.MAJOR);
        when(dupReq.getAcademicYear()).thenReturn((short)2024);
        when(dupReq.getTerm()).thenReturn("1");

        // when
        List<CourseBulkRequest> reqs = List.of(dupReq);

        //  then
        assertThatThrownBy(() -> courseService.bulkInsert(1L, reqs))
                .isInstanceOf(CourseException.class);

        verify(courseRepository, never()).saveAll(anyList());
        verify(summaryCommandService, never()).recomputeAndSave(anyLong());
    }

    @Test
    void bulkInsert_success_updatesCurriculumGrouped_andDesigned_thenRecomputes() {
        // given
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student()));
        when(courseRepository.findByStudentId(1L)).thenReturn(List.of()); // no duplicates

        CourseBulkRequest r1 = mock(CourseBulkRequest.class);
        when(r1.getName()).thenReturn("전공1");
        when(r1.getCategory()).thenReturn(Category.MAJOR);
        when(r1.getCredit()).thenReturn(BigDecimal.valueOf(3));
        when(r1.getDesignedCredit()).thenReturn(2);
        when(r1.getGrade()).thenReturn("A0");
        when(r1.isEnglish()).thenReturn(false);
        when(r1.getAcademicYear()).thenReturn((short)2024);
        when(r1.getTerm()).thenReturn("1");

        CourseBulkRequest r2 = mock(CourseBulkRequest.class);
        when(r2.getName()).thenReturn("교양1");
        when(r2.getCategory()).thenReturn(Category.GENERAL_EDU);
        when(r2.getCredit()).thenReturn(BigDecimal.valueOf(2));
        when(r2.getDesignedCredit()).thenReturn(null);
        when(r2.getGrade()).thenReturn("A0");
        when(r2.isEnglish()).thenReturn(false);
        when(r2.getAcademicYear()).thenReturn((short)2024);
        when(r2.getTerm()).thenReturn("1");

        Curriculum majorCur = mock(Curriculum.class);
        Curriculum genCur = mock(Curriculum.class);
        Curriculum designedCur = mock(Curriculum.class);

        when(curriculumRepository.findByStudentIdAndCategory(1L, Category.MAJOR)).thenReturn(Optional.of(majorCur));
        when(curriculumRepository.findByStudentIdAndCategory(1L, Category.GENERAL_EDU)).thenReturn(Optional.of(genCur));
        when(curriculumRepository.findByStudentIdAndCategory(1L, Category.MAJOR_DESIGNED)).thenReturn(Optional.of(designedCur));

        // when
        courseService.bulkInsert(1L, List.of(r1, r2));

        // then
        verify(courseRepository).saveAll(anyList());
        // MAJOR: 3학점 -> 6 units
        verify(majorCur).addEarnedCredits(6);
        // GENERAL_EDU: 2학점 -> 4 units
        verify(genCur).addEarnedCredits(4);
        // 설계: 2
        verify(designedCur).addEarnedCredits(2);
        verify(summaryCommandService).recomputeAndSave(1L);
    }
}
