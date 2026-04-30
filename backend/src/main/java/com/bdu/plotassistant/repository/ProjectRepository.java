package com.bdu.plotassistant.repository;

import com.bdu.plotassistant.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findByUserId(Long userId, Pageable pageable);

    Page<Project> findByUserIdAndStatus(Long userId, String status, Pageable pageable);

    Page<Project> findByUserIdAndNameContaining(Long userId, String keyword, Pageable pageable);

    Page<Project> findByUserIdAndStatusAndNameContaining(Long userId, String status,
                                                         String keyword, Pageable pageable);

    boolean existsByIdAndUserId(Long id, Long userId);
}
