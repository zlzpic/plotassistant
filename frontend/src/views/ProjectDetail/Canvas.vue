<template>
  <div class="canvas-page">
    <!-- 工具栏保持不变 -->
    <div class="canvas-toolbar">
      <div class="toolbar-left">
        <el-button type="primary" :icon="Plus" @click="addNode">添加节点</el-button>
        <el-button type="info" :icon="Share" @click="showManualEdgeDialog">手动连接节点</el-button>
        <el-button 
          type="success" 
          :icon="Check" 
          @click="saveLayout" 
          :disabled="!hasChanges"
        >
          保存布局
          <el-tag v-if="hasChanges" type="danger" size="small" effect="dark" class="unsaved-tag">
            未保存
          </el-tag>
        </el-button>
        <el-button 
          type="warning" 
          :icon="RefreshLeft" 
          @click="cancelAllChanges" 
          :disabled="!hasChanges"
          plain
        >
          取消改动
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" :icon="Lightning" @click="generateNodes">AI 生成节点 (L4)</el-button>
        <el-button type="warning" :icon="MagicStick" @click="showAIGenerateEdgeDialog">AI 生成选项 (L8)</el-button>
        <el-button type="info" :icon="View" @click="generateDescriptions" :disabled="!selectedNode">生成场景 (L5)</el-button>
      </div>
    </div>

    <div class="canvas-container">
      <!-- VueFlow 画布保持不变 -->
      <VueFlow
        v-model="elements"
        :default-edge-options="{ type: 'smoothstep' }"
        :default-zoom="1"
        :min-zoom="0.2"
        :max-zoom="4"
        @node-click="onNodeClick"
        @edge-click="onEdgeClick"
        @node-drag-stop="onNodeDragStop"
        @connect="onConnect"
        @pane-click="closePropertyPanel"
        fit-view-on-init
      >
        <Background pattern-color="#c9bfb0" :gap="20" />
        <Controls />
        <template #node-default="nodeProps">
          <NodeCard 
            :data="nodeProps.data" 
            :selected="selectedNode?.id === nodeProps.id"
            :has-dialogue="!!nodeProps.data?.hasDialogue"
          />
        </template>
        <!-- 【关键新增】自定义边模板 - 强制分离重叠边 -->
        <template #edge-custom="edgeProps">
          <CustomEdge v-bind="edgeProps" />
        </template>
      </VueFlow>

      <!-- 修改后的属性面板 -->
      <div class="property-panel" v-if="showPropertyPanel">
  <el-card v-if="selectedNode">
    <template #header>
      <div class="panel-header">
        <div>
          <span>节点属性</span>
          <el-tag size="small" type="info" class="node-id-tag">{{ selectedNode.id }}</el-tag>
        </div>
        <div class="panel-actions">
          <el-button type="danger" link :icon="Delete" @click="deleteNode">删除</el-button>
          <el-button link :icon="Close" @click="closePropertyPanel" class="close-btn">关闭</el-button>
        </div>
      </div>
    </template>
      
      <el-form label-position="top" size="small">
        <el-form-item label="节点名称">
          <el-input 
            v-model="selectedNode.data.nodeName" 
            @blur="updateNodeData"
            placeholder="输入节点名称"
          />
        </el-form-item>
        
        <!-- 【修改】场景描述显示区域，支持JSON解析 -->
        <el-form-item label="场景描述">
          <div class="scene-desc-header">
            <span v-if="selectedNode.data?.actIndex || selectedNode.data?.beatIndex" class="scene-index">
              第 {{ selectedNode.data.actIndex || 1 }} 幕 - 节拍 {{ selectedNode.data.beatIndex || 1 }}
            </span>
          </div>
          <el-input 
            v-model="displaySceneDescription" 
            type="textarea" 
            :rows="6" 
            @blur="updateNodeSceneDesc"
            placeholder="暂无场景描述，点击下方按钮生成..."
          />
          <div class="scene-actions">
            <el-button 
              type="primary" 
              link 
              size="small" 
              :icon="View"
              @click="showL5GenerateDialog"
            >
              {{ selectedNode.data?.sceneDescription ? '重新生成场景 (L5)' : '生成场景描述 (L5)' }}
            </el-button>
          </div>
        </el-form-item>
        <!-- 【新增】L6 场景NPC区域 -->
<el-divider content-position="left">
  <span class="divider-text">
    <el-icon><UserFilled /></el-icon>
    场景NPC (L6)
  </span>
</el-divider>

<div class="npc-section">
  <!-- 已生成NPC预览 -->
  <div v-if="selectedNode.data?.generatedNPCIds?.length" class="npc-list">
    <div class="npc-header">
      <span class="npc-count">已生成 {{ selectedNode.data.generatedNPCIds.length }} 个NPC</span>
      <el-button type="primary" link size="small" @click="showGenerateNPCDialog">
        继续生成
      </el-button>
    </div>
    <div class="npc-tags">
      <el-tooltip 
        v-for="npcId in selectedNode.data.generatedNPCIds" 
        :key="npcId"
        :content="getNPCDetail(npcId)?.description || '点击查看详情'"
        placement="top"
      >
        <el-tag 
          type="success" 
          effect="dark" 
          class="npc-tag"
          @click="quickAssociateNPC(npcId)"
          :closable="true"
          @close="removeNPCFromNode(npcId)"
        >
          {{ getNPCDetail(npcId)?.name || '新NPC' }}
        </el-tag>
      </el-tooltip>
    </div>
    <div class="npc-hint" v-if="selectedNode.data?.associatedCharIds?.length">
      <el-icon><InfoFilled /></el-icon>
      点击标签可快速关联到当前节点
    </div>
  </div>

  <!-- 空状态 -->
  <div v-else class="npc-empty">
    <el-empty description="点击下方按钮生成场景NPC" :image-size="60">
      <el-button 
        type="primary" 
        :icon="UserFilled" 
        @click="showGenerateNPCDialog"
        :disabled="!selectedNode.data?.sceneDescription"
        size="small"
      >
        生成场景NPC
      </el-button>
      <div v-if="!selectedNode.data?.sceneDescription" class="prerequisite-hint">
        <el-icon><Warning /></el-icon>
        请先生成场景描述(L5)
      </div>
    </el-empty>
  </div>
