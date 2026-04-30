<template>
  <div class="node-card" :class="{ selected }">
    <Handle type="target" :position="Position.Top" />
    <div class="node-content">
      <div class="node-title">{{ data.label }}</div>
      <div class="node-desc" v-if="data.sceneDescription">
        {{ data.sceneDescription.slice(0, 50) }}...
      </div>
    </div>
    <Handle type="source" :position="Position.Bottom" />
  </div>
</template>

<script setup lang="ts">
import { Handle, Position } from '@vue-flow/core'

defineProps<{
  data: any
  selected?: boolean
}>()
</script>

<style scoped lang="scss">
.node-card {
  padding: 10px 15px;
  background: $paper-card;
  border: 2px solid $accent-gold;
  border-radius: 8px;
  min-width: 150px;
  cursor: pointer;
  transition: all 0.3s;
}

.node-card:hover {
  box-shadow: 0 4px 12px rgba($accent-gold, 0.3);
}

.node-card.selected {
  border-color: $accent-gold;
  box-shadow: 0 4px 12px rgba($accent-gold, 0.3);
}

.node-title {
  font-weight: bold;
  color: $ink-primary;
  margin-bottom: 5px;
}

.node-desc {
  font-size: 12px;
  color: $ink-tertiary;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
