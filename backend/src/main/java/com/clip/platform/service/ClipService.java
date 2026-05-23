package com.clip.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clip.platform.common.BusinessException;
import com.clip.platform.entity.ClipSuggestion;
import com.clip.platform.entity.MediaTask;
import com.clip.platform.mapper.ClipSuggestionMapper;
import com.clip.platform.mapper.MediaTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 视频切片服务 - 使用 FFmpeg 裁剪视频
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClipService {

    private final ClipSuggestionMapper clipMapper;
    private final MediaTaskMapper taskMapper;
    private final TaskService taskService;
    private final FileStorageService fileStorageService;

    @Value("${app.upload-dir:./storage/uploads}")
    private String uploadDir;

    @Value("${app.processed-dir:./storage/processed}")
    private String processedDir;

    /**
     * 渲染单个切片
     */
    public ClipSuggestion renderClip(Long taskId, Long clipId) {
        // 校验
        MediaTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");

        ClipSuggestion clip = clipMapper.selectById(clipId);
        if (clip == null || !clip.getTaskId().equals(taskId)) {
            throw new BusinessException(404, "切片不存在");
        }

        // 更新状态为渲染中
        clip.setClipStatus("RENDERING");
        clipMapper.updateById(clip);
        taskService.addLog(taskId, "INFO", "开始渲染切片 #" + clip.getSortOrder() + ": " + clip.getTitle1());

        try {
            // 获取源视频绝对路径
            Path sourcePath = fileStorageService.getAbsolutePath(task.getFilePath());
            if (!Files.exists(sourcePath)) {
                throw new BusinessException("源视频文件不存在");
            }

            // 确保输出目录存在
            Path outputDir = Paths.get(processedDir).toAbsolutePath().normalize();
            Files.createDirectories(outputDir);

            // 输出文件名
            String ext = fileStorageService.getFileExtension(task.getOriginalFilename());
            if (ext.isEmpty()) ext = "mp4";
            String outputFilename = taskId + "_clip_" + clip.getSortOrder() + "." + ext;
            Path outputPath = outputDir.resolve(outputFilename);

            // 构建 FFmpeg 命令
            double startSec = clip.getStartSeconds();
            double endSec = clip.getEndSeconds();
            double duration = endSec - startSec;

            // -ss 在 -i 之前用于快速seek，再加 -ss 在 -i 之后做精确裁剪
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-y",
                    "-ss", String.format("%.3f", startSec),
                    "-i", sourcePath.toString(),
                    "-t", String.format("%.3f", duration),
                    "-c:v", "libx264", "-c:a", "aac",
                    "-avoid_negative_ts", "make_zero",
                    "-movflags", "+faststart",
                    outputPath.toString()
            );
            pb.redirectErrorStream(true);

            log.info("执行 FFmpeg: {}", String.join(" ", pb.command()));
            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("FFmpeg 失败: {}", output);
                throw new BusinessException("FFmpeg 裁剪失败，退出码: " + exitCode);
            }

            // 更新切片记录
            String relativePath = "processed/" + outputFilename;
            clip.setOutputPath(relativePath);
            clip.setClipStatus("DONE");
            clipMapper.updateById(clip);

            taskService.addLog(taskId, "INFO", "切片 #" + clip.getSortOrder() + " 渲染完成: " + outputFilename);
            log.info("切片渲染完成: taskId={}, clipId={}, output={}", taskId, clipId, outputPath);

            return clip;

        } catch (Exception e) {
            clip.setClipStatus("FAILED");
            clipMapper.updateById(clip);
            taskService.addLog(taskId, "ERROR", "切片 #" + clip.getSortOrder() + " 渲染失败: " + e.getMessage());
            throw new BusinessException("切片渲染失败: " + e.getMessage());
        }
    }

    /**
     * 批量渲染所有切片
     */
    public void renderAllClips(Long taskId) {
        MediaTask task = taskMapper.selectById(taskId);
        if (task == null) throw new BusinessException(404, "任务不存在");

        var clips = clipMapper.selectList(
                new LambdaQueryWrapper<ClipSuggestion>()
                        .eq(ClipSuggestion::getTaskId, taskId)
                        .orderByAsc(ClipSuggestion::getSortOrder));

        if (clips.isEmpty()) throw new BusinessException("没有切片建议可渲染");

        taskService.addLog(taskId, "INFO", "开始批量渲染 " + clips.size() + " 个切片");

        for (ClipSuggestion clip : clips) {
            try {
                renderClip(taskId, clip.getId());
            } catch (Exception e) {
                log.error("批量渲染中切片 {} 失败: {}", clip.getId(), e.getMessage());
                // 继续渲染下一个
            }
        }

        taskService.addLog(taskId, "INFO", "批量渲染完成");
    }

    /**
     * 获取切片的绝对路径（用于下载）
     */
    public Path getClipAbsolutePath(Long taskId, Long clipId) {
        ClipSuggestion clip = clipMapper.selectById(clipId);
        if (clip == null || !clip.getTaskId().equals(taskId)) {
            throw new BusinessException(404, "切片不存在");
        }
        if (!"DONE".equals(clip.getClipStatus()) || clip.getOutputPath() == null) {
            throw new BusinessException("切片尚未渲染");
        }
        // outputPath 格式: processed/xxx.mp4，基于 storage 目录解析
        Path storageDir = Paths.get(uploadDir).toAbsolutePath().normalize().getParent();
        if (storageDir == null) storageDir = Paths.get(".").toAbsolutePath().normalize();
        return storageDir.resolve(clip.getOutputPath()).normalize();
    }
}