</div>

        <el-form-item label="关联角色">
          <el-select 
            v-model="selectedNode.data.associatedCharIds" 
            multiple 
            placeholder="选择关联角色" 
            @change="updateNodeData"
          >
            <el-option 
              v-for="char in characters" 
              :key="char.id" 
              :label="char.name" 
              :value="char.id" 
            />
          </el-select>
        </el-form-item>
      </el-form>

      <!-- 对话区域保持不变 -->
      <el-divider content-position="left">
        <span class="divider-text">
          <el-icon><ChatLineRound /></el-icon>
          场景对话 (L7)
        </span>
      </el-divider>

          <div class="dialogue-section">
            <div v-if="selectedNode.data?.dialogueScene" class="dialogue-preview">
              <div class="dialogue-scene-title">{{ selectedNode.data.dialogueScene }}</div>
              <div class="dialogue-context">{{ selectedNode.data.dialogueContext }}</div>
              <el-button type="primary" link @click="showViewDialogueDialog">
                <el-icon><View /></el-icon>
                查看完整对话 ({{ selectedNode.data.dialogueLineCount || 0 }} 句)
              </el-button>
            </div>
            <el-button 
              v-else
              type="primary" 
              :icon="ChatDotRound" 
              @click="showGenerateDialogueDialog"
              class="generate-dialogue-btn"
            >
              生成场景对话
            </el-button>
            <el-button 
              v-if="selectedNode.data?.dialogueScene"
              type="warning" 
              link 
              size="small"
              @click="showGenerateDialogueDialog"
            >
              重新生成
            </el-button>
          </div>
        </el-card>

        <el-card v-if="selectedEdge" class="edge-card">
    <template #header>
      <div class="panel-header">
        <div>
          <span>边属性</span>
          <el-tag size="small" type="warning" class="edge-id-tag">{{ selectedEdge.id }}</el-tag>
        </div>
        <div class="panel-actions">
          <el-button type="danger" link :icon="Delete" @click="deleteEdge">删除</el-button>
          <el-button link :icon="Close" @click="closePropertyPanel" class="close-btn">关闭</el-button>
        </div>
      </div>
    </template>
          <el-form label-position="top" size="small">
            <el-form-item label="选项文本">
              <el-input v-model="selectedEdge.label" @blur="updateEdgeData" placeholder="如：破门而入" />
            </el-form-item>
            <el-form-item label="条件表达式">
              <el-input 
                v-model="selectedEdge.data.conditionExpr" 
                placeholder="如: player.strength > 10" 
                @blur="updateEdgeData"
              />
            </el-form-item>
            <el-form-item label="成功结果">
              <el-input 
                v-model="selectedEdge.data.onSuccess" 
                type="textarea" 
                :rows="2" 
                @blur="updateEdgeData"
                placeholder="选择后的成功描述..."
              />
            </el-form-item>
            <el-form-item label="失败结果">
              <el-input 
                v-model="selectedEdge.data.onFailure" 
                type="textarea" 
                :rows="2" 
                @blur="updateEdgeData"
                placeholder="选择后的失败描述..."
              />
            </el-form-item>
          </el-form>
        </el-card>
      </div>
    </div>
    <!-- 【新增】L5 生成场景描述弹窗 -->
  <el-dialog 
    v-model="l5DialogVisible" 
    title="生成场景描述 (L5)" 
    width="600px"
    :close-on-click-modal="false"
  >
    <el-form label-position="top">
      <el-form-item label="氛围提示词（描述想要的场景氛围）" required>
        <el-input 
          v-model="l5Form.prompt" 
          type="textarea" 
          :rows="4"
          placeholder="如：氛围突出对生命漠视的严肃荒谬感、雨夜霓虹下的赛博朋克压抑感、古风仙侠的清冷孤寂..."
        />
      </el-form-item>
      <div class="prompt-hints">
        <el-tag 
          v-for="hint in promptHints" 
          :key="hint"
          size="small" 
          class="hint-tag"
          @click="l5Form.prompt = hint"
          style="cursor: pointer;"
        >
          {{ hint }}
        </el-tag>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="l5DialogVisible = false">取消</el-button>
      <el-button 
        type="primary" 
        @click="generateL5Description" 
        :loading="l5Generating"
        :disabled="!l5Form.prompt.trim()"
      >
        开始生成
      </el-button>
    </template>
  </el-dialog>

    <!-- 【新增】生成对话配置弹窗 -->
    <el-dialog 
      v-model="dialogueGenVisible" 
      title="生成场景对话 (L7)" 
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <el-form-item label="参与角色（至少选择2个）" required>
          <el-select
            v-model="dialogueForm.characterIds"
            multiple
            filterable
            placeholder="选择参与对话的角色"
            style="width: 100%"
          >
            <el-option
              v-for="char in characters"
              :key="char.id"
              :label="char.name"
              :value="char.id"
            >
              <div class="char-option">
                <span>{{ char.name }}</span>
                <el-tag size="small" type="info">{{ formatRoleType(char.roleType) }}</el-tag>
              </div>
            </el-option>
          </el-select>
          <div class="form-hint">已选择 {{ dialogueForm.characterIds.length }} 个角色（最少需要2个）</div>
        </el-form-item>

        <el-form-item label="情境提示词（可选）">
          <el-input 
            v-model="dialogueForm.prompt" 
            type="textarea" 
            :rows="3"
            placeholder="描述对话情境，如：展现主角发现背叛时的愤怒与失望、两个角色在雨夜的对峙..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogueGenVisible = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="generateDialogue" 
          :loading="dialogueGenerating"
          :disabled="dialogueForm.characterIds.length < 2"
        >
          开始生成
        </el-button>
      </template>
    </el-dialog>

    <!-- 【新增】对话预览弹窗 -->
    <el-dialog 
      v-model="dialogueViewVisible" 
      :title="`对话预览：${currentDialogue?.scene || '场景对话'}`" 
      width="800px"
      class="dialogue-preview-dialog"
    >
      <div v-if="currentDialogue" class="dialogue-container">
        <!-- 场景背景 -->
        <div class="scene-header">
          <el-alert 
            :title="currentDialogue.context" 
            type="info" 
            :closable="false"
            show-icon
          />
        </div>

        <!-- 对话气泡列表 -->
        <div class="dialogue-lines">
          <div 
            v-for="(line, index) in currentDialogue.lines" 
            :key="index"
            class="dialogue-bubble"
            :class="{ 'self': isProtagonist(line.speaker) }"
          >
            <div class="bubble-header">
              <el-avatar :size="32" :icon="UserFilled" />
              <div class="speaker-info">
                <div class="speaker-name">{{ line.speaker }}</div>
                <el-tag size="small" :type="getEmotionType(line.emotion)" class="emotion-tag">
                  {{ line.emotion }}
                </el-tag>
              </div>
            </div>
            <div class="bubble-content">
              <div class="line-text">{{ line.line }}</div>
              <el-collapse v-if="line.subtext" class="subtext-collapse">
                <el-collapse-item title="潜台词" name="1">
                  <div class="subtext">{{ line.subtext }}</div>
                </el-collapse-item>
              </el-collapse>
            </div>
          </div>
        </div>

        <!-- 玩家选项 -->
        <div v-if="currentDialogue.playerChoices?.length" class="player-choices">
          <el-divider content-position="left">玩家选项</el-divider>
          <div class="choices-list">
            <el-card 
              v-for="(choice, idx) in currentDialogue.playerChoices" 
              :key="idx"
              class="choice-card"
              shadow="hover"
            >
              <div class="choice-text">{{ choice.text }}</div>
              <div class="choice-impact">
                <el-icon><InfoFilled /></el-icon>
                {{ choice.impact }}
              </div>
            </el-card>
          </div>
        </div>
      </div>
    </el-dialog>
    <!-- 【新增】L6 生成NPC弹窗 -->
<el-dialog 
  v-model="npcDialogVisible" 
  title="生成场景NPC (L6)" 
  width="550px"
  :close-on-click-modal="false"
>
  <el-alert
    v-if="selectedNode.data?.sceneDescription"
    :title="`基于场景: ${selectedNode.data.nodeName}`"
    type="info"
    :closable="false"
    show-icon
    class="scene-context-alert"
  >
    <div class="scene-brief">{{ truncateSceneDesc }}</div>
  </el-alert>

  <el-form label-position="top" class="npc-form">
    <!-- 生成数量（新增） -->
    <el-form-item label="生成数量" required>
      <el-input-number 
        v-model="npcForm.count" 
        :min="1" 
        :max="5" 
        :step="1" 
        step-strictly
        controls-position="right"
        style="width: 150px"
      />
      <span class="count-hint">建议一次生成 1-3 个，避免角色过多</span>
    </el-form-item>

    <!-- 生成提示词 -->
    <el-form-item label="生成提示词" required>
      <el-input 
        v-model="npcForm.prompt" 
        type="textarea" 
        :rows="3"
        placeholder="描述你想生成的NPC，如：'生成一个敌对NPC'..."
      />
    </el-form-item>
    
    <!-- 快捷类型标签 -->
    <el-form-item>
      <template #label>
        <span>快捷类型</span>
        <el-text type="info" size="small" style="margin-left: 8px">点击自动填充</el-text>
      </template>
      <div class="quick-tags">
        <el-tag 
          v-for="tag in npcQuickTags" 
          :key="tag"
          size="small" 
          effect="plain"
          class="quick-tag"
          @click="applyNPCTag(tag)"
        >
          {{ tag }}
        </el-tag>
      </div>
    </el-form-item>

    <!-- 继承场景氛围选项 -->
    <el-form-item>
      <el-checkbox v-model="npcForm.inheritScene">
        自动继承当前场景氛围（在prompt前追加场景关键词）
      </el-checkbox>
    </el-form-item>
  </el-form>

  <template #footer>
    <el-button @click="npcDialogVisible = false">取消</el-button>
    <el-button 
      type="primary" 
      @click="generateSceneNPCs" 
      :loading="npcGenerating"
      :disabled="!npcForm.prompt.trim() || npcForm.count < 1"
    >
      开始生成 ({{ npcForm.count }}个)
    </el-button>
  </template>
