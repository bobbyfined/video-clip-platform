<template>
  <div class="parse-page">
    <div class="page-header">
      <h2>🔗 视频解析</h2>
      <p class="desc">粘贴视频链接，解析视频信息后下载或发送到 AI 切片</p>
    </div>

    <div class="page-body">
      <!-- URL 输入 -->
      <div class="url-bar">
        <el-input
          v-model="videoUrl"
          placeholder="粘贴视频链接，如 https://www.bilibili.com/video/BV..."
          size="large"
          clearable
          @keydown.enter="handleParse"
        >
          <template #prefix><el-icon><link /></el-icon></template>
        </el-input>
        <el-button type="primary" size="large" :loading="parsing" @click="handleParse">
          🔍 解析
        </el-button>
      </div>

      <!-- 解析中 -->
      <div v-if="parsing" class="loading-state">
        <el-icon class="is-loading"><loading /></el-icon>
        <span>正在下载并解析视频...</span>
      </div>

      <!-- 解析结果 -->
      <el-card v-if="parsed" class="preview-card" shadow="hover">
        <div class="preview-layout">
          <div class="preview-thumb">🎬</div>
          <div class="preview-info">
            <h3>{{ parsed.title }}</h3>
            <div class="preview-meta">
              <span v-if="parsed.duration">⏱️ {{ formatDuration(parsed.duration) }}</span>
              <span>📁 {{ formatFileSize(parsed.fileSize) }}</span>
              <span :class="parsed.status === 'DOWNLOADED' ? 'status-ready' : 'status-auto'">
                {{ parsed.status === 'DOWNLOADED' ? '✅ 仅下载' : '🚀 自动切片中' }}
              </span>
            </div>
            <div class="preview-actions">
              <el-button type="primary" @click="handleDownload">
                ⬇️ 下载视频
              </el-button>
              <el-button @click="viewTask">
                📋 查看任务详情
              </el-button>
              <el-button @click="$router.push('/clip')">
                ✂️ 发送到 AI 切片
              </el-button>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 错误提示 -->
      <el-alert
        v-if="errorMsg"
        :title="errorMsg"
        type="error"
        show-icon
        :closable="true"
        style="margin-bottom: 16px"
        @close="errorMsg = ''"
      />

      <!-- 支持平台 -->
      <div class="platforms">
        <h4>支持的平台</h4>
        <div class="platform-chips">
          <el-tag
            v-for="p in platforms"
            :key="p.name"
            :type="p.supported ? 'success' : 'info'"
            effect="plain"
          >
            {{ p.supported ? '✅' : '' }} {{ p.name }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Link, Loading } from '@element-plus/icons-vue'
import { downloadFromUrl, getSupportedPlatforms } from '@/api/task'

const router = useRouter()
const videoUrl = ref('')
const parsing = ref(false)
const errorMsg = ref('')
const taskId = ref<number | null>(null)

const parsed = ref<{
  title: string
  fileSize: number
  duration: number
  status: string
} | null>(null)

const platforms = ref<Array<{ name: string; supported: boolean }>>([])

// 加载支持的平台
onMounted(async () => {
  try {
    const { data } = await getSupportedPlatforms()
    if (data.code === 200) {
      platforms.value = data.data.map((p: any) => ({
        name: p.name || p,
        supported: true,
      }))
    }
  } catch {
    // 接口失败时用默认列表
    platforms.value = [
      { name: 'B站', supported: true },
      { name: '抖音', supported: true },
      { name: 'YouTube', supported: true },
      { name: '快手', supported: true },
      { name: '西瓜视频', supported: true },
    ]
  }
})

async function handleParse() {
  if (!videoUrl.value.trim()) {
    ElMessage.warning('请输入视频链接')
    return
  }
  parsing.value = true
  parsed.value = null
  errorMsg.value = ''
  taskId.value = null

  // 自动提取 URL（用户可能粘贴了带标题的文字）
  const extractedUrl = extractUrl(videoUrl.value.trim())
  if (!extractedUrl) {
    ElMessage.warning('未检测到有效链接，请粘贴视频URL')
    parsing.value = false
    return
  }

  try {
    // autoProcess=false → 仅下载，不自动切片
    const { data } = await downloadFromUrl(
      extractedUrl,
      'video',
      'douyin',
      5,
      undefined,
      false
    )
    if (data.code === 200) {
      parsed.value = {
        title: data.data.title,
        fileSize: data.data.fileSize,
        duration: data.data.duration,
        status: data.data.status,
      }
      taskId.value = data.data.id
      ElMessage.success('解析成功！')
    } else {
      errorMsg.value = data.message || '解析失败'
    }
  } catch (err: any) {
    errorMsg.value = err.response?.data?.message || '解析失败，请检查链接是否正确'
  } finally {
    parsing.value = false
  }
}

function viewTask() {
  if (taskId.value) {
    router.push(`/tasks/${taskId.value}`)
  }
}

function handleDownload() {
  if (!taskId.value) return
  const token = localStorage.getItem('token')
  fetch(`/api/tasks/${taskId.value}/video`, {
    headers: { 'Authorization': `Bearer ${token}` }
  })
    .then(res => {
      if (!res.ok) throw new Error('下载失败')
      return res.blob()
    })
    .then(blob => {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = parsed.value?.title || 'video.mp4'
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success('下载完成')
    })
    .catch(() => {
      ElMessage.error('下载失败')
    })
}

/** 从文字中提取 URL */
function extractUrl(text: string): string | null {
  const urlRegex = /(https?:\/\/[^\s\]】]+)/g
  const match = text.match(urlRegex)
  return match ? match[0] : null
}

function formatDuration(seconds: number): string {
  if (!seconds) return '--'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return `${m}:${String(s).padStart(2, '0')}`
}

function formatFileSize(bytes: number): string {
  if (!bytes) return '--'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}
</script>

<style scoped>
.parse-page { max-width: 900px; margin: 0 auto; padding: 40px 20px; }
.page-header h2 { font-size: 28px; font-weight: 700; display: flex; align-items: center; gap: 10px; }
.desc { color: #64748b; margin-top: 6px; font-size: 15px; }
.page-body { margin-top: 28px; }
.url-bar { display: flex; gap: 12px; margin-bottom: 24px; }
.url-bar .el-input { flex: 1; }
.loading-state { display: flex; align-items: center; gap: 12px; padding: 20px 0; color: #6366f1; font-size: 15px; font-weight: 500; }
.preview-card { margin-bottom: 24px; }
.preview-layout { display: flex; gap: 24px; }
.preview-thumb { width: 200px; min-height: 120px; flex-shrink: 0; background: linear-gradient(135deg, #667eea, #764ba2); border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 48px; color: #fff; }
.preview-info { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 10px; }
.preview-info h3 { font-size: 17px; font-weight: 600; }
.preview-meta { display: flex; gap: 16px; font-size: 13px; color: #64748b; flex-wrap: wrap; }
.preview-actions { display: flex; gap: 12px; margin-top: 8px; }
.status-ready { color: #10b981; font-weight: 600; }
.status-auto { color: #6366f1; font-weight: 600; }
.platforms { margin-top: 32px; }
.platforms h4 { font-size: 14px; color: #64748b; margin-bottom: 12px; }
.platform-chips { display: flex; flex-wrap: wrap; gap: 8px; }
@media (max-width: 768px) {
  .url-bar { flex-direction: column; }
  .preview-layout { flex-direction: column; }
  .preview-thumb { width: 100%; min-height: 100px; }
}
</style>
