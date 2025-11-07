package com.example.order_service.infrastructure.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
public class WebClientConfig {

    // 3. TẠO MỘT BEAN WebClient.Builder
    @Bean
    @LoadBalanced // <-- BẮT BUỘC: Bảo Spring Cloud quản lý builder này
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient productWebClient(WebClient.Builder builder) {
        // 4. SỬA LẠI:
        //    Dùng TÊN ĐĂNG KÝ (spring.application.name) của product-service
        //    (Giả sử nó là "product-service" hoặc "PRODUCT-SERVICE")
        return builder.baseUrl("http://product-service/api/v1/products").build();
    }
}