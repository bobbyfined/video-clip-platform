package com.clip.platform.service;

import com.clip.platform.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 语音转写服务 - 用大模型（mimo）做语音转写
 */
@Slf4j
@Service
public class TranscriptionService {

    private final ObjectMapper objectMapper;
    private final String mimoBaseUrl;
    private final String mimoApiKey;
    private final String mimoModel;

    public TranscriptionService(
            ObjectMapper objectMapper,
            @Value("${app.llm.mimo.base-url:}") String mimoBaseUrl,
            @Value("${app.llm.mimo.api-key:}") String mimoApiKey,
            @Value("${app.llm.mimo.model:}") String mimoModel) {
        this.objectMapper = objectMapper;
        this.mimoBaseUrl = mimoBaseUrl;
        this.mimoApiKey = mimoApiKey;
        this.mimoModel = mimoModel;
    }

    /**
     * 用 FFmpeg 从视频中提取音频
     */
    public Path extractAudio(Path videoPath) {
        try {
            Path audioDir = videoPath.getParent();
            String filename = videoPath.getFileName().toString();
            String baseName = filename.substring(0, filename.lastIndexOf('.'));
            Path audioPath = audioDir.resolve(baseName + ".wav");

            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y", "-i", videoPath.toString(),
                    "-vn", "-acodec", "pcm_s16le", "-ar", "16000", "-ac", "1",
                    audioPath.toString());
            pb.redirectErrorStream(true);

            log.info("提取音频: {}", videoPath);
            Process process = pb.start();
            String output = readStream(process.getInputStream());
            int exitCode = process.waitFor();

            if (exitCode != 0 || !Files.exists(audioPath)) {
                throw new BusinessException("音频提取失败: " + output);
            }

            log.info("音频提取完成: {}", audioPath);
            return audioPath;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("音频提取出错: " + e.getMessage());
        }
    }

    /**
     * 转写音频 - 用 mimo 大模型
     */
    public List<TranscriptSegmentData> transcribe(Path audioPath, String originalFilename) {
        if (mimoApiKey == null || mimoApiKey.isBlank() || mimoApiKey.equals("***")) {
            log.warn("mimo API key 未配置，使用模拟转写");
            return transcribeWithMock(audioPath);
        }

        try {
            return transcribeWithMimo(audioPath);
        } catch (Exception e) {
            log.error("mimo 转写失败: {}", e.getMessage());
            throw new BusinessException("语音转写失败: " + e.getMessage());
        }
    }

    /**
     * 用 mimo 大模型转写音频
     */
    private List<TranscriptSegmentData> transcribeWithMimo(Path audioPath) throws Exception {
        // 将音频转为 base64（先压缩为 mp3 减小体积）
        Path mp3Path = audioPath.getParent().resolve(
                audioPath.getFileName().toString().replaceAll("\\.[^.]+$", "") + ".mp3");

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y", "-i", audioPath.toString(),
                "-acodec", "libmp3lame", "-b:a", "64k", "-ar", "16000",
                mp3Path.toString());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.waitFor();

        if (!Files.exists(mp3Path)) {
            throw new BusinessException("音频压缩失败");
        }

        byte[] audioBytes = Files.readAllBytes(mp3Path);
        String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
        Files.deleteIfExists(mp3Path);
        Files.deleteIfExists(audioPath);

        log.info("调用 mimo 转写音频, 大小: {}KB", audioBytes.length / 1024);

        // 构建请求 - 用 mimo 的 omni 模型做语音转写
        String url = mimoBaseUrl.replaceAll("/$", "") + "/chat/completions";

        Map<String, Object> audioContent = new HashMap<>();
        audioContent.put("type", "input_audio");
        audioContent.put("input_audio", Map.of(
                "data", base64Audio,
                "format", "mp3"));

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", "请将这段音频完整转写为文字。输出格式为 JSON 数组，每个元素包含 start（开始秒数）、end（结束秒数）、text（转写文本）。按时间顺序分段，每段大约 5-15 秒。只输出 JSON，不要其他说明。");

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", List.of(audioContent, textContent));

        Map<String, Object> body = new HashMap<>();
        body.put("model", mimoModel);
        body.put("messages", List.of(message));
        body.put("temperature", 0.1);
        body.put("max_tokens", 4096);

        // 用 RestTemplate 发请求
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.setBearerAuth(mimoApiKey);

        org.springframework.http.HttpEntity<Map<String, Object>> request =
                new org.springframework.http.HttpEntity<>(body, headers);

        org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                url, org.springframework.http.HttpMethod.POST, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException("mimo 转写 API 调用失败: " + response.getStatusCode());
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        String content = root.path("choices").path(0).path("message").path("content").asText("");

        if (content.isBlank()) {
            throw new BusinessException("mimo 转写返回为空");
        }

        log.info("mimo 转写完成，内容长度: {}", content.length());
        return parseSegments(content);
    }

    /**
     * 模拟转写（兜底）
     */
    private List<TranscriptSegmentData> transcribeWithMock(Path audioPath) {
        log.warn("使用模拟转写");
        double duration = getAudioDuration(audioPath);
        if (duration <= 0) duration = 60;

        List<TranscriptSegmentData> segments = new ArrayList<>();
        int count = Math.max(3, (int) (duration / 10));
        double segLen = duration / count;

        String[] texts = {
                "大家好，欢迎来到今天的分享",
                "首先我们来看第一个话题",
                "这个问题其实非常有意思",
                "接下来我们深入讨论一下",
                "这里有一个关键的观点",
                "然后我们来看看实际案例",
                "这个发现真的让人意外",
                "最后我们来做一个总结",
                "感谢大家的观看，下期再见"
        };

        for (int i = 0; i < count; i++) {
            TranscriptSegmentData seg = new TranscriptSegmentData();
            seg.start = i * segLen;
            seg.end = (i + 1) * segLen;
            seg.text = texts[i % texts.length];
            seg.order = i + 1;
            segments.add(seg);
        }
        return segments;
    }

    /**
     * 解析转写 JSON
     */
    public List<TranscriptSegmentData> parseSegments(String json) {
        List<TranscriptSegmentData> segments = new ArrayList<>();
        try {
            // 提取 JSON 部分
            String jsonStr = json;
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start >= 0 && end > start) {
                jsonStr = json.substring(start, end + 1);
            }

            JsonNode arr = objectMapper.readTree(jsonStr);
            if (arr.isArray()) {
                for (int i = 0; i < arr.size(); i++) {
                    JsonNode node = arr.get(i);
                    TranscriptSegmentData seg = new TranscriptSegmentData();
                    seg.start = node.has("start") ? node.get("start").asDouble() : 0;
                    seg.end = node.has("end") ? node.get("end").asDouble() : 0;
                    seg.text = node.has("text") ? node.get("text").asText() : "";
                    seg.order = i + 1;
                    segments.add(seg);
                }
            }
        } catch (Exception e) {
            log.warn("解析转写结果失败，使用原始文本: {}", e.getMessage());
            // 兜底：把整个文本当作一个片段
            TranscriptSegmentData seg = new TranscriptSegmentData();
            seg.start = 0;
            seg.end = 0;
            seg.text = json;
            seg.order = 1;
            segments.add(seg);
        }
        return segments;
    }

    private double getAudioDuration(Path audioPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe", "-v", "quiet", "-show_entries", "format=duration",
                    "-of", "csv=p=0", audioPath.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = readStream(process.getInputStream()).trim();
            process.waitFor();
            return Double.parseDouble(output);
        } catch (Exception e) {
            return 60;
        }
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    public static class TranscriptSegmentData {
        public double start;
        public double end;
        public String text;
        public int order;
    }
}
