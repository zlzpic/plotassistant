<template>
  <el-menu
    :default-active="activeMenu"
    class="global-menu"
    router
  >
    <el-menu-item index="/projects">
      <el-icon><Folder /></el-icon>
      <span>项目列表</span>
    </el-menu-item>
    <el-menu-item index="/profile">
      <el-icon><User /></el-icon>
      <span>个人中心</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>

<style scoped lang="scss">
.global-menu {
  border-right: none;
}
</style>