</el-dialog>
<!-- 手动连接节点弹窗 -->
<el-dialog 
  v-model="manualEdgeDialogVisible" 
  title="手动连接节点" 
  width="500px"
  :close-on-click-modal="false"
>
  <el-form label-position="top">
    <el-form-item label="源节点（起点）" required>
      <el-select v-model="manualEdgeForm.sourceId" placeholder="选择起始节点" style="width: 100%">
        <el-option 
          v-for="node in availableNodes" 
          :key="node.id" 
          :label="node.data?.nodeName || node.id" 
          :value="node.id" 
        />
      </el-select>
    </el-form-item>
    
    <el-form-item label="目标节点（终点）" required>
      <el-select v-model="manualEdgeForm.targetId" placeholder="选择目标节点" style="width: 100%">
        <el-option 
          v-for="node in availableTargetNodesForManual" 
          :key="node.id" 
          :label="node.data?.nodeName || node.id" 
          :value="node.id" 
        />
      </el-select>
    </el-form-item>

    <el-divider content-position="left">选项配置</el-divider>
    
    <div v-for="(opt, index) in manualEdgeForm.options" :key="index" class="option-item">
      <el-form-item :label="`选项 ${index + 1}`">
        <el-input v-model="opt.label" placeholder="选项文本，如：破门而入" />
      </el-form-item>
      <el-form-item label="条件表达式（可选）">
        <el-input v-model="opt.conditionExpr" placeholder="如：player.strength > 10" />
      </el-form-item>
      <el-button 
        v-if="manualEdgeForm.options.length > 1" 
        type="danger" 
        link 
        @click="removeManualOption(index)"
      >
        删除此选项
      </el-button>
    </div>
    
    <el-button type="primary" link @click="addManualOption">
      <el-icon><Plus /></el-icon> 添加选项
    </el-button>
  </el-form>

  <template #footer>
    <el-button @click="manualEdgeDialogVisible = false">取消</el-button>
    <el-button 
      type="primary" 
      @click="confirmManualEdge"
      :disabled="!manualEdgeForm.sourceId || !manualEdgeForm.targetId || manualEdgeForm.options.some(o => !o.label)"
    >
      创建连接
    </el-button>
  </template>
</el-dialog>
<!-- L8 AI生成选项弹窗 -->
<el-dialog 
  v-model="aiEdgeDialogVisible" 
  title="AI生成选项 (L8)" 
  width="700px"
  :close-on-click-modal="false"
  class="ai-edge-dialog"
>
  <el-form label-position="top">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="源节点（起点）" required>
          <el-select v-model="aiEdgeForm.sourceId" placeholder="选择起始节点" style="width: 100%">
            <el-option 
              v-for="node in availableNodes" 
              :key="node.id" 
              :label="node.data?.nodeName || node.id" 
              :value="node.id" 
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="目标节点（终点）" required>
          <el-select 
            v-model="aiEdgeForm.targetId" 
            placeholder="选择目标节点" 
            style="width: 100%"
            :disabled="!aiEdgeForm.sourceId"
          >
            <el-option 
              v-for="node in availableTargetNodesForAI" 
              :key="node.id" 
              :label="node.data?.nodeName || node.id" 
              :value="node.id" 
            />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item label="情境提示词（描述两个节点间的过渡情境）" required>
      <el-input 
        v-model="aiEdgeForm.prompt" 
        type="textarea" 
        :rows="3"
        placeholder="如：主角从得知秘密到决定行动的过渡；从废墟逃往安全屋的紧张过程..."
      />
    </el-form-item>

    <el-form-item>
      <el-button 
        type="primary" 
        @click="generateAIEdges" 
        :loading="aiGenerating"
        :disabled="!aiEdgeForm.sourceId || !aiEdgeForm.targetId || !aiEdgeForm.prompt.trim()"
      >
        <el-icon><MagicStick /></el-icon>
        生成选项建议
      </el-button>
    </el-form-item>
  </el-form>

  <!-- 生成结果展示 -->
  <div v-if="aiGeneratedOptions.length > 0" class="ai-options-result">
    <el-divider content-position="left">
      生成结果（选择要添加的选项）
    </el-divider>
    
    <el-checkbox-group v-model="selectedAIOptions">
      <div v-for="(opt, index) in aiGeneratedOptions" :key="opt.id" class="ai-option-card">
        <el-checkbox :label="index" border class="option-checkbox">
          <div class="option-header">
            <span class="option-label">{{ opt.label }}</span>
            <el-tag v-if="opt.conditionExpr" size="small" type="warning">条件：{{ opt.conditionExpr }}</el-tag>
          </div>
        </el-checkbox>
        
        <div class="option-detail">
          <div class="reason-text">
            <el-icon><InfoFilled /></el-icon>
            {{ opt.reason }}
          </div>
          
          <el-collapse>
            <el-collapse-item title="成功结果" name="success">
              <div class="success-text">{{ opt.onSuccess }}</div>
            </el-collapse-item>
            <el-collapse-item title="失败结果" name="failure" v-if="opt.onFailure">
              <div class="failure-text">{{ opt.onFailure }}</div>
            </el-collapse-item>
            <el-collapse-item title="效果" name="effect" v-if="opt.effect">
              <div class="effect-text">{{ opt.effect }}</div>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </el-checkbox-group>
  </div>

  <template #footer>
    <el-button @click="aiEdgeDialogVisible = false">取消</el-button>
    <el-button 
      type="primary" 
      @click="applyAIOptions" 
      :disabled="selectedAIOptions.length === 0"
      v-if="aiGeneratedOptions.length > 0"
    >
      添加选中的选项 ({{ selectedAIOptions.length }}个)
    </el-button>
  </template>
</el-dialog>

    <!-- 其他弹窗（手动创建边、AI生成边等）保持不变... -->
  </div>
</template>
<script lang="ts">
import { defineComponent, h, computed } from 'vue'

