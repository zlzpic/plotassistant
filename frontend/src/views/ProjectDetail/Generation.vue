<template>
  <div class="generation-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>L9: 完整剧情生成</span>
        </div>
      </template>
      <el-form label-position="top">
        <el-form-item label="分支路径">
          <div class="path-input">
            <el-input 
              v-model="branchPath" 
              placeholder="格式: node_001|1->node_002|5->node_003" 
              :disabled="true"
            />
            <el-button @click="openPathSelector">从画布选择</el-button>
          </div>
        </el-form-item>
        <el-form-item label="剧情风格">
          <el-select v-model="style" placeholder="选择剧情风格" style="width: 100%">
            <el-option label="绝望" value="desperate" />
            <el-option label="希望" value="hopeful" />
            <el-option label="悬疑" value="mystery" />
            <el-option label="热血" value="passionate" />
            <el-option label="温馨" value="warm" />
            <el-option label="黑暗" value="dark" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            size="large" 
            :icon="VideoPlay" 
            @click="generateScript" 
            :loading="generating"
          >
            生成完整剧情
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>


    <el-card v-loading="loadingHistory" class="history-card">
      <template #header>
        <div class="card-header">
          <span>历史版本</span>
          <el-tag v-if="canonScript" type="success" effect="dark" class="canon-badge">
            <el-icon><StarFilled /></el-icon>
            当前正史：{{ canonScript.title || `版本#${canonScript.id}` }}
          </el-tag>
        </div>
      </template>
      
      <el-empty v-if="scriptHistory.length === 0" description="暂无历史版本，请生成剧情" />
      
      <div v-else class="script-list">
        <div 
          v-for="script in sortedHistory" 
          :key="script.id"
          class="script-item"
          :class="{ 'is-canon': script.isCanon, 'is-new': script.id === lastGeneratedId }"
        >
          <div class="script-main">
            <div class="script-header">
              <div class="script-title-row">
                <!-- 正史标识 -->
                <el-tag 
                  v-if="script.isCanon" 
                  type="warning" 
                  effect="dark" 
                  class="canon-tag"
                >
                  <el-icon><Trophy /></el-icon>
                  正史
                </el-tag>
                <el-tag v-else type="info" effect="plain">备选</el-tag>
                
                <span class="script-id">#{{ script.id }}</span>
                <span class="script-time">{{ formatTime(script.createdAt) }}</span>
              </div>
              
              <h4 class="script-title">
                {{ getScriptTitle(script) || '未命名剧情' }}
              </h4>
              
              <div class="script-path" :title="script.branchPath">
                <el-icon><Connection /></el-icon>
                {{ formatPath(script.branchPath) }}
              </div>
            </div>
            
            <div class="script-actions">
              <el-button 
                type="primary" 
                link 
                :icon="View" 
                @click="viewScriptDetail(script.id)"
              >
                查看详情
              </el-button>
              
              <el-button 
                v-if="!script.isCanon"
                type="warning" 
                link 
                :icon="Star"
                @click="markAsCanon(script.id)"
                :loading="markingCanon === script.id"
              >
                设为正史
              </el-button>
              
              <el-tag v-else type="success" effect="plain" class="canon-status">
                当前正史
              </el-tag>
            </div>
          </div>
          
          <!-- 内容预览（前100字） -->
          <div v-if="getContentPreview(script)" class="script-preview">
            {{ getContentPreview(script) }}...
          </div>
        </div>
      </div>
    </el-card>
    <el-dialog 
      v-model="detailVisible" 
      :title="currentScript?.title || '剧本详情'" 
      width="900px"
      class="script-detail-dialog"
    >
      <div v-if="currentScript" class="script-detail">
        <div class="detail-meta">
          <el-descriptions :column="3" size="small" border>
            <el-descriptions-item label="剧本ID">#{{ currentScript.id }}</el-descriptions-item>
            <el-descriptions-item label="生成时间">{{ formatTime(currentScript.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="正史状态">
              <el-tag :type="currentScript.isCanon ? 'warning' : 'info'">
                {{ currentScript.isCanon ? '正史' : '备选' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="分支路径" :span="3">
              {{ formatPath(currentScript.branchPath) }}
              <el-tag size="small" type="info" style="margin-left: 8px">{{ currentScript.branchPath }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        
        <el-divider content-position="left">剧情内容</el-divider>
        
        <div class="detail-content markdown-body" v-html="renderedContent" />
      </div>
      
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button 
          v-if="currentScript && !currentScript.isCanon"
          type="warning" 
          @click="markAsCanon(currentScript.id); detailVisible = false"
        >
          设为正史
        </el-button>
      </template>
    </el-dialog>


    <!-- 路径选择器弹窗 -->
    <el-dialog v-model="showPathSelector" title="选择分支路径（点击节点构建路径）" width="900px">
      <div class="path-selector">
        <el-alert 
          title="先点击起点节点，然后依次点击后续节点。若两点间有多条边，将自动弹出选择" 
          type="info" 
          :closable="false" 
          class="path-hint" 
        />
        
        <!-- 【修改后】已选路径展示：边在节点之间 -->
        <div class="selected-path">
          <template v-for="(step, index) in selectedPath" :key="index">
            <!-- 第一个节点（起点） -->
            <template v-if="index === 0">
              <el-tag 
                type="success" 
                effect="dark"
                closable 
                @close="removeFromPath(index)"
                class="node-tag start-node"
              >
                <el-icon><Position /></el-icon>
                {{ step.nodeName }}
              </el-tag>
            </template>
            
            <!-- 后续节点：先显示边，再显示节点 -->
            <template v-else>
              <!-- 到达该节点的边 -->
              <span class="edge-connector">
                <el-icon><ArrowRight /></el-icon>
                <el-tag 
                  type="warning" 
                  effect="light" 
                  class="edge-tag"
                  closable
                  @close="removeFromPath(index)"
                >
                  {{ step.edgeLabel || '未命名选项' }}
                  <span class="edge-id">#{{ step.edgeId }}</span>
                </el-tag>
              </span>
              
              <!-- 节点 -->
              <el-tag 
                type="primary" 
                effect="dark"
                closable 
                @close="removeFromPath(index)"
                class="node-tag"
              >
                {{ step.nodeName }}
              </el-tag>
            </template>
          </template>
          
          <el-button 
            v-if="selectedPath.length > 0" 
            link 
            type="danger" 
            @click="clearPath"
            class="clear-btn"
          >
            <el-icon><Delete /></el-icon>
            清空
          </el-button>
          
          <el-empty v-if="selectedPath.length === 0" description="请点击画布中的节点开始构建路径" :image-size="40" />
        </div>

        <VueFlow 
          v-model="flowElements" 
          :default-zoom="0.8" 
          :min-zoom="0.2" 
          :max-zoom="2" 
          @node-click="onNodeSelect"
          class="selector-flow"
        >
          <Background pattern-color="#aaa" :gap="20" />
          
          <!-- 自定义边（保持防重叠） -->
          <template #edge-custom="edgeProps">
            <CustomEdge v-bind="edgeProps" />
          </template>
          
          <!-- 【新增】自定义节点：带背景框 -->
          <template #node-default="nodeProps">
            <div 
              class="flow-node-card"
              :class="{ 'is-selected': isNodeInPath(nodeProps.id) }"
            >
              <div class="node-title">{{ nodeProps.data?.label || nodeProps.id }}</div>
              <div v-if="isNodeInPath(nodeProps.id)" class="node-order">
                {{ getNodeOrder(nodeProps.id) }}
              </div>
            </div>
          </template>
        </VueFlow>
      </div>
      <template #footer>
        <el-button @click="showPathSelector = false">取消</el-button>
        <el-button type="primary" @click="confirmPath" :disabled="selectedPath.length < 2">
          确认路径 ({{ selectedPath.length }}个节点)
        </el-button>
      </template>
    </el-dialog>

    <!-- 边选择对话框（保持不变） -->
    <el-dialog 
      v-model="showEdgeSelector" 
      title="选择分支选项" 
      width="500px"
      :close-on-click-modal="false"
    >
      <el-alert 
        :title="`从 ${lastNode?.data?.label} 到 ${pendingNode?.data?.label} 有多个选项：`" 
        type="info" 
        :closable="false"
        class="edge-alert"
      />
      <div class="edge-options">
        <el-card 
          v-for="edge in availableEdges" 
          :key="edge.id"
          shadow="hover"
          class="edge-option-card"
          @click="selectEdge(edge)"
        >
          <div class="edge-option-content">
            <div class="edge-label">{{ edge.label || '未命名选项' }}</div>
            <div v-if="edge.data?.conditionExpr" class="edge-condition">
              条件: {{ edge.data.conditionExpr }}
            </div>
            <div v-if="edge.data?.onSuccess" class="edge-success">
              {{ edge.data.onSuccess.substring(0, 30) }}...
            </div>
          </div>
        </el-card>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, defineComponent, h, computed } from 'vue'
import { 
  VideoPlay, View, Star, StarFilled, Trophy, Connection, 
  Position, ArrowRight, Delete 
} from '@element-plus/icons-vue'
import { VueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import '@vue-flow/core/dist/style.css'
import VueJsonPretty from 'vue-json-pretty'
import 'vue-json-pretty/lib/styles.css'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useProjectStore } from '@/store/modules/project'
import { useGenerationStore } from '@/store/modules/generation'
import api from '@/api'

// ============ 类型定义 ============
interface ScriptHistoryItem {
  id: number
  branchPath: string
  contentType: string
  content: string | null
  contentSummary: string | null
  isCanon: boolean
  createdAt: string
  updatedAt: string
  projectId: number
  projectName: string
  referencedNodes: string
  title?: string // 从contentSummary解析
}

interface PathStep {
  nodeId: string
  nodeName: string
  edgeId?: number
  edgeLabel?: string
}

// CustomEdge 组件保持不变
const CustomEdge = defineComponent({
  // CustomEdge 实现
  name: 'CustomEdge',
  props: [
    'id', 'sourceX', 'sourceY', 'targetX', 'targetY', 
    'sourcePosition', 'targetPosition', 'data', 'label',
    'markerEnd', 'style'
  ],
  setup(props) {
    const index = props.data?.edgeIndex || 0
    const total = props.data?.totalEdges || 1
    const offsetX = (index - (total - 1) / 2) * 100
    const labelOffsetY = (index - (total - 1) / 2) * 25

    const path = computed(() => {
      const sx = props.sourceX, sy = props.sourceY
      const tx = props.targetX, ty = props.targetY
      const curvature = 0.5, dx = tx - sx
      const cp1x = sx + dx * curvature + offsetX, cp1y = sy
      const cp2x = tx - dx * curvature + offsetX, cp2y = ty
      return `M ${sx} ${sy} C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${tx} ${ty}`
    })

    const labelPos = computed(() => ({
      x: (props.sourceX + props.targetX) / 2 + offsetX * 0.6,
      y: (props.sourceY + props.targetY) / 2 + labelOffsetY
    }))

    const textWidth = computed(() => {
      if (!props.label) return 80
      const chinese = (props.label.match(/[\u4e00-\u9fa5]/g) || []).length
      return Math.max(80, chinese * 14 + (props.label.length - chinese) * 7 + 20)
    })

    return () => h('g', {}, [
      h('path', {
        d: path.value,
        class: 'vue-flow__edge-path',
        style: { stroke: props.style?.stroke || 'var(--accent-gold)', strokeWidth: 3, fill: 'none' },
        'marker-end': props.markerEnd
      }),
      props.label && h('rect', {
        x: labelPos.value.x - textWidth.value / 2,
        y: labelPos.value.y - 14,
        width: textWidth.value,
        height: 28,
        fill: 'var(--paper-card)',
        stroke: props.style?.stroke || 'var(--accent-gold)',
        'stroke-width': 1.5,
        rx: 14,
        filter: 'drop-shadow(0 2px 6px rgba(0,0,0,0.15))'
      }),
      props.label && h('text', {
        x: labelPos.value.x,
        y: labelPos.value.y,
        dy: '0.35em',
        'text-anchor': 'middle',
        style: { fontSize: '13px', fontWeight: 'bold', fill: '#333' }
      }, props.label)
    ])
  }
})


const projectStore = useProjectStore()
const generationStore = useGenerationStore()

const branchPath = ref('')
const style = ref('')
const generating = ref(false)
const result = ref<any>(null)
const showPathSelector = ref(false)
const flowElements = ref<any[]>([])
const selectedPath = ref<PathStep[]>([])

const showEdgeSelector = ref(false)
const availableEdges = ref<any[]>([])
const pendingNode = ref<any>(null)

// ============ 历史列表状态 ============
const scriptHistory = ref<ScriptHistoryItem[]>([])
const loadingHistory = ref(false)
const markingCanon = ref<number | null>(null) // 正在标记正史的scriptId
const lastGeneratedId = ref<number | null>(null) // 最新生成的ID，用于高亮
const detailVisible = ref(false)
const currentScript = ref<ScriptHistoryItem | null>(null)

const lastNode = computed(() => 
  selectedPath.value.length > 0 
    ? flowElements.value.find(e => e.id === selectedPath.value[selectedPath.value.length - 1].nodeId)
    : null
)

// 判断节点是否已在路径中
const isNodeInPath = (nodeId: string) => {
  return selectedPath.value.some(s => s.nodeId === nodeId)
}

// ============ 计算属性 ============
const sortedHistory = computed(() => {
  // 正史置顶，其余按时间倒序
  return [...scriptHistory.value].sort((a, b) => {
    if (a.isCanon && !b.isCanon) return -1
    if (!a.isCanon && b.isCanon) return 1
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
  })
})

const canonScript = computed(() => {
  return scriptHistory.value.find(s => s.isCanon) || null
})

const renderedContent = computed(() => {
  if (!currentScript.value?.content) return ''
  // 简单Markdown转HTML（实际项目中建议使用marked库）
  let content = currentScript.value.content
  // 去除可能的JSON包装
  try {
    const parsed = JSON.parse(content)
    if (parsed.novel) content = parsed.novel
  } catch (e) {
    // 不是JSON，保持原样
  }
  return content
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/\*\*(.*)\*\*/gim, '<b>$1</b>')
    .replace(/\n/gim, '<br>')
})

// ============ 【新增】历史列表方法 ============

// 加载历史列表（接口2）
const fetchScriptHistory = async () => {
  if (!projectStore.currentProjectId) return
  loadingHistory.value = true
  try {
    const res = await fetch(`/api/project/${projectStore.currentProjectId}/scripts/list`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
      }
    })
    const data = await res.json()
    if (data.code === 200) {
      // 解析contentSummary获取标题
      scriptHistory.value = (data.data || []).map((item: ScriptHistoryItem) => {
        try {
          const summary = item.contentSummary ? JSON.parse(item.contentSummary) : {}
          item.title = summary.title || summary.novel?.match(/^# (.*)/m)?.[1]
        } catch (e) {
          item.title = undefined
        }
        return item
      })
    }
  } catch (error) {
    console.error('加载历史失败:', error)
    ElMessage.error('加载历史版本失败')
  } finally {
    loadingHistory.value = false
  }
}

// 查看详情（接口3）
const viewScriptDetail = async (scriptId: number) => {
  if (!projectStore.currentProjectId) return
  try {
    const res = await fetch(`/api/project/${projectStore.currentProjectId}/scripts/${scriptId}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
      }
    })
    const data = await res.json()
    if (data.code === 200) {
      currentScript.value = data.data
      detailVisible.value = true
    }
  } catch (error) {
    ElMessage.error('加载详情失败')
  }
}

// 标记为正史（接口1）
const markAsCanon = async (scriptId: number) => {
  if (!projectStore.currentProjectId) return
  
  // 二次确认
  try {
    await ElMessageBox.confirm(
      '确定将此版本设为正史（主线剧情）？其他版本将自动取消正史标记。',
      '确认设为正史',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }
  
  markingCanon.value = scriptId
  try {
    const res = await fetch(
      `/api/project/${projectStore.currentProjectId}/scripts/${scriptId}/mark-canon`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
        }
      }
    )
    const data = await res.json()
    if (data.code === 200) {
      ElMessage.success('已设为正史')
      // 刷新列表
      await fetchScriptHistory()
    } else {
      throw new Error(data.msg)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '标记失败')
  } finally {
    markingCanon.value = null
  }
}

// 工具方法：格式化时间
const formatTime = (timeStr: string) => {
  const date = new Date(timeStr)
  return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 工具方法：格式化路径显示
const formatPath = (pathStr: string) => {
  if (!pathStr) return '-'
  // 将 node_xxx|1->node_yyy 转为简化显示
  const nodes = pathStr.split('->').map((segment, idx) => {
    const [nodeId] = segment.split('|')
    // 尝试从flowElements找友好名称
    const node = flowElements.value.find(e => e.id === nodeId && !e.source)
    return node?.data?.label || `节点${idx + 1}`
  })
  return nodes.join(' → ')
}

// 工具方法：获取剧本标题
const getScriptTitle = (script: ScriptHistoryItem) => {
  if (script.title) return script.title
  // 尝试从content解析第一行标题
  if (script.content) {
    try {
      const parsed = JSON.parse(script.content)
      const match = parsed.novel?.match(/^# (.*)/m)
      if (match) return match[1]
    } catch (e) {
      const match = script.content.match(/^# (.*)/m)
      if (match) return match[1]
    }
  }
  return null
}

// 工具方法：获取内容预览
const getContentPreview = (script: ScriptHistoryItem) => {
  if (!script.content) return null
  try {
    const parsed = JSON.parse(script.content)
    const text = parsed.novel || ''
    return text.replace(/#.*\n/g, '').substring(0, 80).trim()
  } catch (e) {
    return script.content.substring(0, 80).trim()
  }
}

//获取节点在路径中的序号
const getNodeOrder = (nodeId: string) => {
  const index = selectedPath.value.findIndex(s => s.nodeId === nodeId)
  return index + 1
}

const calculateEdgeOffsets = (allEdges: any[]) => {
  const groups = new Map()
  allEdges.forEach(e => {
    if (!e.source) return
    const key = `${e.source}-${e.target}`
    if (!groups.has(key)) groups.set(key, [])
    groups.get(key).push(e)
  })
  
  groups.forEach((edges) => {
    edges.forEach((e, i) => {
      e.type = 'custom'
      e.data = { ...e.data, edgeIndex: i, totalEdges: edges.length }
      const colors = ['#a94442', '#6b5b4e', '#4a7c59', '#b8860b', '#8b6914']
      e.style = { stroke: colors[i % colors.length], strokeWidth: 3 }
    })
  })
}

const fetchCanvasData = async () => {
  if (!projectStore.currentProjectId) return
  try {
    const [nodeList, edgeList] = await Promise.all([
      api.node.getList(projectStore.currentProjectId),
      api.edge.getList(projectStore.currentProjectId)
    ])
    
    const nodes = (nodeList || []).map((n: any) => ({ 
      id: n.id, 
      type: 'default', 
      position: { x: n.positionX || 0, y: n.positionY || 0 }, 
      data: { label: n.nodeName || n.id } 
    }))
    
    const edges = (edgeList || []).map((e: any) => ({ 
      id: String(e.id),
      source: e.sourceId, 
      target: e.targetId, 
      label: e.label || '选项',
      type: 'custom',
      data: { 
        conditionExpr: e.conditionExpr,
        onSuccess: e.onSuccess 
      }
    }))
    
    calculateEdgeOffsets(edges)
    flowElements.value = [...nodes, ...edges]
  } catch (error) {
    console.error(error)
    ElMessage.error('加载画布数据失败')
  }
}

const onNodeSelect = (event: any) => {
  const nodeId = event.node.id
  const nodeName = event.node.data?.label || nodeId
  
  if (selectedPath.value.some(s => s.nodeId === nodeId)) {
    ElMessage.warning('该节点已在路径中')
    return
  }

  if (selectedPath.value.length === 0) {
    selectedPath.value.push({ nodeId, nodeName })
    return
  }

  const prevNodeId = selectedPath.value[selectedPath.value.length - 1].nodeId
  const edges = flowElements.value.filter((e: any) => 
    e.source === prevNodeId && e.target === nodeId
  )

  if (edges.length === 0) {
    ElMessage.warning(`从 ${prevNodeId} 无法直接到达 ${nodeId}，请确保有连线`)
    return
  }

  if (edges.length === 1) {
    selectedPath.value.push({
      nodeId,
      nodeName,
      edgeId: Number(edges[0].id),
      edgeLabel: edges[0].label || '未命名'
    })
  } else {
    pendingNode.value = { id: nodeId, data: { label: nodeName } }
    availableEdges.value = edges
    showEdgeSelector.value = true
  }
}

const selectEdge = (edge: any) => {
  if (!pendingNode.value) return
  
  selectedPath.value.push({
    nodeId: pendingNode.value.id,
    nodeName: pendingNode.value.data.label,
    edgeId: Number(edge.id),
    edgeLabel: edge.label || '未命名'
  })
  
  showEdgeSelector.value = false
  pendingNode.value = null
  availableEdges.value = []
}

const removeFromPath = (index: number) => {
  selectedPath.value = selectedPath.value.slice(0, index)
}

const clearPath = () => {
  selectedPath.value = []
}

const openPathSelector = () => {
  selectedPath.value = []
  fetchCanvasData()
  showPathSelector.value = true
}

const confirmPath = () => {
  if (selectedPath.value.length < 2) {
    ElMessage.warning('请至少选择两个节点构成路径')
    return
  }

  // 正确映射：取下一个节点的 edgeId 作为当前节点的出口
  const segments = selectedPath.value.map((step, index) => {
    // 如果不是最后一个节点，需要加上"到达下一个节点的边ID"
    if (index < selectedPath.value.length - 1) {
      const nextStep = selectedPath.value[index + 1]
      return `${step.nodeId}|${nextStep.edgeId}`
    }
    // 最后一个节点只有ID，没有边
    return step.nodeId
  })

  branchPath.value = segments.join('->')
  showPathSelector.value = false
  ElMessage.success('路径已选择')
}

// 生成成功后刷新历史列表
const generateScript = async () => {
  if (!projectStore.currentProjectId) return
  if (!branchPath.value) {
    ElMessage.warning('请输入分支路径')
    return
  }

  generating.value = true
  generationStore.startGeneration('L9')
  
  try {
    const res = await api.script.generateL9(
      projectStore.currentProjectId, 
      branchPath.value,
      style.value
    )
    result.value = res
    lastGeneratedId.value = res.scriptId || res.id // 假设返回中有ID
    ElMessage.success('剧情生成成功')
    
    // 生成成功后自动刷新历史列表
    await fetchScriptHistory()
    
  } catch (error: any) {
    ElMessage.error(error.message || '生成失败')
  } finally {
    generating.value = false
    generationStore.endGeneration()
  }
}
onMounted(() => {
  fetchScriptHistory() // 加载历史列表
})
</script>

<style scoped lang="scss">
.generation-page { max-width: 900px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.path-input { display: flex; gap: 10px; }
.path-input .el-input { flex: 1; }
.result-card { margin-top: 20px; }

.path-selector { height: 500px; display: flex; flex-direction: column; }
.path-hint { margin-bottom: 10px; }

/*路径展示样式：边在节点之间 */
.selected-path {
  padding: 12px;
  background: $paper-deep;
  border-radius: 8px;
  margin-bottom: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
  min-height: 60px;
  border: 1px solid $border-light;
}

.node-tag {
  font-size: 14px;
  padding: 6px 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.node-tag.start-node {
  background-color: $accent-green;
  border-color: $accent-green;
}

.edge-connector {
  display: inline-flex;
  align-items: center;
  color: $ink-tertiary;
  margin: 0 2px;
}

.edge-tag {
  font-size: 13px;
  padding: 4px 8px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.edge-tag:hover {
  transform: scale(1.05);
}

.edge-id {
  font-size: 10px;
  opacity: 0.7;
  background: rgba(0,0,0,0.1);
  padding: 1px 4px;
  border-radius: 3px;
}

.path-arrow {
  margin: 0 4px;
  color: $ink-tertiary;
  font-weight: bold;
}

.clear-btn {
  margin-left: auto;
  padding: 4px 8px;
}

.selector-flow { 
  flex: 1; 
  border: 1px solid $border-light; 
  border-radius: 4px; 
}

/* 自定义节点样式：带背景框 */
.flow-node-card {
  background: $paper-card;
  border: 2px solid $border-light;
  border-radius: 8px;
  padding: 8px 16px;
  min-width: 100px;
  text-align: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
}

.flow-node-card:hover {
  border-color: $accent-gold;
  box-shadow: 0 4px 16px 0 rgba(64, 158, 255, 0.3);
  transform: translateY(-2px);
}

.flow-node-card.is-selected {
  background: rgba($accent-gold, 0.06);
  border-color: $accent-gold;
  border-width: 2px;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.2);
}

.node-title {
  font-size: 14px;
  font-weight: bold;
  color: $ink-primary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 120px;
}

.node-order {
  position: absolute;
  top: -8px;
  right: -8px;
  background: $accent-gold;
  color: white;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  border: 2px solid white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

/* 边选择对话框样式 */
.edge-alert {
  margin-bottom: 15px;
}

.edge-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 400px;
  overflow-y: auto;
}

.edge-option-card {
  cursor: pointer;
  transition: all 0.3s;
  border-left: 4px solid $accent-gold;
}

.edge-option-card:hover {
  border-color: $accent-gold;
  transform: translateX(5px);
  background-color: $paper-deep;
}

.edge-option-content {
  padding: 5px;
}

.edge-label {
  font-weight: bold;
  font-size: 14px;
  color: $ink-primary;
  margin-bottom: 5px;
}

.edge-condition {
  font-size: 12px;
  color: $accent-gold-light;
  font-family: monospace;
  background: rgba($accent-gold, 0.06);
  padding: 2px 6px;
  border-radius: 3px;
  display: inline-block;
  margin-bottom: 4px;
}

.edge-success {
  font-size: 12px;
  color: $accent-green;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 历史列表样式 */
.history-card { margin-top: 20px; }
.canon-badge { display: flex; align-items: center; gap: 5px; }

.script-list { display: flex; flex-direction: column; gap: 12px; }
.script-item {
  border: 1px solid $border-light;
  border-radius: 8px;
  padding: 16px;
  background: $paper-card;
  transition: all 0.3s;
}
.script-item:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}
.script-item.is-canon {
  border-color: $accent-gold-light;
  background: rgba($accent-gold, 0.04);
  box-shadow: 0 0 0 1px $accent-gold-light;
}
.script-item.is-new {
  animation: highlight 3s ease-out;
}

@keyframes highlight {
  0% { background: rgba($accent-gold, 0.06); border-color: $accent-gold; }
  100% { background: $paper-card; border-color: $border-light; }
}

.script-main { display: flex; justify-content: space-between; align-items: flex-start; }
.script-header { flex: 1; }
.script-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.canon-tag {
  background: $accent-gold-light;
  border-color: $accent-gold-light;
  display: flex;
  align-items: center;
  gap: 4px;
}
.script-id { color: $ink-tertiary; font-size: 12px; font-family: monospace; }
.script-time { color: $ink-tertiary; font-size: 13px; margin-left: auto; }
.script-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: $ink-primary;
  font-weight: 600;
}
.script-path {
  font-size: 12px;
  color: $ink-secondary;
  display: flex;
  align-items: center;
  gap: 4px;
}
.script-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
  margin-left: 20px;
}
.canon-status {
  background: rgba($accent-green, 0.06);
  color: $accent-green;
}
.script-preview {
  margin-top: 12px;
  padding: 12px;
  background: $paper-deep;
  border-radius: 4px;
  font-size: 13px;
  color: $ink-secondary;
  line-height: 1.6;
  border-left: 3px solid $border-light;
}

/* 详情弹窗样式 */
.script-detail-dialog :deep(.el-dialog__body) {
  max-height: 60vh;
  overflow-y: auto;
}
.detail-meta { margin-bottom: 20px; }
.detail-content {
  line-height: 1.8;
  color: $ink-primary;
}
.detail-content :deep(h1) {
  font-size: 24px;
  color: $ink-primary;
  border-bottom: 2px solid $accent-gold;
  padding-bottom: 10px;
  margin-bottom: 20px;
}
.detail-content :deep(h2) {
  font-size: 20px;
  color: $ink-secondary;
  margin-top: 30px;
  margin-bottom: 15px;
}
.detail-content :deep(h3) {
  font-size: 16px;
  color: $ink-tertiary;
  margin-top: 20px;
}
</style>