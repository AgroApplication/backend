package com.agrovision.service;

import com.agrovision.dto.StatisticsResponse;
import com.agrovision.entity.ScanRecord;
import com.agrovision.repository.ScanRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final ScanRecordRepository scanRecordRepository;

    public StatisticsResponse getStatistics(Long userId) {
        long totalScans = scanRecordRepository.countByUserId(userId);
        long healthyScans = scanRecordRepository.countByUserIdAndStatus(userId, "healthy");
        long diseasedScans = scanRecordRepository.countByUserIdAndStatus(userId, "diseased");

        double healthyPct = totalScans > 0 ? (double) healthyScans / totalScans * 100 : 0;
        double diseasedPct = totalScans > 0 ? (double) diseasedScans / totalScans * 100 : 0;

        // Disease distribution
        List<Object[]> distribution = scanRecordRepository.findDiseaseDistributionByUserId(userId);
        List<StatisticsResponse.DiseaseDistribution> diseaseDistribution = distribution.stream()
                .map(row -> StatisticsResponse.DiseaseDistribution.builder()
                        .name((String) row[0])
                        .value((Long) row[1])
                        .build())
                .collect(Collectors.toList());

        String mostCommon = diseaseDistribution.isEmpty() ? "None" :
                diseaseDistribution.stream()
                        .filter(d -> !d.getName().equalsIgnoreCase("Healthy"))
                        .findFirst()
                        .map(StatisticsResponse.DiseaseDistribution::getName)
                        .orElse("None");

        // Weekly scans (last 4 weeks)
        LocalDateTime since = LocalDateTime.now().minusDays(28);
        List<ScanRecord> recentScans = scanRecordRepository.findRecentScansByUserId(userId, since);
        List<StatisticsResponse.WeeklyScans> weeklyScans = calculateWeeklyScans(recentScans);

        // Confidence distribution
        List<StatisticsResponse.ConfidenceRange> confidenceDistribution = calculateConfidenceDistribution(userId);

        return StatisticsResponse.builder()
                .totalScans(totalScans)
                .healthyPercentage(Math.round(healthyPct * 10.0) / 10.0)
                .diseasedPercentage(Math.round(diseasedPct * 10.0) / 10.0)
                .mostCommonDisease(mostCommon)
                .weeklyScans(weeklyScans)
                .diseaseDistribution(diseaseDistribution)
                .confidenceDistribution(confidenceDistribution)
                .build();
    }

    private List<StatisticsResponse.WeeklyScans> calculateWeeklyScans(List<ScanRecord> scans) {
        List<StatisticsResponse.WeeklyScans> weeks = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = 3; i >= 0; i--) {
            LocalDate weekStart = now.minus(i * 7L + 6, ChronoUnit.DAYS);
            LocalDate weekEnd = now.minus(i * 7L, ChronoUnit.DAYS);

            long total = scans.stream()
                    .filter(s -> {
                        LocalDate scanDate = s.getCreatedAt().toLocalDate();
                        return !scanDate.isBefore(weekStart) && !scanDate.isAfter(weekEnd);
                    })
                    .count();

            long healthy = scans.stream()
                    .filter(s -> {
                        LocalDate scanDate = s.getCreatedAt().toLocalDate();
                        return !scanDate.isBefore(weekStart) && !scanDate.isAfter(weekEnd)
                                && "healthy".equals(s.getStatus());
                    })
                    .count();

            weeks.add(StatisticsResponse.WeeklyScans.builder()
                    .week("Week " + (4 - i))
                    .scans(total)
                    .healthy(healthy)
                    .diseased(total - healthy)
                    .build());
        }

        return weeks;
    }

    private List<StatisticsResponse.ConfidenceRange> calculateConfidenceDistribution(Long userId) {
        List<ScanRecord> allScans = scanRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);

        long range90 = allScans.stream().filter(s -> s.getConfidence() >= 90).count();
        long range80 = allScans.stream().filter(s -> s.getConfidence() >= 80 && s.getConfidence() < 90).count();
        long range70 = allScans.stream().filter(s -> s.getConfidence() >= 70 && s.getConfidence() < 80).count();
        long range60 = allScans.stream().filter(s -> s.getConfidence() >= 60 && s.getConfidence() < 70).count();
        long rangeLow = allScans.stream().filter(s -> s.getConfidence() < 60).count();

        return List.of(
                new StatisticsResponse.ConfidenceRange("90-100%", range90),
                new StatisticsResponse.ConfidenceRange("80-90%", range80),
                new StatisticsResponse.ConfidenceRange("70-80%", range70),
                new StatisticsResponse.ConfidenceRange("60-70%", range60),
                new StatisticsResponse.ConfidenceRange("<60%", rangeLow)
        );
    }
}
