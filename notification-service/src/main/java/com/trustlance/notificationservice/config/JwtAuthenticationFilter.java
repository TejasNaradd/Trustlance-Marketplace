package com.trustlance.notificationservice.config;

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

@Component @RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");
        String userEmail = request.getHeader("X-User-Email");
        if (userId != null && userRole != null) {
            try {
                UserPrincipal p = new UserPrincipal(UUID.fromString(userId), userEmail != null ? userEmail : "", userRole);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {}
        } else {
            String h = request.getHeader("Authorization");
            if (h != null && h.startsWith("Bearer ")) {
                try {
                    Claims c = jwtUtils.validateAndGetClaims(h.substring(7));
                    String uid = c.getSubject(); String role = c.get("role", String.class); String email = c.get("email", String.class);
                    if (uid != null && role != null) {
                        UserPrincipal p = new UserPrincipal(UUID.fromString(uid), email != null ? email : "", role);
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                } catch (Exception ignored) {}
            }
        }
        chain.doFilter(request, response);
    }
}
