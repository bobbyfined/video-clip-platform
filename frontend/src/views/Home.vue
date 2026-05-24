<template>
  <div class="home">
    <!-- ===== 未登录：产品介绍 ===== -->
    <template v-if="!authStore.isLoggedIn">
      <!-- Hero -->
      <div class="hero">
        <h1>2小时直播 → 5个爆款短视频</h1>
        <p class="subtitle">
          AI 驱动的智能视频切片助手<br />
          一键解析、分析、裁剪，让长视频变身为短视频平台的流量密码
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/parse')">
            🔗 视频解析
          </el-button>
          <el-button size="large" @click="$router.push('/login')">
            ✂️ AI 切片
          </el-button>
        </div>
        <div class="hero-stats">
          <div><h2>16+</h2><p>支持平台</p></div>
          <div><h2>3,200+</h2><p>创作者在用</p></div>
          <div><h2>50,000+</h2><p>视频已切片</p></div>
        </div>
      </div>

      <!-- 作品展示 -->
      <div class="showcase">
        <div class="showcase-head">
          <h2>🔥 看看别人切出来的</h2>
          <p>来自社区创作者的精彩切片，用 AI 从长视频中提炼爆款</p>
        </div>
        <el-row :gutter="16">
          <el-col :span="8" v-for="work in works" :key="work.title">
            <el-card class="work-card" shadow="hover" @click="$router.push('/community')">
              <div class="work-thumb">
                <span class="work-icon">{{ work.icon }}</span>
                <span class="work-dur">{{ work.dur }}</span>
              </div>
              <h4>{{ work.title }}</h4>
              <div class="work-meta">
                <span>{{ work.source }}</span>
                <span>👁 {{ work.views }}</span>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <div class="showcase-more">
          <el-button @click="$router.push('/community')">进入社区看更多 →</el-button>
        </div>
      </div>

      <!-- 功能介绍 -->
      <div class="features">
        <el-row :gutter="24">
          <el-col :span="6" v-for="feat in features" :key="feat.title">
            <el-card class="feature-card" shadow="hover">
              <div class="feature-icon">{{ feat.icon }}</div>
              <h3>{{ feat.title }}</h3>
              <p>{{ feat.desc }}</p>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </template>

    <!-- ===== 已登录：仪表盘 ===== -->
    <template v-else>
      <!-- 欢迎横幅 -->
      <div class="welcome-banner">
        <div class="welcome-content">
          <h2>👋 欢迎回来</h2>
          <p>你已经用 AI 切片处理了 <strong>23</strong> 个视频，生成了 <strong>87</strong> 个精彩片段</p>
          <div class="welcome-actions">
            <el-button type="primary" @click="$router.push('/clip')">✂️ 开始新切片</el-button>
            <el-button @click="$router.push('/parse')">🔗 解析视频</el-button>
          </div>
        </div>
      </div>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stats-row">
        <el-col :span="6" v-for="stat in stats" :key="stat.label">
          <el-card class="stat-card" shadow="hover">
            <div class="stat-num" :style="{ color: stat.color }">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 最近任务 -->
      <div class="section-header">
        <h3>📋 最近任务</h3>
        <el-button text @click="$router.push('/tasks')">查看全部 →</el-button>
      </div>
      <div class="task-list">
        <el-card
          v-for="task in recentTasks"
          :key="task.id"
          class="task-card"
          shadow="hover"
          @click="$router.push(`/tasks/${task.id}`)"
        >
          <div class="task-row">
            <div class="task-thumb">🎬</div>
            <div class="task-info">
              <div class="task-title">{{ task.title }}</div>
              <div class="task-meta">
                <span>🎯 {{ task.platform }}</span>
                <span>🤖 {{ task.engine }}</span>
                <span>📅 {{ task.date }}</span>
              </div>
            </div>
            <el-tag :type="task.statusType" size="small">{{ task.statusText }}</el-tag>
          </div>
        </el-card>
      </div>

      <!-- 热门切片 -->
      <div class="section-header" style="margin-top: 32px">
        <h3>🔥 我的热门切片</h3>
      </div>
      <el-row :gutter="16">
        <el-col :span="12" v-for="clip in hotClips" :key="clip.title">
          <el-card class="clip-card" shadow="hover">
            <div class="clip-thumb">
              <span>{{ clip.icon }}</span>
              <span class="clip-dur">{{ clip.dur }}</span>
            </div>
            <div class="clip-body">
              <h4>{{ clip.title }}</h4>
              <p class="clip-desc">{{ clip.desc }}</p>
              <div class="clip-actions">
                <el-button type="primary" size="small">▶ 预览</el-button>
                <el-button size="small">⬇ 下载</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const works = [
  { icon: '🎤', dur: '0:45', title: '雷军发布会金句：造车这件事我们赌上了全部', source: '来自 2h 发布会', views: '12.3万' },
  { icon: '🎙️', dur: '1:20', title: '播客精华：AI 会取代程序员吗？', source: '来自 3h 播客', views: '8.7万' },
  { icon: '📚', dur: '2:15', title: '机器学习入门：梯度下降讲得最清楚的一次', source: '来自 1.5h 课程', views: '5.2万' },
  { icon: '🎮', dur: '0:30', title: '这波操作直接封神！绝地求生1v4翻盘', source: '来自 4h 直播', views: '23.1万' },
  { icon: '💡', dur: '1:05', title: '张一鸣内部信：为什么我选择退居二线', source: '来自 2h 讲话', views: '15.6万' },
  { icon: '🎵', dur: '0:50', title: '这首歌一开口全场安静了', source: '来自 3h 演唱会', views: '31.8万' },
]

