package com.clip.platform.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 验证码登录请求（邮箱+邮箱验证码）
 */
@Data
public class LoginCodeRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "邮箱验证码不能为空")
    private String emailCode;
}
