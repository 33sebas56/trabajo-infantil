package com.pasto.trabajo_infantil.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${app.cors.origin:http://localhost:5173}")
    private String origenFrontend;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();

        configuracion.setAllowedOrigins(List.of(origenFrontend));

        configuracion.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        configuracion.setAllowedHeaders(
                List.of("Content-Type", "Accept", "X-CSRF-TOKEN")
        );

        configuracion.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource fuente =
                new UrlBasedCorsConfigurationSource();

        fuente.registerCorsConfiguration("/**", configuracion);

        return fuente;
    }
}