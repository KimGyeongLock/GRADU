package com.example.gradu.domain.summary.service;

import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.course.repository.CourseRepository;
import com.example.gradu.domain.student.entity.Student;
import com.example.gradu.domain.student.repository.StudentRepository;
import com.example.gradu.domain.summary.dto.SummaryDto;
import com.example.gradu.domain.summary.entity.Summary;
import com.example.gradu.domain.summary.policy.SummaryPolicy;
import com.example.gradu.domain.summary.policy.SummaryPolicyService;
import com.example.gradu.domain.summary.repository.SummaryRepository;
import com.example.gradu.domain.summary.util.SummaryCalculator;
import com.example.gradu.global.exception.auth.AuthException;
import com.example.gradu.global.exception.json.SummaryJsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryCommandServiceTest {

    @Mock CourseRepository courseRepository;
    @Mock SummaryPolicyService policyService;
    @Mock StudentRepository studentRepository;
    @Mock SummaryRepository summaryRepository;
    @Mock ObjectMapper om;

    @InjectMocks SummaryCommandService service;

    @Test
    void recomputeAndSave_existingSummary_recomputesAndSaves() throws Exception {
        // given
        Long studentId = 1L;

        SummaryPolicy policy = mock(SummaryPolicy.class);
        List<Course> courses = List.of(mock(Course.class));
        Summary summary = mock(Summary.class);

        when(policyService.getActivePolicyFor()).thenReturn(policy);
        when(courseRepository.findByStudentId(studentId)).thenReturn(courses);
        when(summaryRepository.findByStudentId(studentId)).thenReturn(Optional.of(summary));

        SummaryDto calcDto = mock(SummaryDto.class);
        when(calcDto.rows()).thenReturn(List.of());
        when(om.writeValueAsString(any())).thenReturn("[]");

        try (var mocked = mockStatic(SummaryCalculator.class)) {
            mocked.when(() ->
                    SummaryCalculator.compute(any(), any(), anyBoolean())
            ).thenReturn(calcDto);

            // when
            SummaryDto result = service.recomputeAndSave(studentId);

            // then
            assertThat(result).isSameAs(calcDto);
            verify(summary).applyCalc(any());
            verify(summaryRepository).save(summary);
        }
    }

    @Test
    void recomputeAndSave_noSummary_createsFromStudent() throws Exception {
        // given
        Long studentId = 1L;

        when(summaryRepository.findByStudentId(studentId))
                .thenReturn(Optional.empty());

        Student student = mock(Student.class);
        when(studentRepository.findById(studentId))
                .thenReturn(Optional.of(student));

        Summary created = mock(Summary.class);
        when(created.isGradEnglishPassed()).thenReturn(false);

        try (var summaryMock = mockStatic(Summary.class);
             var calcMock = mockStatic(SummaryCalculator.class)) {

            summaryMock.when(() -> Summary.ofStudent(student))
                    .thenReturn(created);

            SummaryPolicy policy = mock(SummaryPolicy.class);
            when(policyService.getActivePolicyFor()).thenReturn(policy);
            when(courseRepository.findByStudentId(studentId)).thenReturn(List.of());

            SummaryDto dto = mock(SummaryDto.class);
            when(dto.rows()).thenReturn(List.of());
            when(om.writeValueAsString(any())).thenReturn("[]");

            calcMock.when(() ->
                    SummaryCalculator.compute(any(), any(), anyBoolean())
            ).thenReturn(dto);

            // when
            service.recomputeAndSave(studentId);

            // then
            verify(summaryRepository).save(created);
        }
    }

    @Test
    void recomputeAndSave_studentNotFound_throwsAuthException() {
        // given
        Long studentId = 1L;

        when(summaryRepository.findByStudentId(studentId))
                .thenReturn(Optional.empty());
        when(studentRepository.findById(studentId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.recomputeAndSave(studentId))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void recomputeAndSave_jsonWriteFails_throwsSummaryJsonProcessingException() throws Exception {
        // given
        Long studentId = 1L;

        Summary summary = mock(Summary.class);
        when(summary.isGradEnglishPassed()).thenReturn(false);

        when(summaryRepository.findByStudentId(studentId))
                .thenReturn(Optional.of(summary));

        when(policyService.getActivePolicyFor()).thenReturn(mock(SummaryPolicy.class));
        when(courseRepository.findByStudentId(studentId)).thenReturn(List.of());

        SummaryDto dto = mock(SummaryDto.class);
        when(dto.rows()).thenReturn(List.of());

        when(om.writeValueAsString(any()))
                .thenThrow(new RuntimeException("JSON fail"));

        try (var calcMock = mockStatic(SummaryCalculator.class)) {
            calcMock.when(() ->
                    SummaryCalculator.compute(any(), any(), anyBoolean())
            ).thenReturn(dto);

            // when & then
            assertThatThrownBy(() -> service.recomputeAndSave(studentId))
                    .isInstanceOf(SummaryJsonProcessingException.class);
        }
    }
}
