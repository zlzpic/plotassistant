package com.bdu.plotassistant.dto.response.character;

import lombok.Data;

@Data
public class CharacterProfileDTO {
    private String id;
    private String name;
    private String roleType;      // PROTAGONIST/ANTAGONIST/NPC
    private String personaPrompt; // 人设
    private String speechPattern; // 语言特征
    private String knowledgeScope; // JSON字符串
    private Integer status;       // 0=草稿,1=重要,2=NPC
}
