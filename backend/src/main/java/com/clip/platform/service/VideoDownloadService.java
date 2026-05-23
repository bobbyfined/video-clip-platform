package com.clip.platform.service;

import com.clip.platform.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 视频下载服务 - 通过 yt-dlp 解析链接下载视频
 */
@Slf4j
@Service
public class VideoDownloadService {

    private final Path uploadDir;

    // 支持的平台
    public static final List<Map<String, String>> SUPPORTED_PLATFORMS = List.of(
            Map.of("id", "douyin", "name", "抖音", "icon", "🎵", "domain", "douyin.com"),
            Map.of("id", "bilibili", "name", "B站", "icon", "📺", "domain", "bilibili.com"),
            Map.of("id", "youtube", "name", "YouTube", "icon", "▶️", "domain", "youtube.com/youtu.be"),
            Map.of("id", "tiktok", "name", "TikTok", "icon", "🎵", "domain", "tiktok.com"),
            Map.of("id", "kuaishou", "name", "快手", "icon", "⚡", "domain", "kuaishou.com"),
            Map.of("id", "xiaohongshu", "name", "小红书", "icon", "📕", "domain", "xiaohongshu.com"),
            Map.of("id", "weibo", "name", "微博", "icon", "🔴", "domain", "weibo.com"),
            Map.of("id", "ixigua", "name", "西瓜视频", "icon", "🍉", "domain", "ixigua.com"),
            Map.of("id", "acfun", "name", "AcFun", "icon", "🅰️", "domain", "acfun.cn"),
            Map.of("id", "twitter", "name", "X/Twitter", "icon", "🐦", "domain", "x.com/twitter.com"),
            Map.of("id", "instagram", "name", "Instagram", "icon", "📸", "domain", "instagram.com"),
            Map.of("id", "facebook", "name", "Facebook", "icon", "📘", "domain", "facebook.com"),
            Map.of("id", "vimeo", "name", "Vimeo", "icon", "🎬", "domain", "vimeo.com"),
            Map.of("id", "twitch", "name", "Twitch", "icon", "🟣", "domain", "twitch.tv"),
            Map.of("id", "zhihu", "name", "知乎", "icon", "💡", "domain", "zhihu.com"),
            Map.of("id", "pinterest", "name", "Pinterest", "icon", "📌", "domain", "pinterest.com")
    );

    public VideoDownloadService(@Value("${app.upload-dir:./storage/uploads}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
    }

    /**
     * 获取支持的平台列表
     */
    public List<Map<String, String>> getSupportedPlatforms() {
        return SUPPORTED_PLATFORMS;
    }

    /**
     * 从 URL 下载视频
     * @return 相对文件路径
     */
    public DownloadResult downloadFromUrl(String url) {
        // 生成唯一文件名
        String uuid = UUID.randomUUID().toString();
        Path outputDir = uploadDir.resolve("downloads");
        try {
            Files.createDirectories(outputDir);
        } catch (Exception e) {
            throw new BusinessException("创建下载目录失败");
        }

        String outputTemplate = outputDir.resolve(uuid + ".%(ext)s").toString();

        try {
            // 构建 yt-dlp 命令
            List<String> cmd = new ArrayList<>(List.of(
                    "yt-dlp",
                    "--no-playlist",
                    "-f", "best[height<=1080]/best",
                    "--max-filesize", "500m",
                    "-o", outputTemplate,
                    "--print", "after_move:filepath",
                    "--print", "title",
                    "--print", "duration",
                    url
            ));

            log.info("yt-dlp 下载: {}", url);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 读取输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                log.info("yt-dlp: {}", line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String output = String.join("\n", lines);
                if (output.contains("File is larger")) {
                    throw new BusinessException("视频文件超过 500MB 限制");
                }
                if (output.contains("Unsupported URL") || output.contains("is not a supported site")) {
                    throw new BusinessException("不支持的链接格式，请检查链接是否正确");
                }
                throw new BusinessException("视频下载失败，请检查链接是否有效");
            }

            // 解析输出：最后3行是 filepath, title, duration
            if (lines.size() < 3) {
                throw new BusinessException("下载结果解析失败");
            }

            String filePath = lines.get(lines.size() - 3).trim();
            String title = lines.get(lines.size() - 2).trim();
            String durationStr = lines.get(lines.size() - 1).trim();

            int duration = 0;
            try {
                duration = Integer.parseInt(durationStr);
            } catch (NumberFormatException ignored) {}

            // 计算相对路径
            Path absolutePath = Paths.get(filePath);
            Path relativePath = uploadDir.getParent().relativize(absolutePath);

            // 获取文件大小
            long fileSize = Files.exists(absolutePath) ? Files.size(absolutePath) : 0;

            // 检测 MIME 类型
            String ext = getFileExtension(filePath);
            String mimeType = switch (ext) {
                case "mp4" -> "video/mp4";
                case "webm" -> "video/webm";
                case "mkv" -> "video/x-matroska";
                case "mov" -> "video/quicktime";
                case "flv" -> "video/x-flv";
                default -> "video/mp4";
            };

            log.info("下载完成: title={}, path={}, size={}KB, duration={}s", title, relativePath, fileSize / 1024, duration);

            return new DownloadResult(relativePath.toString(), title, fileSize, mimeType, duration, ext);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("视频下载出错: {}", e.getMessage());
            throw new BusinessException("视频下载失败: " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "mp4";
    }

    /**
     * 下载结果
     */
    public static class DownloadResult {
        public String relativePath;
        public String title;
        public long fileSize;
        public String mimeType;
        public int duration;
        public String extension;

        public DownloadResult(String relativePath, String title, long fileSize, String mimeType, int duration, String extension) {
            this.relativePath = relativePath;
            this.title = title;
            this.fileSize = fileSize;
            this.mimeType = mimeType;
            this.duration = duration;
            this.extension = extension;
        }
    }
}
