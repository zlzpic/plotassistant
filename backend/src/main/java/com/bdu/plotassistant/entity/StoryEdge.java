package com.bdu.plotassistant.entity;

import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "story_edge")
public class StoryEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private StoryNode source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private StoryNode target;

    @Column(length = 255)
    private String label;

    @Column(length = 500)
    private String conditionExpr;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "on_success", columnDefinition = "TEXT")
    private String onSuccess;

    @Column(name = "on_failure", columnDefinition = "TEXT")
    private String onFailure;

    @Column(columnDefinition = "TEXT")
    private String effect;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
