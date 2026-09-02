package com.trustlance.marketplaceservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserClient {

    private final WebClient.Builder webClientBuilder;

    public record UserDto(UUID userId, String userName, String email, String role, String status) {}

    public Mono<UserDto> getUserById(UUID userId) {
        return webClientBuilder.build()
                .get()
                .uri("http://user-service/api/v1/users/{id}", userId)
                .retrieve()
                .bodyToMono(UserDto.class)
                .onErrorResume(e -> {
                    log.warn("Failed to fetch user {} from user-service: {}", userId, e.getMessage());
                    return Mono.empty();
                });
    }
}
