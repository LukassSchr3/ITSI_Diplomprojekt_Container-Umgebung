package itsi.api.steuerung.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.core.publisher.Mono;

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
                .filter(jwtForwardingFilter())
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

    private ExchangeFilterFunction jwtForwardingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String authHeader = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (authHeader != null) {
                    return Mono.just(ClientRequest.from(clientRequest)
                            .header(HttpHeaders.AUTHORIZATION, authHeader)
                            .build());
                }
            }
            return Mono.just(clientRequest);
        });
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
