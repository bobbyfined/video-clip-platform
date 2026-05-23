-- =============================================
-- VideoClip Platform - 初始化数据库
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `email` VARCHAR(255) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `nickname` VARCHAR(100) DEFAULT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER',
    `plan` VARCHAR(20) NOT NULL DEFAULT 'FREE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 媒体任务表
CREATE TABLE IF NOT EXISTS `media_tasks` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(255) DEFAULT NULL,
    `original_filename` VARCHAR(500) NOT NULL,
    `file_path` VARCHAR(1000) NOT NULL,
    `file_size` BIGINT NOT NULL DEFAULT 0,
    `mime_type` VARCHAR(100) NOT NULL,
    `duration_seconds` INT DEFAULT NULL,
    `content_type` VARCHAR(50) DEFAULT NULL,
    `target_platform` VARCHAR(50) DEFAULT NULL,
    `clip_count` INT NOT NULL DEFAULT 5,
    `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    `progress_stage` VARCHAR(50) DEFAULT NULL,
    `error_message` TEXT DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `completed_at` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_media_tasks_user_id` (`user_id`),
    KEY `idx_media_tasks_status` (`status`),
    CONSTRAINT `fk_tasks_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 转写片段表
CREATE TABLE IF NOT EXISTS `transcript_segments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `start_seconds` DOUBLE NOT NULL,
    `end_seconds` DOUBLE NOT NULL,
    `text` TEXT NOT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_segments_task_id` (`task_id`),
    CONSTRAINT `fk_segments_task` FOREIGN KEY (`task_id`) REFERENCES `media_tasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 分析结果表
CREATE TABLE IF NOT EXISTS `analysis_results` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `summary_short` TEXT DEFAULT NULL,
    `summary_long` TEXT DEFAULT NULL,
    `key_points` JSON DEFAULT NULL,
    `golden_quotes` JSON DEFAULT NULL,
    `raw_model_output` JSON DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_analysis_task_id` (`task_id`),
    CONSTRAINT `fk_analysis_task` FOREIGN KEY (`task_id`) REFERENCES `media_tasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 切片建议表
CREATE TABLE IF NOT EXISTS `clip_suggestions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `start_seconds` DOUBLE NOT NULL,
    `end_seconds` DOUBLE NOT NULL,
    `topic` VARCHAR(255) NOT NULL,
    `title1` VARCHAR(255) NOT NULL,
    `title2` VARCHAR(255) DEFAULT NULL,
    `summary` TEXT DEFAULT NULL,
    `hook_reason` TEXT DEFAULT NULL,
    `suggested_platform` VARCHAR(50) DEFAULT NULL,
    `editing_notes` TEXT DEFAULT NULL,
    `score` INT DEFAULT NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_clips_task_id` (`task_id`),
    CONSTRAINT `fk_clips_task` FOREIGN KEY (`task_id`) REFERENCES `media_tasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 任务日志表
CREATE TABLE IF NOT EXISTS `task_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `level` VARCHAR(10) NOT NULL DEFAULT 'INFO',
    `message` TEXT NOT NULL,
    `meta` JSON DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_logs_task_id` (`task_id`),
    CONSTRAINT `fk_logs_task` FOREIGN KEY (`task_id`) REFERENCES `media_tasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
