package com.farmtohome.api.config;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

  @Bean
  public FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
    return configuration -> configuration
        .cleanDisabled(true)
        .baselineOnMigrate(true)
        .baselineVersion("0")
        .repairOnMigrate(true);
  }
}
