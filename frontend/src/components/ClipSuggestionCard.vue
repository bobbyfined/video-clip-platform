<template>
  <el-card shadow="hover" class="clip-card">
    <template #header>
      <div class="clip-header">
        <span class="clip-topic">{{ clip.topic }}</span>
        <el-tag v-if="clip.score" :type="scoreType" size="small">{{ clip.score }}分</el-tag>
      </div>
    </template>

    <div class="clip-time">
      🕐 {{ formatTimeCode(clip.startSeconds) }} - {{ formatTimeCode(clip.endSeconds) }}
    </div>

    <div class="clip-titles">
      <p><strong>标题1：</strong>{{ clip.title1 }}</p>
      <p v-if="clip.title2"><strong>标题2：</strong>{{ clip.title2 }}</p>
    </div>

    <p v-if="clip.summary" class="clip-summary">{{ clip.summary }}</p>

    <div v-if="clip.hookReason" class="clip-hook">
      <el-icon><star /></el-icon>
      <span>{{ clip.hookReason }}</span>
    </div>

    <div class="clip-footer">
      <el-tag v-if="clip.suggestedPlatform" size="small" type="info">
        {{ platformMap[clip.suggestedPlatform] || clip.suggestedPlatform }}
      </el-tag>
      <span v-if="clip.editingNotes" class="edit-notes">📝 {{ clip.editingNotes }}</span>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatTimeCode, platformMap } from '@/utils/format'
import { Star } from '@element-plus/icons-vue'
import type { ClipSuggestion } from '@/types'

const props = defineProps<{ clip: ClipSuggestion }>()

const scoreType = computed(() => {
  const s = props.clip.score || 0
  if (s >= 80) return 'success'
  if (s >= 60) return 'warning'
  return 'danger'
})
</script>

<style scoped>
.clip-card {
  margin-bottom: 0;
}
.clip-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.clip-topic {
  font-weight: bold;
  font-size: 15px;
  color: #303133;
}
.clip-time {
  font-family: monospace;
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}
.clip-titles p {
  margin-bottom: 4px;
  font-size: 14px;
}
.clip-summary {
  margin: 8px 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
}
.clip-hook {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 8px;
  background: #fdf6ec;
  border-radius: 4px;
  font-size: 13px;
  color: #e6a23c;
  margin: 8px 0;
}
.clip-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}
.edit-notes {
  font-size: 12px;
  color: #909399;
}
</style>
