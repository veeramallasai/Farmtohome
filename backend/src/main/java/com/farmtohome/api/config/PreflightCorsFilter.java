package com.farmtohome.api.config;

import java.io.IOException;
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
 * Handles preflight OPTIONS requests and injects CORS headers for all API requests
 * before Spring Security or Spring MVC routing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PreflightCorsFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && !origin.isBlank()) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            response.setHeader(HttpHeaders.VARY, "Origin");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD");
            
            String reqHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
            if (reqHeaders != null && !reqHeaders.isBlank()) {
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, reqHeaders);
            } else {
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    "Authorization, Content-Type, Accept, X-Requested-With, Origin, Access-Control-Request-Method, Access-Control-Request-Headers, X-CSRF-Token, *");
            }

            response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                "Authorization, Content-Type, X-Total-Count, Access-Control-Allow-Origin, Access-Control-Allow-Credentials");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpStatus.OK.value());
            return;
        }

        filterChain.doFilter(request, response);
    }
}


