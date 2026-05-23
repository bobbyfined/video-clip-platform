<template>
  <div class="register-page">
    <el-card class="register-card" shadow="hover">
      <h2><el-icon><edit-pen /></el-icon> 注册</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱地址" prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（选填）" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（8位以上，含大小写和数字）" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" show-password />
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
          <el-button type="primary" :loading="loading" style="width: 100%" @click="handleRegister">
            <el-icon v-if="!loading"><circle-check-filled /></el-icon>
            {{ loading ? '注册中...' : '注 册' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div class="register-footer">
        已有账号？<router-link to="/login">立即登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { register, getCaptcha } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { EditPen, CircleCheckFilled, Picture } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const captchaId = ref('')
const captchaImage = ref('')

const form = reactive({
  email: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  captchaCode: '',
})

const rules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, message: '密码至少8位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== form.password) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
}

async function refreshCaptcha() {
  try {
    const { data } = await getCaptcha()
    if (data.code === 200) {
      captchaId.value = data.data.captchaId
      captchaImage.value = data.data.image
    }
  } catch { /* ignore */ }
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const { data } = await register(form.email, form.nickname, form.password, captchaId.value, form.captchaCode)
    if (data.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(data.message || '注册失败')
      refreshCaptcha()
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '注册失败')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
}
.register-card {
  width: 420px;
  padding: 20px;
}
.register-card h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #303133;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.register-footer {
  text-align: center;
  color: #909399;
  font-size: 14px;
}
.register-footer a {
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
