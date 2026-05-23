package com.clip.platform.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务
 */
@Slf4j
@Service
public class CaptchaService {

    // 验证码存储：key=captchaId, value={code, expireTime}
    private final ConcurrentHashMap<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        // 每分钟清理过期验证码
        scheduler.scheduleAtFixedRate(this::cleanExpired, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * 生成验证码
     * @return [captchaId, base64Image]
     */
    public String[] generateCaptcha() {
        ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(150, 40, 4, 2);
        String code = captcha.getCode();
        String base64 = captcha.getImageBase64Data();

        String captchaId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 存储，5分钟过期
        captchaStore.put(captchaId, new CaptchaEntry(code.toLowerCase(), System.currentTimeMillis() + 5 * 60 * 1000));

        return new String[]{captchaId, "data:image/png;base64," + base64};
    }

    /**
     * 验证验证码
     * @param captchaId 验证码ID
     * @param userInput 用户输入
     * @return 是否正确
     */
    public boolean verifyCaptcha(String captchaId, String userInput) {
        if (captchaId == null || userInput == null) return false;

        CaptchaEntry entry = captchaStore.remove(captchaId);
        if (entry == null) return false;

        if (System.currentTimeMillis() > entry.expireTime) return false;

        return entry.code.equals(userInput.toLowerCase().trim());
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        captchaStore.entrySet().removeIf(e -> now > e.getValue().expireTime);
    }

    private static class CaptchaEntry {
        String code;
        long expireTime;
        CaptchaEntry(String code, long expireTime) {
            this.code = code;
            this.expireTime = expireTime;
        }
    }
}
