<template>
  <div class="task-list page-container">
    <div class="page-header">
      <h2>我的任务</h2>
      <router-link to="/upload">
        <el-button type="primary">新建任务</el-button>
      </router-link>
    </div>

    <!-- 状态筛选 -->
    <el-select v-model="statusFilter" placeholder="按状态筛选" clearable style="margin-bottom: 16px; width: 200px" @change="loadTasks">
      <el-option label="等待中" value="PENDING" />
      <el-option label="处理中" value="EXTRACTING_AUDIO" />
      <el-option label="转写中" value="TRANSCRIBING" />
      <el-option label="分析中" value="ANALYZING" />
      <el-option label="已完成" value="COMPLETED" />
      <el-option label="失败" value="FAILED" />
    </el-select>

    <!-- 任务列表 -->
    <el-table :data="tasks" v-loading="loading" stripe>
      <el-table-column prop="originalFilename" label="文件名" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <TaskStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="内容类型" width="100">
        <template #default="{ row }">
          {{ row.contentType ? contentTypeMap[row.contentType] || row.contentType : '--' }}
        </template>
      </el-table-column>
      <el-table-column label="目标平台" width="100">
        <template #default="{ row }">
          {{ row.targetPlatform ? platformMap[row.targetPlatform] || row.targetPlatform : '--' }}
        </template>
      </el-table-column>
      <el-table-column label="文件大小" width="100">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <router-link :to="`/tasks/${row.id}`">
            <el-button type="primary" link size="small">查看详情</el-button>
          </router-link>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
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
import { getTasks } from '@/api/task'
import { formatFileSize, formatDate, platformMap, contentTypeMap } from '@/utils/format'
import TaskStatusTag from '@/components/TaskStatusTag.vue'
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
    const { data } = await getTasks(page.value, pageSize, statusFilter.value || undefined)
    if (data.code === 200) {
      tasks.value = data.data.list
      total.value = data.data.total
    }
  } finally {
    loading.value = false
  }
}

onMounted(loadTasks)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>
