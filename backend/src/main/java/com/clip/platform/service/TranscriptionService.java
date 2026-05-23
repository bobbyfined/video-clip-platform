package com.clip.platform.service;

import com.clip.platform.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 语音转写服务 - 调用 Python FunASR 脚本
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptionService {

    private final ObjectMapper objectMapper;

    /**
     * 调用 Python 转写脚本
     * @return 转写结果 JSON 字符串
     */
    public String transcribe(Path audioPath) {
        Path outputPath = audioPath.getParent().resolve(
                audioPath.getFileName().toString().replaceAll("\\.[^.]+$", "") + "_transcript.json");

        String scriptPath = "scripts/transcribe.py";

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "python3", scriptPath, audioPath.toString(), outputPath.toString());
            pb.redirectErrorStream(false);
            pb.environment().putAll(System.getenv());

            log.info("启动转写进程: python3 {} {} {}", scriptPath, audioPath, outputPath);
            Process process = pb.start();

            // 读取标准输出
            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("转写进程失败, exitCode={}, stderr={}", exitCode, stderr);
                throw new BusinessException("语音转写失败: " + stderr);
            }

            // 读取输出文件
            String result = new String(java.nio.file.Files.readAllBytes(outputPath), java.nio.charset.StandardCharsets.UTF_8);

            // 清理临时文件
            try { java.nio.file.Files.deleteIfExists(outputPath); } catch (Exception ignored) {}

            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("语音转写出错: " + e.getMessage());
        }
    }

    /**
     * 解析转写结果为片段列表
     */
    public List<TranscriptSegmentData> parseSegments(String json) {
        List<TranscriptSegmentData> segments = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode segmentsNode = root.has("segments") ? root.get("segments") : root;
            if (segmentsNode.isArray()) {
                for (int i = 0; i < segmentsNode.size(); i++) {
                    JsonNode node = segmentsNode.get(i);
                    TranscriptSegmentData seg = new TranscriptSegmentData();
                    seg.start = node.has("start") ? node.get("start").asDouble() : 0;
                    seg.end = node.has("end") ? node.get("end").asDouble() : 0;
                    seg.text = node.has("text") ? node.get("text").asText() : "";
                    seg.order = i + 1;
                    segments.add(seg);
                }
            }
        } catch (Exception e) {
            throw new BusinessException("解析转写结果失败: " + e.getMessage());
        }
        return segments;
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

    /**
     * 转写结果数据
     */
    public static class TranscriptSegmentData {
        public double start;
        public double end;
        public String text;
        public int order;
    }
}
