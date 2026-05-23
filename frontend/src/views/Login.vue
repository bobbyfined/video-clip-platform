<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <h2><el-icon><key /></el-icon> 登录</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱地址" prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="验证码" prefix-icon="Picture" />
            <div class="captcha-img" @click="refreshCaptcha" title="点击刷新">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
              <span v-else class="captcha-loading">加载中...</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleLogin">
            <el-icon v-if="!loading"><position /></el-icon>
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { login, getCaptcha } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { Key, Position, Picture } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const captchaId = ref('')
const captchaImage = ref('')

const form = reactive({
  email: '',
  password: '',
  captchaCode: '',
})

const rules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

async function refreshCaptcha() {
  try {
    const { data } = await getCaptcha()
    if (data.code === 200) {
      captchaId.value = data.data.captchaId
      captchaImage.value = data.data.image
    }
  } catch {
    // ignore
  }
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const { data } = await login(form.email, form.password, captchaId.value, form.captchaCode)
    if (data.code === 200) {
      authStore.setAuth(data.data.token, data.data.user)
      ElMessage.success('登录成功')
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    } else {
      ElMessage.error(data.message || '登录失败')
      refreshCaptcha()
    }
  } catch (err: any) {
    const msg = err.response?.data?.message || '登录失败'
    ElMessage.error(msg)
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
}
.login-card {
  width: 420px;
  padding: 20px;
}
.login-card h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.login-footer {
  text-align: center;
  color: #909399;
  font-size: 14px;
}
.login-footer a {
  color: #409eff;
}
.captcha-row {
  display: flex;
  gap: 12px;
  width: 100%;
}
.captcha-row .el-input {
  flex: 1;
}
.captcha-img {
  width: 150px;
  height: 40px;
  cursor: pointer;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  flex-shrink: 0;
}
.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.captcha-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #909399;
  font-size: 12px;
  background: #f5f7fa;
}
</style>
