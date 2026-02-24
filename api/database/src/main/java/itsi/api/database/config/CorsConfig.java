package itsi.api.database.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Erlaubte Origins (Angular Dev-Server)
        config.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://localhost:5173",
                "http://localhost:3000",
                "http://127.0.0.1:4200",
                "http://127.0.0.1:5173"
        ));

        // Erlaubte HTTP-Methoden
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Erlaubte Header
        config.setAllowedHeaders(List.of("*"));

        // Credentials erlauben (z.B. für Cookies / Auth-Header)
        config.setAllowCredentials(true);

        // Preflight-Cache für 1 Stunde
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}

