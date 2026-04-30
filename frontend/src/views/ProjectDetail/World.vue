<template>
  <div class="world-page">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>世界观设置 (L1)</span>
          <div class="header-actions">
            <el-button type="primary" :icon="Lightning" @click="showGenerateDialog">
              AI 生成世界观
            </el-button>
            <el-button type="success" :icon="Check" @click="saveWorld" :loading="saving">
              保存设定
            </el-button>
          </div>
        </div>
      </template>

      <el-form label-position="top" :model="worldSetting">
        <!-- 第一行：类型与等级 -->
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="主类型 (Genre)">
              <el-input v-model="worldSetting.genre" placeholder="如：科幻、奇幻、武侠" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="子类型 (Sub Genre)">
              <el-input v-model="worldSetting.subGenre" placeholder="如：赛博朋克、太空歌剧" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="科技等级 (0-10)">
              <el-slider v-model="worldSetting.techLevel" :max="10" show-stops />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="魔法等级 (0-10)">
              <el-slider v-model="worldSetting.magicLevel" :max="10" show-stops />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第二行：时空背景 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="时间背景 (Time)">
              <el-input 
                v-model="worldSetting.timeBackground" 
                placeholder="如：近未来2149年、大航海时代" 
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="地理背景 (Location)">
              <el-input 
                v-model="worldSetting.geoBackground" 
                placeholder="如：新上海城邦，分层都市结构" 
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第三行：核心冲突 -->
        <el-form-item label="核心冲突 (Core Conflict)">
          <el-input 
            v-model="worldSetting.coreConflict" 
            type="textarea" 
            :rows="3" 
            placeholder="故事的核心矛盾是什么？" 
          />
        </el-form-item>

        <!-- 第四行：特殊规则 -->
        <el-form-item label="特殊规则 (Special Rules)">
          <el-input 
            v-model="worldSetting.specialRules" 
            type="textarea" 
            :rows="4" 
            placeholder="这个世界的独特规则？" 
          />
        </el-form-item>

        <!-- 第五行：AI生成描述 (Description) -->
        <el-form-item label="世界观描述 (AI生成)">
          <div class="description-header">
            <span class="desc-label">由 AI 生成的详细世界观文本</span>
            <el-button 
              v-if="worldSetting.description" 
              text 
              size="small" 
              type="primary"
              @click="toggleDescFormat"
            >
              {{ showRawDesc ? '查看渲染文本' : '查看原始JSON' }}
            </el-button>
          </div>
          
          <!-- 渲染后的纯文本（默认显示） -->
          <el-input 
            v-if="!showRawDesc"
            v-model="displayDescription" 
            type="textarea" 
            :rows="8" 
            placeholder="点击上方'AI生成世界观'按钮生成详细描述..."
            class="description-textarea"
          />
          
          <!-- 原始 JSON（调试用） -->
          <el-input 
            v-else
            v-model="worldSetting.description" 
            type="textarea" 
            :rows="8" 
            disabled
            class="description-raw"
          />
          
          <div class="description-hint" v-if="parsedDescInfo">
            <el-tag size="small" type="info">{{ parsedDescInfo }}</el-tag>
          </div>
        </el-form-item>

        <el-form-item label="更新时间">
          <el-input v-model="worldSetting.updatedAt" disabled style="width: 200px;" />
        </el-form-item>
      </el-form>
    </el-card>


  <!-- 大纲展示卡片 -->
  <el-card class="outline-card" v-loading="outlineLoading">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <span>故事大纲 (L3)</span>
          </div>
          <div class="header-actions">
            <el-button 
              text 
              size="small" 
              @click="showRawJson = !showRawJson"
            >
              {{ showRawJson ? '查看可视化' : '查看原始JSON' }}
            </el-button>
            <el-button type="primary" :icon="Lightning" @click="showOutlineGenerateDialog">
              AI 重新生成
            </el-button>
          </div>
        </div>
      </template>

      <!-- 原始 JSON 视图 -->
      <div v-if="showRawJson" class="raw-json-view">
        <pre>{{ JSON.stringify(outline, null, 2) }}</pre>
      </div>

      <!-- 可视化视图 -->
      <div v-else-if="outline && outline.acts" class="outline-visual">
        <!-- 大标题区 -->
        <div class="outline-header">
          <h1 class="story-title">{{ outline.title }}</h1>
          <div class="header-tags">
            <el-tag v-if="outline.endingType" :type="getEndingTypeColor(outline.endingType)" size="large" class="ending-tag">
              {{ formatEndingType(outline.endingType) }}
            </el-tag>
          </div>
        </div>

        <!-- 主题区 -->
        <div class="themes-section" v-if="outline.themes && outline.themes.length">
          <div class="section-label">核心主题</div>
          <div class="themes-list">
            <div v-for="(theme, index) in outline.themes" :key="index" class="theme-quote">
              <el-icon><ChatDotRound /></el-icon>
              <span>{{ theme }}</span>
            </div>
          </div>
        </div>

        <el-divider content-position="left">三幕式结构</el-divider>

        <!-- 手风琴：每幕一个面板 -->
        <el-collapse v-model="activeActs" class="acts-collapse">
          <el-collapse-item 
            v-for="act in outline.acts" 
            :key="act.actNumber"
            :name="act.actNumber"
            class="act-item"
          >
            <template #title>
              <div class="act-header">
                <div class="act-number">第 {{ act.actNumber }} 幕</div>
                <div class="act-info">
                  <span class="act-name">{{ act.name }}</span>
                  <span class="beat-count">{{ act.beats?.length || 0 }} 个节拍</span>
                </div>
              </div>
            </template>

            <!-- 幕简介 -->
            <div class="act-summary">
              <el-alert 
                :title="act.summary" 
                type="info" 
                :closable="false"
                show-icon
              />
            </div>

            <!-- 节拍时间轴 -->
            <el-timeline class="beats-timeline">
              <el-timeline-item
                v-for="(beat, beatIndex) in act.beats"
                :key="beatIndex"
                :type="getBeatType(act.actNumber)"
                :color="getBeatColor(act.actNumber)"
                size="large"
                placement="top"
              >
                <el-card class="beat-card" shadow="hover" :body-style="{ padding: '15px' }">
                  <!-- 对象格式（标准格式） -->
                  <template v-if="typeof beat === 'object' && beat !== null">
                    <div class="beat-header">
                      <div class="beat-number">节拍 {{ beat.beatNumber || beatIndex + 1 }}</div>
                      <h4 class="beat-title">{{ beat.title }}</h4>
                    </div>
                    
                    <p class="beat-description">{{ beat.description }}</p>
                    
                    <!-- 关键角色 -->
                    <div class="beat-characters" v-if="beat.keyCharacters && beat.keyCharacters.length">
                      <span class="label">关键角色：</span>
                      <el-tag 
                        v-for="char in beat.keyCharacters" 
                        :key="char"
                        type="primary"
                        size="small"
                        effect="light"
                        class="character-tag"
                      >
                        {{ char }}
                      </el-tag>
                    </div>

                    <!-- 核心冲突（高亮） -->
                    <div class="beat-conflict" v-if="beat.conflict">
                      <el-divider content-position="left">
                        <span class="conflict-label">核心冲突</span>
                      </el-divider>
                      <div class="conflict-text">{{ beat.conflict }}</div>
                    </div>
                  </template>

                  <!-- 字符串格式（兼容后端返回的纯文本数组） -->
                  <template v-else>
                    <div class="beat-header">
                      <div class="beat-number">节拍 {{ beatIndex + 1 }}</div>
                    </div>
                    <p class="beat-description">{{ beat }}</p>
                  </template>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </el-collapse-item>
        </el-collapse>

        <!-- 生成元信息 -->
        <div class="generation-info" v-if="outlineRaw?.aiModel">
          <el-divider></el-divider>
          <div class="info-grid">
            <div class="info-item">
              <el-icon><Cpu /></el-icon>
              <span>模型：{{ outlineRaw.aiModel }}</span>
            </div>
            <div class="info-item">
              <el-icon><Coin /></el-icon>
              <span>Token：{{ outlineRaw.tokenUsage || '-' }}</span>
            </div>
            <div class="info-item">
              <el-icon><Clock /></el-icon>
              <span>更新：{{ formatDate(outlineRaw.updatedAt) }}</span>
            </div>
            <el-tag v-if="outlineRaw.isEdited" type="warning" size="small" class="edited-tag">已人工编辑</el-tag>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无大纲，点击上方按钮生成" />
    </el-card>

    <!-- L1 生成弹窗 -->
    <el-dialog v-model="generateDialogVisible" title="AI 生成世界观 (L1)" width="600px">
      <el-form label-position="top">
        <el-form-item label="补充提示词（可选）">
          <el-input 
            v-model="generatePrompt" 
            type="textarea" 
            :rows="4" 
            placeholder="输入额外要求，如：强调赛博朋克的压抑感、加入东方元素..." 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="generateWorld" :loading="generating">
          开始生成
        </el-button>
      </template>
    </el-dialog>

    <!-- L3 生成弹窗 -->
    <el-dialog v-model="outlineDialogVisible" title="AI 生成故事大纲 (L3)" width="600px">
      <el-form label-position="top">
        <el-form-item label="黑暗度 (1-10)">
          <el-slider v-model="outlineForm.darkness" :max="10" show-stops />
        </el-form-item>
        <el-form-item label="复杂度 (1-10)">
          <el-slider v-model="outlineForm.complexity" :max="10" show-stops />
        </el-form-item>
        <el-form-item label="特殊要求">
          <el-input 
            v-model="outlineForm.prompt" 
            type="textarea" 
            :rows="3" 
            placeholder="如：三幕式结构，主角最终选择牺牲自己..." 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="outlineDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="generateOutline" :loading="outlineGenerating">
          生成大纲
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted ,computed} from 'vue'
import { Lightning, Check, VideoPlay, Cpu, Coin, Clock, ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useProjectStore } from '@/store/modules/project'
import { useGenerationStore } from '@/store/modules/generation'
import api from '@/api'

