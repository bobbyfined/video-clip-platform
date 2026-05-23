package com.clip.platform.security;

import com.clip.platform.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类 - 获取当前登录用户
 */
public class SecurityUtils {

    public static UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new BusinessException(401, "未登录");
        }
        return (UserPrincipal) auth.getPrincipal();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public static boolean isAdmin() {
        try {
            return "ADMIN".equals(getCurrentUser().getRole());
        } catch (Exception e) {
            return false;
        }
    }
}
