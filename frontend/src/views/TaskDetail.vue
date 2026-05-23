<template>
  <div class="task-detail page-container">
    <el-page-header @back="$router.push('/tasks')">
      <template #content>
        <span>{{ task?.originalFilename || '任务详情' }}</span>
      </template>
    </el-page-header>

    <div v-loading="loading" style="margin-top: 20px">
      <!-- 任务信息卡片 -->
      <el-card shadow="never" v-if="task" class="info-card">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="状态">
            <TaskStatusTag :status="task.status" />
          </el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ formatFileSize(task.fileSize) }}</el-descriptions-item>
          <el-descriptions-item label="时长">{{ formatDuration(task.durationSeconds) }}</el-descriptions-item>
          <el-descriptions-item label="内容类型">{{ contentTypeMap[task.contentType || ''] || '--' }}</el-descriptions-item>
          <el-descriptions-item label="目标平台">{{ platformMap[task.targetPlatform || ''] || '--' }}</el-descriptions-item>
          <el-descriptions-item label="切片数量">{{ task.clipCount }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(task.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatDate(task.completedAt) }}</el-descriptions-item>
          <el-descriptions-item label="错误信息" v-if="task.errorMessage">
            <span style="color: #f56c6c">{{ task.errorMessage }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 处理中进度 -->
        <div v-if="isProcessing" style="margin-top: 16px">
          <el-progress :percentage="progressPercent" :stroke-width="12" striped striped-flow />
          <p style="text-align: center; margin-top: 8px; color: #909399">{{ task.progressStage || '处理中...' }}</p>
        </div>
      </el-card>

      <!-- 视频预览播放器 -->
      <el-card shadow="never" v-if="task?.status === 'COMPLETED'" class="video-card" style="margin-top: 16px">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <span>🎬 视频预览</span>
            <el-button size="small" @click="showVideo = !showVideo">
              {{ showVideo ? '收起' : '展开' }}
            </el-button>
          </div>
        </template>
        <div v-if="showVideo" class="video-container">
          <video
            ref="videoRef"
            :src="videoUrl"
            controls
            preload="metadata"
            class="video-player"
          />
          <div class="video-info">
            当前时间: {{ formatTimeCode(currentTime) }}
          </div>
        </div>
      </el-card>

      <!-- 内容标签页 -->
      <el-tabs v-model="activeTab" v-if="task?.status === 'COMPLETED' || task?.segments?.length" style="margin-top: 20px">
        <!-- 转写文本 -->
        <el-tab-pane label="转写文本" name="transcript">
          <TranscriptViewer :segments="task?.segments || []" @seek="seekTo" />
        </el-tab-pane>

        <!-- AI 分析 -->
        <el-tab-pane label="AI 分析" name="analysis">
          <template v-if="task?.analysis">
            <el-card shadow="never" style="margin-bottom: 16px">
              <h4>简短摘要</h4>
              <p>{{ task.analysis.summaryShort }}</p>
            </el-card>
            <el-card shadow="never" style="margin-bottom: 16px">
              <h4>详细摘要</h4>
              <p>{{ task.analysis.summaryLong }}</p>
            </el-card>
            <el-card shadow="never" style="margin-bottom: 16px">
              <h4>关键观点</h4>
              <ul>
                <li v-for="(point, i) in task.analysis.keyPoints" :key="i">{{ point }}</li>
              </ul>
            </el-card>
            <el-card shadow="never" v-if="task.analysis.goldenQuotes?.length">
              <h4>金句</h4>
              <div v-for="(quote, i) in task.analysis.goldenQuotes" :key="i" class="quote-item">
                <el-tag size="small" type="info">{{ quote.time }}</el-tag>
                <span style="margin-left: 8px">"{{ quote.text }}"</span>
              </div>
            </el-card>
          </template>
          <el-empty v-else description="暂无分析结果" />
        </el-tab-pane>

        <!-- 切片建议 -->
        <el-tab-pane label="切片建议" name="clips">
          <div style="margin-bottom: 16px">
            <el-button type="primary" :loading="batchRendering" @click="handleRenderAll">
              ✂️ 一键裁剪全部
            </el-button>
          </div>
          <div v-if="task?.clips?.length" class="clips-grid">
            <ClipSuggestionCard
              v-for="clip in task.clips"
              :key="clip.id"
              :clip="clip"
              @render="handleRenderClip"
              @download="handleDownloadClip"
              @seek="seekTo"
            />
          </div>
          <el-empty v-else description="暂无切片建议" />
        </el-tab-pane>

        <!-- 导出 -->
        <el-tab-pane label="导出" name="export">
          <el-space wrap>
            <el-button type="primary" @click="handleExport('srt')">下载 SRT 字幕</el-button>
            <el-button @click="handleExport('txt')">下载 TXT 文本</el-button>
            <el-button @click="handleExport('clips')">下载切片建议</el-button>
          </el-space>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTaskDetail, exportSrt, exportTxt, exportClips, renderClip, renderAllClips, getVideoUrl } from '@/api/task'
