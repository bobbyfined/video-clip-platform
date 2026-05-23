package com.clip.platform.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发送邮箱验证码请求（带图片验证码拦截）
 */
@Data
public class SendCodeRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "图片验证码不能为空")
    private String captchaCode;

    @NotBlank(message = "验证码ID不能为空")
    private String captchaId;
}
