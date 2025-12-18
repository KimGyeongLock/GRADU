package com.example.gradu.domain.summary.service;

import com.example.gradu.domain.summary.dto.SummaryDto;
import com.example.gradu.domain.summary.dto.SummaryRowDto;
import com.example.gradu.domain.summary.dto.TogglesDto;
import com.example.gradu.domain.summary.entity.Summary;

import com.example.gradu.domain.summary.repository.SummaryRepository;
import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.json.SummaryJsonProcessingException;
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
                    new TypeReference<List<SummaryRowDto>>() {}
            );

            return SummaryDto.builder()
                    .rows(rows)
                    .pfCredits(e.getPfCredits())
                    .pfLimit(e.getPfLimit())
                    .pfPass(e.isPfPass())
                    .totalCredits(e.getTotalCredits())
                    .totalPass(e.isTotalPass())
                    .gpa(e.getGpa())
                    .engMajorCredits(e.getEngMajorCredits())
                    .engLiberalCredits(e.getEngLiberalCredits())
                    .englishPass(e.isEnglishPass())
                    .gradEnglishPassed(e.isGradEnglishPassed())
                    .deptExtraPassed(e.isDeptExtraPassed())
                    .finalPass(e.isFinalPass())
                    .build();
        } catch (Exception ex) {
            throw new SummaryJsonProcessingException(ErrorCode.SUMMARY_JSON_PROCESSING_ERROR);
        }
    }

    @Transactional
    public void removeForStudent(Long studentId) {
        summaryRepository.deleteByStudentId(studentId);
    }
}
