package com.bdu.plotassistant.service.impl;

import com.bdu.plotassistant.dto.request.project.*;
import com.bdu.plotassistant.dto.response.project.*;
import com.bdu.plotassistant.dto.response.worldsetting.WorldSettingDTO;
import com.bdu.plotassistant.entity.Project;
import com.bdu.plotassistant.entity.User;
import com.bdu.plotassistant.repository.ProjectRepository;
import com.bdu.plotassistant.repository.UserRepository;
import com.bdu.plotassistant.service.ProjectService;
import com.bdu.plotassistant.service.WorldSettingService;
import com.bdu.plotassistant.utils.BizException;
import com.bdu.plotassistant.utils.ServiceUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorldSettingService worldSettingService;

    public ProjectServiceImpl(ProjectRepository projectRepository,
                              UserRepository userRepository,
                              WorldSettingService worldSettingService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.worldSettingService = worldSettingService;
    }

    @Override
    @Transactional
    public Long create(Long userId, CreateProjectRequest request) {
        ServiceUtil.requireNonNull(userId, "用户ID不能为空");

        // 获取用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));

        // 创建项目
        Project project = new Project();
        project.setUser(user);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStatus("DRAFT");

        Project saved = projectRepository.save(project);

        // 初始化默认世界观设定
        worldSettingService.getOrCreateDefault(saved.getId());

        return saved.getId();
    }

    @Override
    public Page<ProjectSummaryDTO> list(Long userId, String status, String keyword, Pageable pageable) {
        ServiceUtil.requireNonNull(userId, "用户ID不能为空");

        Page<Project> page;

        // 根据条件查询
        if (ServiceUtil.isEmpty(status) && ServiceUtil.isEmpty(keyword)) {
            page = projectRepository.findByUserId(userId, pageable);
        } else if (!ServiceUtil.isEmpty(status) && ServiceUtil.isEmpty(keyword)) {
            page = projectRepository.findByUserIdAndStatus(userId, status, pageable);
        } else if (ServiceUtil.isEmpty(status) && !ServiceUtil.isEmpty(keyword)) {
            page = projectRepository.findByUserIdAndNameContaining(userId, keyword, pageable);
        } else {
            page = projectRepository.findByUserIdAndStatusAndNameContaining(
                    userId, status, keyword, pageable);
        }

        return page.map(this::convertToSummaryDTO);
    }

    @Override
    public ProjectDetailDTO getDetail(Long projectId) {
        Project project = getProjectById(projectId);
        return convertToDetailDTO(project);
    }

    @Override
    @Transactional
    public void update(Long projectId, UpdateProjectRequest request) {
        Project project = getProjectById(projectId);

        // 更新字段（非空则更新）
        if (!ServiceUtil.isEmpty(request.getName())) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (!ServiceUtil.isEmpty(request.getStatus())) {
            // 状态机校验
            if (!isValidStatusTransition(project.getStatus(), request.getStatus())) {
                throw new BizException("无效的状态转换: " + project.getStatus() + " -> " + request.getStatus());
            }
            project.setStatus(request.getStatus());
        }

        projectRepository.save(project);
    }

    @Override
    @Transactional
    public void delete(Long projectId) {
        Project project = getProjectById(projectId);

        // 级联删除由JPA外键配置处理
        projectRepository.delete(project);
    }

    @Override
    @Transactional
    public void complete(Long projectId) {
        Project project = getProjectById(projectId);

        if (!"ACTIVE".equals(project.getStatus()) && !"DRAFT".equals(project.getStatus())) {
            throw new BizException("只有进行中的项目可以标记完成");
        }

        project.setStatus("COMPLETED");
        projectRepository.save(project);
    }

    //这个功能暂且搁置
    @Override
    public ExportResultDTO export(Long projectId, ExportRequest request) {
        Project project = getProjectById(projectId);

        // 模拟导出逻辑
        // 实际：组装项目完整数据，生成JSON/Excel文件，上传OSS，返回URL

        String fileName = "project_" + projectId + "_" + System.currentTimeMillis()
                + "." + request.getFormat().toLowerCase();

        ExportResultDTO result = new ExportResultDTO();
        result.setFormat(request.getFormat());
        result.setDownloadUrl("/download/" + fileName);
        result.setFileSize(1024L); // 模拟大小
        result.setGeneratedAt(ServiceUtil.formatDateTime(LocalDateTime.now()));

        return result;
    }

    @Override
    public boolean existsById(Long projectId) {
        return projectRepository.existsById(projectId);
    }

    @Override
    public boolean checkOwnership(Long projectId, Long userId) {
        boolean owned = projectRepository.existsByIdAndUserId(projectId, userId);
        if (!owned) {
            throw new BizException("无权操作此项目");
        }
        return true;
    }

    // ========== 私有方法 ==========

    private Project getProjectById(Long projectId) {
        ServiceUtil.requireNonNull(projectId, "项目ID不能为空");
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在"));
    }

    private boolean isValidStatusTransition(String from, String to) {
        // 简单状态机：DRAFT <-> ACTIVE -> COMPLETED
        if (from.equals(to)) return true;
        if ("DRAFT".equals(from) && "ACTIVE".equals(to)) return true;
        if ("ACTIVE".equals(from) && "DRAFT".equals(to)) return true;
        if ("ACTIVE".equals(from) && "COMPLETED".equals(to)) return true;
        if ("DRAFT".equals(from) && "COMPLETED".equals(to)) return true;
        return false;
    }

    private ProjectSummaryDTO convertToSummaryDTO(Project project) {
        ProjectSummaryDTO dto = new ProjectSummaryDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setStatus(project.getStatus());
        dto.setUpdatedAt(ServiceUtil.formatDateTime(project.getUpdatedAt()));
        return dto;
    }

    private ProjectDetailDTO convertToDetailDTO(Project project) {
        ProjectDetailDTO dto = new ProjectDetailDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setStatus(project.getStatus());
        dto.setDescription(project.getDescription());
        dto.setUpdatedAt(ServiceUtil.formatDateTime(project.getUpdatedAt()));

        // 组装世界观
        WorldSettingDTO worldSetting = worldSettingService.getByProjectId(project.getId());
        dto.setWorldSetting(worldSetting);

        return dto;
    }
}
