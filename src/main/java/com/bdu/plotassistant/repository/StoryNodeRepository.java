package com.bdu.plotassistant.repository;

import com.bdu.plotassistant.entity.StoryNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryNodeRepository extends JpaRepository<StoryNode, String> {

    List<StoryNode> findByProjectIdOrderByCreatedAtAsc(Long projectId);

    // 基础查询
    List<StoryNode> findByProjectId(Long projectId);

    // 按幕和节拍排序（用于L4生成后展示）
    List<StoryNode> findByProjectIdOrderByActIndexAscBeatIndexAsc(Long projectId);

    // 按幕查询（用于L5-L7按幕处理）
    List<StoryNode> findByProjectIdAndActIndex(Long projectId, Integer actIndex);

    // 查询特定节点（用于L5-L7单个处理）
    Optional<StoryNode> findByIdAndProjectId(String id, Long projectId);

    Optional<StoryNode> findById(String id);  // JpaRepository默认已有
}
