package com.clip.platform.dto.response;

import lombok.Data;

@Data
public class AdminStatsResponse {
    private long totalUsers;
    private long totalTasks;
    private long pendingTasks;
    private long processingTasks;
    private long completedTasks;
    private long failedTasks;
}
