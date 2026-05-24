package com.clip.platform.service;

import com.clip.platform.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * 视频下载服务
 * - yt-dlp 通用下载（B站、YouTube 等）
 * - scraper API 抖音专用解析（绕过反爬）
 */
@Slf4j
@Service
public class VideoDownloadService {

    private final Path uploadDir;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String scraperUrl;

    public static final List<Map<String, String>> SUPPORTED_PLATFORMS = List.of(
            Map.of("id", "douyin", "name", "抖音", "icon", "🎵", "domain", "douyin.com", "method", "scraper"),
            Map.of("id", "bilibili", "name", "B站", "icon", "📺", "domain", "bilibili.com", "method", "ytdlp"),
            Map.of("id", "youtube", "name", "YouTube", "icon", "▶️", "domain", "youtube.com/youtu.be", "method", "ytdlp"),
            Map.of("id", "tiktok", "name", "TikTok", "icon", "🎵", "domain", "tiktok.com", "method", "scraper"),
            Map.of("id", "kuaishou", "name", "快手", "icon", "⚡", "domain", "kuaishou.com", "method", "ytdlp"),
            Map.of("id", "xiaohongshu", "name", "小红书", "icon", "📕", "domain", "xiaohongshu.com", "method", "ytdlp"),
            Map.of("id", "weibo", "name", "微博", "icon", "🔴", "domain", "weibo.com", "method", "ytdlp"),
            Map.of("id", "ixigua", "name", "西瓜视频", "icon", "🍉", "domain", "ixigua.com", "method", "ytdlp"),
            Map.of("id", "acfun", "name", "AcFun", "icon", "🅰️", "domain", "acfun.cn", "method", "ytdlp"),
            Map.of("id", "twitter", "name", "X/Twitter", "icon", "🐦", "domain", "x.com/twitter.com", "method", "ytdlp"),
            Map.of("id", "instagram", "name", "Instagram", "icon", "📸", "domain", "instagram.com", "method", "ytdlp"),
            Map.of("id", "vimeo", "name", "Vimeo", "icon", "🎬", "domain", "vimeo.com", "method", "ytdlp"),
            Map.of("id", "twitch", "name", "Twitch", "icon", "🟣", "domain", "twitch.tv", "method", "ytdlp"),
            Map.of("id", "zhihu", "name", "知乎", "icon", "💡", "domain", "zhihu.com", "method", "ytdlp")
    );

    public VideoDownloadService(
            @Value("${app.upload-dir:./storage/uploads}") String uploadDirPath,
            @Value("${app.scraper.url:http://localhost:9000}") String scraperUrl,
            ObjectMapper objectMapper) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        this.scraperUrl = scraperUrl;
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public List<Map<String, String>> getSupportedPlatforms() {
        return SUPPORTED_PLATFORMS;
    }

    /**
     * 从 URL 下载视频
     */
    public DownloadResult downloadFromUrl(String url) {
        if (isDouyinUrl(url)) {
            return downloadDouyinViaScraper(url);
        } else if (isTikTokUrl(url)) {
            return downloadTikTokViaScraper(url);
        } else if (isBilibiliUrl(url)) {
            return downloadBilibiliViaYutto(url);
        } else {
            return downloadViaYtDlp(url);
        }
    }

    /**
     * 通过 scraper API 解析抖音视频
     */
    private DownloadResult downloadDouyinViaScraper(String url) {
        try {
            log.info("通过 scraper 解析抖音链接: {}", url);

            // 1. 调用 scraper API 获取视频信息
            String apiUrl = scraperUrl.replaceAll("/$", "") + "/api/douyin/web/fetch_one_video";
            String encodedUrl = java.net.URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8);

            Map<String, Object> requestBody = Map.of("url", url);
            var response = restTemplate.postForEntity(apiUrl, requestBody, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException("scraper API 调用失败");
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            // 2. 提取视频信息
            JsonNode data = root.path("data").path("aweme_detail");
            if (data.isMissingNode() || data.isNull()) {
                // 尝试其他格式
                data = root.path("data");
                if (data.isMissingNode()) {
                    throw new BusinessException("scraper 返回数据格式异常");
                }
            }

            String title = data.path("desc").asText("未知标题");
            JsonNode videoNode = data.path("video");
            String videoUrl = "";

            // 尝试获取无水印视频地址
            if (videoNode.has("play_addr") && videoNode.path("play_addr").has("url_list")) {
                JsonNode urlList = videoNode.path("play_addr").path("url_list");
                if (urlList.isArray() && urlList.size() > 0) {
                    videoUrl = urlList.get(0).asText();
                }
            }
            if (videoUrl.isEmpty() && videoNode.has("playApi")) {
                videoUrl = videoNode.path("playApi").asText();
            }

            if (videoUrl.isEmpty()) {
                throw new BusinessException("无法获取视频地址，可能需要更新 Cookie");
            }

            // 3. 下载视频文件
            return downloadFile(videoUrl, title, "mp4", "douyin");

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("scraper 解析抖音失败: {}", e.getMessage());
            // 回退到 yt-dlp
            log.info("回退到 yt-dlp 下载...");
            return downloadViaYtDlp(url);
        }
    }

