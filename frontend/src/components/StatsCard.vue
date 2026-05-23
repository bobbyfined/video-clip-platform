<template>
  <el-card shadow="hover" class="stats-card">
    <div class="stats-icon" :style="{ background: color + '15', color: color }">
      <el-icon :size="28"><component :is="iconComponent" /></el-icon>
    </div>
    <div class="stats-info">
      <div class="stats-value">{{ value }}</div>
      <div class="stats-title">{{ title }}</div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { User, Document, CircleCheck, CircleClose, DataAnalysis } from '@element-plus/icons-vue'

const props = defineProps<{
  title: string
  value: number
  icon: string
  color?: string
}>()

const iconMap: Record<string, any> = {
  User, Document, CircleCheck, CircleClose, DataAnalysis,
}

const iconComponent = computed(() => iconMap[props.icon] || Document)
</script>

<style scoped>
.stats-card {
  display: flex;
  align-items: center;
}
.stats-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
}
.stats-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 12px;
  flex-shrink: 0;
}
.stats-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}
.stats-title {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
</style>
