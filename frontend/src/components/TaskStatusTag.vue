<template>
  <el-tag :type="tagType" size="small" :effect="effect">
    {{ label }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TaskStatus } from '@/types'

const props = defineProps<{ status: TaskStatus }>()

const statusConfig: Record<string, { label: string; type: string; effect: string }> = {
  PENDING: { label: '等待中', type: 'info', effect: 'plain' },
  DOWNLOADED: { label: '已下载', type: 'success', effect: 'plain' },
  EXTRACTING_AUDIO: { label: '提取音频', type: '', effect: 'light' },
  TRANSCRIBING: { label: '转写中', type: '', effect: 'light' },
  ANALYZING: { label: '分析中', type: 'warning', effect: 'light' },
  COMPLETED: { label: '已完成', type: 'success', effect: 'light' },
  FAILED: { label: '失败', type: 'danger', effect: 'light' },
}

const config = computed(() => statusConfig[props.status] || { label: props.status, type: 'info', effect: 'plain' })
const label = computed(() => config.value.label)
const tagType = computed(() => config.value.type as any)
const effect = computed(() => config.value.effect as any)
</script>
