package com.clip.platform.controller;

import com.clip.platform.common.Result;
import com.clip.platform.runner.FileCleanupRunner;
import com.clip.platform.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * LLM 提供商控制器
 */
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
public class LlmController {

    private final AnalysisService analysisService;
    private final FileCleanupRunner cleanupRunner;

    /**
     * 获取可用的 LLM 提供商列表
     */
    @GetMapping("/providers")
    public Result<List<Map<String, String>>> getProviders() {
        return Result.ok(analysisService.getAvailableProviders());
    }

    /**
     * 获取存储使用情况
     */
    @GetMapping("/storage")
    public Result<FileCleanupRunner.StorageInfo> getStorageInfo() {
        return Result.ok(cleanupRunner.getStorageInfo());
    }

    /**
     * 手动触发文件清理
     */
    @PostMapping("/cleanup")
    public Result<String> cleanup() {
        int count = cleanupRunner.manualCleanup();
        return Result.ok("清理完成，共清理 " + count + " 个文件");
    }
}
