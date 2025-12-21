package com.example.gradu.domain.summary.service;

import com.example.gradu.domain.summary.dto.SummaryDto;
import com.example.gradu.domain.summary.dto.SummaryRowDto;
import com.example.gradu.domain.summary.dto.TogglesDto;
import com.example.gradu.domain.summary.entity.Summary;
import com.example.gradu.domain.summary.repository.SummaryRepository;
import com.example.gradu.global.exception.json.SummaryJsonProcessingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock ObjectMapper om;
    @Mock SummaryRepository summaryRepository;
    @Mock SummaryCommandService summaryCommandService;

    @InjectMocks SummaryService service;

    @Test
    void getSummary_existingSummary_returnsDto() throws Exception {
        // given
        Summary summary = mock(Summary.class);
        when(summaryRepository.findByStudentId(1L)).thenReturn(Optional.of(summary));
        when(summary.getRowsJson()).thenReturn("[]");
        when(om.readValue(eq("[]"), any(TypeReference.class))).thenReturn(List.of());
        when(summary.getPfCredits()).thenReturn(0.0);
        when(summary.getPfLimit()).thenReturn(0.0);
        when(summary.isPfPass()).thenReturn(false);
        when(summary.getTotalCredits()).thenReturn(0.0);
        when(summary.isTotalPass()).thenReturn(false);
        when(summary.getGpa()).thenReturn(0.0);
        when(summary.getEngMajorCredits()).thenReturn(0);
        when(summary.getEngLiberalCredits()).thenReturn(0);
        when(summary.isEnglishPass()).thenReturn(false);
        when(summary.isGradEnglishPassed()).thenReturn(false);
        when(summary.isDeptExtraPassed()).thenReturn(false);
        when(summary.isFinalPass()).thenReturn(false);

        // when
        SummaryDto result = service.getSummary(1L);

        // then
        assertThat(result).isNotNull();
        verify(summaryCommandService, never()).recomputeAndSave(anyLong());
    }

    @Test
    void getSummary_noSummary_callsRecomputeAndSave() {
        // given
        when(summaryRepository.findByStudentId(1L))
                .thenReturn(Optional.empty());

        SummaryDto dto = mock(SummaryDto.class);
        when(summaryCommandService.recomputeAndSave(1L))
                .thenReturn(dto);

        // when
        SummaryDto result = service.getSummary(1L);

        // then
        assertThat(result).isSameAs(dto);
        verify(summaryCommandService).recomputeAndSave(1L);
        verifyNoInteractions(om);
    }

    @Test
    void getSummary_existingSummary_rowsJsonNull_treatedAsEmptyArray() throws Exception {
        // given
        Summary summary = mock(Summary.class);
        when(summaryRepository.findByStudentId(1L)).thenReturn(Optional.of(summary));
        when(summary.getRowsJson()).thenReturn(null);

        when(om.readValue(eq("[]"), any(TypeReference.class)))
                .thenReturn(List.of(SummaryRowDto.builder().key("k").name("n").build()));

        when(summary.getPfCredits()).thenReturn(0.0);
        when(summary.getPfLimit()).thenReturn(0.0);
        when(summary.isPfPass()).thenReturn(false);
        when(summary.getTotalCredits()).thenReturn(0.0);
        when(summary.isTotalPass()).thenReturn(false);
        when(summary.getGpa()).thenReturn(0.0);
        when(summary.getEngMajorCredits()).thenReturn(0);
        when(summary.getEngLiberalCredits()).thenReturn(0);
        when(summary.isEnglishPass()).thenReturn(false);
        when(summary.isGradEnglishPassed()).thenReturn(false);
        when(summary.isDeptExtraPassed()).thenReturn(false);
        when(summary.isFinalPass()).thenReturn(false);

        // when
        SummaryDto dto = service.getSummary(1L);

        // then
        assertThat(dto).isNotNull();
    }

    @Test
    void getSummary_existingSummary_jsonProcessingFails_throwsSummaryJsonProcessingException() throws Exception {
        // given
        Summary summary = mock(Summary.class);
        when(summaryRepository.findByStudentId(1L)).thenReturn(Optional.of(summary));
        when(summary.getRowsJson()).thenReturn("[]");

        when(om.readValue(eq("[]"), any(TypeReference.class)))
                .thenThrow(new JsonProcessingException("boom") {});

        // when & then
        assertThatThrownBy(() -> service.getSummary(1L))
                .isInstanceOf(SummaryJsonProcessingException.class);

        verify(summaryCommandService, never()).recomputeAndSave(anyLong());
    }

    @Test
    void updateTogglesAndRecompute_updatesToggle_andRecomputes() {
        // given
        Summary summary = mock(Summary.class);
        when(summaryRepository.findByStudentId(1L)).thenReturn(Optional.of(summary));

        // when
        service.updateTogglesAndRecompute(1L, new TogglesDto(true));

        // then
        verify(summary).updateToggles(true);
        verify(summaryCommandService).recomputeAndSave(1L);
    }

    @Test
    void removeForStudent_deletesSummary() {
        // when
        service.removeForStudent(1L);

        // then
        verify(summaryRepository).deleteByStudentId(1L);
    }
}
