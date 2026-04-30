<template>
  <el-dialog
    v-model="visible"
    title="JSON 预览"
    width="800px"
    :close-on-click-modal="false"
  >
    <div class="json-preview">
      <pre><code>{{ formattedJson }}</code></pre>
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" @click="copyToClipboard">复制</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'

const visible = ref(false)
const jsonData = ref<any>(null)

const formattedJson = computed(() => {
  if (!jsonData.value) return ''
  try {
    return JSON.stringify(jsonData.value, null, 2)
  } catch {
    return String(jsonData.value)
  }
})

const open = (data: any) => {
  jsonData.value = data
  visible.value = true
}

const copyToClipboard = () => {
  navigator.clipboard.writeText(formattedJson.value).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

defineExpose({
  open
})
</script>

<style scoped lang="scss">
.json-preview {
  max-height: 500px;
  overflow: auto;
  background: $paper-deep;
  padding: 15px;
  border-radius: 4px;
}

.json-preview pre {
  margin: 0;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  color: $ink-primary;
}
</style>
