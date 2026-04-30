<template>
  <el-container class="app-layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <el-icon size="28"><Connection /></el-icon>
        <span>PlotAssistant</span>
      </div>
      <ProjectMenu v-if="isInProject" />
      <GlobalMenu v-else />
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="breadcrumb">
          <el-breadcrumb>
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="user-info">
          <el-dropdown @command="handleCommand">
            <span class="user-name">
              <el-icon><User /></el-icon>
              {{ username }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
        <div class="page-footer-hint">
          <el-icon><InfoFilled /></el-icon>
          <span>接口导致返回空内容后，请再次尝试</span>
        </div>
      </el-main>
    </el-container>

    <AiGenerating v-if="isGenerating" :task="currentTask" />
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { useGenerationStore } from '@/store/modules/generation'
import ProjectMenu from './ProjectMenu.vue'
import GlobalMenu from './GlobalMenu.vue'
import AiGenerating from '@/components/Common/AiGenerating.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const generationStore = useGenerationStore()

const isInProject = computed(() => route.path.startsWith('/project/'))
const isGenerating = computed(() => generationStore.isGenerating)
const currentTask = computed(() => generationStore.currentTask)
const username = computed(() => userStore.username || 'User')

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(r => r.meta?.title)
  return matched.map(r => ({ title: r.meta.title as string, path: r.path }))
})

const handleCommand = (command: string) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.app-layout {
  height: 100vh;
}

.sidebar {
  background: $paper-deep;
  color: $ink-primary;
  border-right: 1px solid $border-light;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  font-family: $font-serif;
  color: $ink-primary;
  border-bottom: 1px solid $border-light;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: $paper-card;
  box-shadow: $shadow-paper;
  border-bottom: 1px solid $border-light;
}

.user-name {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: $ink-secondary;
  font-family: $font-serif;

  &:hover {
    color: $accent-gold;
  }
}

.main-content {
  background: $paper-base;
  padding: 24px;
  overflow-y: auto;
}

.page-footer-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 16px 0 0;
  margin-top: 16px;
  border-top: 1px solid $border-light;
  font-size: 13px;
  color: $ink-tertiary;
  font-family: $font-serif;
}
</style>
