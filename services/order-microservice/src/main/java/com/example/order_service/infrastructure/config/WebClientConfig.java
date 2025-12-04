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
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000) // Connect timeout: 500ms
                .responseTimeout(Duration.ofSeconds(60)) // Response timeout: 60s
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS))
                );

        // 4. SỬA LẠI:
        //    Dùng TÊN ĐĂNG KÝ (spring.application.name) của product-service
        //    (Giả sử nó là "product-service" hoặc "PRODUCT-SERVICE")
        return builder
                .baseUrl("http://product-service/api/v1/products")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public WebClient userWebClient(WebClient.Builder builder) {
        // Cấu hình HttpClient với timeout
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 500) // Connect timeout: 500ms
                .responseTimeout(Duration.ofSeconds(60)) // Response timeout: 60s
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS))
                );

        return builder
                .baseUrl("http://user-service/api/v1/users")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public WebClient paymentWebClient(WebClient.Builder builder) {
        // Cấu hình HttpClient với timeout
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000) // Connect timeout: 500ms
                .responseTimeout(Duration.ofSeconds(60)) // Response timeout: 60s
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS))
                );

        return builder
                .baseUrl("http://payment-service/api/v1/payments")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * WebClient dùng để gọi AddressKit (Cas AddressKit - danh mục hành chính).
     * Không cần load balancing vì đây là dịch vụ bên ngoài, truy cập qua Internet.
     * Xem tài liệu: https://addresskit.cas.so/
     */
    @Bean
    public WebClient addressKitWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000) // Connect timeout: 2s
                .responseTimeout(Duration.ofSeconds(3)) // Response timeout: 3s
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(3, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(3, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .baseUrl("https://addresskit.cas.so")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * WebClient dùng để gọi Nominatim (OpenStreetMap) cho geocoding.
     */
    @Bean
    public WebClient nominatimWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .responseTimeout(Duration.ofSeconds(3))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(3, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(3, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("User-Agent", "FastfoodDelivery/1.0 (support@fastfooddelivery.com)")
                .build();
    }
}