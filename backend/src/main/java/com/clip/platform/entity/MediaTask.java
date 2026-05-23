package com.clip.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("media_tasks")
public class MediaTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String originalFilename;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private Integer durationSeconds;
    private String contentType;       // live/video/podcast/course/interview/speech/other
    private String targetPlatform;    // douyin/xiaohongshu/weixin_video/bilibili/kuaishou/other
    private Integer clipCount;
    private String llmProvider;       // deepseek/mimo
    private String status;            // PENDING/EXTRACTING_AUDIO/TRANSCRIBING/ANALYZING/COMPLETED/FAILED
    private String progressStage;
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
