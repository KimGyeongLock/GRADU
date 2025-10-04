package com.example.gradu.domain.summary.service;

import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.course.repository.CourseRepository;
import com.example.gradu.domain.summary.dto.SummaryDto;
import com.example.gradu.domain.summary.dto.SummaryRowDto;
import com.example.gradu.domain.summary.dto.TogglesDto;
import com.example.gradu.domain.summary.entity.Summary;
import com.example.gradu.domain.summary.policy.SummaryPolicy;
import com.example.gradu.domain.summary.policy.SummaryPolicyService;
import com.example.gradu.domain.summary.repository.SummaryRepository;
import com.example.gradu.domain.summary.util.SummaryCalculator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final CourseRepository courseRepo;
    private final SummaryRepository summaryRepo;
    private final SummaryPolicyService policyService;
    private final ObjectMapper om = new ObjectMapper();

    @Transactional
    public SummaryDto getSummary(String sid) {
        return summaryRepo.findByStudentId(sid)
                .map(this::toDto)
                .orElseGet(() -> recomputeAndSave(sid));
    }

    @Transactional
    public SummaryDto recomputeAndSave(String sid) {
        SummaryPolicy policy = policyService.getActivePolicyFor(sid);
        List<Course> courses = courseRepo.findByStudentStudentId(sid);

        // 토글은 저장본 있으면 유지
        boolean gradEnglishPassed = summaryRepo.findByStudentId(sid)
                .map(Summary::isGradEnglishPassed).orElse(false);
        boolean deptExtraPassed = summaryRepo.findByStudentId(sid)
                .map(Summary::isDeptExtraPassed).orElse(false);

        SummaryDto calc = SummaryCalculator.compute(courses, policy, gradEnglishPassed, deptExtraPassed);

        Summary entity = summaryRepo.findByStudentId(sid)
                .orElse(Summary.ofStudent(sid));

        String rowsJson;
        try {
            rowsJson = om.writeValueAsString(calc.getRows());
        } catch (Exception e) {
            throw new RuntimeException("rows serialize failed", e);
        }

        // ✅ 도메인 메서드 한 번으로 반영
        entity.applyCalc(
                calc.getPfCredits(), calc.getPfLimit(), calc.isPfPass(),
                calc.getTotalCredits(), calc.isTotalPass(),
                calc.getGpa(),
                calc.getEngMajorCredits(), calc.getEngLiberalCredits(), calc.isEnglishPass(),
                calc.isGradEnglishPassed(), calc.isDeptExtraPassed(), calc.isFinalPass(),
                rowsJson
        );

        summaryRepo.save(entity);
        return calc;
    }

    @Transactional
    public void updateTogglesAndRecompute(String sid, TogglesDto toggles) {
        Summary snap = summaryRepo.findByStudentId(sid)
                .orElse(Summary.ofStudent(sid));

        // ✅ setter 대신 도메인 메서드
        snap.updateToggles(toggles.gradEnglishPassed(), toggles.deptExtraPassed());
        summaryRepo.save(snap);

        // 토글 반영해서 즉시 재계산
        recomputeAndSave(sid);
    }

    // ---- entity -> dto
    private SummaryDto toDto(Summary e) {
        try {
            List<SummaryRowDto> rows = om.readValue(
                    e.getRowsJson() == null ? "[]" : e.getRowsJson(),
                    new TypeReference<List<SummaryRowDto>>() {}
            );
            return SummaryDto.builder()
                    .rows(rows)
                    .pfCredits(e.getPfCredits()).pfLimit(e.getPfLimit()).pfPass(e.isPfPass())
                    .totalCredits(e.getTotalCredits()).totalPass(e.isTotalPass())
                    .gpa(e.getGpa())
                    .engMajorCredits(e.getEngMajorCredits()).engLiberalCredits(e.getEngLiberalCredits())
                    .englishPass(e.isEnglishPass())
                    .gradEnglishPassed(e.isGradEnglishPassed()).deptExtraPassed(e.isDeptExtraPassed())
                    .finalPass(e.isFinalPass())
                    .build();
        } catch (Exception ex) {
            throw new RuntimeException("rows deserialize failed", ex);
        }
    }
}
