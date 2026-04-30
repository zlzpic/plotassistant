package com.bdu.plotassistant.repository;

import com.bdu.plotassistant.entity.GeneratedContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeneratedContentRepository extends JpaRepository<GeneratedContent, Long> {

    boolean existsByProjectIdAndContentType(Long projectId, String contentType);

    Optional<GeneratedContent> findByProjectIdAndContentType(Long projectId, String contentType);

    //按项目+类型+节点查询（L7使用）
    Optional<GeneratedContent> findByProjectIdAndContentTypeAndNodeId(Long projectId, String contentType, String nodeId);

    /**
     * 批量查询指定节点的对话内容
     */
    @Query("SELECT g FROM GeneratedContent g WHERE g.project.id = :projectId " +
            "AND g.nodeId IN :nodeIds AND g.contentType = :contentType")
    List<GeneratedContent> findByProjectIdAndNodeIdInAndContentType(
            @Param("projectId") Long projectId,
            @Param("nodeIds") List<String> nodeIds,
            @Param("contentType") String contentType
    );

    Optional<GeneratedContent> findByNodeIdAndContentType(String nodeId, String contentType);
}
