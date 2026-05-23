package com.clip.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task_logs")
public class TaskLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String level;    // INFO/WARN/ERROR
    private String message;
    private String meta;     // JSON
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
