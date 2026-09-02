package com.farmtohome.api.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Handles preflight requests before MVC/security routing so auth endpoints always emit CORS
 * headers. Reads allowed origins directly from environment variables (not Spring properties)
 * because Railway injects credentials as environment variables at runtime, and any stale
 * application.yml default should never be able to shadow them.
 *
 * <p>Resolution order: {@code APP_CORS_ORIGINS} -&gt; {@code CORS_ORIGINS} -&gt;
 * {@code CORS_ALLOWED_ORIGINS} -&gt; hardcoded default.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PreflightCorsFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PreflightCorsFilter.class);

    private static final String DEFAULT_ORIGINS =
            "https://flutter-frontend-production-1590.up.railway.app,"
                    + "https://flutter-frontend-production-e8d6.up.railway.app,"
                    + "https://*.up.railway.app,"
                    + "https://*.railway.app,"
                    + "http://localhost:*,"
                    + "http://127.0.0.1:*";

    private final List<String> allowedOriginPatterns;

    public PreflightCorsFilter() {
        this(System.getenv("APP_CORS_ORIGINS"), System.getenv("CORS_ORIGINS"), System.getenv("CORS_ALLOWED_ORIGINS"));
    }

    PreflightCorsFilter(String appCorsOrigins, String corsOrigins, String corsAllowedOrigins) {
        String resolved;
        String source;

        if (appCorsOrigins != null && !appCorsOrigins.isBlank()) {
            resolved = appCorsOrigins;
            source = "APP_CORS_ORIGINS";
        } else if (corsOrigins != null && !corsOrigins.isBlank()) {
            resolved = corsOrigins;
            source = "CORS_ORIGINS";
        } else if (corsAllowedOrigins != null && !corsAllowedOrigins.isBlank()) {
            resolved = corsAllowedOrigins;
            source = "CORS_ALLOWED_ORIGINS";
        } else {
            resolved = DEFAULT_ORIGINS;
            source = "hardcoded default";
        }

        this.allowedOriginPatterns = Arrays.stream(resolved.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        log.info("PreflightCorsFilter initialized using {} -> allowed origin patterns: {}",
                source, allowedOriginPatterns);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        boolean allowed = origin != null && isAllowedOrigin(origin);

        if (origin != null) {
            log.info("CORS check for request {} {} -> origin='{}' allowed={}",
                    request.getMethod(), request.getRequestURI(), origin, allowed);
        }

        if (allowed) {
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
        return allowedOriginPatterns.stream().anyMatch(pattern -> matches(pattern, origin));
    }

    /**
     * Matches an origin against a pattern that may contain {@code *} wildcards, e.g.
     * {@code https://*.up.railway.app} or {@code http://localhost:*}. Each {@code *} is
     * translated into a {@code .*} regex segment, with the rest of the pattern escaped so
     * literal characters (dots, colons, slashes) are matched exactly.
     */
    private boolean matches(String pattern, String origin) {
        if ("*".equals(pattern)) {
            return true;
        }
        if (pattern.equalsIgnoreCase(origin)) {
            return true;
        }

        String[] parts = pattern.split("\\*", -1);
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(parts[i]));
        }
        regex.append("$");

        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE).matcher(origin).matches();
    }
}
