package com.bdu.plotassistant.service.impl;

import com.bdu.plotassistant.dto.request.consistencyreport.*;
import com.bdu.plotassistant.dto.response.consistencyreport.*;
import com.bdu.plotassistant.dto.response.consistencyreport.ConsistencyReportDetailDTO.ConflictDTO;
import com.bdu.plotassistant.entity.ConsistencyReport;
import com.bdu.plotassistant.entity.Project;
import com.bdu.plotassistant.entity.StoryEdge;
import com.bdu.plotassistant.entity.StoryNode;
import com.bdu.plotassistant.repository.ConsistencyReportRepository;
import com.bdu.plotassistant.repository.ProjectRepository;
import com.bdu.plotassistant.repository.StoryEdgeRepository;
import com.bdu.plotassistant.repository.StoryNodeRepository;
import com.bdu.plotassistant.service.AiClientService;
import com.bdu.plotassistant.service.ConsistencyReportService;
import com.bdu.plotassistant.utils.BizException;
import com.bdu.plotassistant.utils.ServiceUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConsistencyReportServiceImpl implements ConsistencyReportService {

    private final ConsistencyReportRepository reportRepository;
    private final ProjectRepository projectRepository;
    private final StoryNodeRepository nodeRepository;
    private final StoryEdgeRepository edgeRepository;
    private final AiClientService aiClientService;
    private final ObjectMapper objectMapper;

    public ConsistencyReportServiceImpl(ConsistencyReportRepository reportRepository,
                                        ProjectRepository projectRepository,
                                        StoryNodeRepository nodeRepository,
                                        StoryEdgeRepository edgeRepository,
                                        AiClientService aiClientService,
                                        ObjectMapper objectMapper) {
        this.reportRepository = reportRepository;
        this.projectRepository = projectRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.aiClientService = aiClientService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Long triggerCheck(Long projectId, TriggerCheckRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BizException("项目不存在"));

        // 1. 将旧报告标记为历史（软删除）
        reportRepository.deactivateByProjectId(projectId);

        // 2. 执行检查逻辑
        List<ConflictDTO> conflicts = new ArrayList<>();

        // L1: 规则引擎检查（本地）
        conflicts.addAll(performRuleBasedCheck(projectId, request));

        // L2: AI 语义检查（可选，根据 scope 决定）
        if ("FULL".equals(request.getScope()) || request.getPrompt() != null) {
            conflicts.addAll(performAiSemanticCheck(projectId, request));
        }

        // 3. 统计
        int conflictCount = conflicts.size();
        String status = conflictCount == 0 ? "PASS" : (conflictCount > 5 ? "FAILED" : "WARNING");

        // 4. 保存新报告
        ConsistencyReport report = new ConsistencyReport();
        report.setProject(project);
        report.setCheckScope(request.getScope());
        report.setStatus(status);
        report.setIsActive(1);
        report.setCheckedItemsCount(calculateCheckedItems(projectId));
        report.setConflictsCount(conflictCount);

        // 序列化冲突详情
        try {
            report.setConflictsJson(objectMapper.writeValueAsString(conflicts));
            if (request.getTargetNodeIds() != null) {
                report.setTargetNodeIds(objectMapper.writeValueAsString(request.getTargetNodeIds()));
            } else {
                report.setTargetNodeIds("[]");
            }
        } catch (Exception e) {
            throw new BizException("报告序列化失败: " + e.getMessage());
        }

        ConsistencyReport saved = reportRepository.save(report);
        return saved.getId();
    }

    @Override
    public ConsistencyReportDTO getLatest(Long projectId) {
        ConsistencyReport report = reportRepository
                .findTopByProjectIdAndIsActiveOrderByCheckedAtDesc(projectId, 1)
                .orElseThrow(() -> new BizException("暂无检查报告"));

        return convertToDTO(report);
    }

    @Override
    public List<ReportHistoryDTO> listHistory(Long projectId, Integer limit) {
        List<ConsistencyReport> reports = reportRepository.findByProjectIdOrderByCheckedAtDesc(projectId);
        List<ReportHistoryDTO> result = new ArrayList<>();

        int count = 0;
        for (ConsistencyReport report : reports) {
            if (count >= limit) {
                break;
            }
            result.add(convertToHistoryDTO(report));
            count++;
        }
        return result;
    }

    @Override
    public ConsistencyReportDetailDTO getDetail(Long reportId) {
        ConsistencyReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BizException("报告不存在: " + reportId));

        return convertToDetailDTO(report);
    }

    @Override
    @Transactional
    public void markResolved(Long reportId, ResolveConflictRequest request) {
        // 实际业务中，这里可以记录哪个冲突被解决
        // 简化实现：仅校验报告存在
        ConsistencyReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BizException("报告不存在"));

        // 可选：从 conflicts_json 中移除特定冲突，或标记为已解决
        // 当前版本仅作占位
    }

    // ========== 私有检查方法 ==========

    private List<ConflictDTO> performRuleBasedCheck(Long projectId, TriggerCheckRequest request) {
        List<ConflictDTO> conflicts = new ArrayList<>();
        List<StoryNode> nodes;

        // 根据范围获取节点
        if ("FULL".equals(request.getScope())) {
            nodes = nodeRepository.findByProjectId(projectId);
        } else if (request.getTargetNodeIds() != null && !request.getTargetNodeIds().isEmpty()) {
            nodes = nodeRepository.findAllById(request.getTargetNodeIds());
        } else {
            nodes = new ArrayList<>();
        }

        // 检查1: 孤立节点（无入边也无出边）
        for (StoryNode node : nodes) {
            List<StoryEdge> outEdges = edgeRepository.findBySourceId(node.getId());
            List<StoryEdge> inEdges = new ArrayList<>();

            // 查询入边需要遍历所有边（或添加专门查询），这里简化
            List<StoryEdge> allEdges = edgeRepository.findByProjectId(projectId);
            for (StoryEdge edge : allEdges) {
                if (edge.getTarget().getId().equals(node.getId())) {
                    inEdges.add(edge);
                }
            }

            if (outEdges.isEmpty() && inEdges.isEmpty() && nodes.size() > 1) {
                ConflictDTO conflict = new ConflictDTO();
                conflict.setType("ISOLATED_NODE");
                conflict.setSeverity("WARNING");
                conflict.setDescription("节点 [" + node.getNodeName() + "] 是孤立的，无连接");
                List<String> locs = new ArrayList<>();
                locs.add("node:" + node.getId());
                conflict.setLocations(locs);
                conflicts.add(conflict);
            }
        }

        // 检查2: 重复边（同一源-目标对多次出现）
        Map<String, List<String>> edgeMap = new HashMap<>();
        List<StoryEdge> allEdges = edgeRepository.findByProjectId(projectId);
        for (StoryEdge edge : allEdges) {
            String key = edge.getSource().getId() + "->" + edge.getTarget().getId();
            if (!edgeMap.containsKey(key)) {
                edgeMap.put(key, new ArrayList<>());
            }
            edgeMap.get(key).add(edge.getId().toString());
        }

        for (Map.Entry<String, List<String>> entry : edgeMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                ConflictDTO conflict = new ConflictDTO();
                conflict.setType("DUPLICATE_EDGE");
                conflict.setSeverity("ERROR");
                conflict.setDescription("源节点到目标节点存在重复连接");
                conflict.setLocations(entry.getValue());
                conflicts.add(conflict);
            }
        }

        return conflicts;
    }

    private List<ConflictDTO> performAiSemanticCheck(Long projectId, TriggerCheckRequest request) {
        List<ConflictDTO> conflicts = new ArrayList<>();

        // 组装项目上下文
        StringBuilder context = new StringBuilder();
        List<StoryNode> nodes = nodeRepository.findByProjectId(projectId);

        context.append("剧情节点列表:\n");
        for (StoryNode node : nodes) {
            context.append("- ").append(node.getNodeName()).append(": ")
                    .append(node.getSceneDescription() != null ? node.getSceneDescription() : "无描述")
                    .append("\n");
        }

        String prompt = request.getPrompt() != null ? request.getPrompt() : "检查剧情逻辑一致性";

        String systemPrompt = "你是一位剧情逻辑审查员，擅长发现叙事漏洞、时间线冲突和角色行为不一致。" +
                "请以JSON格式返回发现的冲突列表：[{type, severity, description, locations}]，如无冲突返回空数组[]。";

        String aiResponse = aiClientService.syncCompletion(systemPrompt,
                context.toString() + "\n审查重点：" + prompt);

        // 解析 AI 返回的 JSON（简化实现，实际需健壮解析）
        try {
            List<ConsistencyReportDetailDTO.ConflictDTO> aiConflicts = objectMapper.readValue(aiResponse,
                    new TypeReference<List<ConflictDTO>>() {});
            if (aiConflicts != null) {
                conflicts.addAll(aiConflicts);
            }
        } catch (Exception e) {
            // AI 返回格式不对，忽略或记录日志
            ConflictDTO conflict = new ConflictDTO();
            conflict.setType("AI_PARSE_ERROR");
            conflict.setSeverity("WARNING");
            conflict.setDescription("AI 检查结果解析失败: " + e.getMessage());
            conflicts.add(conflict);
        }

        return conflicts;
    }

    private int calculateCheckedItems(Long projectId) {
        int nodeCount = nodeRepository.findByProjectId(projectId).size();
        int edgeCount = edgeRepository.findByProjectId(projectId).size();
        return nodeCount + edgeCount;
    }

    // ========== 私有转换方法 ==========

    private ConsistencyReportDTO convertToDTO(ConsistencyReport report) {
        ConsistencyReportDTO dto = new ConsistencyReportDTO();
        dto.setId(report.getId());
        dto.setStatus(report.getStatus());
        dto.setIsActive(report.getIsActive() == 1);
        dto.setCheckedItemsCount(report.getCheckedItemsCount());
        dto.setConflictsCount(report.getConflictsCount());
        dto.setCheckedAt(ServiceUtil.formatDateTime(report.getCheckedAt()));
        return dto;
    }

    private ConsistencyReportDetailDTO convertToDetailDTO(ConsistencyReport report) {
        ConsistencyReportDetailDTO dto = new ConsistencyReportDetailDTO();
        dto.setId(report.getId());
        dto.setStatus(report.getStatus());
        dto.setIsActive(report.getIsActive() == 1);
        dto.setCheckedItemsCount(report.getCheckedItemsCount());
        dto.setConflictsCount(report.getConflictsCount());
        dto.setCheckedAt(ServiceUtil.formatDateTime(report.getCheckedAt()));

        // 反序列化冲突列表
        try {
            List<ConsistencyReportDetailDTO.ConflictDTO> conflicts = objectMapper.readValue(report.getConflictsJson(),
                    new TypeReference<List<ConsistencyReportDetailDTO.ConflictDTO>>() {});
            dto.setConflicts(conflicts);
        } catch (Exception e) {
            dto.setConflicts(new ArrayList<>());
        }

        return dto;
    }

    private ReportHistoryDTO convertToHistoryDTO(ConsistencyReport report) {
        ReportHistoryDTO dto = new ReportHistoryDTO();
        dto.setId(report.getId());
        dto.setStatus(report.getStatus());
        dto.setIsActive(report.getIsActive() == 1);
        dto.setCheckedAt(ServiceUtil.formatDateTime(report.getCheckedAt()));
        return dto;
    }
}
