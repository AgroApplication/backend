package com.agrovision.controller;

import com.agrovision.dto.ScanHistoryResponse;
import com.agrovision.entity.User;
import com.agrovision.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping
    public ResponseEntity<List<ScanHistoryResponse>> getHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(historyService.getUserHistory(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScanHistoryResponse> getScan(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(historyService.getScanById(id, user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteScan(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        historyService.deleteScan(id, user.getId());
        return ResponseEntity.ok(Map.of("message", "Scan record deleted successfully"));
    }
}
