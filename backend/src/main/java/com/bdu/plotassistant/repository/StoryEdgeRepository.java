package com.bdu.plotassistant.repository;

import com.bdu.plotassistant.entity.StoryEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryEdgeRepository extends JpaRepository<StoryEdge, Long> {

    List<StoryEdge> findByProjectId(Long projectId);

    List<StoryEdge> findBySourceId(String sourceId);

    List<StoryEdge> findByTargetId(String targetId);

    void deleteByProjectId(Long projectId);

    // 查询特定标签的边（用于检查重复）
    Optional<StoryEdge> findBySourceIdAndTargetIdAndLabel(String sourceId, String targetId, String label);

    // 批量删除
    void deleteAllByIdInBatch(Iterable<Long> ids);
    // 根据源节点和目标节点查询边（支持多条边）
    List<StoryEdge> findBySourceIdAndTargetId(String sourceId, String targetId);
    void deleteBySourceId(String sourceId);
}
