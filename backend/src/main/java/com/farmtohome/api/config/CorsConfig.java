package com.farmtohome.api.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures CORS support for the API so that the Flutter web frontend (hosted on its own
 * Railway domain) can call endpoints such as /api/v1/auth/google-login without being blocked
 * by the browser for missing Access-Control-Allow-Origin headers.
 *
 * <p>Allowed origins are read from the {@code CORS_ALLOWED_ORIGINS} environment variable, falling
 * back to {@code CORS_ORIGINS} if the former is not set. Both variables support a comma-separated
 * list of origins. If neither variable is configured, the Flutter frontend's production domain is
 * allowed by default.
 */
@Configuration
public class CorsConfig {

  private static final String DEFAULT_ALLOWED_ORIGIN =
      "https://flutter-frontend-production-1590.up.railway.app";

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    List<String> allowedOrigins = resolveAllowedOrigins();

    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins.toArray(new String[0]))
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("Content-Type", "Authorization", "X-Requested-With", "X-CSRF-Token")
            .allowCredentials(true)
            .maxAge(3600);

        registry.addMapping("/v1/**")
            .allowedOrigins(allowedOrigins.toArray(new String[0]))
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("Content-Type", "Authorization", "X-Requested-With", "X-CSRF-Token")
            .allowCredentials(true)
            .maxAge(3600);
      }
    };
  }

  private static List<String> resolveAllowedOrigins() {
    String originsEnv = System.getenv("CORS_ALLOWED_ORIGINS");
    if (originsEnv == null || originsEnv.isBlank()) {
      originsEnv = System.getenv("CORS_ORIGINS");
    }

    if (originsEnv == null || originsEnv.isBlank()) {
      return List.of(DEFAULT_ALLOWED_ORIGIN);
    }

    List<String> origins = new ArrayList<>();
    for (String origin : originsEnv.split(",")) {
      String trimmed = origin.trim();
      if (!trimmed.isEmpty()) {
        origins.add(trimmed);
      }
    }

    return origins.isEmpty() ? List.of(DEFAULT_ALLOWED_ORIGIN) : origins;
  }
}