const features = [
  { icon: '🔗', title: '视频解析', desc: '粘贴链接直接解析下载，支持 16+ 平台' },
  { icon: '🤖', title: 'AI 内容分析', desc: '自动转写、生成摘要、提炼金句和切片建议' },
  { icon: '✂️', title: '一键裁剪', desc: 'FFmpeg 精确裁剪，批量导出短视频' },
  { icon: '📤', title: '一键分发', desc: '切片完成直接发布到抖音/B站/小红书' },
]

const stats = [
  { value: '23', label: '处理视频', color: '#6366f1' },
  { value: '87', label: '生成切片', color: '#6366f1' },
  { value: '5.2h', label: '节省时间', color: '#6366f1' },
  { value: '12.8万', label: '切片播放量', color: '#6366f1' },
]

const recentTasks = [
  { id: '1', title: '直播回放-2024-05-20-晚间场.mp4', platform: '抖音', engine: 'mimo', date: '05-20', statusType: 'success', statusText: '✅ 已完成' },
  { id: '2', title: '播客-科技乱炖-第12期.mp3', platform: 'B站', engine: 'GPT-4o', date: '05-19', statusType: '', statusText: '🔄 分析中' },
  { id: '3', title: 'BV1xx411c7mD - 科技发布会完整版', platform: 'YouTube', engine: 'mimo', date: '05-18', statusType: 'success', statusText: '✅ 已完成' },
]

const hotClips = [
  { icon: '🎤', dur: '0:45', title: '雷军发布会金句', desc: '来自直播回放-05-20，AI 识别的高光时刻' },
  { icon: '💡', dur: '1:20', title: '播客精华片段', desc: '来自科技乱炖-第12期，讨论 AI 未来' },
]
</script>

<style scoped>
.home {
  max-width: 1100px;
  margin: 0 auto;
  padding: 40px 20px;
}

/* ===== Hero ===== */
.hero {
  text-align: center;
  padding: 60px 24px 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  color: #fff;
  margin-bottom: 40px;
}
.hero h1 { font-size: 40px; font-weight: 800; margin-bottom: 16px; letter-spacing: -1px; }
.subtitle { font-size: 17px; opacity: 0.9; margin-bottom: 32px; line-height: 1.7; }
.hero-actions { display: flex; gap: 16px; justify-content: center; margin-bottom: 40px; }
.hero-stats { display: flex; gap: 48px; justify-content: center; }
.hero-stats h2 { font-size: 32px; font-weight: 800; }
.hero-stats p { font-size: 14px; opacity: 0.8; }

/* ===== Showcase ===== */
.showcase { margin-bottom: 40px; }
.showcase-head { text-align: center; margin-bottom: 24px; }
.showcase-head h2 { font-size: 24px; margin-bottom: 8px; }
.showcase-head p { color: #64748b; }
.work-card { cursor: pointer; transition: all 0.3s; margin-bottom: 16px; }
.work-card:hover { transform: translateY(-4px); }
.work-thumb {
  height: 120px;
  background: linear-gradient(135deg, #1e1b4b, #4338ca);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  position: relative;
  margin-bottom: 12px;
}
.work-icon { color: #fff; }
.work-dur {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0,0,0,0.7);
  color: #fff;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 12px;
}
.work-card h4 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.work-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #64748b;
}
.showcase-more { text-align: center; margin-top: 16px; }

/* ===== Features ===== */
.features { margin-bottom: 40px; }
.feature-card { text-align: center; padding: 24px 16px; transition: all 0.3s; }
.feature-card:hover { transform: translateY(-4px); }
.feature-icon { font-size: 36px; margin-bottom: 12px; }
.feature-card h3 { font-size: 15px; margin-bottom: 8px; }
.feature-card p { font-size: 13px; color: #64748b; line-height: 1.6; }

/* ===== Logged-in Dashboard ===== */
.welcome-banner {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 32px;
  color: #fff;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
}
.welcome-content h2 { font-size: 22px; margin-bottom: 8px; }
.welcome-content p { opacity: 0.9; font-size: 15px; margin-bottom: 16px; }
.welcome-actions { display: flex; gap: 12px; }

.stats-row { margin-bottom: 28px; }
.stat-card { text-align: center; padding: 16px; }
.stat-num { font-size: 28px; font-weight: 800; }
.stat-label { font-size: 13px; color: #64748b; margin-top: 4px; }

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.section-header h3 { font-size: 18px; font-weight: 600; }

.task-list { margin-bottom: 16px; }
.task-card { margin-bottom: 12px; cursor: pointer; transition: all 0.25s; }
.task-card:hover { transform: translateX(4px); }
.task-row { display: flex; align-items: center; gap: 16px; }
.task-thumb {
  width: 56px; height: 40px; border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: #fff; flex-shrink: 0;
}
.task-info { flex: 1; min-width: 0; }
.task-title { font-size: 14px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.task-meta { display: flex; gap: 12px; margin-top: 4px; font-size: 12px; color: #64748b; }

.clip-card { display: flex; overflow: hidden; margin-bottom: 16px; }
.clip-thumb {
  width: 140px; min-height: 100px; flex-shrink: 0;
  background: linear-gradient(135deg, #1e1b4b, #4338ca);
  display: flex; align-items: center; justify-content: center;
  font-size: 32px; color: #fff; position: relative;
}
.clip-dur {
  position: absolute; bottom: 6px; right: 6px;
  background: rgba(0,0,0,0.75); color: #fff;
  padding: 2px 8px; border-radius: 6px; font-size: 12px;
}
.clip-body { flex: 1; padding: 16px; }
.clip-body h4 { font-size: 14px; font-weight: 600; margin-bottom: 4px; }
.clip-desc { font-size: 12px; color: #64748b; margin-bottom: 12px; }
.clip-actions { display: flex; gap: 8px; }
</style>
