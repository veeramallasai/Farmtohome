package com.farmtohome.api.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
 * Handles preflight requests before MVC/security routing so auth endpoints always emit CORS headers.
 *
 * <p>This filter is the single source of truth for CORS on this API. It intentionally reads its
 * configuration directly from environment variables (rather than a Spring {@code @Value}
 * property) because on Railway the origin allow-list is provided via env vars and we do not want
 * a stale/incorrect {@code application.yml} property silently overriding it.
 *
 * <p>Resolution order: {@code APP_CORS_ORIGINS} -&gt; {@code CORS_ORIGINS} -&gt;
 * {@code CORS_ALLOWED_ORIGINS} -&gt; built-in default (which always covers the known frontend
 * domains plus any {@code *.up.railway.app} / {@code *.railway.app} deployment).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PreflightCorsFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PreflightCorsFilter.class);

    private static final String DEFAULT_ALLOWED_ORIGINS =
        "https://flutter-frontend-production-1590.up.railway.app,"
            + "https://flutter-frontend-production-e8d6.up.railway.app,"
            + "https://*.up.railway.app,"
            + "https://*.railway.app,"
            + "http://localhost:*,"
            + "http://127.0.0.1:*";

    private final List<String> allowedOriginPatterns;

    public PreflightCorsFilter() {
        this.allowedOriginPatterns = Arrays.stream(resolveCorsOrigins().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        log.info("PreflightCorsFilter initialized with allowed origin patterns: {}", allowedOriginPatterns);
    }

    private static String resolveCorsOrigins() {
        String source = "APP_CORS_ORIGINS";
        String origins = System.getenv("APP_CORS_ORIGINS");
        if (origins == null || origins.isBlank()) {
            source = "CORS_ORIGINS";
            origins = System.getenv("CORS_ORIGINS");
        }
        if (origins == null || origins.isBlank()) {
            source = "CORS_ALLOWED_ORIGINS";
            origins = System.getenv("CORS_ALLOWED_ORIGINS");
        }
        if (origins == null || origins.isBlank()) {
            source = "default";
            origins = DEFAULT_ALLOWED_ORIGINS;
        }
        log.info("PreflightCorsFilter resolved CORS origins from '{}': {}", source, origins);
        return origins;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        boolean isOptions = "OPTIONS".equalsIgnoreCase(request.getMethod());
        log.debug("CORS check: method={} uri={} origin={}", request.getMethod(), request.getRequestURI(), origin);

        if (origin != null) {
            boolean allowed = isAllowedOrigin(origin);
            log.debug("CORS check: origin={} allowed={}", origin, allowed);
            if (allowed) {
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                response.setHeader(HttpHeaders.VARY, "Origin");
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS,HEAD");
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    "Authorization,Content-Type,Accept,X-Requested-With,Origin,Access-Control-Request-Method,Access-Control-Request-Headers");
                response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization,Content-Type,X-Total-Count");
                response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
            } else {
                log.warn("CORS check: origin={} did not match any allowed pattern {}", origin, allowedOriginPatterns);
            }
        }

        if (isOptions) {
            log.debug("CORS: handling OPTIONS preflight for uri={} origin={} with 204", request.getRequestURI(), origin);
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedOrigin(String origin) {
        return allowedOriginPatterns.stream().anyMatch(pattern -> matches(pattern, origin));
    }

    private boolean matches(String pattern, String origin) {
        if ("*".equals(pattern)) return true;
        if (pattern.equalsIgnoreCase(origin)) return true;
        String regex = "^" + java.util.regex.Pattern.quote(pattern).replace("\\*", ".*") + "$";
        return origin.matches(regex);
    }
}
