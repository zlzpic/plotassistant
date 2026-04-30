<template>
  <div class="characters-page">
    <div class="page-header">
      <h3>角色管理</h3>
      <div class="header-actions">
        <el-button type="primary" :icon="Lightning" @click="showGenerateDialog">AI 生成角色 (L2)</el-button>
        <el-button type="success" :icon="Plus" @click="showCreateDialog">添加角色</el-button>
      </div>
    </div>

    <el-row :gutter="20" v-loading="loading">
      <el-col v-for="char in characters" :key="char.id" :xs="24" :sm="12" :md="8" :lg="6" class="character-col">
        <el-card class="character-card" shadow="hover">
          <div class="character-avatar">
            <el-avatar :size="60" :icon="UserFilled" />
          </div>
          <!-- 显示ID -->
          <div class="character-id">ID: {{ char.id.slice(0, 8) }}...</div>
          <h4 class="character-name">{{ char.name }}</h4>
          <!-- 使用 roleType -->
          <el-tag :type="getRoleTypeColor(char.roleType)" size="small" class="character-role">
            {{ formatRoleType(char.roleType) }}
          </el-tag>
          <!-- 使用 roleDescription -->
          <p class="character-desc">{{ char.roleDescription || '暂无描述' }}</p>
          <div class="character-actions">
            <el-button type="primary" link @click="editCharacter(char)">编辑</el-button>
            <el-button type="danger" link @click="deleteCharacter(char)">删除</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && characters.length === 0" description="暂无角色" />

    <!-- 创建/编辑弹窗 - 适配详细字段 -->
    <el-dialog :title="isEdit ? '编辑角色' : '添加角色'" v-model="dialogVisible" width="700px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        
        <el-form-item label="角色类型">
          <el-select v-model="form.roleType" placeholder="选择角色类型" style="width: 100%">
            <el-option label="主角" value="PROTAGONIST" />
            <el-option label="反派" value="ANTAGONIST" />
            <el-option label="配角" value="SUPPORT" />
            <el-option label="路人" value="NPC" />
          </el-select>
        </el-form-item>

        <el-form-item label="人格设定">
          <el-input 
            v-model="form.personaPrompt" 
            type="textarea" 
            :rows="4" 
            placeholder="详细描述角色性格、动机、矛盾点...（用于AI生成对话）" 
          />
        </el-form-item>

        <el-form-item label="语言风格">
          <el-input 
            v-model="form.speechPattern" 
            type="textarea" 
            :rows="2" 
            placeholder="如：语调平冷、善用比喻、尾音带叹息..." 
          />
        </el-form-item>

        <el-form-item label="知识范围">
          <el-select
            v-model="form.knowledgeScope"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入角色知道的关键信息，按回车添加"
            style="width: 100%"
          >
            <el-option
              v-for="item in form.knowledgeScope"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
          <div class="form-hint">如：天道裂痕真实扩张速度、逆命者血脉秘密等</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">{{ isEdit ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>

    <!-- AI 生成弹窗 -->
    <el-dialog v-model="generateDialogVisible" title="AI 生成重要角色 (L2)" width="600px">
      <el-form label-position="top">
        <el-form-item label="生成数量">
          <el-slider v-model="generateCount" :min="1" :max="5" show-stops show-input />
          <div class="form-hint">建议一次生成 1-3 个核心角色</div>
        </el-form-item>
        
        <el-form-item label="补充提示词（可选）">
          <el-input 
            v-model="generatePrompt" 
            type="textarea" 
            :rows="6" 
            placeholder="输入具体要求，如：需要一个冷酷的反派..." 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="generateCharacters" :loading="generating">
          开始生成 ({{ generateCount }}个角色)
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Plus, Lightning, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useProjectStore } from '@/store/modules/project'
import { useGenerationStore } from '@/store/modules/generation'
import api from '@/api'

const projectStore = useProjectStore()
const generationStore = useGenerationStore()

const loading = ref(false)
const characters = ref<any[]>([])
const dialogVisible = ref(false)
const generateDialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const generating = ref(false)
const currentCharId = ref<string>('') // 存储当前编辑的角色ID
const generatePrompt = ref('')
const generateCount = ref(2)
const formRef = ref()

// 表单字段适配后端API
const form = reactive({
  name: '',
  roleType: 'NPC',        // 对应 roleType
  roleDescription: '',    // 对应 roleDescription
  personaPrompt: '',      // 对应 personaPrompt（AI人格提示词）
  speechPattern: '',      // 对应 speechPattern（语言风格）
  knowledgeScope: [] as string[] // 对应 knowledgeScope（数组）
})

const rules = { 
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }] 
}

// 角色类型颜色
const getRoleTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    'PROTAGONIST': 'danger',  // 主角用红色突出
    'ANTAGONIST': 'warning',  // 反派用橙色
    'SUPPORT': 'success',     // 配角用绿色
    'NPC': 'info'             // 路人用灰色
  }
  return colors[type] || 'info'
}

