package com.bdu.plotassistant.service.impl;

import com.bdu.plotassistant.dto.request.playthrough.*;
import com.bdu.plotassistant.dto.response.character.CharacterDTO;
import com.bdu.plotassistant.dto.response.playthrough.*;
import com.bdu.plotassistant.dto.response.worldsetting.WorldSettingDTO;
import com.bdu.plotassistant.entity.*;
import com.bdu.plotassistant.repository.*;
import com.bdu.plotassistant.service.*;
import com.bdu.plotassistant.utils.BizException;
import com.bdu.plotassistant.utils.ServiceUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlaythroughInteractionServiceImpl implements PlaythroughInteractionService {

    private final PlaythroughStateRepository stateRepository;
    private final PlaythroughLogRepository logRepository;
    private final PlaythroughSessionRepository sessionRepository;
    private final StoryNodeRepository nodeRepository;
    private final CharacterService characterService;
    private final WorldSettingService worldSettingService;
    private final AiClientService aiClientService;
    private final ObjectMapper objectMapper;

    public PlaythroughInteractionServiceImpl(PlaythroughStateRepository stateRepository,
                                             PlaythroughLogRepository logRepository,
                                             PlaythroughSessionRepository sessionRepository,
                                             StoryNodeRepository nodeRepository,
                                             CharacterService characterService,
                                             WorldSettingService worldSettingService,
                                             AiClientService aiClientService,
                                             ObjectMapper objectMapper) {
        this.stateRepository = stateRepository;
        this.logRepository = logRepository;
        this.sessionRepository = sessionRepository;
        this.nodeRepository = nodeRepository;
        this.characterService = characterService;
        this.worldSettingService = worldSettingService;
        this.aiClientService = aiClientService;
        this.objectMapper = objectMapper;
    }

    @Override
    public PlaythroughStateDTO getCurrentState(String sessionId) {
        PlaythroughState state = stateRepository.findById(sessionId)
                .orElseThrow(() -> new BizException("状态不存在: " + sessionId));

        return convertToStateDTO(state);
    }

    @Override
    @Transactional
    public SseEmitter interact(String sessionId, InteractRequest request) {
        PlaythroughState state = stateRepository.findById(sessionId)
                .orElseThrow(() -> new BizException("状态不存在: " + sessionId));

        PlaythroughSession session = state.getSession();

        // 校验会话状态
        if (!"ACTIVE".equals(session.getStatus())) {
            throw new BizException("会话已结束，无法交互");
        }

        // 获取当前场景信息
        StoryNode currentNode = nodeRepository.findById(state.getCurrentNodeId())
                .orElseThrow(() -> new BizException("当前节点不存在: " + state.getCurrentNodeId()));

        // 组装系统提示词（上下文）
        String systemPrompt = buildSystemPrompt(session, state, currentNode);

        // 获取当前轮次
        Integer currentTurn = state.getTurnCount() + 1;

        // 流式调用AI
        return aiClientService.streamCompletion(systemPrompt, request.getUserInput(), content -> {
            // 回调：保存对话记录
            saveInteractionLog(sessionId, currentTurn, request.getUserInput(), content, state);
            // 更新状态
            updateStateAfterInteraction(state, content, currentNode);
        });
    }

    @Override
    @Transactional
    public void enableGMMode(String sessionId, GMModeRequest request) {
        PlaythroughState state = stateRepository.findById(sessionId)
                .orElseThrow(() -> new BizException("状态不存在: " + sessionId));

        state.setGmPrompt(request.getGmPrompt());
        stateRepository.save(state);
    }

    @Override
    @Transactional
    public void disableGMMode(String sessionId) {
        PlaythroughState state = stateRepository.findById(sessionId)
                .orElseThrow(() -> new BizException("状态不存在: " + sessionId));

        state.setGmPrompt(null);
        stateRepository.save(state);
    }

    @Override
    public List<InteractionLogDTO> getHistory(String sessionId, Integer limit) {
        List<PlaythroughLog> logs = logRepository.findBySessionIdOrderBySequenceNumDesc(sessionId);
        List<InteractionLogDTO> result = new ArrayList<>();

        int count = 0;
        for (PlaythroughLog log : logs) {
            if (count >= limit) {
                break;
            }
            result.add(convertToLogDTO(log));
            count++;
        }

        return result;
    }

    @Override
    public InteractionLogDetailDTO getSpecificLog(String sessionId, Integer sequenceNum) {
        PlaythroughLog log = logRepository.findBySessionIdAndSequenceNum(sessionId, sequenceNum)
                .orElseThrow(() -> new BizException("记录不存在"));

        return convertToLogDetailDTO(log);
    }

    @Override
    @Transactional
    public Long createSavePoint(String sessionId, CreateSavePointRequest request) {
        // Demo阶段简化实现：实际应创建PlaythroughSave实体
        // 这里仅返回模拟ID
        return System.currentTimeMillis();
    }

    @Override
    public List<SavePointDTO> listSavePoints(String sessionId) {
        // Demo阶段返回空列表
        return new ArrayList<>();
    }

    @Override
    @Transactional
    public void restoreToSavePoint(String sessionId, RestoreRequest request) {
        // Demo阶段简化实现
        // 实际应从PlaythroughSave恢复状态
        throw new BizException("功能开发中");
    }

    // ========== 私有辅助方法 ==========

    private String buildSystemPrompt(PlaythroughSession session, PlaythroughState state, StoryNode currentNode) {
        StringBuilder sb = new StringBuilder();

        // 1. 世界观背景
        try {
            WorldSettingDTO world = worldSettingService.getByProjectId(session.getProject().getId());
            sb.append("世界观：").append(world.getGenre());
            if (world.getCoreConflict() != null) {
                sb.append("，核心冲突：").append(world.getCoreConflict());
            }
            sb.append("\n");
        } catch (Exception e) {
            sb.append("世界观：未设定\n");
        }

        // 2. 当前场景
        sb.append("当前场景：").append(currentNode.getNodeName()).append("\n");
        if (currentNode.getSceneDescription() != null) {
            sb.append("场景描述：").append(currentNode.getSceneDescription()).append("\n");
        }

        // 3. 角色设定
        if (state.getActiveCharacterId() != null) {
            try {
                CharacterDTO character = characterService.getByIds(java.util.Collections.singletonList(state.getActiveCharacterId())).get(0);
                sb.append("对话角色：").append(character.getName()).append("\n");
                sb.append("角色人设：").append(character.getPersonaPrompt()).append("\n");
            } catch (Exception e) {
                // 忽略
            }
        }

        // 4. 历史摘要
        if (state.getHistorySummary() != null) {
            sb.append("前文摘要：").append(state.getHistorySummary()).append("\n");
        }

        // 5. GM模式覆盖（如果有）
        if (state.getGmPrompt() != null) {
            sb.append("【GM指令】").append(state.getGmPrompt()).append("\n");
        }

        sb.append("请以第一人称扮演角色回复，保持人设一致性。");

        return sb.toString();
    }

    private void saveInteractionLog(String sessionId, Integer sequenceNum, String userInput, String aiReply, PlaythroughState state) {
        PlaythroughLog log = new PlaythroughLog();
        log.setSession(state.getSession());
        log.setSequenceNum(sequenceNum);
        log.setLogType("DIALOGUE");
        log.setUserInput(userInput);
        log.setAiReply(aiReply);
        log.setCharacterId(state.getActiveCharacterId());
        log.setFromNodeId(state.getCurrentNodeId());

        // 保存变量快照
        try {
            log.setVariablesSnapshot(state.getVariablesJson());
            log.setPromptUsed(""); // 实际应记录完整prompt
        } catch (Exception e) {
            // ignore
        }

        logRepository.save(log);
    }

    private void updateStateAfterInteraction(PlaythroughState state, String aiResponse, StoryNode currentNode) {
        // 更新轮次
        state.setTurnCount(state.getTurnCount() + 1);
        state.setLastInput(state.getLastInput()); // 实际应从交互中获取
        state.setLastReply(aiResponse);

        // 更新历史摘要（简化：追加最后回复）
        String summary = state.getHistorySummary();
        if (summary == null) {
            summary = "";
        }
        summary += "【AI】" + aiResponse.substring(0, Math.min(50, aiResponse.length())) + "... ";
        state.setHistorySummary(summary);

        // 更新变量（简化实现，实际应解析AI响应中的变量变化）
        // ...

        stateRepository.save(state);
    }

    private PlaythroughStateDTO convertToStateDTO(PlaythroughState state) {
        PlaythroughStateDTO dto = new PlaythroughStateDTO();
        dto.setSessionId(state.getSession().getId());
        dto.setCurrentNodeId(state.getCurrentNodeId());
        dto.setActiveCharacterId(state.getActiveCharacterId());

        // 解析变量JSON
        try {
            Map<String, Object> vars = objectMapper.readValue(state.getVariablesJson(),
                    new TypeReference<Map<String, Object>>() {});
            dto.setVariables(vars);
        } catch (Exception e) {
            dto.setVariables(new HashMap<>());
        }

        dto.setHistorySummary(state.getHistorySummary());
        dto.setTurnCount(state.getTurnCount());
        dto.setLastInput(state.getLastInput());
        dto.setLastReply(state.getLastReply());

        return dto;
    }

    private InteractionLogDTO convertToLogDTO(PlaythroughLog log) {
        InteractionLogDTO dto = new InteractionLogDTO();
        dto.setSequenceNum(log.getSequenceNum());
        dto.setLogType(log.getLogType());
        dto.setUserInput(log.getUserInput());
        dto.setAiReply(log.getAiReply());
        dto.setCreatedAt(ServiceUtil.formatDateTime(log.getCreatedAt()));
        return dto;
    }

    private InteractionLogDetailDTO convertToLogDetailDTO(PlaythroughLog log) {
        InteractionLogDetailDTO dto = new InteractionLogDetailDTO();
        dto.setSequenceNum(log.getSequenceNum());
        dto.setLogType(log.getLogType());
        dto.setUserInput(log.getUserInput());
        dto.setAiReply(log.getAiReply());
        dto.setCharacterId(log.getCharacterId());
        dto.setFromNodeId(log.getFromNodeId());
        dto.setToNodeId(log.getToNodeId());
        dto.setTransitionReason(log.getTransitionReason());

        // 解析变量快照
        try {
            Map<String, Object> vars = objectMapper.readValue(log.getVariablesSnapshot(),
                    new TypeReference<Map<String, Object>>() {});
            dto.setVariablesSnapshot(vars);
        } catch (Exception e) {
            dto.setVariablesSnapshot(new HashMap<>());
        }

        dto.setPromptUsed(log.getPromptUsed());
        dto.setCreatedAt(ServiceUtil.formatDateTime(log.getCreatedAt()));

        return dto;
    }
}
