package com.bdu.plotassistant.repository;

import com.bdu.plotassistant.entity.ConsistencyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsistencyReportRepository extends JpaRepository<ConsistencyReport, Long> {

    Optional<ConsistencyReport> findTopByProjectIdAndIsActiveOrderByCheckedAtDesc(Long projectId, Integer isActive);

    List<ConsistencyReport> findByProjectIdOrderByCheckedAtDesc(Long projectId);

    @Modifying
    @Query("UPDATE ConsistencyReport r SET r.isActive = 0 WHERE r.project.id = ?1")
    void deactivateByProjectId(Long projectId);
}
