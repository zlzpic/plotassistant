package com.bdu.plotassistant.repository;

import com.bdu.plotassistant.entity.StoryScript;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StoryScriptRepository extends JpaRepository<StoryScript, Long> {
    List<StoryScript> findByProjectId(Long projectId);
}
