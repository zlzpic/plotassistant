package com.bdu.plotassistant.dto.request.storynode;

import lombok.Data;

@Data
public class GenerateNodesRequest {
    private boolean clearExisting = false;  // 是否清空现有节点（危险操作，默认false）
}