const projectStore = useProjectStore()
const generationStore = useGenerationStore()

const loading = ref(false)
const saving = ref(false)
const generating = ref(false)
const generateDialogVisible = ref(false)
const generatePrompt = ref('')

const outlineLoading = ref(false)
const outlineGenerating = ref(false)
const outlineDialogVisible = ref(false)
const outline = ref<any>(null)
const outlineRaw = ref<any>(null)
const showRawJson = ref(false)
const activeActs = ref([1]) // 默认展开第一幕（actNumber 从 1 开始）
const showRawDesc = ref(false)

// 与后端字段完全匹配的数据结构
const worldSetting = reactive({
  genre: '',           // 主类型
  subGenre: '',        // 子类型
  techLevel: 5,        // 科技等级 (0-10)
  magicLevel: 0,       // 魔法等级 (0-10)
  timeBackground: '',  // 时间背景
  geoBackground: '',   // 地理背景
  coreConflict: '',    // 核心冲突
  specialRules: '',    // 特殊规则
  description: '',
  updatedAt: ''        // 更新时间（只读）
})


// L3 生成参数
const outlineForm = reactive({
  darkness: 5,
  complexity: 5,
  prompt: ''
})

//计算属性：解析 description JSON 显示纯文本
const displayDescription = computed({
  get() {
    if (!worldSetting.description) return ''
    
    // 尝试解析 JSON 提取 text 字段
    try {
      const parsed = JSON.parse(worldSetting.description)
      if (parsed && parsed.text) {
        return parsed.text
      }
      // 如果没有 text 字段，返回整个 JSON 的字符串形式（美化）
      return JSON.stringify(parsed, null, 2)
    } catch (e) {
      // 如果不是 JSON，直接返回原字符串
      return worldSetting.description
    }
  },
  set(val) {
    // 当用户编辑时，尝试保持 JSON 结构，只修改 text 字段
    if (!worldSetting.description) {
      // 如果没有原数据，创建新的 JSON 结构
      worldSetting.description = JSON.stringify({ text: val }, null, 2)
      return
    }
    
    try {
      const parsed = JSON.parse(worldSetting.description)
      if (parsed && typeof parsed === 'object') {
        parsed.text = val
        worldSetting.description = JSON.stringify(parsed, null, 2)
      } else {
        worldSetting.description = val
      }
    } catch (e) {
      // 如果原数据不是 JSON，直接保存文本
      worldSetting.description = val
    }
  }
})

