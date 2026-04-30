package com.bdu.plotassistant.service;

import com.bdu.plotassistant.dto.request.storyscript.GenerateWholeLineRequest;
import com.bdu.plotassistant.dto.response.storyscript.StoryScriptDTO;
import com.bdu.plotassistant.entity.StoryScript;

import java.util.List;

public interface StoryScriptService {

    /**
     * L9: 生成完整剧情
     */
    String generateWholeLine(Long projectId, GenerateWholeLineRequest request);

    /**
     * 保存完整剧情
     */
    Long saveWholeLine(Long projectId, String branchPath, String content, String[] nodeIds);

    /**
     * 标记为正史（主线）
     */
    void markAsCanon(Long scriptId);

    /**
     * 查询项目的所有剧本版本
     */
    List<StoryScriptDTO> listByProject(Long projectId);

    /**
     * 获取剧本详情（包含完整小说内容）
     * @param projectId 项目ID（用于权限校验）
     * @param scriptId 剧本ID
     * @return 剧本详情DTO（含完整内容）
     */
    StoryScriptDTO getScriptDetail(Long projectId, Long scriptId);
}