export const CustomEdge = defineComponent({
  name: 'CustomEdge',
  props: [
    'id', 'sourceX', 'sourceY', 'targetX', 'targetY', 
    'sourcePosition', 'targetPosition', 'data', 'label',
    'markerEnd', 'style'
  ],
  setup(props) {
    const index = props.data?.edgeIndex || 0
    const total = props.data?.totalEdges || 1
    
    // 【优化1】增大左右偏移量，确保彻底分离
    const offsetX = (index - (total - 1) / 2) * 100 // 100px间距，居中分布
    
    // 【优化2】标签垂直错开，避免重叠（像阶梯一样）
    const labelOffsetY = (index - (total - 1) / 2) * 25 // 垂直方向25px错开
    
    // 手动计算贝塞尔曲线路径
    const path = computed(() => {
      const sx = props.sourceX
      const sy = props.sourceY
      const tx = props.targetX
      const ty = props.targetY
      
      const curvature = 0.5
      const dx = tx - sx
      
      // 控制点左右偏移实现分离
      const cp1x = sx + dx * curvature + offsetX
      const cp1y = sy
      
      const cp2x = tx - dx * curvature + offsetX
      const cp2y = ty
      
      return `M ${sx} ${sy} C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${tx} ${ty}`
    })
    
    // 【优化3】标签位置：中点 + 左右偏移 + 垂直错开
    const labelPos = computed(() => {
      const centerX = (props.sourceX + props.targetX) / 2 + offsetX * 0.6 // 0.6系数让标签别太靠外
      const centerY = (props.sourceY + props.targetY) / 2 + labelOffsetY
      return { x: centerX, y: centerY }
    })
    
    // 【优化4】动态计算文字宽度（中文每个字约14px，英文7px，边距20px）
    const textWidth = computed(() => {
      if (!props.label) return 80
      const chineseChars = (props.label.match(/[\u4e00-\u9fa5]/g) || []).length
      const otherChars = props.label.length - chineseChars
      return Math.max(80, chineseChars * 14 + otherChars * 7 + 20)
    })
    
    return () => h('g', {}, [
      // 边线路径
      h('path', {
        d: path.value,
        class: 'vue-flow__edge-path',
        style: {
          stroke: props.style?.stroke || '$accent-gold',
          strokeWidth: props.style?.strokeWidth || 2.5,
          fill: 'none'
        },
        'marker-end': props.markerEnd
      }),
      
      // 【优化5】标签背景：宽度自适应，高度增加
      props.label && h('rect', {
        x: labelPos.value.x - textWidth.value / 2,
        y: labelPos.value.y - 14, // 高度28，居中
        width: textWidth.value,
        height: 28,
        fill: '$paper-cardfff',
        stroke: props.style?.stroke || '$accent-gold',
        'stroke-width': 1.5,
        rx: 14, // 圆角半径增大
        ry: 14,
        filter: 'drop-shadow(0 2px 6px rgba(61, 43, 31, 0.15))'
      }),
      
      // 标签文字
      props.label && h('text', {
        x: labelPos.value.x,
        y: labelPos.value.y,
        dy: '0.35em',
        'text-anchor': 'middle',
        style: {
          fontSize: '13px',
          fontWeight: 'bold',
          fill: '#333',
          pointerEvents: 'none'
        }
      }, props.label)
    ])
  }
})
</script>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import '@vue-flow/core/dist/style.css'
import { Plus, Check, Lightning, View, Share, Delete, RefreshLeft, MagicStick, 
  ChatDotRound, ChatLineRound, UserFilled, InfoFilled, Close, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onBeforeRouteLeave } from 'vue-router'
import { useProjectStore } from '@/store/modules/project'
import { useCanvasStore } from '@/store/modules/canvas'
import { useGenerationStore } from '@/store/modules/generation'
import api from '@/api'
import NodeCard from '@/components/Canvas/NodeCard.vue'

const projectStore = useProjectStore()
const canvasStore = useCanvasStore()
const generationStore = useGenerationStore()
const { addNodes, addEdges, removeNodes, removeEdges } = useVueFlow()

const elements = ref<any[]>([])
const originalElements = ref<any[]>([])
const selectedNode = ref<any>(null)
const selectedEdge = ref<any>(null)
const characters = ref<any[]>([])
const deletedNodeIds = ref<string[]>([])
const deletedEdgeIds = ref<string[]>([])

// 对话生成相关
const dialogueGenVisible = ref(false)
const dialogueGenerating = ref(false)
const dialogueForm = reactive({
  characterIds: [] as string[],
  prompt: ''
})

// NPC生成相关
const npcDialogVisible = ref(false)
const npcGenerating = ref(false)
const npcForm = reactive({
  prompt: '',
  inheritScene: true,
  count: 1
})
const npcQuickTags = [
  '知晓内情的神秘商贩',
  '敌对势力的巡逻守卫', 
  '寻求帮助的受伤路人',
  '守护远古遗物的灵体',
  '提供线索的疯癫老者',
  '隐藏身份的刺客'
]
const dialogueViewVisible = ref(false)
const currentDialogue = ref<any>(null)

// L5 生成场景描述相关
const l5DialogVisible = ref(false)
const l5Generating = ref(false)
const l5Form = reactive({
  prompt: ''
})
const promptHints = [
  '氛围突出对生命漠视的严肃荒谬感',
  '雨夜霓虹下的赛博朋克压抑感',
  '古风仙侠的清冷孤寂氛围',
  '太空恐怖中的幽闭窒息感',
  '中世纪魔幻的史诗悲壮感'
]

// 手动连接节点相关
const manualEdgeDialogVisible = ref(false)
const manualEdgeForm = reactive({
  sourceId: '',
  targetId: '',
  options: [{ label: '', conditionExpr: '' }]
})

// L8 AI生成选项相关
const aiEdgeDialogVisible = ref(false)
const aiGenerating = ref(false)
const aiEdgeForm = reactive({
  sourceId: '',
  targetId: '',
  prompt: ''
})
const aiGeneratedOptions = ref<any[]>([])
const selectedAIOptions = ref<number[]>([])

const hasChanges = computed(() => {
  if (deletedNodeIds.value.length > 0 || deletedEdgeIds.value.length > 0) return true
  if (elements.value.length !== originalElements.value.length) return true
  return canvasStore.hasUnsavedChanges
})

const showPropertyPanel = computed(() => selectedNode.value || selectedEdge.value)
const availableNodes = computed(() => elements.value.filter(el => !el.source))
const availableTargetNodesForManual = computed(() => {
  return availableNodes.value.filter(node => node.id !== manualEdgeForm.sourceId)
})
const availableTargetNodesForAI = computed(() => {
  return availableNodes.value.filter(node => node.id !== aiEdgeForm.sourceId)
})


const calculateEdgeOffsets = (allEdges: any[]) => {
  const groups = new Map()
  
  // 分组
  allEdges.forEach(e => {
    if (!e.source) return
    const key = `${e.source}-${e.target}`
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key).push(e)
  })
  
  // 标记索引和统一类型
  groups.forEach((edges) => {
    edges.forEach((e, i) => {
      // 【关键】强制改为自定义类型，并标记索引
      e.type = 'custom'
      e.data = {
        ...e.data,
        edgeIndex: i, // 0, 1, 2...
        totalEdges: edges.length
      }
      
      // 不同颜色
      const colors = ['#e74c3c', '#3498db', '#2ecc71', '#f39c12', '#9b59b6', '#e67e22']
      e.style = { 
        stroke: colors[i % colors.length], 
        strokeWidth: 3 
      }
      
      // 标签加序号，防止文字重叠时看不清
      if (edges.length > 1 && !e.label?.startsWith(`${i+1}.`)) {
        e.label = `${i+1}. ${e.label}`
      }
    })
  })
}
// 角色类型格式化
const formatRoleType = (type: string) => {
  const map: Record<string, string> = {
    'PROTAGONIST': '主角',
    'ANTAGONIST': '反派',
    'SUPPORT': '配角',
    'NPC': '路人'
  }
  return map[type] || type
}

const closePropertyPanel = () => {
  selectedNode.value = null
  selectedEdge.value = null
}

// 情感标签颜色
const getEmotionType = (emotion: string) => {
  const map: Record<string, string> = {
    '愤怒': 'danger',
    '恐惧': 'warning',
    '喜悦': 'success',
    '悲伤': 'info',
    '冷漠': 'info',
    '温柔': 'primary'
  }
  if (emotion.includes('愤怒') || emotion.includes('杀意')) return 'danger'
  if (emotion.includes('温柔') || emotion.includes('慈悲')) return 'primary'
  if (emotion.includes('冷漠') || emotion.includes('机械')) return 'info'
  if (emotion.includes('恐惧') || emotion.includes('嘲弄')) return 'warning'
  return 'info'
}

// 计算属性：截断场景描述用于显示
const truncateSceneDesc = computed(() => {
  const desc = selectedNode.value?.data?.sceneDescription || ''
  try {
    const parsed = JSON.parse(desc)
    const text = parsed.scene || parsed.description || desc
    return text.length > 60 ? text.slice(0, 60) + '...' : text
  } catch {
    return desc.length > 60 ? desc.slice(0, 60) + '...' : desc
  }
})

