<template>
  <div class="ai-generating-overlay">
    <div class="generating-content">
      <div class="loading-animation">
        <el-icon class="loading-icon"><Loading /></el-icon>
      </div>
      <h3>AI 生成中...</h3>
      <p class="task-name">{{ taskName }}</p>
      <el-progress
        :percentage="progress"
        :stroke-width="10"
        :show-text="true"
        status="success"
      />
      <p class="hint">生成过程可能需要 30-90 秒，请耐心等待</p>
      <el-button type="danger" plain @click="handleCancel">取消生成</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useGenerationStore } from '@/store/modules/generation'

const props = defineProps<{
  task: string
}>()

const generationStore = useGenerationStore()
const progress = ref(0)
let progressTimer: number | null = null

const taskName = computed(() => {
  const taskNames: Record<string, string> = {
    'L1': '生成世界观描述',
    'L2': '生成重要角色',
    'L3': '生成故事大纲',
    'L4': '批量生成节点',
    'L5': '生成场景描述',
    'L6': '生成场景NPC',
    'L7': '生成对话',
    'L8': '生成选项分支',
    'L9': '生成完整剧情'
  }
  return taskNames[props.task] || 'AI 生成中'
})

const startProgress = () => {
  progress.value = 0
  progressTimer = window.setInterval(() => {
    if (progress.value < 90) {
      progress.value += Math.random() * 10
      if (progress.value > 90) progress.value = 90
    }
  }, 3000)
}

const handleCancel = () => {
  if (progressTimer) {
    clearInterval(progressTimer)
  }
  generationStore.endGeneration()
}

onMounted(() => {
  startProgress()
})

onUnmounted(() => {
  if (progressTimer) {
    clearInterval(progressTimer)
  }
})
</script>

<style scoped lang="scss">
.ai-generating-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(61, 43, 31, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.generating-content {
  background: $paper-card;
  padding: 40px 60px;
  border-radius: 4px;
  text-align: center;
  min-width: 400px;
}

.loading-animation {
  margin-bottom: 20px;
}

.loading-icon {
  font-size: 48px;
  color: $accent-gold;
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.task-name {
  color: $ink-secondary;
  margin: 10px 0 20px;
}

.hint {
  color: $ink-tertiary;
  font-size: 12px;
  margin: 20px 0;
}
</style>
