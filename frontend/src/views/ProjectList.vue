<template>
  <div class="project-list-page">
    <div class="page-header">
      <h2>我的项目</h2>
      <el-button type="primary" :icon="Plus" @click="showCreateDialog">
        新建项目
      </el-button>
    </div>

    <el-row :gutter="20" v-loading="loading">
      <el-col
        v-for="project in projects"
        :key="project.id"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
        class="project-col"
      >
        <el-card class="project-card" shadow="hover" @click="enterProject(project)">
          <div class="project-card-header">
            <el-icon size="32" class="project-icon"><Document /></el-icon>
            <el-dropdown @command="(cmd) => handleCommand(cmd, project)" @click.stop>
              <el-icon class="more-icon"><More /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item command="delete" divided type="danger">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
          <h3 class="project-name">{{ project.name }}</h3>
          <p class="project-desc">{{ project.description || '暂无描述' }}</p>
          <div class="project-meta">
            <el-tag :type="getStatusType(project.status)" size="small">
              {{ getStatusText(project.status) }}
            </el-tag>
            <span class="update-time">{{ formatDate(project.updatedAt) }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && projects.length === 0" description="暂无项目，点击右上角创建" />

    <el-dialog v-model="dialogVisible" title="新建项目" width="500px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入项目描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate" :loading="submitting">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑项目" width="500px">
      <el-form ref="editFormRef" :model="editForm" :rules="formRules" label-width="80px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="editForm.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目描述" prop="description">
          <el-input v-model="editForm.description" type="textarea" :rows="4" placeholder="请输入项目描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api'
import { useProjectStore } from '@/store/modules/project'
import type { ProjectItem } from '@/api/project'

const router = useRouter()
const projectStore = useProjectStore()
const loading = ref(false)
const projects = ref<ProjectItem[]>([])
const dialogVisible = ref(false)
const editDialogVisible = ref(false)
const submitting = ref(false)
const currentProject = ref<ProjectItem | null>(null)
const formRef = ref()
const editFormRef = ref()

const form = reactive({ name: '', description: '' })
const editForm = reactive({ name: '', description: '' })
const formRules = { name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }] }

const fetchProjects = async () => {
  loading.value = true
  try {
    const res: any = await api.project.getList({ page: 1, size: 100 })
    
    projects.value = res || []
    
    console.log('API返回:', res)
    console.log('是否是数组:', Array.isArray(res))
  } catch (error) {
    console.error(error)
    ElMessage.error('获取失败')
  } finally {
    loading.value = false
  }
}

const showCreateDialog = () => {
  form.name = ''
  form.description = ''
  dialogVisible.value = true
}

const submitCreate = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await api.project.create(form)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    fetchProjects()
  } catch (error: any) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

const handleCommand = (cmd: string, project: ProjectItem) => {
  if (cmd === 'edit') {
    currentProject.value = project
    editForm.name = project.name
    editForm.description = project.description || ''
    editDialogVisible.value = true
  } else if (cmd === 'delete') {
    handleDelete(project)
  }
}

const submitEdit = async () => {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid || !currentProject.value) return
  submitting.value = true
  try {
    await api.project.update(currentProject.value.id, editForm)
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    fetchProjects()
  } catch (error: any) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    submitting.value = false
  }
}

const handleDelete = (project: ProjectItem) => {
  ElMessageBox.confirm(`确定要删除项目 "${project.name}" 吗？此操作不可恢复。`, '确认删除', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await api.project.delete(project.id)
      ElMessage.success('删除成功')
      fetchProjects()
    } catch (error: any) {
      ElMessage.error(error.message || '删除失败')
    }
  })
}

const enterProject = (project: ProjectItem) => {
  projectStore.selectProject(project.id, project)
  router.push(`/project/${project.id}/world`)
}

const getStatusType = (status: string) => {
  const types: Record<string, string> = { 'DRAFT': 'info', 'IN_PROGRESS': 'warning', 'COMPLETED': 'success' }
  return types[status] || 'info'
}

const getStatusText = (status: string) => {
  const texts: Record<string, string> = { 'DRAFT': '草稿', 'IN_PROGRESS': '进行中', 'COMPLETED': '已完成' }
  return texts[status] || status
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(() => {
  fetchProjects()
})
</script>

<style scoped lang="scss">
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; }
.project-col { margin-bottom: 20px; }
.project-card { cursor: pointer; transition: all 0.3s; }
.project-card:hover { transform: translateY(-2px); }
.project-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.more-icon { cursor: pointer; padding: 4px; border-radius: 4px; }
.more-icon:hover { background: $paper-deep; }
.project-name { margin: 0 0 10px; font-size: 16px; color: $ink-primary; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.project-desc { margin: 0 0 15px; font-size: 13px; color: $ink-tertiary; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.project-meta { display: flex; justify-content: space-between; align-items: center; }
.update-time { font-size: 12px; color: $ink-tertiary; }
.project-icon { color: $accent-gold; }
</style>
