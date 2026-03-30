package com.agrovision.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ScanResultResponse {
    private String disease;
    private String scientificName;
    private String description;
    private String severity;
    private double confidence;
    private double affectedArea;
    private String status;
    private List<String> treatments;
    private String imageUrl;
    private Long scanId;
}
