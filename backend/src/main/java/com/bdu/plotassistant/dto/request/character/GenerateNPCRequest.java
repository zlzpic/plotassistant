package com.bdu.plotassistant.dto.request.character;

import lombok.Data;

@Data
public class GenerateNPCRequest {
    private Integer count = 1;  // 默认生成2个NPC
    private String prompt;      // 特殊要求（如"生成一个敌对NPC"）
}
