<template>
  <div class="upload-page page-container">
    <h2>上传视频/音频</h2>
    <el-card shadow="never" class="upload-card">
      <!-- 上传区域 -->
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :on-exceed="handleExceed"
        :before-upload="beforeUpload"
        accept=".mp4,.mov,.avi,.mkv,.webm,.mp3,.wav,.m4a,.flac,.aac"
      >
        <el-icon class="upload-icon"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 mp4, mov, mp3, wav, m4a, webm 等格式，最大 500MB
          </div>
        </template>
      </el-upload>

      <!-- 参数设置 -->
      <el-divider>参数设置</el-divider>
      <el-form :model="params" label-width="100px">
        <el-form-item label="内容类型">
          <el-select v-model="params.contentType" style="width: 100%">
            <el-option label="直播回放" value="live" />
            <el-option label="长视频" value="video" />
            <el-option label="播客" value="podcast" />
            <el-option label="课程" value="course" />
            <el-option label="访谈" value="interview" />
            <el-option label="演讲" value="speech" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标平台">
          <el-select v-model="params.targetPlatform" style="width: 100%">
            <el-option label="抖音" value="douyin" />
            <el-option label="小红书" value="xiaohongshu" />
            <el-option label="微信视频号" value="weixin_video" />
            <el-option label="B站" value="bilibili" />
            <el-option label="快手" value="kuaishou" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="切片数量">
          <el-input-number v-model="params.clipCount" :min="1" :max="20" />
        </el-form-item>
      </el-form>

      <!-- 上传按钮 & 进度 -->
      <div class="upload-actions">
        <el-button type="primary" size="large" :loading="uploading" :disabled="!selectedFile" @click="handleUpload">
          {{ uploading ? '上传中...' : '开始分析' }}
        </el-button>
      </div>
      <el-progress v-if="uploading" :percentage="progress" :stroke-width="10" style="margin-top: 16px" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { createTask } from '@/api/task'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import type { UploadFile } from 'element-plus'

const router = useRouter()
const uploadRef = ref()
const selectedFile = ref<File | null>(null)
const uploading = ref(false)
const progress = ref(0)

const params = reactive({
  contentType: 'live',
  targetPlatform: 'douyin',
  clipCount: 5,
})

function handleFileChange(file: UploadFile) {
  selectedFile.value = file.raw || null
}

function handleExceed() {
  ElMessage.warning('只能上传一个文件')
}

function beforeUpload(file: File) {
  const maxSize = 500 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 500MB')
    return false
  }
  return true
}

async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }

  uploading.value = true
  progress.value = 0

  try {
    const { data } = await createTask(
      selectedFile.value,
      params.contentType,
      params.targetPlatform,
      params.clipCount,
      (e) => {
        if (e.total) progress.value = Math.round((e.loaded / e.total) * 100)
      }
    )
    if (data.code === 200) {
      ElMessage.success('任务创建成功')
      router.push(`/tasks/${data.data.id}`)
    } else {
      ElMessage.error(data.message || '上传失败')
    }
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.upload-page h2 {
  margin-bottom: 20px;
}
.upload-card {
  max-width: 700px;
}
.upload-icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 10px;
}
.upload-actions {
  text-align: center;
  margin-top: 20px;
}
</style>
