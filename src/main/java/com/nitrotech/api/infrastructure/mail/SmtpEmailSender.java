package com.nitrotech.api.infrastructure.mail;

import com.nitrotech.api.domain.auth.usecase.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;

    @Value("${mail.from}")
    private String from;

    public SmtpEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void sendPasswordReset(String to, String resetLink) {
        send(to, "Reset your password",
                "<p>Click the link below to reset your password (expires in 15 minutes):</p>" +
                "<p><a href=\"" + resetLink + "\">Reset Password</a></p>" +
                "<p>If you didn't request this, ignore this email.</p>");
    }

    @Override
    @Async
    public void sendVerificationEmail(String to, String verifyLink) {
        send(to, "Verify your email address",
                "<p>Click the link below to verify your email address (expires in 24 hours):</p>" +
                "<p><a href=\"" + verifyLink + "\">Verify Email</a></p>" +
                "<p>If you didn't create an account, ignore this email.</p>");
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
