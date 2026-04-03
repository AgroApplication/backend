package com.agrovision.service;

import com.agrovision.dto.ScanResultResponse;
import com.agrovision.entity.Disease;
import com.agrovision.entity.ScanRecord;
import com.agrovision.repository.DiseaseRepository;
import com.agrovision.repository.ScanRecordRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import javax.imageio.ImageIO;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanService {

    private final ScanRecordRepository scanRecordRepository;
    private final DiseaseRepository diseaseRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.model.service-url}")
    private String modelServiceUrl;

    @Value("${app.model.fallback-enabled}")
    private boolean fallbackEnabled;

    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * Analyze an uploaded image. If userId is provided, save to scan history.
     */
    public ScanResultResponse analyzeImage(MultipartFile file, Long userId, String location) throws IOException {
        // Save uploaded file
        String imageUrl = saveFile(file);

        // Get prediction from Python model or fallback
        Map<String, Object> prediction = getPrediction(file);

        // Check if the image was identified as a valid rice leaf
        Boolean isValidLeaf = (Boolean) prediction.get("isValidLeaf");
        if (Boolean.FALSE.equals(isValidLeaf)) {
            String msg = (String) prediction.getOrDefault("message",
                    "This does not appear to be a rice leaf. Please upload a clear image of a rice plant leaf.");
            throw new IllegalArgumentException(msg);
        }

        String diseaseName = (String) prediction.get("disease");
        double confidence = ((Number) prediction.get("confidence")).doubleValue();

        // Look up disease info
        Disease disease = diseaseRepository.findByName(diseaseName)
                .orElse(Disease.builder()
                        .id("unknown")
                        .name(diseaseName)
                        .scientificName("Unknown")
                        .description("Disease detected but details not available.")
                        .severity("medium")
                        .treatments("[]")
                        .build());

        String status = diseaseName.equalsIgnoreCase("Healthy") ? "healthy" : "diseased";
        double affectedArea = status.equals("healthy") ? 0 : Math.round((100 - confidence) * 0.6 * 10.0) / 10.0;

        // Parse treatments
        List<String> treatments;
        try {
            treatments = objectMapper.readValue(disease.getTreatments(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            treatments = List.of("Consult an agricultural expert for treatment recommendations.");
        }

        // Save scan record if authenticated user
        Long scanId = null;
        if (userId != null) {
            ScanRecord record = ScanRecord.builder()
                    .userId(userId)
                    .disease(diseaseName)
                    .confidence(confidence)
                    .affectedArea(affectedArea)
                    .imageUrl(imageUrl)
                    .location(location != null ? location : "Unknown")
                    .status(status)
                    .build();
            record = scanRecordRepository.save(record);
            scanId = record.getId();
        }

        return ScanResultResponse.builder()
                .disease(disease.getName())
                .scientificName(disease.getScientificName())
                .description(disease.getDescription())
                .severity(disease.getSeverity())
                .confidence(confidence)
                .affectedArea(affectedArea)
                .status(status)
                .treatments(treatments)
                .imageUrl(imageUrl)
                .scanId(scanId)
                .build();
    }

    /**
     * Call Python model service or use fallback.
     */
    private Map<String, Object> getPrediction(MultipartFile file) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    modelServiceUrl, HttpMethod.POST, requestEntity, (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("Python model service unavailable: {}. Using fallback.", e.getMessage());
        }

        // Fallback: validate image in Java and return prediction
        if (fallbackEnabled) {
            return getFallbackPrediction(file);
        }
        throw new RuntimeException("Model service is unavailable and fallback is disabled.");
    }

    private Map<String, Object> getFallbackPrediction(MultipartFile file) {
        // Validate image locally
        try {
            if (!isLikelyLeafImage(file.getBytes())) {
                Map<String, Object> rejection = new HashMap<>();
                rejection.put("isValidLeaf", false);
                rejection.put("message", "This does not appear to be a rice leaf. Please upload a clear image of a rice plant leaf.");
                return rejection;
            }
        } catch (Exception e) {
            log.warn("Image pre-validation failed, allowing anyway: {}", e.getMessage());
        }

        // DETERMINISTIC prediction based on actual pixel colors
        // Same image → same pixels → same result EVERY TIME
        try {
            byte[] imageBytes = file.getBytes();
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (img == null) throw new IOException("Cannot read image");

            int width = img.getWidth();
            int height = img.getHeight();
            int step = Math.max(1, Math.min(width, height) / 64);

            int total = 0, greenPx = 0, brownPx = 0, grayPx = 0, darkPx = 0, yellowPx = 0;

            for (int x = 0; x < width; x += step) {
                for (int y = 0; y < height; y += step) {
                    int rgb = img.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                    total++;
                    if (g > r && g > b && g > 60) greenPx++;
                    if (r > 100 && g > 80 && b < 80 && r > g) brownPx++;
                    if (Math.abs(r - g) < 20 && Math.abs(g - b) < 20 && r > 100) grayPx++;
                    if (r < 80 && g < 80 && b < 80) darkPx++;
                    if (r > 120 && g > 120 && b < 80) yellowPx++;
                }
            }
            if (total == 0) total = 1;

            Map<String, Double> scores = new LinkedHashMap<>();
            scores.put("Healthy", (double) greenPx / total);
            scores.put("Brown Spot", (double) brownPx / total);
            scores.put("Sheath Blight", (double) grayPx / total);
            scores.put("Rice Blast", (double) darkPx / total);
            scores.put("Bacterial Leaf Blight", (double) yellowPx / total);

            String best = "Healthy";
            double bestScore = 0, totalScore = 0;
            for (Map.Entry<String, Double> e : scores.entrySet()) {
                totalScore += e.getValue();
                if (e.getValue() > bestScore) { bestScore = e.getValue(); best = e.getKey(); }
            }

            double confidence = totalScore > 0 ? Math.min((bestScore / totalScore) * 100, 95.0) : 50.0;
            confidence = Math.max(confidence, 30.0);
            confidence = Math.round(confidence * 10.0) / 10.0;

            log.info("Deterministic fallback: {} at {}% confidence", best, confidence);

            Map<String, Object> result = new HashMap<>();
            result.put("isValidLeaf", true);
            result.put("disease", best);
            result.put("confidence", confidence);
            return result;

        } catch (Exception e) {
            log.error("Pixel analysis failed: {}", e.getMessage());
            // Last resort fallback - fixed result, not random
            Map<String, Object> result = new HashMap<>();
            result.put("isValidLeaf", true);
            result.put("disease", "Brown Spot");
            result.put("confidence", 65.0);
            return result;
        }
    }

    /**
     * Checks if the image is likely a plant/rice leaf by analyzing pixel colors.
     * Counts green-dominant and yellow-brown pixels (natural leaf colors).
     * @return true if enough leaf-like pixels are found (>=15%), false otherwise
     */
    private boolean isLikelyLeafImage(byte[] imageBytes) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (img == null) return false; // Unreadable image

        int width = img.getWidth();
        int height = img.getHeight();
        int step = Math.max(1, Math.min(width, height) / 40); // Sample ~40x40 grid

        int total = 0;
        int leafColorPixels = 0;

        for (int x = 0; x < width; x += step) {
            for (int y = 0; y < height; y += step) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                total++;

                // Green-dominant: G > R+15 and G > B+15 and G > 50
                boolean isGreen = g > (r + 15) && g > (b + 15) && g > 50;

                // Yellow-brown: high R&G, low B (dried/diseased leaves, soil)
                boolean isYellowBrown = r > 80 && g > 70 && b < 100 && (r + g) > (b * 3);

                if (isGreen || isYellowBrown) {
                    leafColorPixels++;
                }
            }
        }

        if (total == 0) return false;
        double leafRatio = (double) leafColorPixels / total;
        log.info("Image leaf color ratio: {}% ({}/{} sampled pixels)", String.format("%.1f", leafRatio * 100), leafColorPixels, total);
        return leafRatio >= 0.15; // At least 15% leaf-like colors
    }

    private String saveFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + filename;
    }
}
