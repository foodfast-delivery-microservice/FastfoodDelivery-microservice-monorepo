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

    // --- CẤU HÌNH CHUNG CHO CÁC SERVICE NỘI BỘ ---
    // Tăng lên 10s để tránh lỗi Timeout khi chạy Local/Docker
    private static final int CONNECT_TIMEOUT_MS = 10000; // 10 giây
    private static final long READ_TIMEOUT_SEC = 10;     // 10 giây

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    // Hàm tiện ích để tạo HttpClient với cấu hình "thoải mái" hơn
    private HttpClient createResilientHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(Duration.ofSeconds(READ_TIMEOUT_SEC))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_SEC, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(READ_TIMEOUT_SEC, TimeUnit.SECONDS))
                );
    }

    @Bean
    public WebClient productWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://product-service/api/v1/products")
                .clientConnector(new ReactorClientHttpConnector(createResilientHttpClient()))
                .build();
    }

    @Bean
    public WebClient userWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://user-service/api/v1/users")
                .clientConnector(new ReactorClientHttpConnector(createResilientHttpClient()))
                .build();
    }

    @Bean
    public WebClient paymentWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://payment-service/api/v1/payments")
                .clientConnector(new ReactorClientHttpConnector(createResilientHttpClient()))
                .build();
    }

    /**
     * WebClient dùng để gọi AddressKit (External API)
     * External API cũng nên tăng timeout lên 5s vì phụ thuộc tốc độ mạng internet
     */
    @Bean
    public WebClient addressKitWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .baseUrl("https://addresskit.cas.so")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /**
     * WebClient dùng để gọi Nominatim (External API)
     */
    @Bean
    public WebClient nominatimWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("User-Agent", "FastfoodDelivery/1.0 (support@fastfooddelivery.com)")
                .build();
    }
}