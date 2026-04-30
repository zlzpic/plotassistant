package com.bdu.plotassistant.repository;

import com.bdu.plotassistant.entity.WorldSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorldSettingRepository extends JpaRepository<WorldSetting, Long> {

    Optional<WorldSetting> findByProjectId(Long projectId);
}
