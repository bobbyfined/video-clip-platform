package com.clip.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮箱验证服务
 * 生成验证码并发送验证邮件
 */
@Slf4j
@Service
public class EmailVerificationService {

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.url:http://localhost:5173}")
    private String appUrl;

    // 存储: email -> {code, expireTime}
    private final ConcurrentHashMap<String, VerifyEntry> codeStore = new ConcurrentHashMap<>();

    /**
     * 生成6位验证码
     */
    public String generateCode(String email) {
        String code = String.format("%06d", (int) (Math.random() * 1000000));
        codeStore.put(email.toLowerCase(), new VerifyEntry(code, System.currentTimeMillis() + 10 * 60 * 1000)); // 10分钟有效
        log.info("邮箱验证码: email={}, code={}", email, code);
        return code;
    }

    /**
     * 验证码校验
     */
    public boolean verifyCode(String email, String code) {
        if (email == null || code == null) return false;
        VerifyEntry entry = codeStore.get(email.toLowerCase());
        if (entry == null) return false;
        if (System.currentTimeMillis() > entry.expireTime) {
            codeStore.remove(email.toLowerCase());
            return false;
        }
        boolean matched = entry.code.equals(code.trim());
        if (matched) codeStore.remove(email.toLowerCase());
        return matched;
    }

    /**
     * 生成验证链接（用于邮件中的链接点击验证）
     */
    public String generateVerifyLink(String email) {
        String token = generateToken(email);
        return appUrl + "/verify?token=" + token;
    }

    /**
     * 是否启用了邮件发送
     */
    public boolean isMailEnabled() {
        return mailEnabled;
    }

    private String generateToken(String email) {
        try {
            String raw = email + ":" + System.currentTimeMillis() + ":" + Math.random();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString().substring(0, 32);
        } catch (Exception e) {
            return java.util.UUID.randomUUID().toString().replace("-", "");
        }
    }

    private static class VerifyEntry {
        String code;
        long expireTime;
        VerifyEntry(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
        }
    }
}
