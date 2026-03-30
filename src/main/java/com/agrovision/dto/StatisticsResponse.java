package com.agrovision.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class StatisticsResponse {
    private long totalScans;
    private double healthyPercentage;
    private double diseasedPercentage;
    private String mostCommonDisease;
    private List<WeeklyScans> weeklyScans;
    private List<DiseaseDistribution> diseaseDistribution;
    private List<ConfidenceRange> confidenceDistribution;

    @Data
    @Builder
    @AllArgsConstructor
    public static class WeeklyScans {
        private String week;
        private long scans;
        private long healthy;
        private long diseased;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class DiseaseDistribution {
        private String name;
        private long value;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class ConfidenceRange {
        private String range;
        private long count;
    }
}
