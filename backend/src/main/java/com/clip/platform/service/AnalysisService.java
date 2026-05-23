package com.clip.platform.service;

import com.clip.platform.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI 分析服务 - 支持多 LLM 提供商（DeepSeek / mimo 等 OpenAI 兼容 API）
 */
@Slf4j
@Service
public class AnalysisService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    // LLM 配置列表
    private final Map<String, LlmConfig> providers = new LinkedHashMap<>();

    public AnalysisService(
            @Value("${app.llm.base-url}") String defaultBaseUrl,
            @Value("${app.llm.model}") String defaultModel,
            @Value("${app.llm.api-key}") String defaultApiKey,
            // mimo 配置（可选）
            @Value("${app.llm.mimo.base-url:}") String mimoBaseUrl,
            @Value("${app.llm.mimo.model:}") String mimoModel,
            @Value("${app.llm.mimo.api-key:}") String mimoApiKey,
            ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();

        // 注册默认提供商（DeepSeek）
        if (defaultApiKey != null && !defaultApiKey.isBlank() && !defaultApiKey.equals("***")) {
            providers.put("deepseek", new LlmConfig("deepseek", "DeepSeek", defaultBaseUrl, defaultModel, defaultApiKey));
        }

        // 注册 mimo 提供商
        if (mimoApiKey != null && !mimoApiKey.isBlank()) {
            providers.put("mimo", new LlmConfig("mimo", "MiMo", mimoBaseUrl, mimoModel, mimoApiKey));
        }

        log.info("已注册 LLM 提供商: {}", providers.keySet());
    }

    /**
     * 获取可用的 LLM 提供商列表
     */
    public List<Map<String, String>> getAvailableProviders() {
        List<Map<String, String>> list = new ArrayList<>();
        for (LlmConfig config : providers.values()) {
            Map<String, String> info = new HashMap<>();
            info.put("id", config.id);
            info.put("name", config.name);
            info.put("model", config.model);
            list.add(info);
        }
        return list;
    }

    /**
     * 分析转写内容
     * @param providerId 提供商ID，null 时使用第一个可用的
     */
    public String analyze(String transcriptText, String contentType, String targetPlatform, int clipCount, String providerId) {
        LlmConfig config = resolveProvider(providerId);
        String prompt = buildPrompt(transcriptText, contentType, targetPlatform, clipCount);

        Map<String, Object> body = new HashMap<>();
        body.put("model", config.model);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("temperature", 0.7);
        body.put("max_tokens", 4096);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            String url = config.baseUrl.replaceAll("/$", "") + "/chat/completions";
            log.info("调用 LLM [{}]: model={}, url={}", config.name, config.model, url);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new BusinessException("LLM API 调用失败: HTTP " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new BusinessException("LLM 返回内容为空");
            }

            return content;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 分析失败 [{}]: {}", config.name, e.getMessage());
            throw new BusinessException("AI 分析失败 (" + config.name + "): " + e.getMessage());
        }
    }

    /**
     * 兼容旧调用（默认使用第一个可用提供商）
     */
    public String analyze(String transcriptText, String contentType, String targetPlatform, int clipCount) {
        return analyze(transcriptText, contentType, targetPlatform, clipCount, null);
    }

    /**
     * 解析 LLM 分析结果
     */
    public AnalysisResultData parseResult(String rawOutput) {
        try {
            String jsonStr = extractJson(rawOutput);
            JsonNode parsed = objectMapper.readTree(jsonStr);

            AnalysisResultData result = new AnalysisResultData();
            result.summaryShort = parsed.has("summary_short") ? parsed.get("summary_short").asText() : "";
            result.summaryLong = parsed.has("summary_long") ? parsed.get("summary_long").asText() : "";

            result.keyPoints = new ArrayList<>();
            if (parsed.has("key_points") && parsed.get("key_points").isArray()) {
                for (JsonNode node : parsed.get("key_points")) {
                    result.keyPoints.add(node.asText());
                }
            }

            result.goldenQuotes = new ArrayList<>();
            if (parsed.has("golden_quotes") && parsed.get("golden_quotes").isArray()) {
                for (JsonNode node : parsed.get("golden_quotes")) {
                    GoldenQuote quote = new GoldenQuote();
                    quote.time = node.has("time") ? node.get("time").asText() : "";
                    quote.text = node.has("text") ? node.get("text").asText() : "";
                    result.goldenQuotes.add(quote);
                }
            }

            result.clips = new ArrayList<>();
            if (parsed.has("clips") && parsed.get("clips").isArray()) {
                for (JsonNode node : parsed.get("clips")) {
                    ClipData clip = new ClipData();
                    clip.startTime = node.has("start_time") ? node.get("start_time").asText() : "";
                    clip.endTime = node.has("end_time") ? node.get("end_time").asText() : "";
                    clip.topic = node.has("topic") ? node.get("topic").asText() : "";
                    clip.title1 = node.has("title_1") ? node.get("title_1").asText() : "";
                    clip.title2 = node.has("title_2") ? node.get("title_2").asText(null) : null;
                    clip.summary = node.has("summary") ? node.get("summary").asText(null) : null;
                    clip.hookReason = node.has("hook_reason") ? node.get("hook_reason").asText(null) : null;
                    clip.suggestedPlatform = node.has("suggested_platform") ? node.get("suggested_platform").asText(null) : null;
                    clip.editingNotes = node.has("editing_notes") ? node.get("editing_notes").asText(null) : null;
                    clip.score = node.has("score") ? node.get("score").asInt(0) : 0;
                    result.clips.add(clip);
                }
            }

            return result;
        } catch (Exception e) {
            throw new BusinessException("解析 AI 分析结果失败: " + e.getMessage());
        }
    }

    private LlmConfig resolveProvider(String providerId) {
        if (providerId != null && !providerId.isBlank()) {
            LlmConfig config = providers.get(providerId);
            if (config != null) return config;
            throw new BusinessException("不支持的 LLM 提供商: " + providerId);
        }
        // 默认使用第一个可用的
        if (providers.isEmpty()) {
            throw new BusinessException("没有可用的 LLM 提供商，请配置 API Key");
        }
        return providers.values().iterator().next();
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String buildPrompt(String transcript, String contentType, String platform, int clipCount) {
        return """
                你是一个专业的视频内容分析师。请分析以下直播/视频的转写文本，并按照指定JSON格式输出分析结果。

                【内容类型】%s
                【目标平台】%s
                【期望切片数量】%d

                【转写文本】
                %s

                请严格按照以下 JSON 格式输出，不要添加额外说明：
                {
                  "summary_short": "50字以内的简短摘要",
                  "summary_long": "200字左右的详细摘要",
                  "key_points": ["关键观点1", "关键观点2", ...],
                  "golden_quotes": [{"time": "00:05:30", "text": "金句内容"}, ...],
                  "clips": [
                    {
                      "start_time": "HH:MM:SS",
                      "end_time": "HH:MM:SS",
                      "topic": "切片主题",
                      "title_1": "推荐标题（吸引眼球型）",
                      "title_2": "备选标题（信息型）",
                      "summary": "切片内容摘要",
                      "hook_reason": "为什么这段内容适合做短视频",
                      "suggested_platform": "推荐发布平台",
                      "editing_notes": "剪辑建议",
                      "score": 85
                    }
                  ]
                }
                """.formatted(contentType, platform, clipCount, transcript);
    }

    // ---- 内部数据类 ----
    private static class LlmConfig {
        String id, name, baseUrl, model, apiKey;
        LlmConfig(String id, String name, String baseUrl, String model, String apiKey) {
            this.id = id; this.name = name; this.baseUrl = baseUrl; this.model = model; this.apiKey = apiKey;
        }
    }

    public static class AnalysisResultData {
        public String summaryShort;
        public String summaryLong;
        public List<String> keyPoints;
        public List<GoldenQuote> goldenQuotes;
        public List<ClipData> clips;
        public String rawOutput;
    }

    public static class GoldenQuote {
        public String time;
        public String text;
    }

    public static class ClipData {
        public String startTime;
        public String endTime;
        public String topic;
        public String title1;
        public String title2;
        public String summary;
        public String hookReason;
        public String suggestedPlatform;
        public String editingNotes;
        public int score;
    }
}