// 计算属性：显示解析信息
const parsedDescInfo = computed(() => {
  if (!worldSetting.description) return null
  try {
    const parsed = JSON.parse(worldSetting.description)
    if (parsed.text) {
      const wordCount = parsed.text.length
      return `JSON格式 | 文本长度: ${wordCount} 字符`
    }
    return 'JSON格式 | 无text字段'
  } catch (e) {
    return '纯文本格式'
  }
})

//切换显示格式
const toggleDescFormat = () => {
  showRawDesc.value = !showRawDesc.value
}

// 获取世界观详情
const fetchWorldDetail = async () => {
  if (!projectStore.currentProjectId) return
  loading.value = true
  try {
    const res = await api.world.getDetail(projectStore.currentProjectId)
    if (res) {
      // 直接赋值，字段名与后端完全一致
      Object.assign(worldSetting, res)
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取世界观失败')
  } finally {
    loading.value = false
  }
}

// 保存世界观
const saveWorld = async () => {
  if (!projectStore.currentProjectId) return
  
  saving.value = true
  try {
    // 提交时排除 updatedAt（后端自动维护）
    const { updatedAt, ...submitData } = worldSetting
    await api.world.update(projectStore.currentProjectId, submitData)
    ElMessage.success('世界观保存成功')
    fetchWorldDetail() // 刷新以获取最新更新时间
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// L1: AI 生成世界观
const showGenerateDialog = () => {
  generatePrompt.value = ''
  generateDialogVisible.value = true
}

const generateWorld = async () => {
  if (!projectStore.currentProjectId) return
  
  generating.value = true
  generateDialogVisible.value = false
  generationStore.startGeneration('L1')
  
  try {
    const res = await api.world.generateL1(projectStore.currentProjectId, { 
      prompt: generatePrompt.value 
    })
    // AI 生成后，将返回的数据填充到表单（假设后端返回格式与 getDetail 一致）
    if (res && res.text) {
      // 如果 L1 返回的是 text 字符串，需要解析或手动填充
      // 这里假设返回的是结构化数据，直接合并
      Object.assign(worldSetting, res)
    }
    ElMessage.success('世界观生成成功，请检查并保存')
    fetchWorldDetail() // 刷新确认
  } catch (error: any) {
    ElMessage.error(error.message || '生成失败')
  } finally {
    generating.value = false
    generationStore.endGeneration()
  }
}

// L3: AI 生成大纲
const showOutlineGenerateDialog = () => {
  outlineForm.darkness = 5
  outlineForm.complexity = 5
  outlineForm.prompt = ''
  outlineDialogVisible.value = true
}

// L3 生成方法
const generateOutline = async () => {
  if (!projectStore.currentProjectId) {
    ElMessage.warning('请先选择项目')
    return
  }
  
  // 显示全局生成遮罩（禁止切换页面）
  generationStore.startGeneration('L3')
  outlineGenerating.value = true
  outlineDialogVisible.value = false
  
  console.log('开始生成大纲，参数：', outlineForm) // 调试日志
  
  try {
    // 确保调用正确的 API 方法，不使用可选链
    const res = await api.content.generateOutline(
      projectStore.currentProjectId, 
      {
        darkness: outlineForm.darkness,
        complexity: outlineForm.complexity,
        prompt: outlineForm.prompt
      }
    )
    
    console.log('生成响应：', res) // 调试日志
    
    ElMessage.success('大纲生成成功，正在刷新...')
    
    //延迟 500ms 刷新，确保后端已写入数据库
    setTimeout(async () => {
      await fetchOutline()
    }, 500)
    
  } catch (error: any) {
    console.error('生成失败：', error)
    ElMessage.error(error.message || '生成失败，请重试')
  } finally {
    outlineGenerating.value = false
    generationStore.endGeneration()
  }
}

// 获取已保存的大纲
const fetchOutline = async () => {
  if (!projectStore.currentProjectId) return
  
  outlineLoading.value = true
  console.log('正在获取大纲...')
  
  try {
    const res = await api.content.getDetail(projectStore.currentProjectId, 'OUTLINE')
    console.log('获取到大纲数据：', res)
    
    if (res) {
      outlineRaw.value = res
      
      // 解析 contentJson
      let content = res.contentJson
      if (typeof content === 'string') {
        try {
          content = JSON.parse(content)
          console.log('解析后的大纲：', content)
        } catch (e) {
          console.error('解析大纲 JSON 失败:', e)
          ElMessage.error('大纲数据格式错误')
          outline.value = null
          return
        }
      }
      
      outline.value = content
      
      // 默认展开第一幕
      if (content.acts && content.acts.length > 0) {
        activeActs.value = [content.acts[0].actNumber || 1]
      }
    } else {
      console.log('暂无大纲数据')
      outline.value = null
      outlineRaw.value = null
    }
  } catch (error: any) {
    console.error('获取大纲失败:', error)
    ElMessage.error(error.message || '获取大纲失败')
    outline.value = null
  } finally {
    outlineLoading.value = false
  }
}
// 结局类型样式
const getEndingTypeColor = (type: string) => {
  const map: Record<string, string> = {
    'HAPPY': 'success',
    'SAD': 'danger',
    'OPEN': 'warning',
    'BITTER_SWEET': 'primary'
  }
  return map[type] || 'info'
}

const formatEndingType = (type: string) => {
  const map: Record<string, string> = {
    'HAPPY': '圆满结局',
    'SAD': '悲剧结局',
    'OPEN': '开放式结局',
    'BITTER_SWEET': '苦乐参半'
  }
  return map[type] || type
}

// 根据幕和节拍位置返回不同类型（用于时间轴颜色）
// 节拍颜色根据幕数
const getBeatType = (actNumber: number) => {
  const types = ['primary', 'warning', 'danger']
  return types[actNumber - 1] || 'info'
}

const getBeatColor = (actNumber: number) => {
  const colors = ['$accent-gold', '#E6A23C', '#F56C6C']
  return colors[actNumber - 1] || '$ink-tertiary'
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchWorldDetail()
  fetchOutline()
})
</script>

<style scoped lang="scss">
.world-page {
  max-width: 1000px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions { 
  display: flex; 
  gap: 10px; 
}
.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}
/* Description 样式 */
.description-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.desc-label {
  font-size: 13px;
  color: $ink-tertiary;
}
.description-textarea :deep(.el-textarea__inner) {
  font-size: 14px;
  line-height: 1.8;
  background: $paper-card;
}
.description-raw :deep(.el-textarea__inner) {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  background: $paper-deep;
  color: $ink-secondary;
}
.description-hint {
  margin-top: 5px;
  text-align: right;
}

.outline-card { 
  margin-top: 20px; 
}

/* 大纲可视化样式 */
.outline-visual {
  padding: 10px 0;
}

.outline-header {
  text-align: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid $border-light;
}

.story-title {
  font-size: 28px;
  color: $ink-primary;
  margin: 0 0 15px 0;
  font-weight: bold;
}

.header-tags {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.ending-tag {
  font-size: 14px;
  padding: 0 15px;
  height: 32px;
  line-height: 32px;
}

/* 主题区 */
.themes-section {
  background: $paper-deep;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 25px;
}

.section-label {
  font-size: 14px;
  color: $ink-tertiary;
  margin-bottom: 12px;
  font-weight: bold;
}

.themes-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.theme-quote {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 15px;
  color: $ink-secondary;
  font-style: italic;
  line-height: 1.6;
}

.theme-quote .el-icon {
  color: $accent-gold;
  margin-top: 3px;
}

/* 手风琴样式 */
.acts-collapse {
  border: none;
}

.act-item {
  margin-bottom: 15px;
  border: 1px solid $border-light;
  border-radius: 8px;
  overflow: hidden;
}

.act-item :deep(.el-collapse-item__header) {
  padding: 0 20px;
  height: 60px;
  line-height: 60px;
  background: $paper-card;
  border-bottom: 1px solid $border-light;
}

.act-header {
  display: flex;
  align-items: center;
  gap: 15px;
  width: 100%;
}

.act-number {
  background: $accent-gold;
  color: white;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 14px;
  flex-shrink: 0;
}

.act-info {
  display: flex;
  align-items: center;
  gap: 15px;
  flex: 1;
}

.act-name {
  font-size: 17px;
  font-weight: bold;
  color: $ink-primary;
}

.beat-count {
  font-size: 12px;
  color: $ink-tertiary;
  background: $border-light;
  padding: 2px 8px;
  border-radius: 10px;
}

/* 幕简介 */
.act-summary {
  margin: 15px 0 20px 0;
}

.act-summary :deep(.el-alert__title) {
  font-size: 14px;
  line-height: 1.6;
}

/* 节拍时间轴 */
.beats-timeline {
  padding-left: 10px;
}

.beat-card {
  margin-bottom: 10px;
  border-left: 4px solid $accent-gold;
}

.beat-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.beat-number {
  background: #ecf5ff;
  color: $accent-gold;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.beat-title {
  margin: 0;
  font-size: 16px;
  color: $ink-primary;
  font-weight: bold;
}

.beat-description {
  margin: 10px 0;
  color: $ink-secondary;
  line-height: 1.8;
  font-size: 14px;
}

/* 角色标签 */
.beat-characters {
  margin-top: 12px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.beat-characters .label {
  font-size: 13px;
  color: $ink-tertiary;
}

.character-tag {
  font-size: 12px;
}

/* 冲突区 */
.beat-conflict {
  margin-top: 15px;
  background: #fdf6ec;
  border-radius: 4px;
  padding: 0 15px 15px 15px;
}

.conflict-label {
  font-size: 13px;
  color: #e6a23c;
  font-weight: bold;
}

.conflict-text {
  color: #e6a23c;
  font-size: 14px;
  line-height: 1.6;
  font-weight: 500;
}

/* 生成信息 */
.generation-info {
  margin-top: 30px;
  padding-top: 10px;
}

.info-grid {
  display: flex;
  align-items: center;
  gap: 25px;
  flex-wrap: wrap;
  font-size: 13px;
  color: $ink-tertiary;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.info-item .el-icon {
  font-size: 16px;
}

.edited-tag {
  margin-left: auto;
}

/* 原始 JSON 视图 */
.raw-json-view {
  background: $paper-deep;
  padding: 15px;
  border-radius: 4px;
  overflow: auto;
  max-height: 600px;
}

.raw-json-view pre {
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
  color: $ink-primary;
}

.outline-card {
  margin-top: 20px;
}
</style>