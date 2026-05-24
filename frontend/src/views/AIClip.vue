<template>
  <div class="clip-page">
    <div class="page-header">
      <h2>✂️ AI 切片</h2>
      <p class="desc">上传视频或粘贴链接，AI 自动分析并生成精彩切片</p>
    </div>

    <div class="page-body">
      <!-- 第一步：选择来源 -->
      <div class="step-label">第一步 · 选择视频来源</div>
      <el-tabs v-model="sourceMode" class="source-tabs">
        <el-tab-pane label="📁 本地文件" name="file">
          <el-upload
            drag
            :auto-upload="false"
            :on-change="handleFileChange"
            accept=".mp4,.mov,.avi,.mkv,.webm,.mp3,.wav,.m4a"
          >
            <el-icon class="upload-icon"><upload-filled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 mp4, mov, mp3, wav 等格式，最大 500MB</div>
            </template>
          </el-upload>
          <div v-if="selectedFile" class="file-selected">
            <el-card shadow="hover">
              <div style="display:flex;align-items:center;gap:12px">
                <span style="font-size:28px">🎬</span>
                <div style="flex:1">
                  <div style="font-weight:600;font-size:14px">{{ selectedFile.name }}</div>
                  <div style="font-size:12px;color:#64748b">{{ formatSize(selectedFile.size) }}</div>
                </div>
                <el-tag type="success" size="small">✅ 已选择</el-tag>
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane label="🔗 粘贴链接" name="url">
          <div class="url-bar">
            <el-input v-model="videoUrl" placeholder="粘贴视频链接..." size="large" clearable @keydown.enter="handleParseUrl">
              <template #prefix><el-icon><link /></el-icon></template>
            </el-input>
            <el-button type="primary" size="large" :loading="parsingUrl" @click="handleParseUrl">🔍 解析</el-button>
          </div>
          <div v-if="parsingUrl" class="loading-state">
            <el-icon class="is-loading"><loading /></el-icon>
            <span>正在解析...</span>
          </div>
          <el-card v-if="parsedUrl" shadow="hover" style="margin-top:12px">
            <div style="display:flex;align-items:center;gap:16px">
              <div class="url-thumb">🎬</div>
              <div style="flex:1">
                <h4 style="font-size:15px;margin-bottom:4px">{{ parsedUrl.title }}</h4>
                <div style="font-size:13px;color:#64748b;display:flex;gap:16px">
                  <span>⏱️ {{ parsedUrl.duration }}</span>
                  <span>📁 {{ parsedUrl.size }}</span>
                  <span>📺 {{ parsedUrl.platform }}</span>
                </div>
              </div>
              <el-tag type="success" size="small">✅ 已解析</el-tag>
            </div>
          </el-card>
        </el-tab-pane>
      </el-tabs>

      <!-- 第二步：配置参数 -->
      <div class="step-label" style="margin-top:28px">第二步 · 配置切片参数</div>
      <el-card class="settings-card" shadow="never">
        <template #header>
          <div style="display:flex;align-items:center;gap:8px;font-weight:600">
            <span>⚙️</span> 切片设置
          </div>
        </template>
        <el-form :model="params" label-width="100px">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="目标平台">
                <el-select v-model="params.targetPlatform" style="width:100%">
                  <el-option label="抖音" value="douyin" />
                  <el-option label="视频号" value="shipinhao" />
                  <el-option label="B站" value="bilibili" />
                  <el-option label="YouTube Shorts" value="youtube" />
                  <el-option label="小红书" value="xiaohongshu" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="AI 引擎">
                <el-select v-model="params.llmProvider" style="width:100%">
                  <el-option label="mimo 大模型" value="mimo" />
                  <el-option label="GPT-4o" value="gpt4o" />
                  <el-option label="Claude 3.5" value="claude" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="切片数量">
                <el-input-number v-model="params.clipCount" :min="1" :max="20" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="内容类型">
                <el-select v-model="params.contentType" style="width:100%">
                  <el-option label="视频" value="video" />
                  <el-option label="音频/播客" value="audio" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <div style="text-align:right;margin-top:12px">
            <el-button type="primary" size="large" @click="handleSubmit">
              🚀 开始切片处理
            </el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, Link, Loading } from '@element-plus/icons-vue'

const router = useRouter()
const sourceMode = ref('file')
const videoUrl = ref('')
const parsingUrl = ref(false)
const selectedFile = ref<File | null>(null)
const parsedUrl = ref<{ title: string; duration: string; size: string; platform: string } | null>(null)

const params = reactive({
  targetPlatform: 'douyin',
  llmProvider: 'mimo',
  clipCount: 5,
  contentType: 'video',
})

function handleFileChange(file: any) {
  selectedFile.value = file.raw
}

function handleParseUrl() {
  if (!videoUrl.value.trim()) {
    ElMessage.warning('请输入视频链接')
    return
  }
  parsingUrl.value = true
  parsedUrl.value = null
  setTimeout(() => {
    parsedUrl.value = {
      title: '直播回放 - 科技发布会完整版',
      duration: '2:15:30',
      size: '325.4 MB',
      platform: 'B站',
    }
    parsingUrl.value = false
  }, 1200)
}

function handleSubmit() {
  ElMessage.success('切片任务已提交！')
  router.push('/tasks/1')
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}
</script>

<style scoped>
.clip-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 20px;
}
.page-header h2 { font-size: 28px; font-weight: 700; display: flex; align-items: center; gap: 10px; }
.desc { color: #64748b; margin-top: 6px; font-size: 15px; }
.page-body { margin-top: 28px; }
.step-label { font-size: 13px; font-weight: 600; color: #64748b; margin-bottom: 12px; }
.source-tabs { margin-bottom: 8px; }
.upload-icon { font-size: 48px; color: #c0c4cc; margin-bottom: 12px; }
.file-selected { margin-top: 16px; }
.url-bar { display: flex; gap: 12px; }
.url-bar .el-input { flex: 1; }
.loading-state { display: flex; align-items: center; gap: 12px; padding: 16px 0; color: #6366f1; font-size: 14px; }
.url-thumb {
  width: 60px; height: 40px; border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; color: #fff; flex-shrink: 0;
}
.settings-card { border: 1px solid #e2e8f0; }
@media (max-width: 768px) {
  .url-bar { flex-direction: column; }
}
</style>
