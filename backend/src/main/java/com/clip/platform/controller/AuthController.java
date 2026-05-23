package com.clip.platform.controller;

import com.clip.platform.common.Result;
import com.clip.platform.dto.request.*;
import com.clip.platform.dto.response.AuthResponse;
import com.clip.platform.dto.response.UserResponse;
import com.clip.platform.security.SecurityUtils;
import com.clip.platform.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final EmailVerificationService emailVerificationService;
    private final EmailSenderService emailSenderService;

    /**
     * 发送邮箱验证码（图片验证码拦截）
     */
    @PostMapping("/send-code")
    public Result<String> sendCode(@Valid @RequestBody SendCodeRequest request) {
        // 图片验证码校验
        if (!captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode())) {
            return Result.fail("图片验证码错误或已过期");
        }
        String code = emailVerificationService.generateCode(request.getEmail());
        emailSenderService.sendVerificationCode(request.getEmail(), code);
        return Result.ok("验证码已发送");
    }

    /**
     * 用户注册（邮箱+邮箱验证码+密码）
     */
    @PostMapping("/register")
    public Result<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (!emailVerificationService.verifyCode(request.getEmail(), request.getEmailCode())) {
            return Result.fail("邮箱验证码错误或已过期");
        }
        return Result.ok(authService.register(request));
    }

    /**
     * 密码登录（邮箱+密码+图片验证码）
     */
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /**
     * 验证码登录（邮箱+邮箱验证码）
     */
    @PostMapping("/login-code")
    public Result<AuthResponse> loginByCode(@Valid @RequestBody LoginCodeRequest request) {
        if (!emailVerificationService.verifyCode(request.getEmail(), request.getEmailCode())) {
            return Result.fail("邮箱验证码错误或已过期");
        }
        return Result.ok(authService.loginByCode(request.getEmail()));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<UserResponse> me() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.ok(authService.getCurrentUser(userId));
    }
}
