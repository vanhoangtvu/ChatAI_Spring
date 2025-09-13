package com.chatai.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        // Simplified connection provider for better stability
        ConnectionProvider connectionProvider = ConnectionProvider.builder("groq-client")
            .maxConnections(20)
            .maxIdleTime(Duration.ofMinutes(2))
            .maxLifeTime(Duration.ofMinutes(10))
            .build();
            
        // Simplified HttpClient configuration optimized for streaming
        HttpClient httpClient = HttpClient.create(connectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 10 seconds
            .responseTimeout(Duration.ofMinutes(3)) // 3 minutes for streaming
            .keepAlive(true)
            .compress(false); // Disable compression for streaming
            
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(configurer -> {
                // Unlimited buffer for streaming
                configurer.defaultCodecs().maxInMemorySize(-1);
            });
    }
}
