package com.clip.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录频率限制服务
 * 5分钟内最多5次失败，锁定15分钟
 */
@Slf4j
@Service
public class RateLimitService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 5 * 60 * 1000;     // 5分钟窗口
    private static final long LOCKOUT_MS = 15 * 60 * 1000;   // 锁定15分钟

    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    /**
     * 记录一次登录失败
     */
    public void recordFailure(String key) {
        AttemptInfo info = attempts.computeIfAbsent(key, k -> new AttemptInfo());
        info.failureCount++;
        info.lastAttempt = System.currentTimeMillis();

        if (info.failureCount >= MAX_ATTEMPTS) {
            info.lockedUntil = System.currentTimeMillis() + LOCKOUT_MS;
            log.warn("登录频率限制: key={}, 锁定至 {}", key, info.lockedUntil);
        }
    }

    /**
     * 登录成功，清除记录
     */
    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    /**
     * 检查是否被锁定
     * @return null 表示可以登录，非 null 表示剩余锁定秒数
     */
    public Integer checkLocked(String key) {
        AttemptInfo info = attempts.get(key);
        if (info == null) return null;

        // 窗口过期，清除
        if (System.currentTimeMillis() - info.lastAttempt > WINDOW_MS && info.lockedUntil == 0) {
            attempts.remove(key);
            return null;
        }

        // 锁定中
        if (info.lockedUntil > 0 && System.currentTimeMillis() < info.lockedUntil) {
            long remainMs = info.lockedUntil - System.currentTimeMillis();
            return (int) (remainMs / 1000) + 1;
        }

        // 锁定已过期
        if (info.lockedUntil > 0 && System.currentTimeMillis() >= info.lockedUntil) {
            attempts.remove(key);
            return null;
        }

        return null;
    }

    /**
     * 获取剩余尝试次数
     */
    public int getRemainingAttempts(String key) {
        AttemptInfo info = attempts.get(key);
        if (info == null) return MAX_ATTEMPTS;
        return Math.max(0, MAX_ATTEMPTS - info.failureCount);
    }

    private static class AttemptInfo {
        int failureCount = 0;
        long lastAttempt = 0;
        long lockedUntil = 0;
    }
}
