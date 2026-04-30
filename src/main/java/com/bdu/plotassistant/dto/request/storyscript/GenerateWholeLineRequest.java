package com.bdu.plotassistant.dto.request.storyscript;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Data;

@Data
public class GenerateWholeLineRequest {

    @NotBlank(message = "分支路径不能为空")
    @Pattern(
            regexp = "^[a-zA-Z0-9_]+(\\|[0-9]+)?(->[a-zA-Z0-9_]+(\\|[0-9]+)?)*$",
            message = "路径格式错误，正确格式：node_001|1->node_002|5->node_003 或 node_001->node_002"
    )
    private String branchPath;
    // 格式："node_001|1->node_002|5->node_003"
    // 格式说明：节点ID|边ID->节点ID|边ID->节点ID
    // 边ID为数据库中的Long类型ID（如1, 5, 12）

    private String style;       // 写作风格（可选）："悬疑", "轻松", "黑暗", "史诗"
}
