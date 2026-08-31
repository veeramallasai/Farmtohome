package com.farmtohome.api.config;

import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Filter placed at HIGHEST_PRECEDENCE using Spring's official CorsFilter to handle CORS
 * preflight OPTIONS and cross-origin requests cleanly without header duplication.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PreflightCorsFilter extends CorsFilter {

    public PreflightCorsFilter() {
        super(createCorsConfigurationSource());
    }

    private static UrlBasedCorsConfigurationSource createCorsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", "*"));
        config.setExposedHeaders(List.of(
            "Authorization", "Content-Type", "X-Total-Count", 
            "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"
        ));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

