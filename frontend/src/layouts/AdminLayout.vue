<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="sidebar">
      <div class="sidebar-title">🎬 管理后台</div>
      <el-menu
        :default-active="route.path"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/admin">
          <el-icon><data-analysis /></el-icon>
          <span>数据概览</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><user /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/tasks">
          <el-icon><document /></el-icon>
          <span>任务管理</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <router-link to="/">
          <el-button size="small" text>← 返回前台</el-button>
        </router-link>
      </div>
    </el-aside>

    <!-- 主内容 -->
    <el-container>
      <el-header class="admin-header">
        <span>欢迎, {{ authStore.user?.nickname || authStore.user?.email }}</span>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { DataAnalysis, User, Document } from '@element-plus/icons-vue'

const route = useRoute()
const authStore = useAuthStore()
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}
.sidebar {
  background: #304156;
  color: #bfcbd9;
  display: flex;
  flex-direction: column;
}
.sidebar-title {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.sidebar-menu {
  flex: 1;
  border-right: none;
  background: #304156;
}
.sidebar-footer {
  padding: 15px;
  text-align: center;
}
.admin-header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
  color: #606266;
}
.admin-main {
  background: #f5f7fa;
  padding: 20px;
}
</style>
