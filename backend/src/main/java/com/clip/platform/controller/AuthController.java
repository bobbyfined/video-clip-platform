package com.clip.platform.controller;

import com.clip.platform.common.Result;
import com.clip.platform.dto.request.LoginRequest;
import com.clip.platform.dto.request.RegisterRequest;
import com.clip.platform.dto.response.AuthResponse;
import com.clip.platform.dto.response.UserResponse;
import com.clip.platform.security.SecurityUtils;
import com.clip.platform.service.AuthService;
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

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
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

    /**
     * 邮箱验证
     */
    @GetMapping("/verify")
    public Result<String> verifyEmail(@RequestParam String token) {
        String email = emailVerificationService.verifyToken(token);
        if (email == null) {
            return Result.fail("验证链接无效或已过期");
        }
        return Result.ok("邮箱验证成功：" + email);
    }
}
