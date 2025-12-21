package com.example.gradu.domain.summary.service;

import com.example.gradu.domain.summary.dto.SummaryDto;
import com.example.gradu.domain.summary.dto.SummaryRowDto;
import com.example.gradu.domain.summary.dto.TogglesDto;
import com.example.gradu.domain.summary.entity.Summary;

import com.example.gradu.domain.summary.repository.SummaryRepository;
import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.json.SummaryJsonProcessingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final ObjectMapper om;
    private final SummaryRepository summaryRepository;
    private final SummaryCommandService summaryCommandService;

    @Transactional
    public SummaryDto getSummary(Long studentId) {
        return summaryRepository.findByStudentId(studentId)
                .map(this::toDto)
                .orElseGet(() -> summaryCommandService.recomputeAndSave(studentId));
    }

    @Transactional
    public void updateTogglesAndRecompute(Long studentId, TogglesDto toggles) {
        Summary summary = summaryRepository.findByStudentId(studentId)
                .orElseThrow(() -> new IllegalStateException("Summary not found for studentId=" + studentId));
        summary.updateToggles(toggles.gradEnglishPassed());
        summaryCommandService.recomputeAndSave(studentId);
    }

    private SummaryDto toDto(Summary e) {
        try {
            List<SummaryRowDto> rows = om.readValue(
                    e.getRowsJson() == null ? "[]" : e.getRowsJson(),
                    new TypeReference<>() {
                    }
            );

            return new SummaryDto(
                    rows,
                    e.getPfCredits(),
                    e.getPfLimit(),
                    e.isPfPass(),
                    e.getTotalCredits(),
                    e.isTotalPass(),
                    e.getGpa(),
                    e.getEngMajorCredits(),
                    e.getEngLiberalCredits(),
                    e.isEnglishPass(),
                    e.isGradEnglishPassed(),
                    e.isDeptExtraPassed(),
                    e.isFinalPass()
            );

        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new SummaryJsonProcessingException(ErrorCode.SUMMARY_JSON_PROCESSING_ERROR);
        }
    }

    @Transactional
    public void removeForStudent(Long studentId) {
        summaryRepository.deleteByStudentId(studentId);
    }
}
