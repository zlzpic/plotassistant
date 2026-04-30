package com.bdu.plotassistant.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "generated_content",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "content_type"}))
public class GeneratedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;


    @Column(name = "content_type", nullable = false, length = 32)
    private String contentType;

    @Column(name = "node_id", length = 32)
    private String nodeId;

    @Column(columnDefinition = "JSON")
    private String generationParams;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String contentJson;

    @Column(length = 50)
    private String aiModel;

    private Integer tokenUsage;

    private Integer generationTimeMs;

    @Column(nullable = false)
    private Integer isEdited = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