    /**
     * 通过 scraper API 解析 TikTok 视频
     */
    private DownloadResult downloadTikTokViaScraper(String url) {
        try {
            log.info("通过 scraper 解析 TikTok 链接: {}", url);
            String apiUrl = scraperUrl.replaceAll("/$", "") + "/api/tiktok/web/fetch_one_video";

            Map<String, Object> requestBody = Map.of("url", url);
            var response = restTemplate.postForEntity(apiUrl, requestBody, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BusinessException("scraper API 调用失败");
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.path("data");
            String title = data.path("desc").asText("TikTok Video");

            JsonNode playAddr = data.path("video").path("play_addr");
            String videoUrl = "";
            if (playAddr.has("url_list")) {
                JsonNode urls = playAddr.path("url_list");
                if (urls.isArray() && urls.size() > 0) videoUrl = urls.get(0).asText();
            }

            if (videoUrl.isEmpty()) throw new BusinessException("无法获取视频地址");

            return downloadFile(videoUrl, title, "mp4", "tiktok");

        } catch (Exception e) {
            log.error("scraper 解析 TikTok 失败: {}", e.getMessage());
            return downloadViaYtDlp(url);
        }
    }

    /**
     * 通过 yt-dlp 下载（通用）
     */
    private DownloadResult downloadViaYtDlp(String url) {
        String uuid = UUID.randomUUID().toString();
        Path outputDir = uploadDir.resolve("downloads");
        try { Files.createDirectories(outputDir); } catch (Exception ignored) {}

        String outputTemplate = outputDir.resolve(uuid + ".%(ext)s").toString();

        try {
            List<String> cmd = new ArrayList<>(List.of(
                    "yt-dlp", "--no-playlist", "-f", "best[height<=1080]/best",
                    "--max-filesize", "500m", "-o", outputTemplate,
                    "--print", "after_move:filepath", "--print", "title", "--print", "duration", url));

            log.info("yt-dlp 下载: {}", url);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) { lines.add(line); }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String output = String.join("\n", lines);
                if (output.contains("File is larger")) throw new BusinessException("视频超过 500MB 限制");
                if (output.contains("Unsupported URL")) throw new BusinessException("不支持的链接格式");
                throw new BusinessException("视频下载失败，请检查链接是否有效");
            }

            if (lines.size() < 3) throw new BusinessException("下载结果解析失败");

            String filePath = lines.get(lines.size() - 3).trim();
            String title = lines.get(lines.size() - 2).trim();
            int duration = 0;
            try { duration = Integer.parseInt(lines.get(lines.size() - 1).trim()); } catch (Exception ignored) {}

            Path absolutePath = Paths.get(filePath);
            Path relativePath = uploadDir.relativize(absolutePath);
            long fileSize = Files.exists(absolutePath) ? Files.size(absolutePath) : 0;
            String ext = getFileExtension(filePath);
            String mimeType = getMimeType(ext);

            return new DownloadResult(relativePath.toString(), title, fileSize, mimeType, duration, ext);

        } catch (BusinessException e) { throw e; }
        catch (Exception e) {
            throw new BusinessException("视频下载失败: " + e.getMessage());
        }
    }

    /**
     * 通用文件下载
     */
    private DownloadResult downloadFile(String fileUrl, String title, String defaultExt, String platform) {
        try {
            String uuid = UUID.randomUUID().toString();
            Path outputDir = uploadDir.resolve("downloads");
            Files.createDirectories(outputDir);
            Path outputPath = outputDir.resolve(uuid + "." + defaultExt);

            // 下载文件
            log.info("下载视频文件: {} -> {}", fileUrl.substring(0, Math.min(80, fileUrl.length())), outputPath);
            byte[] fileBytes = restTemplate.getForObject(fileUrl, byte[].class);
            if (fileBytes == null || fileBytes.length == 0) {
                throw new BusinessException("视频文件下载为空");
            }
            Files.write(outputPath, fileBytes);

            Path relativePath = uploadDir.relativize(outputPath);
            String ext = defaultExt;
            String mimeType = getMimeType(ext);

            log.info("下载完成: title={}, size={}KB", title, fileBytes.length / 1024);
            return new DownloadResult(relativePath.toString(), title, fileBytes.length, mimeType, 0, ext);

        } catch (Exception e) {
            throw new BusinessException("文件下载失败: " + e.getMessage());
        }
    }

    private boolean isBilibiliUrl(String url) {
        return url.contains("bilibili.com") || url.contains("b23.tv");
    }

    /**
     * 用 yutto 下载 B站视频（不需要 Cookie）
     */
    private DownloadResult downloadBilibiliViaYutto(String url) {
        try {
            String uuid = UUID.randomUUID().toString();
            Path outputDir = uploadDir.resolve("downloads");
            Files.createDirectories(outputDir);

            log.info("yutto 下载 B站视频: {}", url);
            ProcessBuilder pb = new ProcessBuilder(
                    "yutto", "-q", "64", url, "-d", outputDir.toString(), "--no-danmaku");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                log.info("yutto: {}", line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new BusinessException("yutto 下载失败，请检查链接是否正确");
            }

            // 找到下载的文件（最新的 mp4/mkv）
            Path downloadedFile = null;
            try (var stream = Files.list(outputDir)) {
                downloadedFile = stream
                        .filter(p -> p.toString().endsWith(".mp4") || p.toString().endsWith(".mkv"))
                        .max((a, b) -> Long.compare(a.toFile().lastModified(), b.toFile().lastModified()))
                        .orElse(null);
            }

            if (downloadedFile == null || !Files.exists(downloadedFile)) {
                throw new BusinessException("yutto 下载文件未找到");
            }

            // 重命名为 UUID 避免冲突
            String ext = getFileExtension(downloadedFile.toString());
            Path renamed = outputDir.resolve(uuid + "." + ext);
            Files.move(downloadedFile, renamed, StandardCopyOption.REPLACE_EXISTING);

            Path relativePath = uploadDir.relativize(renamed);
            long fileSize = Files.size(renamed);
            String mimeType = getMimeType(ext);
            String title = downloadedFile.getFileName().toString().replaceAll("\\.[^.]+$", "");

            log.info("yutto 下载完成: title={}, size={}KB", title, fileSize / 1024);
            return new DownloadResult(relativePath.toString(), title, fileSize, mimeType, 0, ext);

        } catch (BusinessException e) { throw e; }
        catch (Exception e) {
            log.error("yutto 下载失败，回退到 yt-dlp: {}", e.getMessage());
            return downloadViaYtDlp(url);
        }
    }

    private boolean isDouyinUrl(String url) {
        return url.contains("douyin.com") || url.contains("iesdouyin.com");
    }

    private boolean isTikTokUrl(String url) {
        return url.contains("tiktok.com") || url.contains("vm.tiktok.com");
    }

    private String getFileExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "mp4";
    }

    private String getMimeType(String ext) {
        return switch (ext) {
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "mkv" -> "video/x-matroska";
            case "mov" -> "video/quicktime";
            default -> "video/mp4";
        };
    }

    public static class DownloadResult {
        public String relativePath;
        public String title;
        public long fileSize;
        public String mimeType;
        public int duration;
        public String extension;

        public DownloadResult(String relativePath, String title, long fileSize, String mimeType, int duration, String extension) {
            this.relativePath = relativePath; this.title = title; this.fileSize = fileSize;
            this.mimeType = mimeType; this.duration = duration; this.extension = extension;
        }
    }
}
