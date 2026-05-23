<template>
  <el-container class="layout-container">
    <!-- 顶部导航 -->
    <el-header class="header">
      <div class="header-left">
        <router-link to="/" class="logo">🎬 VideoClip</router-link>
      </div>
      <div class="header-right">
        <template v-if="authStore.isLoggedIn">
          <router-link to="/upload">
            <el-button type="primary" size="small">上传视频</el-button>
          </router-link>
          <router-link to="/tasks">
            <el-button size="small">我的任务</el-button>
          </router-link>
          <router-link v-if="authStore.isAdmin" to="/admin">
            <el-button size="small" type="warning">管理后台</el-button>
          </router-link>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              {{ authStore.user?.nickname || authStore.user?.email }}
              <el-icon><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login">
            <el-button size="small">登录</el-button>
          </router-link>
          <router-link to="/register">
            <el-button type="primary" size="small">注册</el-button>
          </router-link>
        </template>
      </div>
    </el-header>

    <!-- 主内容 -->
    <el-main class="main">
      <router-view />
    </el-main>

    <!-- 底部 -->
    <el-footer class="footer">
      <span>© 2024 VideoClip - 直播长视频切片助手</span>
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

function handleCommand(command: string) {
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
  padding: 0 20px;
}
.logo {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #606266;
}
.main {
  background: #f5f7fa;
  min-height: calc(100vh - 120px);
}
.footer {
  text-align: center;
  color: #999;
  font-size: 13px;
  line-height: 60px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
}
</style>
