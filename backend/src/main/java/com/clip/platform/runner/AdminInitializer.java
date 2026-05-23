package com.clip.platform.runner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clip.platform.entity.User;
import com.clip.platform.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时检查并创建默认管理员账号
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Long adminCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, "ADMIN"));
        if (adminCount == 0) {
            User admin = new User();
            admin.setEmail("admin@videoclip.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123456"));
            admin.setNickname("管理员");
            admin.setRole("ADMIN");
            admin.setPlan("PRO");
            userMapper.insert(admin);
            log.info("默认管理员账号已创建: admin@videoclip.com / admin123456");
        }
    }
}
