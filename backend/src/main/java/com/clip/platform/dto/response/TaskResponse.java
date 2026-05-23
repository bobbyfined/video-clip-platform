package com.clip.platform.dto.response;

import com.clip.platform.entity.MediaTask;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskResponse {
    private Long id;
    private String title;
    private String originalFilename;
    private Long fileSize;
    private String mimeType;
    private Integer durationSeconds;
    private String contentType;
    private String targetPlatform;
    private Integer clipCount;
    private String llmProvider;
    private String status;
    private String progressStage;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    public static TaskResponse fromEntity(MediaTask task) {
        TaskResponse resp = new TaskResponse();
        resp.setId(task.getId());
        resp.setTitle(task.getTitle());
        resp.setOriginalFilename(task.getOriginalFilename());
        resp.setFileSize(task.getFileSize());
        resp.setMimeType(task.getMimeType());
        resp.setDurationSeconds(task.getDurationSeconds());
        resp.setContentType(task.getContentType());
        resp.setTargetPlatform(task.getTargetPlatform());
        resp.setClipCount(task.getClipCount());
        resp.setLlmProvider(task.getLlmProvider());
        resp.setStatus(task.getStatus());
        resp.setProgressStage(task.getProgressStage());
        resp.setErrorMessage(task.getErrorMessage());
        resp.setCreatedAt(task.getCreatedAt());
        resp.setUpdatedAt(task.getUpdatedAt());
        resp.setCompletedAt(task.getCompletedAt());
        return resp;
    }
}
