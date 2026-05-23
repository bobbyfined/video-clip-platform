package com.clip.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clip.platform.common.BusinessException;
import com.clip.platform.common.PageResult;
import com.clip.platform.dto.response.TaskDetailResponse;
import com.clip.platform.dto.response.TaskResponse;
import com.clip.platform.entity.*;
import com.clip.platform.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final MediaTaskMapper mediaTaskMapper;
    private final TranscriptSegmentMapper segmentMapper;
    private final AnalysisResultMapper analysisMapper;
    private final ClipSuggestionMapper clipMapper;
    private final TaskLogMapper logMapper;
    private final FileStorageService fileStorageService;

    /**
     * 创建任务
     */
    @Transactional
    public TaskResponse createTask(Long userId, MultipartFile file,
                                   String contentType, String targetPlatform, Integer clipCount, String llmProvider) {
        // 存储文件
        String relativePath = fileStorageService.store(file, "uploads");

        // 创建任务记录
        MediaTask task = new MediaTask();
        task.setUserId(userId);
        task.setTitle(file.getOriginalFilename());
        task.setOriginalFilename(file.getOriginalFilename());
        task.setFilePath(relativePath);
        task.setFileSize(file.getSize());
        task.setMimeType(file.getContentType());
        task.setContentType(contentType != null ? contentType : "live");
        task.setTargetPlatform(targetPlatform != null ? targetPlatform : "douyin");
        task.setClipCount(clipCount != null ? clipCount : 5);
        task.setLlmProvider(llmProvider);
        task.setStatus("PENDING");
        mediaTaskMapper.insert(task);

        // 记录日志
        addLog(task.getId(), "INFO", "任务创建成功");

        log.info("任务创建成功: taskId={}, file={}", task.getId(), task.getOriginalFilename());
        return TaskResponse.fromEntity(task);
    }

    /**
     * 获取用户任务列表
     */
    public PageResult<TaskResponse> getUserTasks(Long userId, int page, int size, String status) {
        LambdaQueryWrapper<MediaTask> wrapper = new LambdaQueryWrapper<MediaTask>()
                .eq(MediaTask::getUserId, userId)
                .eq(status != null && !status.isBlank(), MediaTask::getStatus, status)
                .orderByDesc(MediaTask::getCreatedAt);

        Page<MediaTask> result = mediaTaskMapper.selectPage(new Page<>(page, size), wrapper);
        List<TaskResponse> list = result.getRecords().stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, size);
    }

    /**
     * 获取任务详情（含转写、分析、切片）
     */
    public TaskDetailResponse getTaskDetail(Long taskId, Long userId) {
        MediaTask task = mediaTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        // 非管理员只能看自己的任务
        if (!task.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该任务");
        }

        TaskDetailResponse resp = new TaskDetailResponse();
        // 复制基础字段
        resp.setId(task.getId());
        resp.setTitle(task.getTitle());
        resp.setOriginalFilename(task.getOriginalFilename());
        resp.setFileSize(task.getFileSize());
        resp.setMimeType(task.getMimeType());
        resp.setDurationSeconds(task.getDurationSeconds());
        resp.setContentType(task.getContentType());
        resp.setTargetPlatform(task.getTargetPlatform());
        resp.setClipCount(task.getClipCount());
        resp.setStatus(task.getStatus());
        resp.setProgressStage(task.getProgressStage());
        resp.setErrorMessage(task.getErrorMessage());
        resp.setCreatedAt(task.getCreatedAt());
        resp.setUpdatedAt(task.getUpdatedAt());
        resp.setCompletedAt(task.getCompletedAt());

        // 查询关联数据
        if ("COMPLETED".equals(task.getStatus()) || "ANALYZING".equals(task.getStatus())
                || "TRANSCRIBING".equals(task.getStatus())) {
            // 转写片段
            List<TranscriptSegment> segments = segmentMapper.selectList(
                    new LambdaQueryWrapper<TranscriptSegment>()
                            .eq(TranscriptSegment::getTaskId, taskId)
                            .orderByAsc(TranscriptSegment::getSortOrder));
            resp.setSegments(segments);

            // 分析结果
            AnalysisResult analysis = analysisMapper.selectOne(
                    new LambdaQueryWrapper<AnalysisResult>()
                            .eq(AnalysisResult::getTaskId, taskId));
            resp.setAnalysis(analysis);

            // 切片建议
            if (analysis != null) {
                List<ClipSuggestion> clips = clipMapper.selectList(
                        new LambdaQueryWrapper<ClipSuggestion>()
                                .eq(ClipSuggestion::getTaskId, taskId)
                                .orderByAsc(ClipSuggestion::getSortOrder));
                resp.setClips(clips);
            }
        }

        return resp;
    }

    /**
     * 更新任务状态
     */
    @Transactional
    public void updateStatus(Long taskId, String status, String stage) {
        MediaTask task = new MediaTask();
        task.setId(taskId);
        task.setStatus(status);
        task.setProgressStage(stage);
        if ("COMPLETED".equals(status)) {
            task.setCompletedAt(LocalDateTime.now());
        }
        mediaTaskMapper.updateById(task);
    }

    /**
     * 设置任务失败
     */
    @Transactional
    public void markFailed(Long taskId, String errorMessage) {
        MediaTask task = new MediaTask();
        task.setId(taskId);
        task.setStatus("FAILED");
        task.setErrorMessage(errorMessage);
        mediaTaskMapper.updateById(task);
        addLog(taskId, "ERROR", "任务失败: " + errorMessage);
    }

    /**
     * 保存转写结果
     */
    @Transactional
    public void saveTranscript(Long taskId, List<TranscriptionService.TranscriptSegmentData> segments) {
        for (TranscriptionService.TranscriptSegmentData seg : segments) {
            TranscriptSegment entity = new TranscriptSegment();
            entity.setTaskId(taskId);
            entity.setStartSeconds(seg.start);
            entity.setEndSeconds(seg.end);
            entity.setText(seg.text);
            entity.setSortOrder(seg.order);
            segmentMapper.insert(entity);
        }
    }

    /**
     * 保存分析结果
     */
    @Transactional
    public void saveAnalysis(Long taskId, AnalysisService.AnalysisResultData result, String rawOutput) {
        AnalysisResult entity = new AnalysisResult();
        entity.setTaskId(taskId);
        entity.setSummaryShort(result.summaryShort);
        entity.setSummaryLong(result.summaryLong);
        entity.setKeyPoints(toJsonString(result.keyPoints));
        entity.setGoldenQuotes(toJsonString(result.goldenQuotes));
        // 清理 rawOutput 中的 markdown 代码块标记
        String cleanOutput = rawOutput;
        if (cleanOutput != null) {
            cleanOutput = cleanOutput.trim();
            if (cleanOutput.startsWith("```")) {
                int firstNewline = cleanOutput.indexOf('\n');
                if (firstNewline > 0) cleanOutput = cleanOutput.substring(firstNewline + 1);
            }
            if (cleanOutput.endsWith("```")) {
                cleanOutput = cleanOutput.substring(0, cleanOutput.lastIndexOf("```")).trim();
            }
            // 验证是否为合法 JSON
            try {
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(cleanOutput);
            } catch (Exception e) {
                cleanOutput = toJsonString(Map.of("raw", rawOutput));
            }
        }
        entity.setRawModelOutput(cleanOutput);
        analysisMapper.insert(entity);

        // 保存切片建议
        if (result.clips != null) {
            for (int i = 0; i < result.clips.size(); i++) {
                AnalysisService.ClipData clip = result.clips.get(i);
                ClipSuggestion clipEntity = new ClipSuggestion();
                clipEntity.setTaskId(taskId);
                clipEntity.setStartSeconds(timeToSeconds(clip.startTime));
                clipEntity.setEndSeconds(timeToSeconds(clip.endTime));
                clipEntity.setTopic(clip.topic);
                clipEntity.setTitle1(clip.title1);
                clipEntity.setTitle2(clip.title2);
                clipEntity.setSummary(clip.summary);
                clipEntity.setHookReason(clip.hookReason);
                clipEntity.setSuggestedPlatform(clip.suggestedPlatform);
                clipEntity.setEditingNotes(clip.editingNotes);
                clipEntity.setScore(clip.score);
                clipEntity.setSortOrder(i + 1);
                clipMapper.insert(clipEntity);
            }
        }
    }

    /**
     * 设置任务时长
     */
    public void setDuration(Long taskId, int durationSeconds) {
        MediaTask task = new MediaTask();
        task.setId(taskId);
        task.setDurationSeconds(durationSeconds);
        mediaTaskMapper.updateById(task);
    }

    /**
     * 添加任务日志
     */
    public void addLog(Long taskId, String level, String message) {
        TaskLog logEntity = new TaskLog();
        logEntity.setTaskId(taskId);
        logEntity.setLevel(level);
        logEntity.setMessage(message);
        logMapper.insert(logEntity);
    }

    /**
     * 重试失败任务
     */
    @Transactional
    public void retryTask(Long taskId) {
        MediaTask task = mediaTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        if (!"FAILED".equals(task.getStatus())) {
            throw new BusinessException("只有失败的任务才能重试");
        }
        task.setStatus("PENDING");
        task.setErrorMessage(null);
        task.setProgressStage(null);
        mediaTaskMapper.updateById(task);
        addLog(taskId, "INFO", "任务已重新加入队列");
    }

    private double timeToSeconds(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return 0;
        String[] parts = timeStr.split(":");
        try {
            if (parts.length == 3) {
                return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60 + Double.parseDouble(parts[2]);
            } else if (parts.length == 2) {
                return Integer.parseInt(parts[0]) * 60 + Double.parseDouble(parts[1]);
            }
        } catch (NumberFormatException ignored) {}
        return 0;
    }

    /**
     * 获取视频文件绝对路径
     */
    public Path getVideoFilePath(Long taskId) {
        MediaTask task = mediaTaskMapper.selectById(taskId);
        if (task == null || task.getFilePath() == null) return null;
        Path absPath = fileStorageService.getAbsolutePath(task.getFilePath());
        return absPath;
    }

    private String toJsonString(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
