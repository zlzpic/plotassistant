package com.bdu.plotassistant.repository;

import com.bdu.plotassistant.entity.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CharacterRepository extends JpaRepository<Character, String> {

    List<Character> findByProjectId(Long projectId);

    List<Character> findByProjectIdAndRoleType(Long projectId, String roleType);

    // 排除指定ID（IN 的反向操作）
    @Query("SELECT c FROM Character c WHERE c.project.id = ?1 AND c.id NOT IN ?2")
    List<Character> findByProjectIdAndIdNotIn(Long projectId, List<String> excludeIds);

    // 只查指定的
    @Query("SELECT c FROM Character c WHERE c.project.id = ?1 AND c.id IN ?2")
    List<Character> findByProjectIdAndIdIn(Long projectId, List<String> ids);

    // 用于L2保存和查询
    List<Character> findByProjectIdAndStatus(Long projectId, Integer status);

    // 用于L6排除重要角色
    List<Character> findByProjectIdAndStatusNot(Long projectId, Integer status);
}