// 角色类型中文映射
const formatRoleType = (type: string) => {
  const names: Record<string, string> = {
    'PROTAGONIST': '主角',
    'ANTAGONIST': '反派',
    'SUPPORT': '配角',
    'NPC': '路人'
  }
  return names[type] || type
}

const fetchCharacters = async () => {
  if (!projectStore.currentProjectId) return
  loading.value = true
  try {
    const res = await api.character.getList(projectStore.currentProjectId)
    characters.value = res || []
  } catch (error) {
    console.error(error)
    ElMessage.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

const showCreateDialog = () => {
  isEdit.value = false
  currentCharId.value = ''
  // 重置表单
  form.name = ''
  form.roleType = 'NPC'
  form.roleDescription = ''
  form.personaPrompt = ''
  form.speechPattern = ''
  form.knowledgeScope = []
  dialogVisible.value = true
}

// 编辑时获取详情
const editCharacter = async (char: any) => {
  if (!projectStore.currentProjectId) return
  
  isEdit.value = true
  currentCharId.value = char.id
  
  // 获取角色详情
  loading.value = true
  try {
    const detail = await api.character.getDetail(projectStore.currentProjectId, char.id)
    if (detail) {
      form.name = detail.name || ''
      form.roleType = detail.roleType || 'NPC'
      form.roleDescription = detail.roleDescription || ''
      form.personaPrompt = detail.personaPrompt || ''
      form.speechPattern = detail.speechPattern || ''
      // 处理 knowledgeScope 可能是 JSON 字符串或数组的情况
      if (Array.isArray(detail.knowledgeScope)) {
        form.knowledgeScope = detail.knowledgeScope
      } else if (typeof detail.knowledgeScope === 'string') {
        try {
          form.knowledgeScope = JSON.parse(detail.knowledgeScope)
        } catch (e) {
          form.knowledgeScope = detail.knowledgeScope ? [detail.knowledgeScope] : []
        }
      } else {
        form.knowledgeScope = []
      }
    }
    dialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '获取角色详情失败')
  } finally {
    loading.value = false
  }
}

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  
  submitting.value = true
  try {
    //提交时转换 knowledgeScope 为 JSON 字符串（如果后端需要）
    const submitData = {
      ...form
    }
    
    if (isEdit.value && currentCharId.value) {
      await api.character.update(projectStore.currentProjectId!, currentCharId.value, submitData)
      ElMessage.success('更新成功')
    } else {
      await api.character.create(projectStore.currentProjectId!, submitData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchCharacters()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const deleteCharacter = (char: any) => {
  ElMessageBox.confirm(`确定要删除角色 "${char.name}" 吗？`, '确认删除', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await api.character.delete(projectStore.currentProjectId!, char.id)
      ElMessage.success('删除成功')
      fetchCharacters()
    } catch (error: any) {
      ElMessage.error(error.message || '删除失败')
    }
  })
}

const showGenerateDialog = () => {
  generatePrompt.value = ''
  generateCount.value = 2
  generateDialogVisible.value = true
}

const generateCharacters = async () => {
  if (!projectStore.currentProjectId) return
  
  generating.value = true
  generateDialogVisible.value = false
  generationStore.startGeneration('L2')
  
  try {
    await api.character.generateL2(projectStore.currentProjectId, { 
      count: generateCount.value,
      prompt: generatePrompt.value 
    })
    ElMessage.success(`成功生成 ${generateCount.value} 个角色`)
    fetchCharacters()
  } catch (error: any) {
    ElMessage.error(error.message || '生成失败')
  } finally {
    generating.value = false
    generationStore.endGeneration()
  }
}

onMounted(() => {
  fetchCharacters()
})
</script>

<style scoped lang="scss">
.page-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin-bottom: 20px; 
}
.page-header h3 { 
  margin: 0; 
}
.header-actions { 
  display: flex; 
  gap: 10px; 
}
.character-col { 
  margin-bottom: 20px; 
}
.character-card { 
  text-align: center; 
  padding: 10px; 
  position: relative;
}
.character-avatar { 
  margin-bottom: 10px; 
}
/* ID显示样式 */
.character-id {
  font-size: 11px;
  color: $ink-tertiary;
  font-family: 'Courier New', monospace;
  margin-bottom: 8px;
  background: $paper-deep;
  padding: 2px 6px;
  border-radius: 4px;
  display: inline-block;
}
.character-name { 
  margin: 0 0 8px; 
  font-size: 18px; 
  color: $ink-primary; 
  font-weight: bold;
}
.character-role { 
  margin: 0 0 10px; 
}
.character-desc { 
  margin: 0 0 15px; 
  font-size: 13px; 
  color: $ink-secondary; 
  overflow: hidden; 
  text-overflow: ellipsis; 
  display: -webkit-box; 
  -webkit-line-clamp: 3; 
  -webkit-box-orient: vertical; 
  line-height: 1.5;
  min-height: 60px;
  text-align: left;
}
.character-actions { 
  display: flex; 
  justify-content: center; 
  gap: 10px; 
}
.form-hint {
  font-size: 12px;
  color: $ink-tertiary;
  margin-top: 5px;
  line-height: 1.4;
}
</style>