package com.bdu.plotassistant.entity;

import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "world_setting")
public class WorldSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false, unique = true)
    private Project project;

    @Column(nullable = false, length = 64)
    private String genre;

    @Column(length = 64)
    private String subGenre;

    private Integer techLevel = 0;

    private Integer magicLevel = 0;

    @Column(length = 64)
    private String timeBackground;

    @Column(length = 128)
    private String geoBackground;

    @Column(columnDefinition = "TEXT")
    private String coreConflict;

    @Column(columnDefinition = "TEXT")
    private String specialRules;

    @Column(columnDefinition = "TEXT")
    private String description;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
