# 🚀 部署启动手册

## 一、环境要求

| 依赖 | 最低版本 | 说明 |
|---|---|---|
| Docker | 20.10+ | 推荐用 Docker Compose 一键部署 |
| Docker Compose | 2.0+ | 随 Docker Desktop 自带 |
| 内存 | 4GB+ | 后端 + MySQL + scraper 至少需要 4GB |
| 磁盘 | 20GB+ | 视频文件较大，建议预留空间 |

> 💡 如果不用 Docker，也可以手动部署（见第四节）

---

## 二、Docker 一键部署（推荐）

### 1. 克隆代码

```bash
git clone https://github.com/bobbyfined/video-clip-platform.git
cd video-clip-platform
```

### 2. 配置环境变量

创建 `.env` 文件：

```bash
# 必填：mimo API Key（用于 AI 分析 + 语音转写）
LLM_MIMO_API_KEY=你的mimo-api-key

# 可选：DeepSeek API Key（不用可以不填）
LLM_API_KEY=你的deepseek-api-key
```

> ⚠️ `.env` 文件已在 `.gitignore` 中，不会提交到 GitHub

### 3. 配置抖音 Cookie（可选，不配则抖音链接无法解析）

```bash
# 创建配置目录
mkdir -p scraper-config

# 创建配置文件
cat > scraper-config/douyin_config.yaml << 'EOF'
TokenManager:
  douyin:
    headers:
      Accept-Language: zh-CN,zh;q=0.8
      User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36
      Referer: https://www.douyin.com/
      # ⚠️ 粘贴你的抖音 Cookie
      Cookie: "在这里粘贴你的抖音Cookie"
EOF
```

**获取抖音 Cookie 的方法：**
1. 浏览器打开 https://www.douyin.com
2. 登录你的抖音账号
3. 按 F12 打开开发者工具
4. 切到 Application → Cookies → www.douyin.com
5. 复制所有 Cookie 值，粘贴到上面的配置文件

### 4. 启动服务

```bash
docker compose up -d
```

启动后会运行 5 个容器：

| 服务 | 端口 | 说明 |
|---|---|---|
| **frontend** | 80 | 前端页面（Nginx） |
| **backend** | 8080 | 后端 API + Worker |
| **mysql** | 3306 | 数据库 |
| **redis** | 6379 | 缓存（预留） |
| **scraper** | 9000 | 抖音/TikTok 解析 |

### 5. 访问应用

- 前端页面：http://localhost
- 后端 API：http://localhost:8080
- API 文档：http://localhost:8080/swagger-ui.html

### 6. 默认管理员账号

| 字段 | 值 |
|---|---|
| 邮箱 | admin@videoclip.com |
| 密码 | admin123456 |

> ⚠️ 首次登录后请立即修改密码

---

## 三、使用流程

### 本地上传视频

1. 登录后点击「上传」
2. 选择「📁 本地上传」Tab
3. 拖拽视频文件到上传区域
4. 选择 AI 引擎（mimo / DeepSeek）
5. 点击「开始上传」
6. Worker 自动处理：提取音频 → 语音转写 → AI 分析 → 生成切片
7. 处理完成后查看「切片建议」，点击「裁剪视频」
8. 下载裁剪后的短视频

### 通过链接下载视频

1. 点击「上传」→「🔗 视频链接」Tab
2. 粘贴视频链接（支持抖音/B站/YouTube/快手等 16+ 平台）
3. 点击「解析并创建任务」
4. 后续流程同上

### 支持的视频平台

| 平台 | 解析方式 | 是否需要配置 |
|---|---|---|
| 🎵 抖音 | scraper API | 需要填 Cookie |
| 🎵 TikTok | scraper API | 需要填 Cookie |
| 📺 B站 | yt-dlp | 无需配置 |
| ▶️ YouTube | yt-dlp | 无需配置 |
| ⚡ 快手 | yt-dlp | 无需配置 |
| 📕 小红书 | yt-dlp | 无需配置 |
| 🔴 微博 | yt-dlp | 无需配置 |
| 🍉 西瓜视频 | yt-dlp | 无需配置 |
| 🐦 Twitter/X | yt-dlp | 无需配置 |
| 📸 Instagram | yt-dlp | 无需配置 |
| 🎬 Vimeo | yt-dlp | 无需配置 |
| 🟣 Twitch | yt-dlp | 无需配置 |

