package com.agrovision.controller;

import com.agrovision.dto.ScanResultResponse;
import com.agrovision.entity.User;
import com.agrovision.service.ScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/scan")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;

    /**
     * Authenticated scan - saves to user's history.
     */
    @PostMapping("/predict")
    public ResponseEntity<ScanResultResponse> predict(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "location", required = false) String location,
            @AuthenticationPrincipal User user) throws IOException {
        return ResponseEntity.ok(scanService.analyzeImage(image, user.getId(), location));
    }

    /**
     * Guest scan - no history saved.
     */
    @PostMapping("/guest")
    public ResponseEntity<ScanResultResponse> guestScan(
            @RequestParam("image") MultipartFile image) throws IOException {
        return ResponseEntity.ok(scanService.analyzeImage(image, null, null));
    }
}