// 计算属性：解析场景描述用于显示（可能是纯文本或JSON）
const displaySceneDescription = computed({
  get() {
    if (!selectedNode.value?.data?.sceneDescription) return ''
    const desc = selectedNode.value.data.sceneDescription
    try {
      const parsed = JSON.parse(desc)
      return parsed.scene || parsed.description || desc
    } catch (e) {
      return desc
    }
  },
  set(val) {
    if (!selectedNode.value) return
    const originalDesc = selectedNode.value.data.sceneDescription
    if (!originalDesc) {
      selectedNode.value.data.sceneDescription = val
      return
    }
    try {
      const parsed = JSON.parse(originalDesc)
      if (parsed.scene !== undefined) {
        parsed.scene = val
      } else if (parsed.description !== undefined) {
        parsed.description = val
      }
      selectedNode.value.data.sceneDescription = JSON.stringify(parsed)
    } catch (e) {
      selectedNode.value.data.sceneDescription = val
    }
    canvasStore.setUnsaved(true)
  }
})

// 显示NPC生成弹窗
const showGenerateNPCDialog = () => {
  if (!selectedNode.value?.data?.sceneDescription) {
    ElMessage.warning('请先生成场景描述(L5)，AI需要基于场景上下文生成合适的NPC')
    return
  }
  npcForm.prompt = ''
  npcForm.inheritScene = true
  npcForm.count = 1
  npcDialogVisible.value = true
}

// 应用快捷标签
const applyNPCTag = (tag: string) => {
  npcForm.prompt = `生成${tag}，与"${selectedNode.value.data.nodeName}"场景氛围契合`
}

// 生成NPC核心方法
const generateSceneNPCs = async () => {
  if (!projectStore.currentProjectId || !selectedNode.value) return
  
  if (!npcForm.count || npcForm.count < 1) {
    ElMessage.warning('生成数量至少为1')
    return
  }
  if (npcForm.count > 5) {
    ElMessage.warning('单次最多生成5个NPC')
    npcForm.count = 5
  }

  npcGenerating.value = true
  generationStore.startGeneration('L6')
  
  try {
    let finalPrompt = npcForm.prompt
    if (npcForm.inheritScene && selectedNode.value.data?.sceneDescription) {
      const sceneContext = truncateSceneDesc.value
      finalPrompt = `[场景背景：${sceneContext}] ${npcForm.prompt}`
    }

    const requestData = {
      prompt: finalPrompt,
      count: npcForm.count
    }

    const pid = Number(projectStore.currentProjectId)
    const res = await api.character.generateNPC(pid, selectedNode.value.id, requestData)
    
    const newNPCIds = Array.isArray(res) ? res : (res.data || [])
    
    if (newNPCIds.length === 0) {
      ElMessage.warning('未生成新的NPC，请尝试调整描述')
      return
    }

    if (!selectedNode.value.data.generatedNPCIds) {
      selectedNode.value.data.generatedNPCIds = []
    }
    
    selectedNode.value.data.generatedNPCIds.push(...newNPCIds)
    canvasStore.setUnsaved(true)
    await refreshCharacters()
    
    ElMessage.success(`成功生成 ${newNPCIds.length} 个场景NPC`)
    npcDialogVisible.value = false
    
  } catch (error: any) {
    console.error('生成NPC错误:', error)
    ElMessage.error(error.message || '生成NPC失败')
  } finally {
    npcGenerating.value = false
    generationStore.endGeneration()
  }
}

// 刷新角色列表
const refreshCharacters = async () => {
  if (!projectStore.currentProjectId) return
  try {
    const charList = await api.character.getList(projectStore.currentProjectId)
    characters.value = charList || []
  } catch (error) {
    console.error('刷新角色列表失败:', error)
  }
}

// 通过ID获取角色详情
const getNPCDetail = (npcId: string) => {
  return characters.value.find(c => c.id === npcId)
}

// 快速关联NPC到当前节点
const quickAssociateNPC = (npcId: string) => {
  if (!selectedNode.value) return
  
  const currentIds = selectedNode.value.data.associatedCharIds || []
  if (currentIds.includes(npcId)) {
    ElMessage.info('该NPC已关联')
    return
  }
  
  selectedNode.value.data.associatedCharIds = [...currentIds, npcId]
  updateNodeData()
  ElMessage.success('已关联该NPC到当前场景')
}

// 从节点中移除NPC记录
const removeNPCFromNode = (npcId: string) => {
  if (!selectedNode.value?.data?.generatedNPCIds) return
  
  const index = selectedNode.value.data.generatedNPCIds.indexOf(npcId)
  if (index > -1) {
    selectedNode.value.data.generatedNPCIds.splice(index, 1)
  }
}

// 判断是否主角
const isProtagonist = (speaker: string) => {
  const char = characters.value.find(c => c.name === speaker)
  return speaker.includes('主角') || char?.roleType === 'PROTAGONIST'
}

const fetchData = async () => {
  if (!projectStore.currentProjectId) return
  try {
    const [nodeList, edgeList, charList] = await Promise.all([
      api.node.getList(projectStore.currentProjectId),
      api.edge.getList(projectStore.currentProjectId),
      api.character.getList(projectStore.currentProjectId)
    ])
        
    // 处理节点
    const flowNodes = (nodeList || []).map((n: any) => {
      let dialogueData = null
      if (n.dialogueContent) {
        try {
          dialogueData = typeof n.dialogueContent === 'string' 
            ? JSON.parse(n.dialogueContent) 
            : n.dialogueContent
        } catch (e) {
          console.warn('解析对话 JSON 失败:', n.id, e)
          dialogueData = null
        }
      }
      
      return {
        id: n.id,
        type: 'default',
        position: { 
          x: Number(n.positionX) || 0, 
          y: Number(n.positionY) || 0 
        },
        data: { 
          ...n,
          label: n.nodeName || n.id,
          hasDialogue: !!n.dialogueContent,
          dialogueContent: dialogueData,
          sceneDescription: n.sceneDescription || '',
          actIndex: n.actIndex || 0,
          beatIndex: n.beatIndex || 0,
          associatedCharIds: Array.isArray(n.associatedCharIds) ? n.associatedCharIds : []
        }
      }
    })


    const flowEdges = (edgeList || []).map((e: any) => ({
  id: String(e.id),
  source: e.sourceId,
  target: e.targetId,
  label: e.label || '选项',
  type: 'custom', // 【修改】统一使用自定义边
  data: { 
    conditionExpr: e.conditionExpr || '',
    onSuccess: e.onSuccess || '',
    onFailure: e.onFailure || '',
    isNew: false,
    edgeIndex: 0 // 初始值，会被 calculateEdgeOffsets 覆盖
  },
  style: { stroke: '$accent-gold', strokeWidth: 3 }
}))
    
    // 合并数据
    elements.value = [...flowNodes, ...flowEdges]
    
    // 【关键】计算所有边的偏移，防止重叠
    calculateEdgeOffsets(elements.value.filter(el => el.source))
    
    // 深拷贝备份
    originalElements.value = JSON.parse(JSON.stringify(elements.value))
    
    characters.value = charList || []
    
    console.log('画布加载完成:', {
      nodes: flowNodes.length,
      edges: flowEdges.length
    })
    
  } catch (error: any) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载画布数据失败：' + error.message)
  }
}

// 显示L5生成弹窗
const showL5GenerateDialog = () => {
  l5Form.prompt = ''
  l5DialogVisible.value = true
}

// 生成L5场景描述
const generateL5Description = async () => {
  if (!projectStore.currentProjectId || !selectedNode.value) return
  
  if (!l5Form.prompt.trim()) {
    ElMessage.warning('请输入氛围提示词')
    return
  }

  l5Generating.value = true
  generationStore.startGeneration('L5')
  
  try {
    const res = await api.node.generateL5(
      projectStore.currentProjectId,
      selectedNode.value.id,
      { prompt: l5Form.prompt }
    )
    
    let sceneText = ''
    if (typeof res === 'string') {
      try {
        const parsed = JSON.parse(res)
        sceneText = parsed.scene || parsed.description || res
      } catch (e) {
        sceneText = res
      }
    } else if (res && typeof res === 'object') {
      sceneText = res.scene || res.description || JSON.stringify(res)
    }
    
    if (selectedNode.value) {
      selectedNode.value.data.sceneDescription = typeof res === 'string' ? res : JSON.stringify(res)
    }
    
    ElMessage.success('场景描述生成成功')
    l5DialogVisible.value = false
    await onNodeClick({ node: selectedNode.value })
    
  } catch (error: any) {
    ElMessage.error(error.message || '生成失败')
  } finally {
    l5Generating.value = false
    generationStore.endGeneration()
  }
}