---

## 四、手动部署（不用 Docker）

### 环境要求

| 依赖 | 版本 |
|---|---|
| Java | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.0 |
| FFmpeg | 4.0+ |
| Python | 3.8+ |
| yt-dlp | 最新版 |

### 1. 安装系统依赖

```bash
# Ubuntu/Debian
apt update && apt install -y ffmpeg python3 python3-pip mysql-server
pip3 install -U yt-dlp

# macOS
brew install ffmpeg python mysql yt-dlp
```

### 2. 启动 MySQL

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE video_clip CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 3. 启动后端

```bash
cd backend

# 配置环境变量
export DB_URL="jdbc:mysql://localhost:3306/video_clip?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8mb4"
export DB_USER="root"
export DB_PASS="root"
export LLM_MIMO_API_KEY="你的mimo-api-key"

# 启动
mvn spring-boot:run -DskipTests
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 5. 访问

- 前端：http://localhost:5173
- 后端：http://localhost:8080

---

## 五、常见问题

### Q: 抖音视频下载失败？
A: 需要填入抖音 Cookie，参考第二节第 3 步。B站和 YouTube 不需要。

### Q: AI 分析失败？
A: 检查 mimo API Key 是否正确配置。用 `echo $LLM_MIMO_API_KEY` 确认。

### Q: 视频裁剪失败？
A: 确认 FFmpeg 已安装：`ffmpeg -version`

### Q: Docker 启动后访问不了？
A: 检查端口是否被占用：`lsof -i:80` 或 `lsof -i:8080`

### Q: 数据库连接失败？
A: 等 MySQL 健康检查通过后再启动后端：`docker compose logs mysql` 查看状态

---

## 六、项目结构

```
video-clip-platform/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/.../
│   │   ├── controller/         # REST API
│   │   ├── service/            # 业务逻辑
│   │   │   ├── AnalysisService # AI 分析（mimo/DeepSeek）
│   │   │   ├── TranscriptionService # 语音转写（mimo）
│   │   │   ├── ClipService     # 视频裁剪（FFmpeg）
│   │   │   ├── VideoDownloadService # 视频下载（yt-dlp+scraper）
│   │   │   └── FileStorageService   # 文件存储
│   │   ├── runner/
│   │   │   ├── TaskWorker      # 自动任务处理
│   │   │   └── AdminInitializer # 初始化管理员
│   │   └── entity/             # 数据模型
│   └── Dockerfile
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── views/              # 页面
│   │   ├── components/         # 组件
│   │   ├── api/                # API 调用
│   │   └── stores/             # 状态管理
│   └── Dockerfile
├── scraper-config/             # 抖音 Cookie 配置（不提交 Git）
├── docker-compose.yml          # Docker 编排
├── .env                        # 环境变量（不提交 Git）
└── README.md
```

---

## 七、API 接口一览

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/auth/register | 注册 |
| POST | /api/auth/login | 登录 |
| GET | /api/auth/me | 当前用户 |
| POST | /api/tasks | 上传文件创建任务 |
| GET | /api/tasks | 任务列表 |
| GET | /api/tasks/{id} | 任务详情 |
| GET | /api/tasks/{id}/video | 视频预览 |
| POST | /api/tasks/{id}/clips/{clipId}/render | 裁剪单个切片 |
| POST | /api/tasks/{id}/clips/render-all | 批量裁剪 |
| GET | /api/tasks/{id}/clips/{clipId}/download | 下载切片 |
| GET | /api/tasks/{id}/export/srt | 导出 SRT |
| GET | /api/tasks/{id}/export/txt | 导出 TXT |
| POST | /api/download | 链接下载创建任务 |
| GET | /api/download/platforms | 支持的平台列表 |
| GET | /api/llm/providers | 可用 AI 引擎 |
| GET | /api/admin/stats | 管理统计 |
| GET | /api/admin/users | 用户列表 |
| GET | /api/admin/tasks | 所有任务 |
