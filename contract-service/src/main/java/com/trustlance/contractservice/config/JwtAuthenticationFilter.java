package com.trustlance.contractservice.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Primary path: read headers injected by API Gateway
        String gatewayUserId = request.getHeader("X-User-Id");
        String gatewayUserRole = request.getHeader("X-User-Role");
        String gatewayUserEmail = request.getHeader("X-User-Email");

        if (gatewayUserId != null && gatewayUserRole != null) {
            try {
                UserPrincipal principal = new UserPrincipal(
                        UUID.fromString(gatewayUserId),
                        gatewayUserEmail != null ? gatewayUserEmail : "",
                        gatewayUserRole
                );
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {}
        } else {
            // Fallback: direct calls without Gateway (dev/test only) — validate Bearer token
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    Claims claims = jwtUtils.validateAndGetClaims(authHeader.substring(7));
                    String userIdStr = claims.getSubject();
                    String role = claims.get("role", String.class);
                    String email = claims.get("email", String.class);
                    if (userIdStr != null && role != null) {
                        UserPrincipal principal = new UserPrincipal(
                                UUID.fromString(userIdStr),
                                email != null ? email : "",
                                role
                        );
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities()
                        );
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } catch (Exception ignored) {}
            }
        }

        filterChain.doFilter(request, response);
    }
}
