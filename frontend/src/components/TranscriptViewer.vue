<template>
  <div class="transcript-viewer">
    <div v-if="segments.length === 0" class="empty">
      <el-empty description="暂无转写数据" />
    </div>
    <div v-else class="segments">
      <div v-for="seg in segments" :key="seg.id" class="segment">
        <span class="timecode" @click="$emit('seek', seg.startSeconds)">
          {{ formatTimeCode(seg.startSeconds) }}
        </span>
        <span class="text">{{ seg.text }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatTimeCode } from '@/utils/format'
import type { TranscriptSegment } from '@/types'

defineProps<{ segments: TranscriptSegment[] }>()
defineEmits<{ seek: [time: number] }>()
</script>

<style scoped>
.transcript-viewer {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  max-height: 500px;
  overflow-y: auto;
}
.segment {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;
  line-height: 1.6;
}
.segment:last-child {
  border-bottom: none;
}
.timecode {
  flex-shrink: 0;
  color: #409eff;
  font-family: monospace;
  font-size: 13px;
  cursor: pointer;
  padding: 2px 6px;
  background: #ecf5ff;
  border-radius: 4px;
  height: fit-content;
}
.timecode:hover {
  background: #d9ecff;
}
.text {
  color: #303133;
  font-size: 14px;
}
</style>
