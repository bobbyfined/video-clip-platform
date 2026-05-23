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

    /**
     * 用户注册（邮箱+邮箱验证码+密码，不需要图片验证码）
     */
    public UserResponse register(RegisterRequest request) {
        validatePassword(request.getPassword());

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

        log.info("用户注册成功: email={}", request.getEmail());
        return UserResponse.fromEntity(user);
    }

    /**
     * 密码登录（邮箱+密码+图片验证码）
     */
    public AuthResponse login(LoginRequest request) {
        String rateLimitKey = "login:" + request.getEmail();

        Integer lockedSeconds = rateLimitService.checkLocked(rateLimitKey);
        if (lockedSeconds != null) {
            throw new BusinessException("登录失败次数过多，请 " + lockedSeconds + " 秒后再试");
        }

        // 图片验证码校验
        if (!captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode())) {
            rateLimitService.recordFailure(rateLimitKey);
            throw new BusinessException("验证码错误或已过期");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (Exception e) {
            rateLimitService.recordFailure(rateLimitKey);
            int remaining = rateLimitService.getRemainingAttempts(rateLimitKey);
            throw new BusinessException("邮箱或密码错误，剩余 " + remaining + " 次尝试机会");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, request.getEmail()));
        if (user == null) {
            rateLimitService.recordFailure(rateLimitKey);
            throw new BusinessException("用户不存在");
        }

        rateLimitService.recordSuccess(rateLimitKey);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, UserResponse.fromEntity(user));
    }

    /**
     * 验证码登录（邮箱+邮箱验证码，不需要图片验证码）
     */
    public AuthResponse loginByCode(String email) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new BusinessException("该邮箱未注册");
        }

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

    private void validatePassword(String password) {
        if (password.length() < 8) throw new BusinessException("密码长度至少8个字符");
        if (!password.matches(".*[a-z].*")) throw new BusinessException("密码必须包含小写字母");
        if (!password.matches(".*[A-Z].*")) throw new BusinessException("密码必须包含大写字母");
        if (!password.matches(".*\\d.*")) throw new BusinessException("密码必须包含数字");
    }
}
