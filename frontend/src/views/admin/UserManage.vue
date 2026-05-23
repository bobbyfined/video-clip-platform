<template>
  <div>
    <h3 style="margin-bottom: 20px">用户管理</h3>
    <el-table :data="users" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="email" label="邮箱" min-width="200" />
      <el-table-column prop="nickname" label="昵称" width="150" />
      <el-table-column label="角色" width="120">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
            {{ row.role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="套餐" width="100">
        <template #default="{ row }">
          <el-tag :type="row.plan === 'PRO' ? 'success' : ''" size="small">
            {{ row.plan }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="170">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-select v-model="row.role" size="small" style="width: 80px; margin-right: 8px" @change="updateUserRole(row)">
            <el-option label="USER" value="USER" />
            <el-option label="ADMIN" value="ADMIN" />
          </el-select>
          <el-select v-model="row.plan" size="small" style="width: 80px" @change="updateUserPlan(row)">
            <el-option label="FREE" value="FREE" />
            <el-option label="PRO" value="PRO" />
          </el-select>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="margin-top: 20px; justify-content: center"
      v-model:current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="loadUsers"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUsers, updateUser } from '@/api/admin'
import { formatDate } from '@/utils/format'
import { ElMessage } from 'element-plus'
import type { User } from '@/types'

const users = ref<User[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const total = ref(0)

async function loadUsers() {
  loading.value = true
  try {
    const { data } = await getUsers(page.value, pageSize)
    if (data.code === 200) {
      users.value = data.data.list
      total.value = data.data.total
    }
  } finally {
    loading.value = false
  }
}

async function updateUserRole(row: User) {
  try {
    await updateUser(row.id, { role: row.role })
    ElMessage.success('更新成功')
  } catch {
    ElMessage.error('更新失败')
    loadUsers()
  }
}

async function updateUserPlan(row: User) {
  try {
    await updateUser(row.id, { plan: row.plan })
    ElMessage.success('更新成功')
  } catch {
    ElMessage.error('更新失败')
    loadUsers()
  }
}

onMounted(loadUsers)
</script>
