package com.bdu.plotassistant.service.impl;

import com.bdu.plotassistant.dto.request.playthrough.*;
import com.bdu.plotassistant.dto.response.playthrough.*;
import com.bdu.plotassistant.dto.response.storynode.NodeDTO;
import com.bdu.plotassistant.entity.PlaythroughSession;
import com.bdu.plotassistant.entity.PlaythroughState;
import com.bdu.plotassistant.entity.Project;
import com.bdu.plotassistant.entity.User;
import com.bdu.plotassistant.repository.*;
import com.bdu.plotassistant.service.PlaythroughSessionService;
import com.bdu.plotassistant.service.StoryNodeService;
import com.bdu.plotassistant.utils.BizException;
import com.bdu.plotassistant.utils.ServiceUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
public class PlaythroughSessionServiceImpl implements PlaythroughSessionService {

    private final PlaythroughSessionRepository sessionRepository;
    private final PlaythroughStateRepository stateRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final StoryNodeService storyNodeService;
    private final ObjectMapper objectMapper;

    public PlaythroughSessionServiceImpl(PlaythroughSessionRepository sessionRepository,
                                         PlaythroughStateRepository stateRepository,
                                         ProjectRepository projectRepository,
                                         UserRepository userRepository,
                                         StoryNodeService storyNodeService,
                                         ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.stateRepository = stateRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.storyNodeService = storyNodeService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public String create(Long userId, CreatePlaythroughRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new BizException("项目不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));

        // 生成UUID
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        // 确定起始节点
        String startNodeId = request.getSourceNodeId();
        if (ServiceUtil.isEmpty(startNodeId)) {
            // 默认取项目第一个节点
            List<NodeDTO> nodes = storyNodeService.getByProject(project.getId());
            if (nodes.isEmpty()) {
                throw new BizException("项目暂无剧情节点，无法开始");
            }
            startNodeId = nodes.get(0).getId();
        }

        // 创建会话
        PlaythroughSession session = new PlaythroughSession();
        session.setId(sessionId);
        session.setProject(project);
        session.setUser(user);
        session.setSessionType(request.getSessionType());
        session.setSourceNodeId(startNodeId);
        session.setStatus("ACTIVE");
        session.setInitialPrompt(request.getInitialPrompt());

        PlaythroughSession savedSession = sessionRepository.save(session);

        // 初始化状态
        PlaythroughState state = new PlaythroughState();
        state.setSession(savedSession);
        state.setCurrentNodeId(startNodeId);
        state.setVariablesJson("{}");
        state.setTurnCount(0);

        stateRepository.save(state);

        return savedSession.getId();
    }

    @Override
    public List<PlaythroughSummaryDTO> list(Long userId, Long projectId, String sessionType) {
        List<PlaythroughSession> sessions = sessionRepository.findByUserId(userId);
        List<PlaythroughSummaryDTO> result = new ArrayList<>();

        for (PlaythroughSession session : sessions) {
            // 过滤条件
            if (projectId != null && !session.getProject().getId().equals(projectId)) {
                continue;
            }
            if (sessionType != null && !sessionType.equals(session.getSessionType())) {
                continue;
            }

            result.add(convertToSummaryDTO(session));
        }

        return result;
    }

    @Override
    public PlaythroughDetailDTO getDetail(String sessionId) {
        PlaythroughSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BizException("会话不存在: " + sessionId));

        return convertToDetailDTO(session);
    }

    @Override
    @Transactional
    public void delete(String sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new BizException("会话不存在: " + sessionId);
        }
        sessionRepository.deleteById(sessionId);
        // 级联删除状态由JPA外键处理
    }

    @Override
    @Transactional
    public void complete(String sessionId) {
        PlaythroughSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BizException("会话不存在: " + sessionId));

        session.setStatus("COMPLETED");
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void abandon(String sessionId) {
        PlaythroughSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BizException("会话不存在: " + sessionId));

        session.setStatus("ABANDONED");
        session.setEndedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Override
    @Transactional
    public void jumpToNode(String sessionId, JumpToNodeRequest request) {
        PlaythroughSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BizException("会话不存在: " + sessionId));

        // 只有预演模式支持跳转
        if (!"TRIAL".equals(session.getSessionType())) {
            throw new BizException("只有预演模式支持节点跳转");
        }

        PlaythroughState state = stateRepository.findById(sessionId)
                .orElseThrow(() -> new BizException("状态不存在"));

        state.setCurrentNodeId(request.getTargetNodeId());
        state.setTurnCount(0); // 重置轮次
        // 可选：清空历史或保留
        state.setHistorySummary(null);

        stateRepository.save(state);
    }

    @Override
    @Transactional
    public void reset(String sessionId) {
        PlaythroughSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BizException("会话不存在: " + sessionId));

        PlaythroughState state = stateRepository.findById(sessionId)
                .orElseThrow(() -> new BizException("状态不存在"));

        // 重置到起始节点
        state.setCurrentNodeId(session.getSourceNodeId());
        state.setVariablesJson("{}");
        state.setHistorySummary(null);
        state.setTurnCount(0);
        state.setLastInput(null);
        state.setLastReply(null);
        state.setGmPrompt(null);

        stateRepository.save(state);
    }

    @Override
    @Transactional
    public String convertToOfficial(String sessionId) {
        PlaythroughSession oldSession = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BizException("会话不存在: " + sessionId));

        // 深拷贝为新正式会话
        CreatePlaythroughRequest request = new CreatePlaythroughRequest();
        request.setProjectId(oldSession.getProject().getId());
        request.setSessionType("PLAYTHROUGH");
        request.setSourceNodeId(oldSession.getSourceNodeId());
        request.setInitialPrompt(oldSession.getInitialPrompt());

        return create(oldSession.getUser().getId(), request);
    }

    @Override
    public PlaythroughSessionDTO getById(String sessionId) {
        PlaythroughSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BizException("会话不存在: " + sessionId));

        PlaythroughSessionDTO dto = new PlaythroughSessionDTO();
        dto.setId(session.getId());
        dto.setProjectId(session.getProject().getId());
        dto.setSessionType(session.getSessionType());
        dto.setStatus(session.getStatus());
        return dto;
    }

    // ========== 私有转换方法 ==========

    private PlaythroughSummaryDTO convertToSummaryDTO(PlaythroughSession session) {
        PlaythroughSummaryDTO dto = new PlaythroughSummaryDTO();
        dto.setId(session.getId());
        dto.setProjectId(session.getProject().getId());
        dto.setSessionType(session.getSessionType());
        dto.setStatus(session.getStatus());
        dto.setStartedAt(ServiceUtil.formatDateTime(session.getStartedAt()));
        dto.setEndedAt(session.getEndedAt() != null ? ServiceUtil.formatDateTime(session.getEndedAt()) : null);
        return dto;
    }

    private PlaythroughDetailDTO convertToDetailDTO(PlaythroughSession session) {
        PlaythroughDetailDTO dto = new PlaythroughDetailDTO();
        dto.setId(session.getId());
        dto.setProjectId(session.getProject().getId());
        dto.setSessionType(session.getSessionType());
        dto.setStatus(session.getStatus());
        dto.setSourceNodeId(session.getSourceNodeId());
        dto.setInitialPrompt(session.getInitialPrompt());
        dto.setStartedAt(ServiceUtil.formatDateTime(session.getStartedAt()));
        dto.setEndedAt(session.getEndedAt() != null ? ServiceUtil.formatDateTime(session.getEndedAt()) : null);
        return dto;
    }
}
