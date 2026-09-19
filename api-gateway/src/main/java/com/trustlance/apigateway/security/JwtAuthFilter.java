package com.trustlance.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${trustlance.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    private static final List<String> PUBLIC_AUTH_ENDPOINTS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/google",
            "/api/v1/auth/refresh"
    );

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/actuator/",
            "/actuator",
            "/swagger/",
            "/swagger-ui/",
            "/swagger-ui.html",
            "/v3/api-docs/",
            "/v3/api-docs",
            "/webjars/",
            "/webjars",
            "/eureka"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        // 1. Allow CORS preflight requests
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }

        // 2. Allow whitelisted public endpoints (auth without logout, swagger, actuator, docs)
        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }

        // 3. For all other endpoints (including /logout and all domain APIs), require JWT
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            return onError(exchange, "Bearer token is empty", HttpStatus.UNAUTHORIZED);
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            String email = claims.get("email", String.class);

            // Coarse RBAC for admin endpoints
            if (path.contains("/admin/") && !"ADMIN".equalsIgnoreCase(role)) {
                return onError(exchange, "Access denied: Admin role required", HttpStatus.FORBIDDEN);
            }

            // Propagate user context downstream
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-User-Role", role != null ? role : "")
                    .header("X-User-Email", email != null ? email : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            return onError(exchange, "Invalid or expired JWT token", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isPublicEndpoint(String path) {
        // Exact auth endpoints (excluding /logout)
        for (String endpoint : PUBLIC_AUTH_ENDPOINTS) {
            if (path.equalsIgnoreCase(endpoint) || path.equalsIgnoreCase(endpoint + "/")) {
                return true;
            }
        }
        // System and documentation endpoints
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix) || path.equalsIgnoreCase(prefix)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonBody = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now().toString(),
                httpStatus.value(),
                httpStatus.getReasonPhrase(),
                err,
                exchange.getRequest().getURI().getPath()
        );

        DataBuffer buffer = response.bufferFactory().wrap(jsonBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -10; // Run early in filter chain
    }
}
