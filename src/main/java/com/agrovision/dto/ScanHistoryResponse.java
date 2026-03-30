package com.agrovision.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class ScanHistoryResponse {
    private Long id;
    private String imageUrl;
    private String disease;
    private double confidence;
    private double affectedArea;
    private String location;
    private String status;
    private LocalDateTime date;
}
