package com.clip.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clip.platform.common.BusinessException;
import com.clip.platform.dto.request.LoginRequest;
import com.clip.platform.dto.request.RegisterRequest;
import com.clip.platform.dto.response.AuthResponse;
import com.clip.platform.dto.response.UserResponse;
import com.clip.platform.entity.User;
import com.clip.platform.mapper.UserMapper;
import com.clip.platform.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CaptchaService captchaService;
    private final RateLimitService rateLimitService;
    private final EmailVerificationService emailVerificationService;

    /**
     * 用户注册
     */
    public UserResponse register(RegisterRequest request) {
        // 验证码校验
        if (!captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode())) {
            throw new BusinessException("验证码错误或已过期");
        }

        // 密码强度校验
        validatePassword(request.getPassword());

        // 检查邮箱是否已存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (count > 0) {
            throw new BusinessException("该邮箱已被注册");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getEmail().split("@")[0]);
        user.setRole("USER");
        user.setPlan("FREE");
        userMapper.insert(user);

        // 生成验证链接（日志输出，后续可接入邮件发送）
        String verifyLink = emailVerificationService.generateVerifyLink(request.getEmail());
        log.info("用户注册成功，验证链接: email={}, link={}", request.getEmail(), verifyLink);

        return UserResponse.fromEntity(user);
    }

    /**
     * 用户登录
     */
    public AuthResponse login(LoginRequest request) {
        String rateLimitKey = "login:" + request.getEmail();

        // 频率限制检查
        Integer lockedSeconds = rateLimitService.checkLocked(rateLimitKey);
        if (lockedSeconds != null) {
            throw new BusinessException("登录失败次数过多，请 " + lockedSeconds + " 秒后再试");
        }

        // 验证码校验
        if (!captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode())) {
            rateLimitService.recordFailure(rateLimitKey);
            throw new BusinessException("验证码错误或已过期");
        }

        try {
            // 认证
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (Exception e) {
            rateLimitService.recordFailure(rateLimitKey);
            int remaining = rateLimitService.getRemainingAttempts(rateLimitKey);
            throw new BusinessException("邮箱或密码错误，剩余 " + remaining + " 次尝试机会");
        }

        // 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (user == null) {
            rateLimitService.recordFailure(rateLimitKey);
            throw new BusinessException("用户不存在");
        }

        // 登录成功，清除记录
        rateLimitService.recordSuccess(rateLimitKey);

        // 生成 Token
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole());

        return new AuthResponse(token, UserResponse.fromEntity(user));
    }

    /**
     * 获取当前用户信息
     */
    public UserResponse getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return UserResponse.fromEntity(user);
    }

    /**
     * 密码强度校验
     */
    private void validatePassword(String password) {
        if (password.length() < 8) {
            throw new BusinessException("密码长度至少8个字符");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new BusinessException("密码必须包含小写字母");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new BusinessException("密码必须包含大写字母");
        }
        if (!password.matches(".*\\d.*")) {
            throw new BusinessException("密码必须包含数字");
        }
    }
}
