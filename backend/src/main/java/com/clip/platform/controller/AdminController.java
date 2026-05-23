package com.clip.platform.controller;

import com.clip.platform.common.PageResult;
import com.clip.platform.common.Result;
import com.clip.platform.dto.request.AdminUserUpdateRequest;
import com.clip.platform.dto.response.AdminStatsResponse;
import com.clip.platform.dto.response.TaskResponse;
import com.clip.platform.dto.response.UserResponse;
import com.clip.platform.security.SecurityUtils;
import com.clip.platform.service.AdminService;
import com.clip.platform.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台控制器
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final TaskService taskService;

    /**
     * 获取统计数据
     */
    @GetMapping("/stats")
    public Result<AdminStatsResponse> getStats() {
        return Result.ok(adminService.getStats());
    }

    /**
     * 用户列表
     */
    @GetMapping("/users")
    public Result<PageResult<UserResponse>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(adminService.getUsers(page, size));
    }

    /**
     * 更新用户
     */
    @PutMapping("/users/{id}")
    public Result<UserResponse> updateUser(@PathVariable Long id,
                                           @RequestBody AdminUserUpdateRequest request) {
        return Result.ok(adminService.updateUser(id, request));
    }

    /**
     * 所有任务列表
     */
    @GetMapping("/tasks")
    public Result<PageResult<TaskResponse>> getAllTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return Result.ok(adminService.getAllTasks(page, size, status));
    }

    /**
     * 重试失败任务
     */
    @PutMapping("/tasks/{id}/retry")
    public Result<Void> retryTask(@PathVariable Long id) {
        taskService.retryTask(id);
        return Result.ok();
    }
}
