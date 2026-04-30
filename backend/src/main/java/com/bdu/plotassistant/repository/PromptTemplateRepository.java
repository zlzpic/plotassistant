package com.bdu.plotassistant.repository;

import com.bdu.plotassistant.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {

    Optional<PromptTemplate> findByTemplateCodeAndVersion(String templateCode, Integer version);

    Optional<PromptTemplate> findTopByTemplateCodeAndIsActiveOrderByVersionDesc(String templateCode, Integer isActive);

    List<PromptTemplate> findByIsActive(Integer isActive);

    List<PromptTemplate> findByTemplateCodeOrderByVersionDesc(String templateCode);

    boolean existsByTemplateCodeAndVersion(String templateCode, Integer version);

    @Query("SELECT MAX(t.version) FROM PromptTemplate t WHERE t.templateCode = ?1")
    Optional<Integer> findMaxVersionByTemplateCode(String templateCode);

    @Modifying
    @Query("UPDATE PromptTemplate t SET t.isActive = 0 WHERE t.templateCode = ?1")
    void deactivateAllByTemplateCode(String templateCode);
}
