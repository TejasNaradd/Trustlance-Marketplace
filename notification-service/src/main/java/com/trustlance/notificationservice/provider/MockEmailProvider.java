package com.trustlance.notificationservice.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stub email provider for development. Replace with SendGrid/SES/SMTP in production.
 */
@Component
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
