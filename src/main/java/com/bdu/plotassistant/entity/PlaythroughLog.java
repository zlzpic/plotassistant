package com.bdu.plotassistant.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "playthrough_log",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "sequence_num"}))
public class PlaythroughLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private PlaythroughSession session;

    @Column(name = "sequence_num",nullable = false)
    private Integer sequenceNum;

    @Column(nullable = false, length = 20)
    private String logType;

    @Column(columnDefinition = "TEXT")
    private String userInput;

    @Column(columnDefinition = "TEXT")
    private String aiReply;

    @Column(length = 32)
    private String characterId;

    @Column(length = 32)
    private String fromNodeId;

    @Column(length = 32)
    private String toNodeId;

    @Column(columnDefinition = "TEXT")
    private String transitionReason;

    @Column(columnDefinition = "JSON")
    private String variablesSnapshot;

    @Column(columnDefinition = "TEXT")
    private String promptUsed;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
