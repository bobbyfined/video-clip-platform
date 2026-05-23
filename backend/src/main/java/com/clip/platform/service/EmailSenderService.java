package com.clip.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务
 */
@Slf4j
@Service
public class EmailSenderService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:}")
    private String fromEmail;

    @Value("${spring.mail.username:}")
    private String username;

    public EmailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 发送验证码邮件
     */
    public void sendVerificationCode(String toEmail, String code) {
        if (!mailEnabled) {
            log.info("[邮件未启用] 验证码: email={}, code={}", toEmail, code);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail.isEmpty() ? username : fromEmail);
            message.setTo(toEmail);
            message.setSubject("【VideoClip】邮箱验证码");
            message.setText(String.format(
                    "您好！\n\n" +
                    "您的邮箱验证码是：%s\n\n" +
                    "验证码 10 分钟内有效，请勿泄露给他人。\n\n" +
                    "如非本人操作，请忽略此邮件。\n\n" +
                    "—— VideoClip 视频切片助手", code));
            mailSender.send(message);
            log.info("验证码邮件已发送: email={}", toEmail);
        } catch (Exception e) {
            log.error("邮件发送失败: email={}, error={}", toEmail, e.getMessage());
            // 不抛异常，降级为日志输出
            log.info("[降级] 验证码: email={}, code={}", toEmail, code);
        }
    }
}
