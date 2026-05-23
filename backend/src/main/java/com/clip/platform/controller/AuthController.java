package com.clip.platform.controller;

import com.clip.platform.common.Result;
import com.clip.platform.dto.request.LoginRequest;
import com.clip.platform.dto.request.RegisterRequest;
import com.clip.platform.dto.response.AuthResponse;
import com.clip.platform.dto.response.UserResponse;
import com.clip.platform.security.SecurityUtils;
import com.clip.platform.service.AuthService;
import com.clip.platform.service.EmailSenderService;
import com.clip.platform.service.EmailVerificationService;
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
    private final EmailVerificationService emailVerificationService;
    private final EmailSenderService emailSenderService;

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/send-code")
    public Result<String> sendCode(@RequestParam String email) {
        String code = emailVerificationService.generateCode(email);
        emailSenderService.sendVerificationCode(email, code);
        return Result.ok("验证码已发送");
    }

    /**
     * 用户注册（带邮箱验证码）
     */
    @PostMapping("/register")
    public Result<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        // 验证邮箱验证码
        if (!emailVerificationService.verifyCode(request.getEmail(), request.getEmailCode())) {
            return Result.fail("邮箱验证码错误或已过期");
        }
        return Result.ok(authService.register(request));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
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
