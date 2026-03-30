package com.agrovision.repository;

import com.agrovision.entity.Disease;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DiseaseRepository extends JpaRepository<Disease, String> {
    Optional<Disease> findByName(String name);
}
