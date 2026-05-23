import api from './index'
import type { ApiResult, AuthResponse, User } from '@/types'

/** 获取图片验证码 */
export function getCaptcha() {
  return api.get<ApiResult<{ captchaId: string; image: string }>>('/captcha')
}

/** 发送邮箱验证码（带图片验证码拦截） */
export function sendEmailCode(email: string, captchaId: string, captchaCode: string) {
  return api.post<ApiResult<string>>('/auth/send-code', { email, captchaId, captchaCode })
}

/** 用户注册（邮箱+邮箱验证码+密码，不需要图片验证码） */
export function register(email: string, emailCode: string, password: string) {
  return api.post<ApiResult<User>>('/auth/register', { email, emailCode, password })
}

/** 密码登录（邮箱+密码+图片验证码） */
export function login(email: string, password: string, captchaId: string, captchaCode: string) {
  return api.post<ApiResult<AuthResponse>>('/auth/login', { email, password, captchaId, captchaCode })
}

/** 验证码登录（邮箱+邮箱验证码，不需要图片验证码） */
export function loginByCode(email: string, emailCode: string) {
  return api.post<ApiResult<AuthResponse>>('/auth/login-code', { email, emailCode })
}

/** 获取当前用户信息 */
export function getMe() {
  return api.get<ApiResult<User>>('/auth/me')
}
