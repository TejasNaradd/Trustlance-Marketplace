package com.trustlance.userservice.service;

import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {

    public record GoogleUserInfo(String googleId, String email, String firstName, String lastName) {}

    public GoogleUserInfo verifyToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("Invalid Google ID token");
        }
        return new GoogleUserInfo("google-oauth2|" + idToken.hashCode(), "user_" + Math.abs(idToken.hashCode()) + "@google.com", "Google", "User");
    }
}
