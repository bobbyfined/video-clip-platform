import api from './index'
import type { ApiResult, PageResult, User, MediaTask, AdminStats } from '@/types'

/** 获取统计数据 */
export function getStats() {
  return api.get<ApiResult<AdminStats>>('/admin/stats')
}

/** 用户列表 */
export function getUsers(page = 1, size = 10) {
  return api.get<ApiResult<PageResult<User>>>('/admin/users', {
    params: { page, size },
  })
}

/** 更新用户 */
export function updateUser(id: number, data: { role?: string; plan?: string }) {
  return api.put<ApiResult<User>>(`/admin/users/${id}`, data)
}

/** 所有任务列表 */
export function getAllTasks(page = 1, size = 10, status?: string) {
  return api.get<ApiResult<PageResult<MediaTask>>>('/admin/tasks', {
    params: { page, size, status },
  })
}

/** 重试任务 */
export function retryTask(id: number) {
  return api.put<ApiResult<void>>(`/admin/tasks/${id}/retry`)
}
