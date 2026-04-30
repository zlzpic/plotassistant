package com.bdu.plotassistant.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "story_script")
public class StoryScript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "branch_path", length = 500)
    private String branchPath;  // 如: node_001->node_002->node_005

    @Column(name = "content_type", length = 32)
    private String contentType = "FULL_STORY";  // FULL_STORY/BRANCH/END

    @Column(columnDefinition = "LONGTEXT")
    private String content;  // 生成的完整剧情文本

    @Column(columnDefinition = "JSON")
    private String referencedNodes;  // 引用的节点ID列表

    @Column(name = "is_canon")
    private Boolean isCanon = false;  // 是否正史

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
