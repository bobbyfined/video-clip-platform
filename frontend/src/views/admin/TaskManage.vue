<template>
  <div>
    <h3 style="margin-bottom: 20px">任务管理</h3>

    <el-select v-model="statusFilter" placeholder="按状态筛选" clearable style="margin-bottom: 16px; width: 200px" @change="loadTasks">
      <el-option label="等待中" value="PENDING" />
      <el-option label="已完成" value="COMPLETED" />
      <el-option label="失败" value="FAILED" />
    </el-select>

    <el-table :data="tasks" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="originalFilename" label="文件名" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <TaskStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="文件大小" width="100">
        <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <router-link :to="`/tasks/${row.id}`">
            <el-button type="primary" link size="small">查看</el-button>
          </router-link>
          <el-button
            v-if="row.status === 'FAILED'"
            type="warning"
            link
            size="small"
            @click="handleRetry(row.id)"
          >
            重试
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 20px; justify-content: center"
      v-model:current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="loadTasks"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAllTasks, retryTask } from '@/api/admin'
import { formatFileSize, formatDate } from '@/utils/format'
import TaskStatusTag from '@/components/TaskStatusTag.vue'
import { ElMessage } from 'element-plus'
import type { MediaTask } from '@/types'

const tasks = ref<MediaTask[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)
const statusFilter = ref('')

async function loadTasks() {
  loading.value = true
  try {
    const { data } = await getAllTasks(page.value, pageSize, statusFilter.value || undefined)
    if (data.code === 200) {
      tasks.value = data.data.list
      total.value = data.data.total
    }
  } finally {
    loading.value = false
  }
}

async function handleRetry(id: number) {
  try {
    await retryTask(id)
    ElMessage.success('任务已重新加入队列')
    loadTasks()
  } catch {
    ElMessage.error('重试失败')
  }
}

onMounted(loadTasks)
</script>
