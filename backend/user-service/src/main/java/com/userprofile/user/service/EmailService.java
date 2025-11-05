package com.userprofile.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * 邮件服务
 * 解决P1-2: TODO未实现 - 邮件发送功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@userprofile.com}")
    private String fromEmail;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    /**
     * 发送简单文本邮件
     */
    public void sendSimpleEmail(String to, String subject, String content) {
        if (!mailEnabled) {
            log.info("邮件功能未启用,跳过发送: to={}, subject={}", to, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("邮件发送成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
            // 不抛出异常,避免影响主流程
        }
    }

    /**
     * 发送HTML邮件
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!mailEnabled) {
            log.info("邮件功能未启用,跳过发送: to={}, subject={}", to, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true表示HTML格式

            mailSender.send(message);
            log.info("HTML邮件发送成功: to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("HTML邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
        } catch (Exception e) {
            log.error("邮件发��异常: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
        }
    }

    /**
     * 发送欢迎邮件
     */
    public void sendWelcomeEmail(String to, String username) {
        String subject = "欢迎加入用户画像系统";
        String content = buildWelcomeEmailContent(username);
        sendHtmlEmail(to, subject, content);
    }

    /**
     * 发送密码重置邮件
     */
    public void sendPasswordResetEmail(String to, String username, String resetToken) {
        String subject = "密码重置请求";
        String content = buildPasswordResetEmailContent(username, resetToken);
        sendHtmlEmail(to, subject, content);
    }

    /**
     * 发送账号激活邮件
     */
    public void sendActivationEmail(String to, String username, String activationToken) {
        String subject = "激活您的账号";
        String content = buildActivationEmailContent(username, activationToken);
        sendHtmlEmail(to, subject, content);
    }

    /**
     * 构建欢迎邮件内容
     */
    private String buildWelcomeEmailContent(String username) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                        .footer { text-align: center; margin-top: 20px; color: #888; font-size: 12px; }
                        .button { display: inline-block; padding: 12px 30px; background: #667eea; color: white; text-decoration: none; border-radius: 5px; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>欢迎加入!</h1>
                        </div>
                        <div class="content">
                            <h2>你好, %s!</h2>
                            <p>感谢您注册用户画像系统。我们很高兴您能加入我们!</p>

                            <h3>接下来您可以:</h3>
                            <ul>
                                <li>完善您的个人资料</li>
                                <li>探索系统功能</li>
                                <li>查看个性化推荐</li>
                                <li>管理您的标签</li>
                            </ul>

                            <p>如果您有任何问题,请随时联系我们的支持团队。</p>

                            <a href="#" class="button">开始使用</a>
                        </div>
                        <div class="footer">
                            <p>此邮件由系统自动发送,请勿回复。</p>
                            <p>&copy; 2024 用户画像系统. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username);
    }

    /**
     * 构建密码重置邮件内容
     */
    private String buildPasswordResetEmailContent(String username, String resetToken) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #f44336; color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                        .token { background: #fff; border: 2px dashed #667eea; padding: 15px; font-family: monospace; font-size: 18px; text-align: center; margin: 20px 0; }
                        .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0; }
                        .footer { text-align: center; margin-top: 20px; color: #888; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>密码重置请求</h1>
                        </div>
                        <div class="content">
                            <h2>你好, %s!</h2>
                            <p>我们收到了您的密码重置请求。请使用以下重置码来重置您的密码:</p>

                            <div class="token">%s</div>

                            <div class="warning">
                                <strong>⚠️ 安全提示:</strong>
                                <ul>
                                    <li>此重置码将在30分钟后过期</li>
                                    <li>如果不是您本人操作,请忽略此邮件</li>
                                    <li>请勿将此重置码分享给他人</li>
                                </ul>
                            </div>

                            <p>如果您没有请求重置密码,请立即联系我们的支持团队。</p>
                        </div>
                        <div class="footer">
                            <p>此邮件由系统自动发送,请勿回复。</p>
                            <p>&copy; 2024 用户画像系统. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username, resetToken);
    }

    /**
     * 构建账号激活邮件内容
     */
    private String buildActivationEmailContent(String username, String activationToken) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #4caf50; color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                        .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                        .button { display: inline-block; padding: 12px 30px; background: #4caf50; color: white; text-decoration: none; border-radius: 5px; margin-top: 20px; }
                        .footer { text-align: center; margin-top: 20px; color: #888; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>激活您的账号</h1>
                        </div>
                        <div class="content">
                            <h2>你好, %s!</h2>
                            <p>感谢您注册用户画像系统!请点击下面的按钮激活您的账号:</p>

                            <a href="#" class="button">激活账号</a>

                            <p style="margin-top: 20px;">或复制以下链接到浏览器:</p>
                            <p style="color: #667eea;">https://userprofile.com/activate?token=%s</p>

                            <p style="margin-top: 30px; color: #888;">此激活链接将在24小时后过期。</p>
                        </div>
                        <div class="footer">
                            <p>此邮件由系统自动发送,请勿回复。</p>
                            <p>&copy; 2024 用户画像系统. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username, activationToken);
    }
}
