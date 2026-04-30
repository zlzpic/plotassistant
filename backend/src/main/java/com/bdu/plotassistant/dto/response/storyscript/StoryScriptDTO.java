package com.bdu.plotassistant.dto.response.storyscript;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StoryScriptDTO {
    private Long id;
    private String branchPath;
    private String contentType;
    private String content;  // 完整小说内容
    private String referencedNodes;
    private Boolean isCanon;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // 精简的项目信息（避免嵌套整个 Project 对象）
    private Long projectId;
    private String projectName;

    // 可以添加一个摘要字段（前200字）
    private String contentSummary;
}
