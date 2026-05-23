import api from './index'
import type { ApiResult, AuthResponse, User } from '@/types'

/** 用户注册 */
export function register(email: string, nickname: string, password: string) {
  return api.post<ApiResult<User>>('/auth/register', { email, nickname, password })
}

/** 用户登录 */
export function login(email: string, password: string) {
  return api.post<ApiResult<AuthResponse>>('/auth/login', { email, password })
}

/** 获取当前用户信息 */
export function getMe() {
  return api.get<ApiResult<User>>('/auth/me')
}
