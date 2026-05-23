-- 媒体任务表增加 LLM 提供商字段
ALTER TABLE `media_tasks`
    ADD COLUMN `llm_provider` VARCHAR(20) DEFAULT NULL COMMENT 'LLM提供商: deepseek/mimo';
