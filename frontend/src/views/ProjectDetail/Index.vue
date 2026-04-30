<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useProjectStore } from '@/store/modules/project'
import api from '@/api'

const route = useRoute()
const projectStore = useProjectStore()

onMounted(async () => {
  const projectId = Number(route.params.id)
  if (projectId && projectId !== projectStore.currentProjectId) {
    projectStore.setProjectId(projectId)
    try {
      const project = await api.project.getDetail(projectId)
      projectStore.setProject(project)
    } catch (error) {
      console.error('获取项目详情失败:', error)
    }
  }
})
</script>
