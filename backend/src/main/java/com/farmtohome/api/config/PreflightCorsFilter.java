package com.farmtohome.api.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handles preflight requests before MVC/security routing so auth endpoints always emit CORS headers.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PreflightCorsFilter extends OncePerRequestFilter {

    private final List<String> allowedOriginPatterns;

    public PreflightCorsFilter(
            @Value("${app.cors-origins:${APP_CORS_ORIGINS:${CORS_ORIGINS:https://flutter-frontend-production-1590.up.railway.app,https://flutter-frontend-production-e8d6.up.railway.app,https://*.up.railway.app,https://*.railway.app,http://localhost:*,http://127.0.0.1:*}}}")
            String corsOrigins) {
        this.allowedOriginPatterns = Arrays.stream(corsOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(java.util.Locale.ROOT))
                .toList();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && isAllowedOrigin(origin)) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.setHeader(HttpHeaders.VARY, "Origin");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS,HEAD");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                "Authorization,Content-Type,Accept,X-Requested-With,Origin,Access-Control-Request-Method,Access-Control-Request-Headers");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization,Content-Type,X-Total-Count");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedOrigin(String origin) {
        String normalizedOrigin = origin.toLowerCase(java.util.Locale.ROOT);
        return allowedOriginPatterns.stream().anyMatch(pattern -> matches(pattern, normalizedOrigin));
    }

    private boolean matches(String pattern, String origin) {
        if ("*".equals(pattern)) return true;
        if (pattern.equalsIgnoreCase(origin)) return true;

        String[] parts = pattern.split("\\*", -1);
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(".*");
            }
            sb.append(java.util.regex.Pattern.quote(parts[i]));
        }
        sb.append("$");
        return origin.matches(sb.toString().toLowerCase(java.util.Locale.ROOT));
    }
}


