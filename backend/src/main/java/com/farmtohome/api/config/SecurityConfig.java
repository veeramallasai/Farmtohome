package com.farmtohome.api.config;

import com.farmtohome.api.auth.JwtAuthFilter;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Value("${app.cors-origins:https://flutter-frontend-production-1590.up.railway.app,https://flutter-frontend-production-e8d6.up.railway.app,https://*.up.railway.app,https://*.railway.app,http://localhost:*,http://127.0.0.1:*,*}")
  private String corsOrigins;

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthFilter jwtAuthFilter) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    List<String> origins = Arrays.stream(corsOrigins.split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .toList();

    configuration.setAllowedOriginPatterns(origins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", "*"));
    configuration.setExposedHeaders(List.of(
        "Authorization", "Content-Type", "X-Total-Count", 
        "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"
    ));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
