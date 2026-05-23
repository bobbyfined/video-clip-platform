package com.clip.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("transcript_segments")
public class TranscriptSegment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Double startSeconds;
    private Double endSeconds;
    private String text;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