// 对话生成相关
const showGenerateDialogueDialog = async () => {
  if (selectedNode.value?.data?.hasDialogue) {
    try {
      await ElMessageBox.confirm(
        '该节点已存在生成的对话，重新生成将覆盖现有内容。是否继续？',
        '确认覆盖',
        {
          confirmButtonText: '重新生成',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch {
      return
    }
  }
  
  dialogueForm.characterIds = []
  dialogueForm.prompt = ''
  dialogueGenVisible.value = true
}

const generateDialogue = async () => {
  if (!projectStore.currentProjectId || !selectedNode.value) return
  
  if (dialogueForm.characterIds.length < 2) {
    ElMessage.warning('请至少选择2个角色')
    return
  }

  dialogueGenerating.value = true
  generationStore.startGeneration('L7')
  
  try {
    const res = await api.content.generateDialogue(
      projectStore.currentProjectId,
      {
        nodeId: selectedNode.value.id,
        characterIds: dialogueForm.characterIds,
        prompt: dialogueForm.prompt
      }
    )
    
    let dialogueData = res
    if (typeof res === 'string') {
      try {
        dialogueData = JSON.parse(res)
      } catch (e) {
        console.error('解析对话JSON失败:', e)
        ElMessage.error('对话数据格式错误')
        return
      }
    }
    
    if (selectedNode.value) {
      selectedNode.value.data.hasDialogue = true
      selectedNode.value.data.dialogueScene = dialogueData.scene
      selectedNode.value.data.dialogueContext = dialogueData.context
      selectedNode.value.data.dialogueLineCount = dialogueData.lines?.length || 0
      selectedNode.value.data.dialogueContent = dialogueData
    }
    
    ElMessage.success('对话生成成功')
    dialogueGenVisible.value = false
    currentDialogue.value = dialogueData
    dialogueViewVisible.value = true
    
  } catch (error: any) {
    ElMessage.error(error.message || '生成失败')
  } finally {
    dialogueGenerating.value = false
    generationStore.endGeneration()
  }
}

const showViewDialogueDialog = () => {
  if (selectedNode.value?.data?.dialogueContent) {
    currentDialogue.value = selectedNode.value.data.dialogueContent
    dialogueViewVisible.value = true
  }
}

// L8 AI生成选项
const showAIGenerateEdgeDialog = () => {
  aiEdgeForm.sourceId = ''
  aiEdgeForm.targetId = ''
  aiEdgeForm.prompt = ''
  aiGeneratedOptions.value = []
  selectedAIOptions.value = []
  aiEdgeDialogVisible.value = true
}

const generateAIEdges = async () => {
  if (!projectStore.currentProjectId) return
  
  aiGenerating.value = true
  generationStore.startGeneration('L8')
  
  try {
    const res = await api.edge.generateSuggestions(
      projectStore.currentProjectId,
      {
        sourceId: aiEdgeForm.sourceId,
        targetId: aiEdgeForm.targetId,
        prompt: aiEdgeForm.prompt
      }
    )
    
    const result = res.data || res
    const options = Array.isArray(result) ? result : []
    
    if (options.length === 0) {
      ElMessage.info('AI正在生成中，请稍候...')
      
      let attempts = 0
      const checkNewEdges = setInterval(async () => {
        attempts++
        await fetchData()
        
        const newEdges = elements.value.filter((el: any) => 
          el.source === aiEdgeForm.sourceId && 
          el.target === aiEdgeForm.targetId &&
          !el.data?.isNew
        )
        
        if (newEdges.length > 0 || attempts >= 3) {
          clearInterval(checkNewEdges)
          if (newEdges.length > 0) {
            ElMessage.success(`已生成 ${newEdges.length} 个新选项`)
            aiEdgeDialogVisible.value = false
          } else {
            ElMessage.warning('生成可能需要更长时间，请手动刷新查看')
          }
          aiGenerating.value = false
          generationStore.endGeneration()
        }
      }, 2000)
      
      return
    }
    
    aiGeneratedOptions.value = options
    selectedAIOptions.value = options.map((_, idx) => idx)
    ElMessage.success(`成功生成 ${options.length} 个选项`)
    
  } catch (error: any) {
    console.error('L8生成失败:', error)
    ElMessage.error(error.message || '生成选项失败')
    aiGenerating.value = false
    generationStore.endGeneration()
  }
}

//应用AI生成的选项，并重新计算偏移
const applyAIOptions = () => {
  if (selectedAIOptions.value.length === 0) return
  
  selectedAIOptions.value.forEach(index => {
    const opt = aiGeneratedOptions.value[index]
    const newEdge = {
      id: `edge-${Date.now()}-${index}`,
      source: aiEdgeForm.sourceId,
      target: aiEdgeForm.targetId,
      label: opt.label,
      type: 'default',  //改为 default 才能用 curvature
      pathOptions: {
        curvature: 0.5  // 默认曲率，后面会重新计算
      },
      style: { stroke: '$accent-gold', strokeWidth: 3 },
      labelShowBg: true,
      labelBgStyle: { fill: '$paper-card', stroke: '#ccc', rx: 4 },
      data: {
        label: opt.label,
        conditionExpr: opt.conditionExpr || '',
        onSuccess: opt.onSuccess || '',
        onFailure: opt.onFailure || '',
        effect: opt.effect || '',
        reason: opt.reason || '',
        isNew: true
      }
    }
    elements.value.push(newEdge)
  })
  
  // 重新计算曲率，让边分离
  calculateEdgeOffsets(elements.value.filter(el => el.source))
  
  canvasStore.setUnsaved(true)
  aiEdgeDialogVisible.value = false
  ElMessage.success(`已添加 ${selectedAIOptions.value.length} 个选项`)
}

// 节点点击
const onNodeClick = async (event: any) => {
  selectedNode.value = event.node
  selectedEdge.value = null
  
  if (projectStore.currentProjectId && event.node?.id) {
    try {
      const detail = await api.node.getDetail(projectStore.currentProjectId, event.node.id)
      
      if (detail) {
        const existingDialogue = event.node.data?.dialogueContent
        let dialogueData = existingDialogue
        
        if (detail.dialogueContent) {
          try {
            dialogueData = typeof detail.dialogueContent === 'string' 
              ? JSON.parse(detail.dialogueContent) 
              : detail.dialogueContent
          } catch (e) {
            console.warn('解析详情中的对话 JSON 失败:', e)
            dialogueData = existingDialogue
          }
        }
        
        const fullData = {
          ...event.node.data,
          ...detail,
          dialogueContent: dialogueData,
          hasDialogue: !!dialogueData,
          dialogueScene: dialogueData?.scene || event.node.data?.dialogueScene || '',
          dialogueContext: dialogueData?.context || event.node.data?.dialogueContext || '',
          dialogueLineCount: dialogueData?.lines?.length || event.node.data?.dialogueLineCount || 0,
          label: detail.nodeName || event.node.id,
          associatedCharIds: typeof detail.associatedCharIds === 'string' 
            ? JSON.parse(detail.associatedCharIds) 
            : (detail.associatedCharIds || [])
        }
        
        selectedNode.value.data = fullData
        
        const index = elements.value.findIndex(el => el.id === event.node.id)
        if (index !== -1) {
          elements.value[index].data = fullData
        }
      }
    } catch (error: any) {
      console.error('获取节点详情失败:', error)
      ElMessage.warning('获取最新详情失败，显示本地缓存数据')
    }
  }
}

const onEdgeClick = (event: any) => {
  selectedEdge.value = event.edge
  selectedNode.value = null
}

const onNodeDragStop = () => {
  canvasStore.setUnsaved(true)
}

// 拖拽连接时添加边并重新计算偏移
// 2. 修改 onConnect（拖拽连接）
const onConnect = (connection: any) => {
  const newEdge = {
    id: `edge-${Date.now()}`,
    source: connection.source,
    target: connection.target,
    label: '新选项',
    type: 'default',  // 【修改】改为 default
    pathOptions: {
      curvature: 0.5
    },
    style: { stroke: '$accent-gold', strokeWidth: 3 },
    labelShowBg: true,
    labelBgStyle: { fill: '$paper-card', stroke: '#ccc', rx: 4 },
    data: { 
      label: '新选项',
      conditionExpr: '',
      onSuccess: '',
      onFailure: '',
      isNew: true
    }
  }
  elements.value.push(newEdge)
  
  calculateEdgeOffsets(elements.value.filter(el => el.source))
  canvasStore.setUnsaved(true)
}

const addNode = () => {
  const timestamp = Date.now()
  const newNode = {
    id: `node-${timestamp}`,
    type: 'default',
    position: { x: 100 + Math.random() * 200, y: 100 + Math.random() * 200 },
    data: { 
      nodeName: '新节点',
      label: '新节点',
      sceneDescription: '',
      associatedCharIds: [],
      isNew: true
    }
  }
  elements.value.push(newNode)
  canvasStore.setUnsaved(true)
  ElMessage.success('已添加新节点')
}

const saveLayout = async () => {
  if (!projectStore.currentProjectId) return
  
  const nodes = elements.value.filter(el => !el.source)
  const edges = elements.value.filter(el => el.source)
  
  try {
    const saveNodes = nodes.map(n => ({
      id: n.id,
      nodeName: n.data?.nodeName || n.id,
      sceneDescription: n.data?.sceneDescription || '',
      associatedCharIds: n.data?.associatedCharIds || [],
      positionX: n.position.x, 
      positionY: n.position.y,
      actIndex: n.data?.actIndex || 0,
      beatIndex: n.data?.beatIndex || 0
    }))
    
    await api.node.batchSave(projectStore.currentProjectId, saveNodes)
    
    const edgesToSave = edges.map(e => {
      const isNew = e.id.startsWith('edge-')
      return {
        ...(isNew ? {} : { id: e.id }),
        sourceId: e.source,
        targetId: e.target,
        label: e.label || '选项',
        conditionExpr: e.data?.conditionExpr || '',
        reason: e.data?.reason || '',
        onSuccess: e.data?.onSuccess || '',
        onFailure: e.data?.onFailure || '',
        effect: e.data?.effect || null
      }
    })
    
    await api.edge.batchSave(projectStore.currentProjectId, {
      edges: edgesToSave,
      deleteIds: deletedEdgeIds.value
    })
    
    deletedNodeIds.value = []
    deletedEdgeIds.value = []
    
    await fetchData()
    
    canvasStore.setUnsaved(false)
    ElMessage.success('保存成功')
    
  } catch (error: any) {
    console.error('保存失败:', error)
    ElMessage.error(error.message || '保存失败')
  }
}

// 手动连接节点
const showManualEdgeDialog = () => {
  manualEdgeForm.sourceId = ''
  manualEdgeForm.targetId = ''
  manualEdgeForm.options = [{ label: '', conditionExpr: '' }]
  manualEdgeDialogVisible.value = true
}

const addManualOption = () => {
  manualEdgeForm.options.push({ label: '', conditionExpr: '' })
}

const removeManualOption = (index: number) => {
  manualEdgeForm.options.splice(index, 1)
}

// 确认手动连接并重新计算偏移
// 3. 修改 confirmManualEdge（手动连接）
const confirmManualEdge = () => {
  if (!manualEdgeForm.sourceId || !manualEdgeForm.targetId) {
    ElMessage.warning('请选择源节点和目标节点')
    return
  }
  
  manualEdgeForm.options.forEach((opt, index) => {
    if (opt.label) {
      const newEdge = {
        id: `edge-${Date.now()}-${index}`,
        source: manualEdgeForm.sourceId,
        target: manualEdgeForm.targetId,
        label: opt.label,
        type: 'default',  //改为 default
        pathOptions: {
          curvature: 0.5
        },
        style: { stroke: '$accent-gold', strokeWidth: 3 },
        labelShowBg: true,
        labelBgStyle: { fill: '$paper-card', stroke: '#ccc', rx: 4 },
        data: {
          label: opt.label,
          conditionExpr: opt.conditionExpr || '',
          onSuccess: '',
          onFailure: '',
          isNew: true
        }
      }
      elements.value.push(newEdge)
    }
  })
  
  calculateEdgeOffsets(elements.value.filter(el => el.source))
  canvasStore.setUnsaved(true)
  manualEdgeDialogVisible.value = false
  ElMessage.success('连接创建成功')
}

const updateNodeSceneDesc = () => {
  if (!selectedNode.value) return
  console.log('场景描述已更新')
}

const updateNodeData = () => {
  if (!selectedNode.value) return
  const index = elements.value.findIndex(el => el.id === selectedNode.value.id)
  if (index !== -1) {
    elements.value[index].data.label = elements.value[index].data.nodeName
    canvasStore.setUnsaved(true)
  }
}

const updateEdgeData = () => {
  if (!selectedEdge.value) return
  const index = elements.value.findIndex(el => el.id === selectedEdge.value.id)
  if (index !== -1) {
    elements.value[index].label = elements.value[index].data.label
    canvasStore.setUnsaved(true)
  }
}

const deleteNode = () => {
  if (!selectedNode.value) return
  
  if (!selectedNode.value.id.startsWith('node-')) {
    deletedNodeIds.value.push(selectedNode.value.id)
  }
  
  const relatedEdges = elements.value.filter(
    el => el.source === selectedNode.value.id || el.target === selectedNode.value.id
  )
  relatedEdges.forEach(edge => {
    if (!edge.id.startsWith('edge-')) {
      deletedEdgeIds.value.push(edge.id)
    }
  })
  
  elements.value = elements.value.filter(el => 
    el.id !== selectedNode.value.id && 
    el.source !== selectedNode.value.id && 
    el.target !== selectedNode.value.id
  )
  
  selectedNode.value = null
  canvasStore.setUnsaved(true)
  ElMessage.success('已删除节点')
}

const deleteEdge = () => {
  if (!selectedEdge.value) return
  
  if (!selectedEdge.value.id.startsWith('edge-')) {
    deletedEdgeIds.value.push(selectedEdge.value.id)
  }
  
  elements.value = elements.value.filter(el => el.id !== selectedEdge.value.id)
  
  // 删除边后重新计算偏移，避免剩余边偏移不对称
  calculateEdgeOffsets(elements.value.filter(el => el.source))
  
  selectedEdge.value = null
  canvasStore.setUnsaved(true)
  ElMessage.success('已删除连接')
}

const cancelAllChanges = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要放弃所有未保存的更改吗？',
      '确认取消改动',
      {
        confirmButtonText: '放弃更改',
        cancelButtonText: '继续编辑',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    elements.value = JSON.parse(JSON.stringify(originalElements.value))
    deletedNodeIds.value = []
    deletedEdgeIds.value = []
    selectedNode.value = null
    selectedEdge.value = null
    canvasStore.setUnsaved(false)
    ElMessage.info('已恢复到最后保存的状态')
    
  } catch {
    // 用户取消
  }
}

const generateNodes = async () => {
  if (!projectStore.currentProjectId) return
  generationStore.startGeneration('L4')
  try {
    await api.node.generateL4(projectStore.currentProjectId, false)
    ElMessage.success('节点生成成功')
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '生成失败')
  } finally {
    generationStore.endGeneration()
  }
}

const generateDescriptions = async () => {
  if (!projectStore.currentProjectId || !selectedNode.value) return
  generationStore.startGeneration('L5')
  try {
    await api.node.generateL5(
      projectStore.currentProjectId, 
      selectedNode.value.id, 
      '生成详细的场景描述'
    )
    ElMessage.success('场景描述生成成功')
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '生成失败')
  } finally {
    generationStore.endGeneration()
  }
}

