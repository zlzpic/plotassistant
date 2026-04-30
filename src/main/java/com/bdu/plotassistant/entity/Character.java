package com.bdu.plotassistant.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "story_character")
public class Character {

    @Id
    @Column(length = 32)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 32)
    private String roleType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String personaPrompt;

    @Column(length = 255)
    private String speechPattern;

    @Column(columnDefinition = "JSON")
    private String knowledgeScope;

    @Column(columnDefinition = "JSON")
    private String validatedInsights;
    @Column(nullable = false)
    private Integer status = 0;  // 0=草稿/测试, 1=正式, 2=隐藏/归档

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
