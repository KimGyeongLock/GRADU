package com.example.gradu.domain.summary.service;

import com.example.gradu.domain.course.entity.Course;
import com.example.gradu.domain.course.repository.CourseRepository;
import com.example.gradu.domain.student.entity.Student;
import com.example.gradu.domain.student.repository.StudentRepository;
import com.example.gradu.domain.summary.dto.SummaryCalcResult;
import com.example.gradu.domain.summary.dto.SummaryDto;
import com.example.gradu.domain.summary.entity.Summary;
import com.example.gradu.domain.summary.policy.SummaryPolicy;
import com.example.gradu.domain.summary.policy.SummaryPolicyService;
import com.example.gradu.domain.summary.repository.SummaryRepository;
import com.example.gradu.domain.summary.util.SummaryCalculator;
import com.example.gradu.global.exception.ErrorCode;
import com.example.gradu.global.exception.auth.AuthException;
import com.example.gradu.global.exception.json.SummaryJsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SummaryCommandService {

    private final CourseRepository courseRepository;
    private final SummaryPolicyService policyService;
    private final StudentRepository studentRepository;
    private final SummaryRepository summaryRepository;
    private final ObjectMapper om;

    @Transactional
    public SummaryDto recomputeAndSave(Long studentId) {
        // 1) 정책 + 과목 조회
        SummaryPolicy policy = policyService.getActivePolicyFor();
        List<Course> courses = courseRepository.findByStudentId(studentId);

        // 2) Summary 엔티티 조회 (없으면 생성)
        Summary summary = summaryRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    Student student = studentRepository.findById(studentId)
                            .orElseThrow(() -> new AuthException(ErrorCode.STUDENT_NOT_FOUND));
                    // Summary.ofStudent(Student) 형태로 팩토리 수정해놨다는 전제
                    return Summary.ofStudent(student);
                });

        // 3) 계산 (기존 토글값은 유지해서 전달)
        SummaryDto calc = SummaryCalculator.compute(
                courses, policy,
                summary.isGradEnglishPassed()
        );

        // 4) rowsJson 변환
        String rowsJson;
        try {
            rowsJson = om.writeValueAsString(calc.getRows());
        } catch (Exception e) {
            throw new SummaryJsonProcessingException(ErrorCode.SUMMARY_JSON_PROCESSING_ERROR);
        }

        // 5) 결과 객체로 묶기
        SummaryCalcResult result = new SummaryCalcResult(
                calc.getPfCredits(), calc.getPfLimit(), calc.isPfPass(),
                calc.getTotalCredits(), calc.isTotalPass(),
                calc.getGpa(),
                calc.getEngMajorCredits(), calc.getEngLiberalCredits(), calc.isEnglishPass(),
                calc.isGradEnglishPassed(), calc.isDeptExtraPassed(), calc.isFinalPass(),
                rowsJson
        );

        // 6) 엔티티 반영
        summary.applyCalc(result);
        summaryRepository.save(summary);

        return calc;
    }
}
