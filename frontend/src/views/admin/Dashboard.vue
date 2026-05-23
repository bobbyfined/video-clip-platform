<template>
  <div>
    <h3 style="margin-bottom: 20px">数据概览</h3>
    <el-row :gutter="20">
      <el-col :span="6">
        <StatsCard title="总用户数" :value="stats.totalUsers" icon="User" color="#409eff" />
      </el-col>
      <el-col :span="6">
        <StatsCard title="总任务数" :value="stats.totalTasks" icon="Document" color="#67c23a" />
      </el-col>
      <el-col :span="6">
        <StatsCard title="完成任务" :value="stats.completedTasks" icon="CircleCheck" color="#e6a23c" />
      </el-col>
      <el-col :span="6">
        <StatsCard title="失败任务" :value="stats.failedTasks" icon="CircleClose" color="#f56c6c" />
      </el-col>
    </el-row>
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="never">
          <h4>任务状态分布</h4>
          <el-descriptions :column="1" border style="margin-top: 12px">
            <el-descriptions-item label="等待中">{{ stats.pendingTasks }}</el-descriptions-item>
            <el-descriptions-item label="处理中">{{ stats.processingTasks }}</el-descriptions-item>
            <el-descriptions-item label="已完成">{{ stats.completedTasks }}</el-descriptions-item>
            <el-descriptions-item label="失败">{{ stats.failedTasks }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getStats } from '@/api/admin'
import StatsCard from '@/components/StatsCard.vue'
import type { AdminStats } from '@/types'

const stats = ref<AdminStats>({
  totalUsers: 0, totalTasks: 0, pendingTasks: 0,
  processingTasks: 0, completedTasks: 0, failedTasks: 0,
})

onMounted(async () => {
  const { data } = await getStats()
  if (data.code === 200) stats.value = data.data
})
</script>
