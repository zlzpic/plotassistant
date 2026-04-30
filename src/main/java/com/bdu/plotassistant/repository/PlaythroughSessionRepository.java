package com.bdu.plotassistant.repository;

import com.bdu.plotassistant.entity.PlaythroughSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaythroughSessionRepository extends JpaRepository<PlaythroughSession, String> {

    List<PlaythroughSession> findByUserId(Long userId);

    List<PlaythroughSession> findByProjectId(Long projectId);

    List<PlaythroughSession> findByUserIdAndProjectId(Long userId, Long projectId);

    List<PlaythroughSession> findByUserIdAndSessionType(Long userId, String sessionType);
}
