package com.bdu.plotassistant.entity;

import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "playthrough_state")
public class PlaythroughState implements Serializable {

    private static final long serialVersionUID = 1L;

    //主键字段：与 PlaythroughSession.id 类型一致（String）
    @Id
    @Column(name = "session_id")
    private String sessionId;

    //关联关系：使用 @MapsId 表明使用 PlaythroughSession 的 id 作为主键
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "session_id")
    private PlaythroughSession session;

    @Column(nullable = false, length = 32)
    private String currentNodeId;

    @Column(length = 32)
    private String activeCharacterId;

    @Column(nullable = false, columnDefinition = "JSON")
    private String variablesJson;

    @Column(columnDefinition = "TEXT")
    private String historySummary;

    @Column(nullable = false)
    private Integer turnCount = 0;

    @Column(columnDefinition = "TEXT")
    private String lastInput;

    @Column(columnDefinition = "TEXT")
    private String lastReply;

    @Column(columnDefinition = "TEXT")
    private String gmPrompt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
