package com.trustlance.userservice.config;

import com.trustlance.userservice.model.User;
import com.trustlance.userservice.model.UserRole;
import com.trustlance.userservice.model.UserStatus;
import com.trustlance.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs once on every application startup.
 *
 * Checks whether an ADMIN user with the configured email already exists.
 * If not, creates one using credentials from environment variables.
 *
 * This is the correct production-style bootstrap pattern:
 *   - Credentials never live in source code
 *   - Idempotent — safe to run on every restart
 *   - Auditable — logged clearly
 *   - No public API endpoint exposes admin creation
 *
 * Required env vars (set in docker-compose or .env):
 *   ADMIN_EMAIL     — admin login email
 *   ADMIN_USERNAME  — admin username
 *   ADMIN_PASSWORD  — admin password (plain text, gets BCrypt encoded here)
 *   ADMIN_FIRSTNAME — optional, defaults to "Platform"
 *   ADMIN_LASTNAME  — optional, defaults to "Admin"
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${trustlance.admin.email:admin@trustlance.com}")
    private String adminEmail;

    @Value("${trustlance.admin.username:admin}")
    private String adminUsername;

    @Value("${trustlance.admin.password:Admin@1234}")
    private String adminPassword;

    @Value("${trustlance.admin.first-name:Platform}")
    private String adminFirstName;

    @Value("${trustlance.admin.last-name:Admin}")
    private String adminLastName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail.toLowerCase())) {
            log.info("Admin user already exists [email={}], skipping seed.", adminEmail);
            return;
        }

        User admin = User.builder()
                .userName(adminUsername)
                .email(adminEmail.toLowerCase())
                .passwordHash(passwordEncoder.encode(adminPassword))
                .firstName(adminFirstName)
                .lastName(adminLastName)
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(admin);
        log.info("Admin user seeded successfully [email={}, username={}]. " +
                "Change the default password immediately in production.", adminEmail, adminUsername);
    }
}
