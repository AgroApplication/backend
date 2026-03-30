package com.agrovision.controller;

import com.agrovision.dto.StatisticsResponse;
import com.agrovision.entity.User;
import com.agrovision.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    public ResponseEntity<StatisticsResponse> getStatistics(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(statisticsService.getStatistics(user.getId()));
    }
}
