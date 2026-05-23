<template>
  <div class="profile page-container">
    <h2>个人信息</h2>
    <el-card shadow="never" style="max-width: 500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="邮箱">{{ authStore.user?.email }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ authStore.user?.nickname || '--' }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="authStore.isAdmin ? 'danger' : 'info'">
            {{ authStore.isAdmin ? '管理员' : '普通用户' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="套餐">{{ authStore.user?.plan || 'FREE' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDate(authStore.user?.createdAt || '') }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { formatDate } from '@/utils/format'

const authStore = useAuthStore()
</script>
