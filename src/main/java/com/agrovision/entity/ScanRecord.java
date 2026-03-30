package com.agrovision.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scan_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String disease;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "affected_area")
    private Double affectedArea;

    @Column(name = "image_url")
    private String imageUrl;

    private String location;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
