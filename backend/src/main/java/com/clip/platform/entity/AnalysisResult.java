package com.clip.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("analysis_results")
public class AnalysisResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String summaryShort;
    private String summaryLong;
    private String keyPoints;       // JSON
    private String goldenQuotes;    // JSON
    private String rawModelOutput;  // JSON
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
