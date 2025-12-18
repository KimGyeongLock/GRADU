package com.example.gradu.domain.summary.controller;

import com.example.gradu.domain.summary.dto.SummaryDto;
import com.example.gradu.domain.summary.dto.TogglesDto;
import com.example.gradu.domain.summary.service.SummaryCommandService;
import com.example.gradu.domain.summary.service.SummaryService;
import com.example.gradu.global.security.CheckStudentAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/students/{sid}/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;
    private final SummaryCommandService summaryCommandService;

    @GetMapping
    @CheckStudentAccess
    public SummaryDto get(@PathVariable Long sid) {
        return summaryService.getSummary(sid);
    }

    @PatchMapping("/toggles")
    @CheckStudentAccess
    public void patchToggles(@PathVariable Long sid, @RequestBody TogglesDto dto) {
        summaryService.updateTogglesAndRecompute(sid, dto);
    }

    @PostMapping("/rebuild")
    @CheckStudentAccess
    public void rebuild(@PathVariable Long sid) {
        summaryCommandService.recomputeAndSave(sid);
    }
}