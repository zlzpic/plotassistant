package com.bdu.plotassistant.repository;

import com.bdu.plotassistant.entity.PlaythroughState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaythroughStateRepository extends JpaRepository<PlaythroughState, String> {
    // 主键即sessionId，一对一关系
}
