<template>
  <div class="upload-page page-container">
    <h2>上传视频/音频</h2>

    <el-tabs v-model="uploadMode" class="upload-tabs">
      <!-- Tab 1: 本地上传 -->
      <el-tab-pane label="📁 本地上传" name="file">
        <el-card shadow="never" class="upload-card">
          <el-upload
            ref="uploadRef"
            drag
            multiple
            :auto-upload="false"
            :on-change="handleFileChange"
            :before-upload="beforeUpload"
            accept=".mp4,.mov,.avi,.mkv,.webm,.mp3,.wav,.m4a,.flac,.aac"
          >
            <el-icon class="upload-icon"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                支持 mp4, mov, mp3, wav, m4a, webm 等格式，最大 500MB，支持多文件
              </div>
            </template>
          </el-upload>

          <!-- 上传队列 -->
          <div v-if="fileQueue.length > 0" class="upload-queue">
            <el-divider>上传队列 ({{ fileQueue.length }} 个文件)</el-divider>
            <div v-for="(item, index) in fileQueue" :key="index" class="queue-item">
              <div class="queue-info">
                <span class="queue-name">{{ item.file.name }}</span>
                <span class="queue-size">{{ formatFileSize(item.file.size) }}</span>
                <el-tag :type="queueStatusType(item.status)" size="small">{{ queueStatusText(item) }}</el-tag>
              </div>
              <el-progress
                v-if="item.status === 'uploading' || item.status === 'done'"
                :percentage="item.progress"
                :stroke-width="6"
                :status="item.status === 'done' ? 'success' : undefined"
              />
            </div>
          </div>

          <div class="upload-actions">
            <el-button
              type="primary"
              size="large"
              :loading="isUploading"
              :disabled="fileQueue.length === 0"
              @click="handleUploadAll"
            >
              {{ isUploading ? `上传中 (${doneCount}/${fileQueue.length})...` : `开始上传 (${fileQueue.length} 个文件)` }}
            </el-button>
            <el-button v-if="fileQueue.length > 0 && !isUploading" @click="fileQueue = []">清空队列</el-button>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- Tab 2: 链接下载 -->
      <el-tab-pane label="🔗 视频链接" name="url">
        <el-card shadow="never" class="upload-card">
          <div class="url-input-section">
            <el-input
              v-model="videoUrl"
              placeholder="粘贴视频链接，如 https://www.douyin.com/video/xxx"
              size="large"
              clearable
              @keyup.enter="handleUrlDownload"
            >
              <template #prefix>
                <el-icon><link /></el-icon>
              </template>
            </el-input>

            <el-button
              type="primary"
              size="large"
              :loading="isDownloading"
              :disabled="!videoUrl.trim()"
              @click="handleUrlDownload"
              style="margin-top: 12px; width: 100%"
            >
              {{ isDownloading ? '正在解析下载...' : '🚀 解析并创建任务' }}
            </el-button>
          </div>

          <!-- 支持的平台 -->
          <div class="supported-platforms">
            <h4>支持的平台</h4>
            <div class="platform-grid">
              <div v-for="p in platforms" :key="p.id" class="platform-item">
                <span class="platform-icon">{{ p.icon }}</span>
                <span class="platform-name">{{ p.name }}</span>
              </div>
            </div>
            <p class="platform-note">以及更多 yt-dlp 支持的平台...</p>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 公共参数设置 -->
    <el-card shadow="never" class="upload-card" style="margin-top: 16px">
      <el-divider>参数设置</el-divider>
      <el-form :model="params" label-width="100px">
        <el-form-item label="内容类型">
          <el-select v-model="params.contentType" style="width: 100%">
            <el-option label="直播回放" value="live" />
            <el-option label="长视频" value="video" />
            <el-option label="播客" value="podcast" />
            <el-option label="课程" value="course" />
            <el-option label="访谈" value="interview" />
            <el-option label="演讲" value="speech" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标平台">
          <el-select v-model="params.targetPlatform" style="width: 100%">
            <el-option label="抖音" value="douyin" />
            <el-option label="小红书" value="xiaohongshu" />
            <el-option label="微信视频号" value="weixin_video" />
            <el-option label="B站" value="bilibili" />
            <el-option label="快手" value="kuaishou" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="切片数量">
          <el-input-number v-model="params.clipCount" :min="1" :max="20" />
        </el-form-item>
        <el-form-item label="AI 引擎" v-if="llmProviders.length > 0">
          <el-radio-group v-model="params.llmProvider">
            <el-radio-button v-for="p in llmProviders" :key="p.id" :value="p.id">
              {{ p.name }} ({{ p.model }})
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { createTask, getLlmProviders, getSupportedPlatforms, downloadFromUrl } from '@/api/task'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import type { UploadFile } from 'element-plus'

interface QueueItem {
  file: File
  status: 'pending' | 'uploading' | 'done' | 'error'
  progress: number
  taskId?: number
  error?: string
}

