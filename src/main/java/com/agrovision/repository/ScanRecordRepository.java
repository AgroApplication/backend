package com.agrovision.repository;

import com.agrovision.entity.ScanRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScanRecordRepository extends JpaRepository<ScanRecord, Long> {

    List<ScanRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, String status);

    @Query("SELECT s.disease, COUNT(s) FROM ScanRecord s WHERE s.userId = :userId GROUP BY s.disease ORDER BY COUNT(s) DESC")
    List<Object[]> findDiseaseDistributionByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM ScanRecord s WHERE s.userId = :userId AND s.createdAt >= :since ORDER BY s.createdAt ASC")
    List<ScanRecord> findRecentScansByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
