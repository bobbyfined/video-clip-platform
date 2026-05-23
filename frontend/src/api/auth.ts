import api from './index'
import type { ApiResult, AuthResponse, User } from '@/types'

/** 获取图片验证码 */
export function getCaptcha() {
  return api.get<ApiResult<{ captchaId: string; image: string }>>('/captcha')
}

/** 发送邮箱验证码 */
export function sendEmailCode(email: string) {
  return api.post<ApiResult<string>>('/auth/send-code', null, { params: { email } })
}

/** 用户注册 */
export function register(email: string, nickname: string, password: string, captchaId: string, captchaCode: string, emailCode: string) {
  return api.post<ApiResult<User>>('/auth/register', { email, nickname, password, captchaId, captchaCode, emailCode })
}

/** 用户登录 */
export function login(email: string, password: string, captchaId: string, captchaCode: string) {
  return api.post<ApiResult<AuthResponse>>('/auth/login', { email, password, captchaId, captchaCode })
}

/** 获取当前用户信息 */
export function getMe() {
  return api.get<ApiResult<User>>('/auth/me')
}
