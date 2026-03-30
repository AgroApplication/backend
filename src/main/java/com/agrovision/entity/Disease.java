package com.agrovision.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "diseases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disease {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "scientific_name")
    private String scientificName;

    @Column(length = 1000)
    private String description;

    private String severity;

    @Column(length = 2000)
    private String treatments;
}
