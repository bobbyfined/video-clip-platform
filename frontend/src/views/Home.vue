<template>
  <div class="home">
    <div class="hero">
      <h1>🎬 直播长视频切片助手</h1>
      <p class="subtitle">AI 驱动的智能视频内容分析，一键将长视频切片为适合短视频平台的精彩片段</p>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="goStart">
          <el-icon><video-play /></el-icon> 开始使用
        </el-button>
        <el-button size="large" @click="goTasks" v-if="authStore.isLoggedIn">
          <el-icon><list /></el-icon> 我的任务
        </el-button>
      </div>
    </div>

    <div class="features">
      <el-row :gutter="24">
        <el-col :span="6">
          <el-card class="feature-card" shadow="hover">
            <div class="feature-icon">🎤</div>
            <h3>智能语音转写</h3>
            <p>mimo 大模型精准转写直播、播客、课程内容</p>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="feature-card" shadow="hover">
            <div class="feature-icon">🤖</div>
            <h3>AI 内容分析</h3>
            <p>自动生成摘要、关键观点、金句和切片建议</p>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="feature-card" shadow="hover">
            <div class="feature-icon">✂️</div>
            <h3>一键裁剪</h3>
            <p>FFmpeg 精确裁剪，支持批量导出短视频</p>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="feature-card" shadow="hover">
            <div class="feature-icon">🔗</div>
            <h3>链接解析</h3>
            <p>支持抖音/B站/YouTube等 16+ 平台视频链接</p>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <div class="platforms-section" v-if="!authStore.isLoggedIn">
      <h2>支持的平台</h2>
      <p class="platforms-desc">覆盖主流视频平台，粘贴链接即可自动解析</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { VideoPlay, List } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

function goStart() {
  router.push(authStore.isLoggedIn ? '/upload' : '/login')
}
function goTasks() {
  router.push('/tasks')
}
</script>

<style scoped>
.home {
  max-width: 1000px;
  margin: 0 auto;
  padding: 40px 20px;
}
.hero {
  text-align: center;
  padding: 50px 0 40px;
}
.hero h1 {
  font-size: 36px;
  margin-bottom: 16px;
  color: #303133;
}
.subtitle {
  font-size: 16px;
  color: #909399;
  margin-bottom: 30px;
}
.hero-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
.features { margin-top: 20px; }
.feature-card {
  text-align: center;
  padding: 20px 16px;
  transition: transform 0.2s;
}
.feature-card:hover { transform: translateY(-4px); }
.feature-icon { font-size: 40px; margin-bottom: 12px; }
.feature-card h3 { margin-bottom: 8px; color: #303133; font-size: 15px; }
.feature-card p { color: #606266; font-size: 13px; line-height: 1.6; }
.platforms-section {
  text-align: center;
  margin-top: 40px;
  padding-top: 30px;
  border-top: 1px solid #ebeef5;
}
.platforms-section h2 { color: #303133; margin-bottom: 8px; }
.platforms-desc { color: #909399; }
</style>
