package com.clip.platform.controller;

import com.clip.platform.common.Result;
import com.clip.platform.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 验证码控制器
 */
@RestController
@RequestMapping("/api/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    /**
     * 获取验证码图片
     * @return {captchaId, image (base64)}
     */
    @GetMapping
    public Result<Map<String, String>> getCaptcha() {
        String[] result = captchaService.generateCaptcha();
        return Result.ok(Map.of("captchaId", result[0], "image", result[1]));
    }
}