const router = useRouter()
const uploadRef = ref()
const fileQueue = ref<QueueItem[]>([])
const isUploading = ref(false)
const uploadMode = ref('file')
const videoUrl = ref('')
const isDownloading = ref(false)

const params = reactive({
  contentType: 'live',
  targetPlatform: 'douyin',
  clipCount: 5,
  llmProvider: '',
})

const llmProviders = ref<{id: string, name: string, model: string}[]>([])
const platforms = ref<{id: string, name: string, icon: string, domain: string}[]>([])

const doneCount = computed(() => fileQueue.value.filter(i => i.status === 'done').length)

onMounted(async () => {
  try {
    const [providersRes, platformsRes] = await Promise.all([
      getLlmProviders(),
      getSupportedPlatforms(),
    ])
    if (providersRes.data.code === 200) {
      llmProviders.value = providersRes.data.data
      if (llmProviders.value.length > 0 && !params.llmProvider) {
        params.llmProvider = llmProviders.value[0].id
      }
    }
    if (platformsRes.data.code === 200) {
      platforms.value = platformsRes.data.data
    }
  } catch { /* ignore */ }
})

function handleFileChange(file: UploadFile) {
  if (!file.raw) return
  const maxSize = 500 * 1024 * 1024
  if (file.raw.size > maxSize) {
    ElMessage.error(`${file.name} 超过 500MB 限制`)
    return
  }
  if (fileQueue.value.some(i => i.file.name === file.raw!.name && i.file.size === file.raw!.size)) return
  fileQueue.value.push({ file: file.raw, status: 'pending', progress: 0 })
}

function beforeUpload() { return false }

function queueStatusType(status: string) {
  const map: Record<string, string> = { pending: 'info', uploading: '', done: 'success', error: 'danger' }
  return map[status] || 'info'
}

function queueStatusText(item: QueueItem) {
  const map: Record<string, string> = { pending: '待上传', uploading: '上传中', done: '已完成', error: '失败' }
  return item.error || map[item.status] || ''
}

async function handleUploadAll() {
  if (fileQueue.value.length === 0) return
  isUploading.value = true
  let lastTaskId: number | null = null

  for (const item of fileQueue.value) {
    if (item.status === 'done') continue
    item.status = 'uploading'
    try {
      const { data } = await createTask(
        item.file, params.contentType, params.targetPlatform, params.clipCount, params.llmProvider,
        (e) => { if (e.total) item.progress = Math.round((e.loaded / e.total) * 100) }
      )
      if (data.code === 200) {
        item.status = 'done'
        item.progress = 100
        item.taskId = data.data.id
        lastTaskId = data.data.id
      } else {
        item.status = 'error'
        item.error = data.message
      }
    } catch (err: any) {
      item.status = 'error'
      item.error = err.response?.data?.message || '上传失败'
    }
  }

  isUploading.value = false
  const successCount = fileQueue.value.filter(i => i.status === 'done').length
  if (successCount > 0) {
    ElMessage.success(`成功上传 ${successCount} 个文件`)
    if (successCount === 1 && lastTaskId) {
      router.push(`/tasks/${lastTaskId}`)
    }
  }
}

async function handleUrlDownload() {
  if (!videoUrl.value.trim()) {
    ElMessage.warning('请输入视频链接')
    return
  }
  isDownloading.value = true
  try {
    const { data } = await downloadFromUrl(
      videoUrl.value.trim(),
      params.contentType,
      params.targetPlatform,
      params.clipCount,
      params.llmProvider,
    )
    if (data.code === 200) {
      ElMessage.success(`下载成功：${data.data.title}，任务已创建`)
      router.push(`/tasks/${data.data.id}`)
    } else {
      ElMessage.error(data.message || '下载失败')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '下载失败，请检查链接是否正确')
  } finally {
    isDownloading.value = false
  }
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}
</script>

<style scoped>
.upload-page h2 { margin-bottom: 20px; }
.upload-card { max-width: 700px; }
.upload-tabs { max-width: 700px; }
.upload-icon { font-size: 48px; color: #c0c4cc; margin-bottom: 10px; }
.upload-actions { text-align: center; margin-top: 20px; }
.upload-queue { margin: 16px 0; }
.queue-item { margin-bottom: 12px; }
.queue-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}
.queue-name { font-weight: 500; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.queue-size { color: #909399; font-size: 13px; }
.url-input-section { margin-bottom: 20px; }
.supported-platforms {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}
.supported-platforms h4 {
  color: #606266;
  font-size: 14px;
  margin-bottom: 12px;
}
.platform-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
}
.platform-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  font-size: 13px;
}
.platform-icon { font-size: 18px; }
.platform-name { color: #303133; }
.platform-note {
  color: #909399;
  font-size: 12px;
  margin-top: 10px;
}
</style>
