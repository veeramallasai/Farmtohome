package com.farmtohome.api.config;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Spring Boot EnvironmentPostProcessor that automatically detects Railway and external
 * PostgreSQL environment variables (SPRING_DATASOURCE_URL, DB_URL, DATABASE_URL, DATABASE_PUBLIC_URL,
 * PGHOST/PGPORT/PGDATABASE/PGUSER/PGPASSWORD), formats them with jdbc:postgresql:// prefix,
 * and configures spring.datasource properties before Spring Data / Flyway starts up.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    Map<String, Object> map = new HashMap<>();

    String dbUrl = environment.getProperty("SPRING_DATASOURCE_URL");
    if (dbUrl == null || dbUrl.trim().isEmpty()) {
      dbUrl = environment.getProperty("DB_URL");
    }
    if (dbUrl == null || dbUrl.trim().isEmpty()) {
      dbUrl = environment.getProperty("DATABASE_URL");
    }
    if (dbUrl == null || dbUrl.trim().isEmpty()) {
      dbUrl = environment.getProperty("DATABASE_PUBLIC_URL");
    }

    String pgHost = environment.getProperty("PGHOST");
    String pgPort = environment.getProperty("PGPORT");
    String pgDb = environment.getProperty("PGDATABASE");
    String pgUser = environment.getProperty("PGUSER");
    String pgPass = environment.getProperty("PGPASSWORD");

    if (dbUrl != null && !dbUrl.trim().isEmpty()) {
      String trimmed = dbUrl.trim();
      String jdbcUrl = formatJdbcUrl(trimmed);
      map.put("spring.datasource.url", jdbcUrl);

      try {
        String cleanUri = trimmed;
        if (cleanUri.startsWith("jdbc:")) {
          cleanUri = cleanUri.substring(5);
        }
        URI uri = URI.create(cleanUri);
        if (uri.getUserInfo() != null && uri.getUserInfo().contains(":")) {
          String[] userPass = uri.getUserInfo().split(":", 2);
          if (environment.getProperty("spring.datasource.username") == null && environment.getProperty("DB_USERNAME") == null) {
            map.put("spring.datasource.username", userPass[0]);
          }
          if (environment.getProperty("spring.datasource.password") == null && environment.getProperty("DB_PASSWORD") == null) {
            map.put("spring.datasource.password", userPass[1]);
          }
        }
      } catch (Exception ignored) {
      }
    } else if (pgHost != null && !pgHost.trim().isEmpty() && pgDb != null && !pgDb.trim().isEmpty()) {
      int port = 5432;
      if (pgPort != null && !pgPort.trim().isEmpty()) {
        try {
          port = Integer.parseInt(pgPort.trim());
        } catch (NumberFormatException ignored) {}
      }
      String jdbcUrl = "jdbc:postgresql://" + pgHost.trim() + ":" + port + "/" + pgDb.trim();
      map.put("spring.datasource.url", jdbcUrl);
      if (pgUser != null && !pgUser.trim().isEmpty()) {
        map.put("spring.datasource.username", pgUser.trim());
      }
      if (pgPass != null && !pgPass.trim().isEmpty()) {
        map.put("spring.datasource.password", pgPass.trim());
      }
    }

    map.put("spring.datasource.driver-class-name", "org.postgresql.Driver");

    if (!map.isEmpty()) {
      environment.getPropertySources().addFirst(new MapPropertySource("railwayDatabaseProperties", map));
    }
  }

  private String formatJdbcUrl(String url) {
    if (url.startsWith("jdbc:")) {
      return url;
    }
    if (url.startsWith("postgres://")) {
      return "jdbc:postgresql://" + url.substring("postgres://".length());
    }
    if (url.startsWith("postgresql://")) {
      return "jdbc:postgresql://" + url.substring("postgresql://".length());
    }
    return "jdbc:postgresql://" + url;
  }
}
