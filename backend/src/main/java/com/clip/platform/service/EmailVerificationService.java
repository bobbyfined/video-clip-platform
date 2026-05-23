package com.clip.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 邮箱验证服务
 * 生成验证链接，验证 Token
 */
@Slf4j
@Service
public class EmailVerificationService {

    @Value("${app.jwt.secret:myDefaultSecretKeyForJwtTokenGeneration2024}")
    private String jwtSecret;

    @Value("${app.url:http://localhost:5173}")
    private String appUrl;

    // 存储: token -> {email, expireTime}
    private final ConcurrentHashMap<String, VerifyEntry> tokenStore = new ConcurrentHashMap<>();

    /**
     * 生成邮箱验证链接
     */
    public String generateVerifyLink(String email) {
        String token = generateToken(email);
        tokenStore.put(token, new VerifyEntry(email, System.currentTimeMillis() + 24 * 60 * 60 * 1000)); // 24小时有效
        String link = appUrl + "/verify?token=" + token;
        log.info("邮箱验证链接: email={}, link={}", email, link);
        return link;
    }

    /**
     * 验证 Token
     * @return 验证的邮箱，null 表示无效
     */
    public String verifyToken(String token) {
        if (token == null) return null;
        VerifyEntry entry = tokenStore.remove(token);
        if (entry == null) return null;
        if (System.currentTimeMillis() > entry.expireTime) return null;
        return entry.email;
    }

    private String generateToken(String email) {
        try {
            String raw = email + ":" + System.currentTimeMillis() + ":" + jwtSecret;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 32);
        } catch (Exception e) {
            return java.util.UUID.randomUUID().toString().replace("-", "");
        }
    }

    private static class VerifyEntry {
        String email;
        long expireTime;
        VerifyEntry(String email, long expireTime) {
            this.email = email;
            this.expireTime = expireTime;
        }
    }
}
