<template>
  <div class="register-page">
    <el-card class="register-card" shadow="hover">
      <h2><el-icon><edit-pen /></el-icon> 注册</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱地址" prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="emailCode">
          <div class="email-code-row">
            <el-input v-model="form.emailCode" placeholder="邮箱验证码" prefix-icon="message" />
            <el-button type="primary" :disabled="emailCodeCooldown > 0" @click="handleSendEmailCode" :loading="sendingEmailCode">
              {{ emailCodeCooldown > 0 ? `${emailCodeCooldown}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="password">
          <div class="password-row">
            <el-input v-model="form.password" type="password" placeholder="密码（8位以上，含大小写和数字）" prefix-icon="Lock" show-password />
            <el-button @click="generatePassword" title="随机生成密码">🎲 随机密码</el-button>
          </div>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <div class="password-row">
            <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" show-password />
            <el-button @click="copyPassword" :disabled="!form.password" title="复制上面的密码">📋 复制</el-button>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleRegister">
            {{ loading ? '注册中...' : '注 册' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="register-footer">
        已有账号？<router-link to="/login">立即登录</router-link>
      </div>
    </el-card>

    <!-- 图片验证码弹窗（仅发送邮箱验证码时拦截） -->
    <el-dialog v-model="showCaptchaDialog" title="验证" width="360" :close-on-click-modal="false">
      <div class="captcha-dialog">
        <p style="margin-bottom: 12px; color: #606266;">请输入图片验证码以继续</p>
        <div class="captcha-row">
          <el-input v-model="captchaInput" placeholder="验证码" size="large" @keyup.enter="confirmCaptcha" />
          <div class="captcha-img" @click="refreshCaptcha" title="点击刷新">
            <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showCaptchaDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmCaptcha" :loading="sendingEmailCode">确定发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { register, getCaptcha, sendEmailCode } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const captchaId = ref('')
const captchaImage = ref('')
const captchaInput = ref('')
const showCaptchaDialog = ref(false)
const emailCodeCooldown = ref(0)
const sendingEmailCode = ref(false)

const form = reactive({
  email: '',
  emailCode: '',
  password: '',
  confirmPassword: '',
})

const rules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  emailCode: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: (_r: any, v: string, cb: Function) => v === form.password ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' },
  ],
}

async function refreshCaptcha() {
  try {
    const { data } = await getCaptcha()
    if (data.code === 200) {
      captchaId.value = data.data.captchaId
      captchaImage.value = data.data.image
      captchaInput.value = ''
    }
  } catch { /* ignore */ }
}

function handleSendEmailCode() {
  if (!form.email) {
    ElMessage.warning('请先输入邮箱地址')
    return
  }
  refreshCaptcha()
  showCaptchaDialog.value = true
}

async function confirmCaptcha() {
  if (!captchaInput.value.trim()) {
    ElMessage.warning('请输入验证码')
    return
  }
  sendingEmailCode.value = true
  try {
    const { data } = await sendEmailCode(form.email, captchaId.value, captchaInput.value)
    if (data.code === 200) {
      ElMessage.success('验证码已发送到邮箱')
      showCaptchaDialog.value = false
      emailCodeCooldown.value = 60
      const timer = setInterval(() => {
        emailCodeCooldown.value--
        if (emailCodeCooldown.value <= 0) clearInterval(timer)
      }, 1000)
    } else {
      ElMessage.error(data.message || '发送失败')
      refreshCaptcha()
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '发送失败')
    refreshCaptcha()
  } finally {
    sendingEmailCode.value = false
  }
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const { data } = await register(form.email, form.emailCode, form.password)
    if (data.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(data.message || '注册失败')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '注册失败')
  } finally {
    loading.value = false
  }
}

function generatePassword() {
  const upper = 'ABCDEFGHJKLMNPQRSTUVWXYZ'
  const lower = 'abcdefghjkmnpqrstuvwxyz'
  const digits = '23456789'
  const all = upper + lower + digits
  let pwd = upper[Math.floor(Math.random() * upper.length)]
      + lower[Math.floor(Math.random() * lower.length)]
      + digits[Math.floor(Math.random() * digits.length)]
  for (let i = 3; i < 12; i++) pwd += all[Math.floor(Math.random() * all.length)]
  form.password = pwd.split('').sort(() => Math.random() - 0.5).join('')
  form.confirmPassword = form.password
  ElMessage.success('已生成随机密码')
}

async function copyPassword() {
  if (!form.password) return
  try {
    await navigator.clipboard.writeText(form.password)
    ElMessage.success('密码已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}
</script>

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
}
.register-card { width: 400px; padding: 20px; }
.register-card h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.register-footer { text-align: center; color: #909399; font-size: 14px; }
.register-footer a { color: #409eff; }
.email-code-row { display: flex; gap: 12px; width: 100%; }
.email-code-row .el-input { flex: 1; }
.email-code-row .el-button { width: 120px; flex-shrink: 0; }
.captcha-dialog { text-align: center; }
.captcha-row { display: flex; gap: 12px; justify-content: center; }
.captcha-row .el-input { width: 150px; }
.captcha-img { width: 150px; height: 40px; cursor: pointer; border-radius: 4px; overflow: hidden; border: 1px solid #dcdfe6; }
.captcha-img img { width: 100%; height: 100%; object-fit: cover; }
.password-row { display: flex; gap: 10px; width: 100%; }
.password-row .el-input { flex: 1; }
.password-row .el-button { width: 120px; flex-shrink: 0; }
</style>
