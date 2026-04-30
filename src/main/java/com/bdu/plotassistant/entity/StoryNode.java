package com.bdu.plotassistant.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "story_node")
public class StoryNode {

    @Id
    @Column(length = 32)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 128)
    private String nodeName;

    @Column(columnDefinition = "TEXT")
    private String sceneDescription;

    @Column(nullable = false, columnDefinition = "JSON")
    private String associatedChars;

    @Column(columnDefinition = "JSON")
    private String initialVariables;

    // 大纲结构索引
    @Column(name = "act_index")
    private Integer actIndex = 0;

    @Column(name = "beat_index")
    private Integer beatIndex = 0;

    // 画布坐标
    @Column(name = "position_x",nullable = false, precision = 10, scale = 2)
    private BigDecimal positionX = BigDecimal.ZERO;

    @Column(name = "position_y",nullable = false, precision = 10, scale = 2)
    private BigDecimal positionY = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
