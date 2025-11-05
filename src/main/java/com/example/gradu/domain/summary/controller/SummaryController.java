package com.example.gradu.domain.summary.controller;

import com.example.gradu.domain.summary.dto.SummaryDto;
import com.example.gradu.domain.summary.dto.TogglesDto;
import com.example.gradu.domain.summary.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/students/{sid}/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping
    public SummaryDto get(@PathVariable String sid) {
        return summaryService.getSummary(sid);
    }

    @PatchMapping("/toggles")
    public void patchToggles(@PathVariable String sid, @RequestBody TogglesDto dto) {
        summaryService.updateTogglesAndRecompute(sid, dto);
    }

    @PostMapping("/rebuild")
    public void rebuild(@PathVariable String sid) {
        summaryService.recomputeAndSave(sid);
    }
}