package com.clip.platform.controller;

import com.clip.platform.common.PageResult;
import com.clip.platform.common.Result;
import com.clip.platform.dto.response.TaskDetailResponse;
import com.clip.platform.dto.response.TaskResponse;
import com.clip.platform.security.SecurityUtils;
import com.clip.platform.service.ExportService;
import com.clip.platform.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 任务控制器
 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final ExportService exportService;

    /**
     * 创建任务（上传文件）
     */
    @PostMapping
    public Result<TaskResponse> createTask(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "contentType", defaultValue = "live") String contentType,
            @RequestParam(value = "targetPlatform", defaultValue = "douyin") String targetPlatform,
            @RequestParam(value = "clipCount", defaultValue = "5") Integer clipCount) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(taskService.createTask(userId, file, contentType, targetPlatform, clipCount));
    }

    /**
     * 获取当前用户任务列表
     */
    @GetMapping
    public Result<PageResult<TaskResponse>> getTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(taskService.getUserTasks(userId, page, size, status));
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    public Result<TaskDetailResponse> getTaskDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(taskService.getTaskDetail(id, userId));
    }

    /**
     * 导出 SRT 字幕
     */
    @GetMapping("/{id}/export/srt")
    public ResponseEntity<byte[]> exportSrt(@PathVariable Long id) {
        // 校验权限
        taskService.getTaskDetail(id, SecurityUtils.getCurrentUserId());
        String srt = exportService.exportSrt(id);
        return buildDownloadResponse(srt, id + ".srt");
    }

    /**
     * 导出 TXT 文本
     */
    @GetMapping("/{id}/export/txt")
    public ResponseEntity<byte[]> exportTxt(@PathVariable Long id) {
        taskService.getTaskDetail(id, SecurityUtils.getCurrentUserId());
        String txt = exportService.exportTxt(id);
        return buildDownloadResponse(txt, id + ".txt");
    }

    /**
     * 导出切片建议 TXT
     */
    @GetMapping("/{id}/export/clips")
    public ResponseEntity<byte[]> exportClips(@PathVariable Long id) {
        taskService.getTaskDetail(id, SecurityUtils.getCurrentUserId());
        String txt = exportService.exportClipsTxt(id);
        return buildDownloadResponse(txt, id + "_clips.txt");
    }

    private ResponseEntity<byte[]> buildDownloadResponse(String content, String filename) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(bytes.length)
                .body(bytes);
    }
}
