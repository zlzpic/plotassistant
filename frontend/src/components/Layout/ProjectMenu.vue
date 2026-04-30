<template>
  <div class="project-menu">
    <div class="project-info" v-if="projectStore.currentProject">
      <el-icon><Document /></el-icon>
      <span class="project-name">{{ projectStore.currentProject.name }}</span>
    </div>
    <el-menu
      :default-active="activeMenu"
      router
    >
      <el-menu-item :index="`/project/${projectId}/world`">
        <el-icon><MapLocation /></el-icon>
        <span>世界观 (L1/L3)</span>
      </el-menu-item>
      <el-menu-item :index="`/project/${projectId}/characters`">
        <el-icon><UserFilled /></el-icon>
        <span>角色 (L2)</span>
      </el-menu-item>
      <el-menu-item :index="`/project/${projectId}/canvas`">
        <el-icon><Connection /></el-icon>
        <span>剧情树 (L4/L5/L6/L7/L8)</span>
      </el-menu-item>
      <el-menu-item :index="`/project/${projectId}/generation`">
        <el-icon><View /></el-icon>
        <span>生成预览 (L9)</span>
      </el-menu-item>
      <el-divider />
      <el-menu-item index="/projects">
        <el-icon><Back /></el-icon>
        <span>返回项目列表</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useProjectStore } from '@/store/modules/project'

const route = useRoute()
const projectStore = useProjectStore()

const activeMenu = computed(() => route.path)
const projectId = computed(() => projectStore.currentProjectId)
</script>

<style scoped lang="scss">
.project-menu {
  height: 100%;
}

.project-info {
  padding: 15px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: $ink-primary;
  border-bottom: 1px solid $border-light;
  font-family: $font-serif;
  font-weight: 600;
}

.project-name {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-divider) {
  background-color: $border-light;
  margin: 10px 0;
}
</style>
