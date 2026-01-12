package itsi.api.steuerung.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${database.api.url}")
    private String databaseApiUrl;

    @Value("${backend.api.url}")
    private String backendApiUrl;

    @Value("${database.api.timeout}")
    private long databaseTimeout;

    @Value("${backend.api.timeout}")
    private long backendTimeout;

    @Bean
    public WebClient databaseWebClient() {
        return WebClient.builder()
                .baseUrl(databaseApiUrl)
                .build();
    }

    @Bean
    public WebClient backendWebClient() {
        return WebClient.builder()
                .baseUrl(backendApiUrl)
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("*").allowedMethods("*").allowedHeaders("*");
            }
        };
    }
}
