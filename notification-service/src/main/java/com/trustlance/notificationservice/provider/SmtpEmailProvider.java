package com.trustlance.notificationservice.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP-backed email provider using Spring's JavaMailSender.
 * Activated when trustlance.notification.email.enabled=true.
 * Configure via environment variables: SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD, SMTP_FROM.
 */
@Component
@ConditionalOnProperty(name = "trustlance.notification.email.enabled", havingValue = "true")
@Slf4j
public class SmtpEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;

    @Value("${trustlance.notification.email.from:noreply@trustlance.com}")
    private String fromAddress;

    public SmtpEmailProvider(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        log.info("SmtpEmailProvider initialized — real email delivery is ENABLED");
    }

    @Override
    public String getName() {
        return "smtp";
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("[SmtpEmail] Sent email to={} subject={}", to, subject);
        } catch (Exception e) {
            log.error("[SmtpEmail] Failed to send email to={} subject={}: {}", to, subject, e.getMessage(), e);
            throw new RuntimeException("Email delivery failed: " + e.getMessage(), e);
        }
    }
}
