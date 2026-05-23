<template>
  <el-container class="layout-container">
    <!-- 顶部导航 -->
    <el-header class="header">
      <div class="header-left">
        <router-link to="/" class="logo">
          <span class="logo-icon">🎬</span>
          <span class="logo-text">VideoClip</span>
        </router-link>
      </div>
      <div class="header-right">
        <template v-if="authStore.isLoggedIn">
          <router-link to="/upload">
            <el-button type="primary" size="small">
              <el-icon><upload-filled /></el-icon>
              <span>上传视频</span>
            </el-button>
          </router-link>
          <router-link to="/tasks">
            <el-button size="small">
              <el-icon><list /></el-icon>
              <span>我的任务</span>
            </el-button>
          </router-link>
          <router-link v-if="authStore.isAdmin" to="/admin">
            <el-button size="small" type="warning">
              <el-icon><setting /></el-icon>
              <span>管理后台</span>
            </el-button>
          </router-link>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><user /></el-icon>
              {{ authStore.user?.nickname || authStore.user?.email }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><user /></el-icon>个人信息
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><switch-button /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login">
            <el-button size="small">
              <el-icon><key /></el-icon>登录
            </el-button>
          </router-link>
          <router-link to="/register">
            <el-button type="primary" size="small">
              <el-icon><edit-pen /></el-icon>注册
            </el-button>
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
      <span>© 2026 VideoClip - AI 智能视频切片助手</span>
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ArrowDown, UploadFilled, List, Setting, User, SwitchButton, Key, EditPen } from '@element-plus/icons-vue'

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
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}
.logo-icon { font-size: 24px; }
.logo-text { color: #409eff; }
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
  font-size: 14px;
}
.user-info:hover { color: #409eff; }
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
