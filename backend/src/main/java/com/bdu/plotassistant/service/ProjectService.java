package com.bdu.plotassistant.service;

import com.bdu.plotassistant.dto.request.project.CreateProjectRequest;
import com.bdu.plotassistant.dto.request.project.ExportRequest;
import com.bdu.plotassistant.dto.request.project.UpdateProjectRequest;
import com.bdu.plotassistant.dto.response.project.ExportResultDTO;
import com.bdu.plotassistant.dto.response.project.ProjectDetailDTO;
import com.bdu.plotassistant.dto.response.project.ProjectSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProjectService {

    Long create(Long userId, CreateProjectRequest request);

    Page<ProjectSummaryDTO> list(Long userId, String status, String keyword, Pageable pageable);

    ProjectDetailDTO getDetail(Long projectId);

    void update(Long projectId, UpdateProjectRequest request);

    void delete(Long projectId);

    void complete(Long projectId);

    ExportResultDTO export(Long projectId, ExportRequest request);

    boolean existsById(Long projectId);

    boolean checkOwnership(Long projectId, Long userId);
}
