package com.bdu.plotassistant.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "consistency_report")
public class ConsistencyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 50)
    private String checkScope;

    @Column(columnDefinition = "JSON")
    private String targetNodeIds;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Integer isActive = 1;

    @Column(columnDefinition = "JSON")
    private String conflictsJson;

    private Integer checkedItemsCount = 0;

    private Integer conflictsCount = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime checkedAt;
}
