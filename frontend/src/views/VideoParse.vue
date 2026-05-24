<template>
  <div class="parse-page">
    <div class="page-header">
      <h2>🔗 视频解析</h2>
      <p class="desc">粘贴视频链接，解析视频信息后下载</p>
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
        <span>正在解析视频信息...</span>
      </div>

      <!-- 解析结果 -->
      <el-card v-if="parsed" class="preview-card" shadow="hover">
        <div class="preview-layout">
          <div class="preview-thumb">🎬</div>
          <div class="preview-info">
            <h3>{{ parsed.title }}</h3>
            <div class="preview-meta">
              <span>⏱️ {{ parsed.duration }}</span>
              <span>📁 {{ parsed.size }}</span>
              <span>📺 {{ parsed.platform }}</span>
              <span>👁️ {{ parsed.views }}</span>
            </div>
            <div class="preview-actions">
              <el-button type="primary" @click="handleDownload">
                ⬇️ 下载视频
              </el-button>
              <el-button @click="$router.push('/clip')">
                ✂️ 发送到 AI 切片
              </el-button>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 下载完成 -->
      <div v-if="downloaded" class="done-state">
        <div class="done-icon">✅</div>
        <h3>下载完成</h3>
        <p>文件已保存到本地</p>
        <el-button @click="reset">下载其他视频</el-button>
      </div>

      <!-- 支持平台 -->
      <div class="platforms">
        <h4>支持的平台</h4>
        <div class="platform-chips">
          <el-tag type="success" v-for="p in supportedPlatforms" :key="p">{{ p }}</el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Link, Loading } from '@element-plus/icons-vue'

const videoUrl = ref('')
const parsing = ref(false)
const downloaded = ref(false)

const parsed = ref<{
  title: string; duration: string; size: string; platform: string; views: string
} | null>(null)

const supportedPlatforms = ['✅ B站', '✅ 抖音', '✅ YouTube', '✅ 快手', '✅ 西瓜视频', '小红书', '微博', '更多...']

async function handleParse() {
  if (!videoUrl.value.trim()) {
    ElMessage.warning('请输入视频链接')
    return
  }
  parsing.value = true
  parsed.value = null
  downloaded.value = false

  // 模拟解析
  setTimeout(() => {
    parsed.value = {
      title: '直播回放 - 科技发布会完整版',
      duration: '2:15:30',
      size: '325.4 MB',
      platform: 'B站',
      views: '12.3万',
    }
    parsing.value = false
  }, 1200)
}

function handleDownload() {
  ElMessage.success('开始下载...')
  setTimeout(() => {
    downloaded.value = true
    parsed.value = null
  }, 800)
}

function reset() {
  videoUrl.value = ''
  parsed.value = null
  downloaded.value = false
}
</script>

<style scoped>
.parse-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 20px;
}
.page-header h2 { font-size: 28px; font-weight: 700; display: flex; align-items: center; gap: 10px; }
.desc { color: #64748b; margin-top: 6px; font-size: 15px; }
.page-body { margin-top: 28px; }

.url-bar { display: flex; gap: 12px; margin-bottom: 24px; }
.url-bar .el-input { flex: 1; }

.loading-state {
  display: flex; align-items: center; gap: 12px;
  padding: 20px 0; color: #6366f1; font-size: 15px; font-weight: 500;
}

.preview-card { margin-bottom: 24px; }
.preview-layout { display: flex; gap: 24px; }
.preview-thumb {
  width: 200px; min-height: 120px; flex-shrink: 0;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 48px; color: #fff;
}
.preview-info { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 10px; }
.preview-info h3 { font-size: 17px; font-weight: 600; }
.preview-meta { display: flex; gap: 16px; font-size: 13px; color: #64748b; flex-wrap: wrap; }
.preview-actions { display: flex; gap: 12px; margin-top: 8px; }

.done-state { text-align: center; padding: 40px 0; }
.done-icon { font-size: 64px; margin-bottom: 16px; }
.done-state h3 { font-size: 20px; margin-bottom: 8px; }
.done-state p { color: #64748b; margin-bottom: 24px; }

.platforms { margin-top: 32px; }
.platforms h4 { font-size: 14px; color: #64748b; margin-bottom: 12px; }
.platform-chips { display: flex; flex-wrap: wrap; gap: 8px; }

@media (max-width: 768px) {
  .url-bar { flex-direction: column; }
  .preview-layout { flex-direction: column; }
  .preview-thumb { width: 100%; min-height: 100px; }
}
</style>
