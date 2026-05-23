package com.clip.platform.controller;

import com.clip.platform.common.Result;
import com.clip.platform.entity.MediaTask;
import com.clip.platform.mapper.MediaTaskMapper;
import com.clip.platform.security.SecurityUtils;
import com.clip.platform.service.VideoDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 视频链接下载控制器
 */
@RestController
@RequestMapping("/api/download")
@RequiredArgsConstructor
public class DownloadController {

    private final VideoDownloadService downloadService;
    private final MediaTaskMapper taskMapper;

    /**
     * 获取支持的平台列表
     */
    @GetMapping("/platforms")
    public Result<List<Map<String, String>>> getSupportedPlatforms() {
        return Result.ok(downloadService.getSupportedPlatforms());
    }

    /**
     * 通过链接下载视频并创建任务
     */
    @PostMapping
    public Result<Map<String, Object>> downloadAndCreateTask(
            @RequestParam("url") String url,
            @RequestParam(value = "contentType", defaultValue = "video") String contentType,
            @RequestParam(value = "targetPlatform", defaultValue = "douyin") String targetPlatform,
            @RequestParam(value = "clipCount", defaultValue = "5") Integer clipCount,
            @RequestParam(value = "llmProvider", required = false) String llmProvider) {

        Long userId = SecurityUtils.getCurrentUserId();

        // 下载视频
        VideoDownloadService.DownloadResult result = downloadService.downloadFromUrl(url);

        // 创建任务
        MediaTask task = new MediaTask();
        task.setUserId(userId);
        task.setTitle(result.title);
        task.setOriginalFilename(result.title + "." + result.extension);
        task.setFilePath(result.relativePath);
        task.setFileSize(result.fileSize);
        task.setMimeType(result.mimeType);
        task.setContentType(contentType);
        task.setTargetPlatform(targetPlatform);
        task.setClipCount(clipCount);
        task.setLlmProvider(llmProvider);
        task.setStatus("PENDING");
        taskMapper.insert(task);

        return Result.ok(Map.of(
                "id", task.getId(),
                "title", result.title,
                "fileSize", result.fileSize,
                "duration", result.duration,
                "status", "PENDING"
        ));
    }
}
