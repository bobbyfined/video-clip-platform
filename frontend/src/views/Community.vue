<template>
  <div class="community-page">
    <div class="page-header">
      <h2>🌐 社区</h2>
      <p class="desc">发现精彩切片，分享你的创作</p>
    </div>

    <div class="page-body">
      <!-- 管理员开关 -->
      <el-alert
        v-if="authStore.isAdmin"
        type="warning"
        :closable="false"
        style="margin-bottom:20px"
      >
        <template #title>
          <div style="display:flex;align-items:center;justify-content:space-between;width:100%">
            <span>⚙️ 管理员：社区模块{{ communityVisible ? '对用户可见' : '已隐藏' }}</span>
            <el-switch v-model="communityVisible" active-text="可见" inactive-text="隐藏" />
          </div>
        </template>
      </el-alert>

      <!-- 社区内容 -->
      <template v-if="communityVisible">
        <!-- 横幅 -->
        <div class="comm-banner">
          <h3>🎬 切片作品广场</h3>
          <p>展示你的 AI 切片成果，获取灵感和反馈</p>
          <el-button type="primary">📤 发布我的切片</el-button>
        </div>

        <!-- 分类筛选 -->
        <div class="filter-bar">
          <el-tag
            v-for="cat in categories"
            :key="cat"
            :effect="activeCat === cat ? 'dark' : 'plain'"
            @click="activeCat = cat"
            style="cursor:pointer"
          >
            {{ cat }}
          </el-tag>
        </div>

        <!-- 作品列表 -->
        <el-row :gutter="16">
          <el-col :span="12" v-for="work in works" :key="work.title">
            <el-card class="work-card" shadow="hover">
              <div class="work-thumb">
                <span>{{ work.icon }}</span>
                <span class="work-dur">{{ work.dur }}</span>
              </div>
              <h4>{{ work.title }}</h4>
              <div class="work-meta">
                <span>{{ work.author }}</span>
                <span>❤️ {{ work.likes }}</span>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <!-- 隐藏状态 -->
      <div v-else class="hidden-state">
        <div style="font-size:48px;margin-bottom:16px">🚫</div>
        <h3>社区模块已关闭</h3>
        <p>管理员已隐藏此功能</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const communityVisible = ref(true)
const activeCat = ref('🔥 热门')

const categories = ['🔥 热门', '🆕 最新', '🎤 直播', '🎙️ 播客', '📚 课程', '🎮 游戏']

const works = [
  { icon: '🎤', dur: '0:45', title: '雷军发布会金句：造车赌上全部', author: 'by 创作者小王', likes: '2.3k' },
  { icon: '🎙️', dur: '1:20', title: 'AI 会取代程序员吗？', author: 'by 播客切片师', likes: '1.8k' },
  { icon: '📚', dur: '2:15', title: '梯度下降讲得最清楚的一次', author: 'by 教育切片号', likes: '956' },
  { icon: '🎮', dur: '0:30', title: '绝地求生1v4翻盘名场面', author: 'by 游戏切片Bot', likes: '4.1k' },
]
</script>

<style scoped>
.community-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 20px;
}
.page-header h2 { font-size: 28px; font-weight: 700; display: flex; align-items: center; gap: 10px; }
.desc { color: #64748b; margin-top: 6px; font-size: 15px; }
.page-body { margin-top: 28px; }

.comm-banner {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  border-radius: 16px; padding: 32px; color: #fff;
  text-align: center; margin-bottom: 24px;
}
.comm-banner h3 { font-size: 20px; margin-bottom: 8px; }
.comm-banner p { opacity: 0.9; font-size: 14px; margin-bottom: 16px; }

.filter-bar { display: flex; gap: 10px; margin-bottom: 24px; flex-wrap: wrap; }

.work-card { cursor: pointer; transition: all 0.3s; margin-bottom: 16px; }
.work-card:hover { transform: translateY(-4px); }
.work-thumb {
  height: 120px;
  background: linear-gradient(135deg, #1e1b4b, #4338ca);
  border-radius: 8px; display: flex; align-items: center; justify-content: center;
  font-size: 40px; position: relative; margin-bottom: 12px;
}
.work-dur {
  position: absolute; bottom: 8px; right: 8px;
  background: rgba(0,0,0,0.7); color: #fff;
  padding: 2px 8px; border-radius: 6px; font-size: 12px;
}
.work-card h4 {
  font-size: 14px; font-weight: 600; margin-bottom: 6px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.work-meta {
  display: flex; justify-content: space-between;
  font-size: 12px; color: #64748b;
}

.hidden-state { text-align: center; padding: 60px 0; color: #64748b; }
.hidden-state h3 { margin-bottom: 8px; }
</style>
