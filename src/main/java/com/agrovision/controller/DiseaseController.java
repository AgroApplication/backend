package com.agrovision.controller;

import com.agrovision.entity.Disease;
import com.agrovision.repository.DiseaseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/diseases")
@RequiredArgsConstructor
public class DiseaseController {

    private final DiseaseRepository diseaseRepository;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllDiseases() {
        List<Map<String, Object>> result = diseaseRepository.findAll().stream()
                .map(this::toMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDisease(@PathVariable String id) {
        Disease disease = diseaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Disease not found"));
        return ResponseEntity.ok(toMap(disease));
    }

    private Map<String, Object> toMap(Disease disease) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", disease.getId());
        map.put("name", disease.getName());
        map.put("scientificName", disease.getScientificName());
        map.put("description", disease.getDescription());
        map.put("severity", disease.getSeverity());
        try {
            List<String> treatments = objectMapper.readValue(
                    disease.getTreatments(), new TypeReference<List<String>>() {});
            map.put("treatment", treatments);
        } catch (Exception e) {
            map.put("treatment", List.of());
        }
        return map;
    }
}
