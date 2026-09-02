package com.trustlance.notificationservice.provider;

/**
 * Email provider adapter — swap implementations for different email providers.
 */
public interface EmailProvider {

    String getName();

    void sendEmail(String to, String subject, String body);
}
