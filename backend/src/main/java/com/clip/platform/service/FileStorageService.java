package com.clip.platform.service;

import com.clip.platform.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * 文件存储服务
 */
@Slf4j
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "mp4", "mov", "avi", "mkv", "webm",
            "mp3", "wav", "m4a", "flac", "aac"
    );

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir:./storage/uploads}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new BusinessException("创建上传目录失败: " + e.getMessage());
        }
    }

    /**
     * 存储上传文件
     * @return 相对文件路径
     */
    public String store(MultipartFile file, String subDir) {
        // 校验文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }

        String ext = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("不支持的文件格式: " + ext);
        }

        // 生成唯一文件名
        String newFilename = UUID.randomUUID() + "." + ext;
        Path targetDir = uploadDir.resolve(subDir);
        try {
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(newFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件存储成功: {}", targetPath);
            // 返回相对路径（subDir/filename）
            return subDir + "/" + newFilename;
        } catch (IOException e) {
            throw new BusinessException("文件存储失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件的绝对路径
     */
    public Path getAbsolutePath(String relativePath) {
        return uploadDir.resolve(relativePath).normalize();
    }

    /**
     * 获取文件扩展名
     */
    public String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0) return "";
        return filename.substring(lastDot + 1);
    }

    /**
     * 判断是否为视频文件
     */
    public boolean isVideoFile(String filename) {
        String ext = getFileExtension(filename).toLowerCase();
        return Set.of("mp4", "mov", "avi", "mkv", "webm").contains(ext);
    }
}
