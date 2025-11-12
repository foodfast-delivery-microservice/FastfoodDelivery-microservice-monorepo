package com.example.payment.infrastructure.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    @SuppressWarnings("null")
    public WebClient orderWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
                .responseTimeout(Duration.ofSeconds(2))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(2, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(2, TimeUnit.SECONDS))
                );

        return builder
                .baseUrl("http://order-service/api/v1/orders")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    @SuppressWarnings("null")
    public WebClient userWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500)
                .responseTimeout(Duration.ofSeconds(2))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(2, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(2, TimeUnit.SECONDS))
                );

        return builder
                .baseUrl("http://user-service/api/v1/users")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}

