package com.clip.platform.controller;

import com.clip.platform.common.Result;
import com.clip.platform.entity.ClipSuggestion;
import com.clip.platform.security.SecurityUtils;
import com.clip.platform.service.ClipService;
import com.clip.platform.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;

/**
 * 视频切片控制器
 */
@RestController
@RequestMapping("/api/tasks/{taskId}/clips")
@RequiredArgsConstructor
public class ClipController {

    private final ClipService clipService;
    private final TaskService taskService;

    /**
     * 渲染单个切片
     */
    @PostMapping("/{clipId}/render")
    public Result<ClipSuggestion> renderClip(@PathVariable Long taskId, @PathVariable Long clipId) {
        Long userId = SecurityUtils.getCurrentUserId();
        taskService.getTaskDetail(taskId, userId); // 权限校验
        return Result.ok(clipService.renderClip(taskId, clipId));
    }

    /**
     * 批量渲染所有切片
     */
    @PostMapping("/render-all")
    public Result<String> renderAll(@PathVariable Long taskId) {
        Long userId = SecurityUtils.getCurrentUserId();
        taskService.getTaskDetail(taskId, userId); // 权限校验
        clipService.renderAllClips(taskId);
        return Result.ok("批量渲染已启动");
    }

    /**
     * 下载切片视频
     */
    @GetMapping("/{clipId}/download")
    public ResponseEntity<Resource> downloadClip(@PathVariable Long taskId, @PathVariable Long clipId) {
        Long userId = SecurityUtils.getCurrentUserId();
        taskService.getTaskDetail(taskId, userId); // 权限校验

        Path clipPath = clipService.getClipAbsolutePath(taskId, clipId);
        Resource resource = new FileSystemResource(clipPath);

        String filename = clipPath.getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(resource);
    }
}
