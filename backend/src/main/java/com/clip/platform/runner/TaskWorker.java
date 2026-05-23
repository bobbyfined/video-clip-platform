package com.clip.platform.runner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clip.platform.entity.MediaTask;
import com.clip.platform.mapper.MediaTaskMapper;
import com.clip.platform.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 任务处理 Worker - 自动轮询待处理任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskWorker {

    private final MediaTaskMapper taskMapper;
    private final TaskService taskService;
    private final FileStorageService fileStorageService;
    private final TranscriptionService transcriptionService;
    private final AnalysisService analysisService;

    /**
     * 每 5 秒检查一次待处理任务
     */
    @Scheduled(fixedDelay = 5000)
    public void pollTasks() {
        List<MediaTask> pendingTasks = taskMapper.selectList(
                new LambdaQueryWrapper<MediaTask>()
                        .eq(MediaTask::getStatus, "PENDING")
                        .orderByAsc(MediaTask::getCreatedAt)
                        .last("LIMIT 1"));

        for (MediaTask task : pendingTasks) {
            processTask(task);
        }
    }

    private void processTask(MediaTask task) {
        Long taskId = task.getId();
        log.info("开始处理任务: taskId={}", taskId);

        try {
            // 1. 提取音频
            taskService.updateStatus(taskId, "EXTRACTING_AUDIO", "正在提取音频...");
            Path videoPath = fileStorageService.getAbsolutePath(task.getFilePath());
            Path audioPath = transcriptionService.extractAudio(videoPath);
            taskService.addLog(taskId, "INFO", "音频提取完成");

            // 2. 语音转写
            taskService.updateStatus(taskId, "TRANSCRIBING", "正在语音转写...");
            var segments = transcriptionService.transcribe(audioPath, task.getOriginalFilename());
            taskService.saveTranscript(taskId, segments);
            taskService.addLog(taskId, "INFO", "语音转写完成，共 " + segments.size() + " 个片段");

            // 计算时长
            if (!segments.isEmpty()) {
                var last = segments.get(segments.size() - 1);
                taskService.setDuration(taskId, (int) Math.ceil(last.end));
            }

            // 3. AI 分析
            taskService.updateStatus(taskId, "ANALYZING", "正在 AI 分析...");
            String transcriptText = segments.stream()
                    .map(s -> "[" + formatTime(s.start) + "→" + formatTime(s.end) + "] " + s.text)
                    .reduce("", (a, b) -> a + "\n" + b);

            // 使用任务指定的 LLM 提供商
            String providerId = task.getLlmProvider();
            String rawResult = analysisService.analyze(
                    transcriptText,
                    task.getContentType(),
                    task.getTargetPlatform(),
                    task.getClipCount(),
                    providerId);

            var result = analysisService.parseResult(rawResult);
            result.rawOutput = rawResult;
            taskService.saveAnalysis(taskId, result, rawResult);
            taskService.addLog(taskId, "INFO", "AI 分析完成 [" + (providerId != null ? providerId : "default") + "]，生成 " + result.clips.size() + " 个切片建议");

            // 4. 完成
            taskService.updateStatus(taskId, "COMPLETED", "处理完成");
            taskService.addLog(taskId, "INFO", "任务处理完成");
            log.info("任务处理完成: taskId={}", taskId);

        } catch (Exception e) {
            log.error("任务处理失败: taskId={}", taskId, e);
            taskService.markFailed(taskId, e.getMessage());
        }
    }

    private String formatTime(double seconds) {
        int h = (int) (seconds / 3600);
        int m = (int) ((seconds % 3600) / 60);
        int s = (int) (seconds % 60);
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
