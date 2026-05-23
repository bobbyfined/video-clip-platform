<template>
  <div class="home">
    <div class="hero">
      <h1>🎬 直播长视频切片助手</h1>
      <p class="subtitle">AI 驱动的智能视频内容分析，一键将长视频切片为适合短视频平台的精彩片段</p>
      <el-button type="primary" size="large" @click="goStart">
        开始使用
      </el-button>
    </div>

    <div class="features">
      <el-row :gutter="30">
        <el-col :span="8">
          <el-card class="feature-card" shadow="hover">
            <div class="feature-icon">🎤</div>
            <h3>智能语音转写</h3>
            <p>基于 FunASR (Paraformer) 的中文语音识别，精准转写直播、播客、课程等长视频内容</p>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="feature-card" shadow="hover">
            <div class="feature-icon">🤖</div>
            <h3>AI 内容分析</h3>
            <p>使用大语言模型自动生成摘要、关键观点、金句，深度理解视频内容</p>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="feature-card" shadow="hover">
            <div class="feature-icon">✂️</div>
            <h3>智能切片建议</h3>
            <p>根据抖音、小红书、B站等平台特征，智能推荐最佳切片方案和标题</p>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function goStart() {
  router.push(authStore.isLoggedIn ? '/upload' : '/login')
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
  padding: 60px 0 40px;
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
.features {
  margin-top: 20px;
}
.feature-card {
  text-align: center;
  padding: 20px;
}
.feature-icon {
  font-size: 48px;
  margin-bottom: 16px;
}
.feature-card h3 {
  margin-bottom: 10px;
  color: #303133;
}
.feature-card p {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}
</style>
