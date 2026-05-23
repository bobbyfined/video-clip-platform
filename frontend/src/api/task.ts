import api from './index'
import type { ApiResult, PageResult, MediaTask, TaskDetail } from '@/types'

/** 创建任务（上传文件） */
export function createTask(file: File, contentType: string, targetPlatform: string, clipCount: number, llmProvider?: string, onProgress?: (e: any) => void) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('contentType', contentType)
  formData.append('targetPlatform', targetPlatform)
  formData.append('clipCount', String(clipCount))
  if (llmProvider) formData.append('llmProvider', llmProvider)

  return api.post<ApiResult<MediaTask>>('/tasks', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress,
  })
}

/** 获取任务列表 */
export function getTasks(page = 1, size = 10, status?: string) {
  return api.get<ApiResult<PageResult<MediaTask>>>('/tasks', {
    params: { page, size, status },
  })
}

/** 获取任务详情 */
export function getTaskDetail(id: number) {
  return api.get<ApiResult<TaskDetail>>(`/tasks/${id}`)
}

/** 导出 SRT */
export function exportSrt(id: number) {
  return api.get(`/tasks/${id}/export/srt`, { responseType: 'blob' })
}

/** 导出 TXT */
export function exportTxt(id: number) {
  return api.get(`/tasks/${id}/export/txt`, { responseType: 'blob' })
}

/** 导出切片建议 */
export function exportClips(id: number) {
  return api.get(`/tasks/${id}/export/clips`, { responseType: 'blob' })
}

/** 渲染单个切片 */
export function renderClip(taskId: number, clipId: number) {
  return api.post(`/tasks/${taskId}/clips/${clipId}/render`)
}

/** 批量渲染所有切片 */
export function renderAllClips(taskId: number) {
  return api.post(`/tasks/${taskId}/clips/render-all`)
}

/** 获取任务原始视频URL（用于播放器） */
export function getVideoUrl(taskId: number) {
  return `/api/tasks/${taskId}/video`
}

/** 获取可用 LLM 提供商列表 */
export function getLlmProviders() {
  return api.get('/llm/providers')
}

/** 获取支持的视频平台列表 */
export function getSupportedPlatforms() {
  return api.get('/download/platforms')
}

/** 通过链接下载视频并创建任务 */
export function downloadFromUrl(url: string, contentType: string, targetPlatform: string, clipCount: number, llmProvider?: string) {
  const formData = new URLSearchParams()
  formData.append('url', url)
  formData.append('contentType', contentType)
  formData.append('targetPlatform', targetPlatform)
  formData.append('clipCount', String(clipCount))
  if (llmProvider) formData.append('llmProvider', llmProvider)
  return api.post('/download', formData, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  })
}