onBeforeRouteLeave((to, from, next) => {
  if (hasChanges.value) {
    ElMessageBox.confirm(
      '您有未保存的更改，确定要离开吗？', 
      '确认离开', {
        confirmButtonText: '离开',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(() => next()).catch(() => next(false))
  } else {
    next()
  }
})

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
/* 基础样式保持不变 */
.canvas-page { 
  height: calc(100vh - 140px); 
  display: flex; 
  flex-direction: column; 
}
.canvas-toolbar { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  padding: 10px 20px; 
  background: $paper-card; 
  border-bottom: 1px solid $border-light; 
}
.toolbar-left, .toolbar-right { 
  display: flex; 
  gap: 10px; 
  align-items: center;
}
.unsaved-tag { 
  margin-left: 5px; 
}
.canvas-container { 
  flex: 1; 
  display: flex; 
  position: relative; 
}
.canvas-container :deep(.vue-flow) { 
  flex: 1; 
}
.property-panel { 
  width: 320px; 
  padding: 15px; 
  background: $paper-card; 
  border-left: 1px solid $border-light; 
  overflow-y: auto;
  box-shadow: -2px 0 8px rgba(61, 43, 31, 0.05);
}
.panel-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center;
}
.panel-header > div {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.node-id-tag, .edge-id-tag {
  font-family: 'Courier New', monospace;
  font-size: 11px;
}
.dialogue-tag {
  display: flex;
  align-items: center;
  gap: 4px;
}
.edge-card {
  margin-top: 10px;
}

/* 对话区域样式 */
.dialogue-section {
  padding: 10px 0;
}
.divider-text {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: $ink-secondary;
}
.dialogue-preview {
  background: $paper-deep;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 10px;
}
.dialogue-scene-title {
  font-weight: bold;
  color: $ink-primary;
  margin-bottom: 5px;
  font-size: 14px;
}
.dialogue-context {
  font-size: 12px;
  color: $ink-secondary;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.generate-dialogue-btn {
  width: 100%;
  margin-bottom: 10px;
}

/* 角色选择下拉样式 */
.char-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 10px;
}
.form-hint {
  font-size: 12px;
  color: $ink-tertiary;
  margin-top: 5px;
}

/* 对话预览弹窗样式 */
.dialogue-preview-dialog :deep(.el-dialog__body) {
  padding: 0 20px 20px 20px;
  max-height: 60vh;
  overflow-y: auto;
}
.dialogue-container {
  padding: 10px 0;
}
.scene-header {
  margin-bottom: 20px;
}
.dialogue-lines {
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.dialogue-bubble {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 85%;
  align-self: flex-start;
}
.dialogue-bubble.self {
  align-self: flex-end;
  align-items: flex-end;
}
.bubble-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.dialogue-bubble.self .bubble-header {
  flex-direction: row-reverse;
}
.speaker-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.speaker-name {
  font-weight: bold;
  font-size: 14px;
  color: $ink-primary;
}
.emotion-tag {
  font-size: 11px;
}
.bubble-content {
  background: $paper-deep;
  padding: 12px 15px;
  border-radius: 12px;
  border-top-left-radius: 4px;
  margin-left: 42px;
}
.dialogue-bubble.self .bubble-content {
  background: rgba($accent-gold, 0.06);
  border-top-left-radius: 12px;
  border-top-right-radius: 4px;
  margin-left: 0;
  margin-right: 42px;
}
.line-text {
  font-size: 14px;
  color: $ink-primary;
  line-height: 1.6;
}
.subtext-collapse {
  margin-top: 8px;
}
.subtext-collapse :deep(.el-collapse-item__header) {
  font-size: 12px;
  color: $ink-tertiary;
}
.subtext {
  font-size: 12px;
  color: $ink-tertiary;
  font-style: italic;
  padding: 5px 0;
}

/* 玩家选项样式 */
.player-choices {
  margin-top: 30px;
}
.choices-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.choice-card {
  cursor: pointer;
  transition: all 0.3s;
}
.choice-card:hover {
  border-color: $accent-gold;
  transform: translateX(5px);
}
.choice-text {
  font-weight: bold;
  color: $ink-primary;
  margin-bottom: 8px;
  font-size: 14px;
}
.choice-impact {
  font-size: 12px;
  color: $ink-tertiary;
  display: flex;
  align-items: flex-start;
  gap: 5px;
}
/* 场景描述相关样式 */
.scene-desc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.scene-index {
  font-size: 12px;
  color: $accent-gold;
  background: rgba($accent-gold, 0.06);
  padding: 2px 8px;
  border-radius: 4px;
}
.scene-actions {
  margin-top: 8px;
  text-align: right;
}

/* L5弹窗样式 */
.prompt-hints {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.hint-tag {
  margin-right: 5px;
  margin-bottom: 5px;
}
.hint-tag:hover {
  background-color: $accent-gold;
  color: white;
}
/* NPC区域样式 */
.npc-section {
  margin-bottom: 20px;
}

.npc-list {
  background: rgba($accent-gold, 0.04);
  border-radius: 8px;
  padding: 12px;
  border: 1px solid rgba($accent-gold, 0.15);
}

.npc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.npc-count {
  font-size: 13px;
  color: $accent-gold;
  font-weight: 500;
}

.npc-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.npc-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.npc-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(61, 43, 31, 0.1);
}

.npc-hint {
  font-size: 12px;
  color: $ink-tertiary;
  display: flex;
  align-items: center;
  gap: 4px;
}

.npc-empty {
  padding: 10px;
  background: $paper-card;
  border-radius: 8px;
  border: 1px dashed $border-light;
}

.prerequisite-hint {
  margin-top: 8px;
  color: $accent-gold-light;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

/* 弹窗内样式 */
.scene-context-alert {
  margin-bottom: 20px;
}

.scene-brief {
  margin-top: 5px;
  font-size: 12px;
  color: $ink-secondary;
  line-height: 1.5;
}

.npc-form {
  margin-top: 10px;
}

.quick-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.quick-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.quick-tag:hover {
  background-color: rgba($accent-gold, 0.04);
  border-color: $accent-gold;
  color: $accent-gold;
}
.count-hint {
  margin-left: 12px;
  color: $ink-tertiary;
  font-size: 12px;
}
.panel-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.close-btn {
  color: $ink-secondary;
}
.close-btn:hover {
  color: $accent-gold;
}
/* AI选项卡片样式 */
.ai-options-result {
  max-height: 400px;
  overflow-y: auto;
  margin-top: 20px;
}

.ai-option-card {
  border: 1px solid $border-light;
  border-radius: 8px;
  padding: 15px;
  margin-bottom: 15px;
  background: $paper-deep;
}

.option-checkbox {
  width: 100%;
  margin-bottom: 10px;
}

.option-checkbox :deep(.el-checkbox__label) {
  width: 100%;
}

.option-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 14px;
}

.option-label {
  color: $ink-primary;
}

.option-detail {
  margin-top: 10px;
  padding-left: 24px;
}

.reason-text {
  color: $ink-secondary;
  font-size: 13px;
  margin-bottom: 10px;
  display: flex;
  align-items: flex-start;
  gap: 5px;
}

.success-text {
  color: $accent-green;
  font-size: 13px;
  line-height: 1.5;
}

.failure-text {
  color: #f56c6c;
  font-size: 13px;
  line-height: 1.5;
}

.effect-text {
  color: #409eff;
  font-size: 13px;
  font-family: monospace;
  background: rgba($accent-gold, 0.06);
  padding: 5px;
  border-radius: 4px;
}

.ai-edge-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
}
</style>