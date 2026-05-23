/**
 * 格式化文件大小
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + units[i]
}

/**
 * 格式化时长（秒 -> HH:MM:SS）
 */
export function formatDuration(seconds: number | null): string {
  if (seconds == null) return '--'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  return `${m}:${String(s).padStart(2, '0')}`
}

/**
 * 格式化秒数为时间码 HH:MM:SS
 */
export function formatTimeCode(seconds: number): string {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

/**
 * 格式化日期
 */
export function formatDate(dateStr: string | null): string {
  if (!dateStr) return '--'
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

/**
 * 平台名称映射
 */
export const platformMap: Record<string, string> = {
  douyin: '抖音',
  xiaohongshu: '小红书',
  weixin_video: '微信视频号',
  bilibili: 'B站',
  kuaishou: '快手',
  other: '其他',
}

/**
 * 内容类型映射
 */
export const contentTypeMap: Record<string, string> = {
  live: '直播回放',
  video: '长视频',
  podcast: '播客',
  course: '课程',
  interview: '访谈',
  speech: '演讲',
  other: '其他',
}
