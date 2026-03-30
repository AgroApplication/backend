package com.agrovision.config;

import com.agrovision.entity.Disease;
import com.agrovision.repository.DiseaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final DiseaseRepository diseaseRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        if (diseaseRepository.count() > 0) {
            log.info("Disease data already seeded. Skipping.");
            return;
        }

        log.info("Seeding disease data...");

        List<Disease> diseases = List.of(
            Disease.builder()
                .id("rice-blast")
                .name("Rice Blast")
                .scientificName("Magnaporthe oryzae")
                .description("A fungal disease causing lesions on leaves, stems, and panicles.")
                .severity("high")
                .treatments(objectMapper.writeValueAsString(List.of(
                    "Apply fungicides containing tricyclazole or isoprothiolane",
                    "Remove and destroy infected plant debris",
                    "Ensure proper field drainage",
                    "Use resistant rice varieties"
                )))
                .build(),

            Disease.builder()
                .id("brown-spot")
                .name("Brown Spot")
                .scientificName("Bipolaris oryzae")
                .description("Fungal disease causing oval brown spots on leaves.")
                .severity("medium")
                .treatments(objectMapper.writeValueAsString(List.of(
                    "Apply mancozeb or zineb fungicides",
                    "Improve soil fertility with balanced fertilization",
                    "Use certified disease-free seeds",
                    "Maintain proper water management"
                )))
                .build(),

            Disease.builder()
                .id("bacterial-leaf-blight")
                .name("Bacterial Leaf Blight")
                .scientificName("Xanthomonas oryzae")
                .description("Bacterial disease causing wilting and yellowing of leaves.")
                .severity("high")
                .treatments(objectMapper.writeValueAsString(List.of(
                    "Apply copper-based bactericides",
                    "Practice crop rotation",
                    "Remove infected plant material",
                    "Use resistant varieties"
                )))
                .build(),

            Disease.builder()
                .id("sheath-blight")
                .name("Sheath Blight")
                .scientificName("Rhizoctonia solani")
                .description("Fungal disease affecting leaf sheaths and stems.")
                .severity("medium")
                .treatments(objectMapper.writeValueAsString(List.of(
                    "Apply validamycin or hexaconazole",
                    "Reduce planting density",
                    "Avoid excessive nitrogen fertilization",
                    "Improve air circulation in the field"
                )))
                .build(),

            Disease.builder()
                .id("healthy")
                .name("Healthy")
                .scientificName("N/A")
                .description("No disease detected. Plant appears healthy.")
                .severity("none")
                .treatments(objectMapper.writeValueAsString(List.of(
                    "Continue regular monitoring",
                    "Maintain proper irrigation",
                    "Follow recommended fertilization schedule"
                )))
                .build()
        );

        diseaseRepository.saveAll(diseases);
        log.info("Seeded {} diseases successfully.", diseases.size());
    }
}
