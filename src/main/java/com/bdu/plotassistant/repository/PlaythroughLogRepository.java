package com.bdu.plotassistant.repository;

import com.bdu.plotassistant.entity.PlaythroughLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaythroughLogRepository extends JpaRepository<PlaythroughLog, Long> {

    List<PlaythroughLog> findBySessionIdOrderBySequenceNumAsc(String sessionId);

    List<PlaythroughLog> findBySessionIdOrderBySequenceNumDesc(String sessionId);

    Optional<PlaythroughLog> findBySessionIdAndSequenceNum(String sessionId, Integer sequenceNum);

    Integer countBySessionId(String sessionId);
}
