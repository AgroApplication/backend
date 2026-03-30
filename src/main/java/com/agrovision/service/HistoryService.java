package com.agrovision.service;

import com.agrovision.dto.ScanHistoryResponse;
import com.agrovision.entity.ScanRecord;
import com.agrovision.repository.ScanRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final ScanRecordRepository scanRecordRepository;

    public List<ScanHistoryResponse> getUserHistory(Long userId) {
        return scanRecordRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ScanHistoryResponse getScanById(Long scanId, Long userId) {
        ScanRecord record = scanRecordRepository.findById(scanId)
                .orElseThrow(() -> new RuntimeException("Scan record not found"));

        if (!record.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        return toResponse(record);
    }

    public void deleteScan(Long scanId, Long userId) {
        ScanRecord record = scanRecordRepository.findById(scanId)
                .orElseThrow(() -> new RuntimeException("Scan record not found"));

        if (!record.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        scanRecordRepository.delete(record);
    }

    private ScanHistoryResponse toResponse(ScanRecord record) {
        return ScanHistoryResponse.builder()
                .id(record.getId())
                .imageUrl(record.getImageUrl())
                .disease(record.getDisease())
                .confidence(record.getConfidence())
                .affectedArea(record.getAffectedArea() != null ? record.getAffectedArea() : 0)
                .location(record.getLocation())
                .status(record.getStatus())
                .date(record.getCreatedAt())
                .build();
    }
}
