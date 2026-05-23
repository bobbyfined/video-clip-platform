package com.clip.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("clip_suggestions")
public class ClipSuggestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Double startSeconds;
    private Double endSeconds;
    private String topic;
    private String title1;
    private String title2;
    private String summary;
    private String hookReason;
    private String suggestedPlatform;
    private String editingNotes;
    private Integer score;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
