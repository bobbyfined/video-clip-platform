package com.clip.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clip.platform.common.BusinessException;
import com.clip.platform.common.PageResult;
import com.clip.platform.dto.request.AdminUserUpdateRequest;
import com.clip.platform.dto.response.AdminStatsResponse;
import com.clip.platform.dto.response.TaskResponse;
import com.clip.platform.dto.response.UserResponse;
import com.clip.platform.entity.MediaTask;
import com.clip.platform.entity.User;
import com.clip.platform.mapper.MediaTaskMapper;
import com.clip.platform.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理后台服务
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserMapper userMapper;
    private final MediaTaskMapper taskMapper;

    /**
     * 获取统计数据
     */
    public AdminStatsResponse getStats() {
        AdminStatsResponse stats = new AdminStatsResponse();
        stats.setTotalUsers(userMapper.selectCount(null));
        stats.setTotalTasks(taskMapper.selectCount(null));
        stats.setPendingTasks(taskMapper.countByStatus("PENDING"));
        stats.setCompletedTasks(taskMapper.countByStatus("COMPLETED"));
        stats.setFailedTasks(taskMapper.countByStatus("FAILED"));
        // 处理中 = EXTRACTING_AUDIO + TRANSCRIBING + ANALYZING
        stats.setProcessingTasks(
                taskMapper.countByStatus("EXTRACTING_AUDIO") +
                taskMapper.countByStatus("TRANSCRIBING") +
                taskMapper.countByStatus("ANALYZING"));
        return stats;
    }

    /**
     * 用户列表
     */
    public PageResult<UserResponse> getUsers(int page, int size) {
        Page<User> result = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));
        List<UserResponse> list = result.getRecords().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, size);
    }

    /**
     * 更新用户信息
     */
    public UserResponse updateUser(Long userId, AdminUserUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getPlan() != null) {
            user.setPlan(request.getPlan());
        }
        userMapper.updateById(user);
        return UserResponse.fromEntity(user);
    }

    /**
     * 所有任务列表
     */
    public PageResult<TaskResponse> getAllTasks(int page, int size, String status) {
        LambdaQueryWrapper<MediaTask> wrapper = new LambdaQueryWrapper<MediaTask>()
                .eq(status != null && !status.isBlank(), MediaTask::getStatus, status)
                .orderByDesc(MediaTask::getCreatedAt);
        Page<MediaTask> result = taskMapper.selectPage(new Page<>(page, size), wrapper);
        List<TaskResponse> list = result.getRecords().stream()
                .map(TaskResponse::fromEntity)
                .collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, size);
    }
}
