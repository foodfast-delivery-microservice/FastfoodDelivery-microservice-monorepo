package com.example.order_service.infrastructure.config;

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

    // 2. Không cần inject giá trị này nữa
    // @Value("${microservice.product-service.url}")
    // private String productServiceUrl;

    // 3. TẠO MỘT BEAN WebClient.Builder
    @Bean
    @LoadBalanced // <-- BẮT BUỘC: Bảo Spring Cloud quản lý builder này
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient productWebClient(WebClient.Builder builder) {
        // Cấu hình HttpClient với timeout
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500) // Connect timeout: 500ms
                .responseTimeout(Duration.ofSeconds(1)) // Response timeout: 1s
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(1, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(1, TimeUnit.SECONDS))
                );

        // 4. SỬA LẠI:
        //    Dùng TÊN ĐĂNG KÝ (spring.application.name) của product-service
        //    (Giả sử nó là "product-service" hoặc "PRODUCT-SERVICE")
        return builder
                .baseUrl("http://product-service/api/v1/products")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}