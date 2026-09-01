package com.farmtohome.api.config;

import com.farmtohome.api.auth.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * CORS is handled exclusively by {@link PreflightCorsFilter}, which runs at
 * {@code Ordered.HIGHEST_PRECEDENCE} and reads the allowed origins directly from environment
 * variables. Spring Security's own CORS support is intentionally left disabled here to avoid two
 * different CORS configurations (this class and the filter) conflicting and producing
 * inconsistent/missing {@code Access-Control-Allow-Origin} headers on preflight requests.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthFilter jwtAuthFilter) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.disable())
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/actuator", "/actuator/**", "/health", "/error").permitAll()
            .requestMatchers("/api/v1/auth/**", "/v1/auth/**", "/auth/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**", "/v1/catalog/**", "/api/v1/products/**", "/v1/products/**", "/api/v1/categories/**", "/v1/categories/**", "/api/v1/offers", "/v1/offers", "/api/v1/farmers/**", "/v1/farmers/**", "/api/v1/delivery-slots", "/v1/delivery-slots").permitAll()
            .anyRequest().permitAll())
        .exceptionHandling(exceptions -> exceptions
            .authenticationEntryPoint((request, response, error) -> {
              response.setStatus(401);
              response.setContentType("application/json");
              response.getWriter().write(
                  "{\"success\":false,\"message\":\"Authentication required.\","
                      + "\"code\":\"UNAUTHORIZED\"}");
            })
            .accessDeniedHandler((request, response, error) -> {
              response.setStatus(403);
              response.setContentType("application/json");
              response.getWriter().write(
                  "{\"success\":false,\"message\":\"Access denied.\","
                      + "\"code\":\"FORBIDDEN\"}");
            }))
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  UserDetailsService userDetailsService() {
    return username -> {
      throw new UsernameNotFoundException("Password login is not enabled on this API.");
    };
  }
}
