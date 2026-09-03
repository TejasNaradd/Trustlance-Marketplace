package com.trustlance.notificationservice.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stub email provider — active when trustlance.notification.email.enabled=false (default).
 * Logs emails instead of sending them. Set SMTP_ENABLED=true in environment to switch to real SMTP.
 */
@Component
@ConditionalOnProperty(name = "trustlance.notification.email.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class MockEmailProvider implements EmailProvider {

    @Override
    public String getName() {
        return "mock";
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("[MockEmail] TO={} SUBJECT={} BODY={}", to, subject, body.substring(0, Math.min(body.length(), 100)));
    }
}