import { formatFileSize, formatDuration, formatTimeCode, formatDate, platformMap, contentTypeMap } from '@/utils/format'
import { ElMessage } from 'element-plus'
import TaskStatusTag from '@/components/TaskStatusTag.vue'
import TranscriptViewer from '@/components/TranscriptViewer.vue'
import ClipSuggestionCard from '@/components/ClipSuggestionCard.vue'
import type { TaskDetail } from '@/types'

const route = useRoute()
const task = ref<TaskDetail | null>(null)
const loading = ref(false)
const activeTab = ref('transcript')
const showVideo = ref(false)
const videoRef = ref<HTMLVideoElement | null>(null)
const currentTime = ref(0)
const batchRendering = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

const videoUrl = computed(() => {
  const id = Number(route.params.id)
  return getVideoUrl(id)
})

const isProcessing = computed(() => {
  const s = task.value?.status
  return s === 'PENDING' || s === 'EXTRACTING_AUDIO' || s === 'TRANSCRIBING' || s === 'ANALYZING'
})

const progressPercent = computed(() => {
  const stage = task.value?.status
  const map: Record<string, number> = {
    PENDING: 5, EXTRACTING_AUDIO: 25, TRANSCRIBING: 50, ANALYZING: 75, COMPLETED: 100,
  }
  return map[stage || ''] || 10
})

async function loadDetail() {
  try {
    const { data } = await getTaskDetail(Number(route.params.id))
    if (data.code === 200) {
      task.value = data.data as TaskDetail
    }
  } catch { /* ignore */ }
}

function seekTo(timeSeconds: number) {
  if (videoRef.value) {
    videoRef.value.currentTime = timeSeconds
    videoRef.value.play()
    showVideo.value = true
  }
}

function startPolling() {
  timer = setInterval(async () => {
    if (!isProcessing.value) {
      if (timer) clearInterval(timer)
      return
    }
    await loadDetail()
  }, 3000)
}

async function handleRenderClip(clipId: number) {
  const id = Number(route.params.id)
  try {
    await renderClip(id, clipId)
    ElMessage.success('切片裁剪完成')
    await loadDetail()
  } catch {
    ElMessage.error('切片裁剪失败')
  }
}

async function handleRenderAll() {
  const id = Number(route.params.id)
  batchRendering.value = true
  try {
    await renderAllClips(id)
    ElMessage.success('全部切片裁剪完成')
    await loadDetail()
  } catch {
    ElMessage.error('批量裁剪失败')
  } finally {
    batchRendering.value = false
  }
}

async function handleDownloadClip(clipId: number) {
  const id = Number(route.params.id)
  const url = `/api/tasks/${id}/clips/${clipId}/download`
  const a = document.createElement('a')
  a.href = url
  a.download = ''
  a.click()
}

async function handleExport(type: 'srt' | 'txt' | 'clips') {
  const id = Number(route.params.id)
  try {
    const resp = type === 'srt' ? await exportSrt(id) :
                 type === 'txt' ? await exportTxt(id) :
                 await exportClips(id)
    const blob = new Blob([resp.data])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${id}_${type}.${type === 'clips' ? 'txt' : type}`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('导出失败')
  }
}

// 视频时间更新
function onTimeUpdate() {
  if (videoRef.value) {
    currentTime.value = videoRef.value.currentTime
  }
}

onMounted(async () => {
  loading.value = true
  await loadDetail()
  loading.value = false
  if (isProcessing.value) startPolling()
  // 监听视频时间更新
  if (videoRef.value) {
    videoRef.value.addEventListener('timeupdate', onTimeUpdate)
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (videoRef.value) {
    videoRef.value.removeEventListener('timeupdate', onTimeUpdate)
  }
})
</script>

<style scoped>
.info-card { margin-top: 20px; }
.video-card { margin-top: 16px; }
.video-container { text-align: center; }
.video-player {
  width: 100%;
  max-height: 480px;
  border-radius: 8px;
  background: #000;
}
.video-info {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}
.clips-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 16px;
}
.quote-item {
  margin-bottom: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}
h4 { margin-bottom: 10px; color: #303133; }
ul { padding-left: 20px; }
li { margin-bottom: 6px; line-height: 1.6; }
</style>
