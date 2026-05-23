// 用户
export interface User {
  id: number
  email: string
  nickname: string | null
  role: 'USER' | 'ADMIN'
  plan: string
  createdAt: string
}

// 认证响应
export interface AuthResponse {
  token: string
  user: User
}

// 任务状态
export type TaskStatus = 'PENDING' | 'DOWNLOADED' | 'EXTRACTING_AUDIO' | 'TRANSCRIBING' | 'ANALYZING' | 'COMPLETED' | 'FAILED'

// 任务
export interface MediaTask {
  id: number
  title: string | null
  originalFilename: string
  fileSize: number
  mimeType: string
  durationSeconds: number | null
  contentType: string | null
  targetPlatform: string | null
  clipCount: number
  llmProvider: string | null
  status: TaskStatus
  progressStage: string | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
  completedAt: string | null
}

// 转写片段
export interface TranscriptSegment {
  id: number
  startSeconds: number
  endSeconds: number
  text: string
  sortOrder: number
}

// 金句
export interface GoldenQuote {
  time: string
  text: string
}

// 分析结果
export interface AnalysisResult {
  summaryShort: string | null
  summaryLong: string | null
  keyPoints: string[]
  goldenQuotes: GoldenQuote[]
}

// 切片建议
export interface ClipSuggestion {
  id: number
  startSeconds: number
  endSeconds: number
  topic: string
  title1: string
  title2: string | null
  summary: string | null
  hookReason: string | null
  suggestedPlatform: string | null
  editingNotes: string | null
  score: number | null
  sortOrder: number
  outputPath: string | null
  clipStatus: 'PENDING' | 'RENDERING' | 'DONE' | 'FAILED'
}

// 任务详情
export interface TaskDetail extends MediaTask {
  segments: TranscriptSegment[]
  analysis: AnalysisResult | null
  clips: ClipSuggestion[]
}

// 分页
export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}

// 统一响应
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

// 管理统计
export interface AdminStats {
  totalUsers: number
  totalTasks: number
  pendingTasks: number
  processingTasks: number
  completedTasks: number
  failedTasks: number
}
