<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <h2><el-icon><key /></el-icon> 登录</h2>

      <el-tabs v-model="loginMode">
        <!-- 密码登录 -->
        <el-tab-pane label="密码登录" name="password">
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="0" size="large">
            <el-form-item prop="email">
              <el-input v-model="pwdForm.email" placeholder="邮箱地址" prefix-icon="Message" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="pwdForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password @keyup.enter="handlePwdLogin" />
            </el-form-item>
            <!-- 连续失败后才显示图片验证码 -->
            <el-form-item v-if="needPwdCaptcha" prop="captchaCode">
              <div class="captcha-row">
                <el-input v-model="pwdForm.captchaCode" placeholder="图片验证码" prefix-icon="Picture" @keyup.enter="handlePwdLogin" />
                <div class="captcha-img" @click="refreshPwdCaptcha" title="点击刷新">
                  <img v-if="pwdCaptchaImage" :src="pwdCaptchaImage" alt="验证码" />
                </div>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" style="width: 100%" @click="handlePwdLogin">
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 验证码登录 -->
        <el-tab-pane label="验证码登录" name="code">
          <el-form ref="codeFormRef" :model="codeForm" :rules="codeRules" label-width="0" size="large">
            <el-form-item prop="email">
              <el-input v-model="codeForm.email" placeholder="邮箱地址" prefix-icon="Message" />
            </el-form-item>
            <el-form-item prop="emailCode">
              <div class="email-code-row">
                <el-input v-model="codeForm.emailCode" placeholder="邮箱验证码" prefix-icon="message" @keyup.enter="handleCodeLogin" />
                <el-button type="primary" :disabled="emailCodeCooldown > 0" @click="handleSendLoginCode" :loading="sendingEmailCode">
                  {{ emailCodeCooldown > 0 ? `${emailCodeCooldown}s` : '发送验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" style="width: 100%" @click="handleCodeLogin">
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="login-footer">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </el-card>

    <!-- 图片验证码弹窗（发送验证码前拦截） -->
    <el-dialog v-model="showCaptchaDialog" title="验证" width="360" :close-on-click-modal="false">
      <div class="captcha-dialog">
        <p style="margin-bottom: 12px; color: #606266;">请输入图片验证码</p>
        <div class="captcha-row">
          <el-input v-model="captchaInput" placeholder="验证码" size="large" @keyup.enter="confirmCaptcha" />
          <div class="captcha-img" @click="refreshDialogCaptcha" title="点击刷新">
            <img v-if="dialogCaptchaImage" :src="dialogCaptchaImage" alt="验证码" />
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
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { login, loginByCode, getCaptcha, sendEmailCode } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { Key } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const loginMode = ref('password')

// === 密码登录 ===
const pwdFormRef = ref<FormInstance>()
const pwdCaptchaId = ref('')
const pwdCaptchaImage = ref('')
const needPwdCaptcha = ref(false)
const pwdFailCount = ref(0)

const pwdForm = reactive({ email: '', password: '', captchaCode: '' })
const pwdRules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function refreshPwdCaptcha() {
  const { data } = await getCaptcha()
  if (data.code === 200) {
    pwdCaptchaId.value = data.data.captchaId
    pwdCaptchaImage.value = data.data.image
    pwdForm.captchaCode = ''
  }
}

async function handlePwdLogin() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const captchaId = needPwdCaptcha.value ? pwdCaptchaId.value : ''
    const captchaCode = needPwdCaptcha.value ? pwdForm.captchaCode : ''
    const { data } = await login(pwdForm.email, pwdForm.password, captchaId, captchaCode)
    if (data.code === 200) {
      authStore.setAuth(data.data.token, data.data.user)
      ElMessage.success('登录成功')
      pwdFailCount.value = 0
      needPwdCaptcha.value = false
      router.push((route.query.redirect as string) || '/')
    } else {
      handlePwdFail(data.message)
    }
  } catch (err: any) {
    handlePwdFail(err.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

function handlePwdFail(msg: string) {
  ElMessage.error(msg)
  pwdFailCount.value++
  if (pwdFailCount.value >= 3) {
    needPwdCaptcha.value = true
    refreshPwdCaptcha()
    ElMessage.warning('失败次数过多，请输入验证码')
  }
}

// === 验证码登录 ===
const codeFormRef = ref<FormInstance>()
const emailCodeCooldown = ref(0)
const sendingEmailCode = ref(false)
const showCaptchaDialog = ref(false)
const captchaInput = ref('')
const dialogCaptchaId = ref('')
const dialogCaptchaImage = ref('')

const codeForm = reactive({ email: '', emailCode: '' })
const codeRules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  emailCode: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
}

async function refreshDialogCaptcha() {
  const { data } = await getCaptcha()
  if (data.code === 200) {
    dialogCaptchaId.value = data.data.captchaId
    dialogCaptchaImage.value = data.data.image
    captchaInput.value = ''
  }
}

function handleSendLoginCode() {
  if (!codeForm.email) { ElMessage.warning('请先输入邮箱地址'); return }
  refreshDialogCaptcha()
  showCaptchaDialog.value = true
}

async function confirmCaptcha() {
  if (!captchaInput.value.trim()) { ElMessage.warning('请输入验证码'); return }
  sendingEmailCode.value = true
  try {
    const { data } = await sendEmailCode(codeForm.email, dialogCaptchaId.value, captchaInput.value)
    if (data.code === 200) {
      ElMessage.success('验证码已发送到邮箱')
      showCaptchaDialog.value = false
      emailCodeCooldown.value = 60
      const timer = setInterval(() => { emailCodeCooldown.value--; if (emailCodeCooldown.value <= 0) clearInterval(timer) }, 1000)
    } else {
      ElMessage.error(data.message || '发送失败')
      refreshDialogCaptcha()
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '发送失败')
    refreshDialogCaptcha()
  } finally {
    sendingEmailCode.value = false
  }
}

async function handleCodeLogin() {
  const valid = await codeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const { data } = await loginByCode(codeForm.email, codeForm.emailCode)
    if (data.code === 200) {
      authStore.setAuth(data.data.token, data.data.user)
      ElMessage.success('登录成功')
      router.push((route.query.redirect as string) || '/')
    } else {
      ElMessage.error(data.message)
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => { /* 密码登录默认不加载验证码 */ })
</script>

<style scoped>
.login-page { display: flex; justify-content: center; align-items: center; min-height: 70vh; }
.login-card { width: 400px; padding: 20px; }
.login-card h2 { text-align: center; margin-bottom: 20px; color: #303133; display: flex; align-items: center; justify-content: center; gap: 8px; }
.login-footer { text-align: center; color: #909399; font-size: 14px; margin-top: 10px; }
.login-footer a { color: #409eff; }
.captcha-row { display: flex; gap: 12px; width: 100%; }
.captcha-row .el-input { flex: 1; }
.captcha-img { width: 150px; height: 40px; cursor: pointer; border-radius: 4px; overflow: hidden; border: 1px solid #dcdfe6; flex-shrink: 0; }
.captcha-img img { width: 100%; height: 100%; object-fit: cover; }
.email-code-row { display: flex; gap: 12px; width: 100%; }
.email-code-row .el-input { flex: 1; }
.email-code-row .el-button { width: 120px; flex-shrink: 0; }
.captcha-dialog { text-align: center; }
</style>
