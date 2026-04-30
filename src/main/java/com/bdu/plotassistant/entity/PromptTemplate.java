package com.bdu.plotassistant.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "prompt_template",
        uniqueConstraints = @UniqueConstraint(columnNames = {"template_code", "version"}))
public class PromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_code",nullable = false, length = 64)
    private String templateCode;

    @Column(nullable = false, length = 128)
    private String templateName;

    @Column(columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userPromptTemplate;

    @Column(columnDefinition = "JSON")
    private String paramSchema;

    @Column(name = "version",nullable = false)
    private Integer version = 1;

    @Column(nullable = false)
    private Integer isActive = 1;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
