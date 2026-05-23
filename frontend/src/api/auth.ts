import api from './index'
import type { ApiResult, AuthResponse, User } from '@/types'

/** 获取验证码 */
export function getCaptcha() {
  return api.get<ApiResult<{ captchaId: string; image: string }>>('/captcha')
}

/** 用户注册 */
export function register(email: string, nickname: string, password: string, captchaId: string, captchaCode: string) {
  return api.post<ApiResult<User>>('/auth/register', { email, nickname, password, captchaId, captchaCode })
}

/** 用户登录 */
export function login(email: string, password: string, captchaId: string, captchaCode: string) {
  return api.post<ApiResult<AuthResponse>>('/auth/login', { email, password, captchaId, captchaCode })
}

/** 获取当前用户信息 */
export function getMe() {
  return api.get<ApiResult<User>>('/auth/me')
}

/** 邮箱验证 */
export function verifyEmail(token: string) {
  return api.get<ApiResult<string>>('/auth/verify', { params: { token } })
}
