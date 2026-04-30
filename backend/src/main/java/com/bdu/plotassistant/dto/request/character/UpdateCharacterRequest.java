package com.bdu.plotassistant.dto.request.character;

import lombok.Data;

import javax.validation.constraints.Size;
import java.util.List;

@Data
public class UpdateCharacterRequest {

    @Size(max = 64, message = "角色名最长64")
    private String name;

    @Size(max = 32, message = "角色类型最长32")
    private String roleType;

    private String personaPrompt;

    @Size(max = 255, message = "语言特征最长255")
    private String speechPattern;

    private List<String> knowledgeScope;
}
