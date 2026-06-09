package ru.hotdog.multicam_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
// Настройки WebClient для HTTP запросов.
public class WebClientConfig {

    @Bean
    // Создает builder для WebClient.
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
