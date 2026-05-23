package com.clip.platform.runner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clip.platform.entity.MediaTask;
import com.clip.platform.mapper.MediaTaskMapper;
import com.clip.platform.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件清理服务
 * 定期清理过期的上传文件、下载文件、裁剪文件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupRunner {

    private final MediaTaskMapper taskMapper;
    private final FileStorageService fileStorageService;

    @Value("${app.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${app.cleanup.max-age-hours:72}")
    private int maxAgeHours;

    @Value("${app.cleanup.min-disk-gb:2}")
    private long minDiskGb;

    /**
     * 每小时检查一次，清理过期文件
     */
    @Scheduled(fixedDelay = 3600000) // 1小时
    public void cleanup() {
        if (!cleanupEnabled) return;

        log.info("开始文件清理检查...");
        int deletedCount = 0;

        try {
            // 1. 清理已完成/失败任务的原始文件（保留裁剪后的文件）
            deletedCount += cleanCompletedTaskFiles();

            // 2. 清理孤立文件（没有对应任务记录的文件）
            deletedCount += cleanOrphanFiles();

            // 3. 清理超过 maxAgeHours 的下载目录文件
            deletedCount += cleanOldDownloads();

            if (deletedCount > 0) {
                log.info("文件清理完成，共清理 {} 个文件", deletedCount);
            }

            // 4. 检查磁盘空间
            checkDiskSpace();

        } catch (Exception e) {
            log.error("文件清理出错: {}", e.getMessage());
        }
    }

    /**
     * 手动触发清理（API 调用）
     */
    public int manualCleanup() {
        log.info("手动触发文件清理");
        int count = 0;
        count += cleanCompletedTaskFiles();
        count += cleanOrphanFiles();
        count += cleanOldDownloads();
        return count;
    }

    /**
     * 清理已完成任务的原始上传文件（裁剪文件保留）
     */
    private int cleanCompletedTaskFiles() {
        int count = 0;
        List<MediaTask> completedTasks = taskMapper.selectList(
                new LambdaQueryWrapper<MediaTask>()
                        .in(MediaTask::getStatus, "COMPLETED", "FAILED")
                        .isNotNull(MediaTask::getFilePath));

        for (MediaTask task : completedTasks) {
            try {
                Path filePath = fileStorageService.getAbsolutePath(task.getFilePath());
                if (Files.exists(filePath)) {
                    // 检查文件年龄
                    BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                    LocalDateTime modified = LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
                    if (modified.isBefore(LocalDateTime.now().minusHours(maxAgeHours))) {
                        Files.deleteIfExists(filePath);
                        // 也删除提取的音频文件
                        String audioPath = task.getFilePath().replaceAll("\\.[^.]+$", ".wav");
                        Files.deleteIfExists(fileStorageService.getAbsolutePath(audioPath));
                        count++;
                        log.debug("清理任务文件: taskId={}, path={}", task.getId(), task.getFilePath());
                    }
                }
            } catch (Exception e) {
                log.warn("清理任务文件失败: taskId={}, error={}", task.getId(), e.getMessage());
            }
        }
        return count;
    }

    /**
     * 清理孤立文件（downloads 目录中没有对应任务的文件）
     */
    private int cleanOrphanFiles() {
        int count = 0;
        try {
            Path downloadsDir = fileStorageService.getAbsolutePath("downloads").getParent()
                    .resolve("downloads");
            if (!Files.exists(downloadsDir)) return 0;

            // 获取所有任务的文件路径
            List<String> taskPaths = taskMapper.selectList(null).stream()
                    .map(MediaTask::getFilePath)
                    .filter(p -> p != null)
                    .toList();

            int maxAge = maxAgeHours;
            List<Path> toDelete = new ArrayList<>();
            Files.walkFileTree(downloadsDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String relativePath = "downloads/" + file.getFileName().toString();
                    if (!taskPaths.contains(relativePath)) {
                        LocalDateTime modified = LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
                        if (modified.isBefore(LocalDateTime.now().minusHours(maxAge))) {
                            toDelete.add(file);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            for (Path file : toDelete) {
                try {
                    Files.deleteIfExists(file);
                    count++;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.warn("清理孤立文件失败: {}", e.getMessage());
        }
        return count;
    }

    /**
     * 清理超过 maxAgeHours 的 processed 目录文件
     */
    private int cleanOldDownloads() {
        java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);
        try {
            Path processedDir = fileStorageService.getAbsolutePath("processed").getParent()
                    .resolve("processed");
            if (!Files.exists(processedDir)) return 0;

            int maxAge = maxAgeHours * 2; // 裁剪文件保留更久
            Files.walkFileTree(processedDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    LocalDateTime modified = LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
                    if (modified.isBefore(LocalDateTime.now().minusHours(maxAge))) {
                        try { Files.delete(file); count.incrementAndGet(); } catch (Exception ignored) {}
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            log.warn("清理裁剪文件失败: {}", e.getMessage());
        }
        return count.get();
    }

    /**
     * 检查磁盘空间
     */
    private void checkDiskSpace() {
        try {
            Path storageDir = fileStorageService.getAbsolutePath("").getParent();
            long freeSpaceGb = Files.getFileStore(storageDir).getUsableSpace() / (1024 * 1024 * 1024);
            if (freeSpaceGb < minDiskGb) {
                log.warn("⚠️ 磁盘空间不足！剩余 {}GB，最低要求 {}GB", freeSpaceGb, minDiskGb);
                // 紧急清理：删除所有已完成任务的原始文件
                cleanCompletedTaskFiles();
            }
        } catch (Exception e) {
            log.warn("检查磁盘空间失败: {}", e.getMessage());
        }
    }

    /**
     * 获取存储使用情况
     */
    public StorageInfo getStorageInfo() {
        try {
            Path storageDir = fileStorageService.getAbsolutePath("").getParent();
            long totalSpace = Files.getFileStore(storageDir).getTotalSpace();
            long usableSpace = Files.getFileStore(storageDir).getUsableSpace();
            long usedSpace = totalSpace - usableSpace;

            long uploadSize = getDirSize(fileStorageService.getAbsolutePath("").getParent().resolve("uploads"));
            long processedSize = getDirSize(fileStorageService.getAbsolutePath("").getParent().resolve("processed"));
            long downloadsSize = getDirSize(fileStorageService.getAbsolutePath("").getParent().resolve("downloads"));

            return new StorageInfo(
                    totalSpace / (1024 * 1024),   // MB
                    usedSpace / (1024 * 1024),
                    usableSpace / (1024 * 1024),
                    uploadSize / (1024 * 1024),
                    processedSize / (1024 * 1024),
                    downloadsSize / (1024 * 1024)
            );
        } catch (Exception e) {
            return new StorageInfo(0, 0, 0, 0, 0, 0);
        }
    }

    private long getDirSize(Path dir) {
        if (!Files.exists(dir)) return 0;
        try {
            long[] size = {0};
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    size[0] += attrs.size();
                    return FileVisitResult.CONTINUE;
                }
            });
            return size[0];
        } catch (Exception e) {
            return 0;
        }
    }

    public record StorageInfo(
            long totalMb, long usedMb, long freeMb,
            long uploadMb, long processedMb, long downloadsMb
    ) {}
}
