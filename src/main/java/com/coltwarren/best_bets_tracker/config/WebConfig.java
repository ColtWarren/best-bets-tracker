package com.coltwarren.best_bets_tracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration.
 * CORS is now handled by SecurityConfig's CorsConfigurationSource bean
 * to ensure proper integration with Spring Security's filter chain.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
