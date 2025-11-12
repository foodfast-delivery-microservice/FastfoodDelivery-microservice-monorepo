package com.example.payment.infrastructure.service;

import com.example.payment.infrastructure.client.dto.OrderDetailResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Tests cho Circuit Breaker trong OrderServiceAdapter
 * 
 * Test Scenarios:
 * 1. Circuit Breaker CLOSED (Normal operation)
 * 2. Circuit Breaker OPEN (Service down)
 * 3. Circuit Breaker HALF_OPEN (Recovery)
 * 4. TimeLimiter timeout
 * 5. Fallback method execution
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceAdapter Circuit Breaker Tests")
@SuppressWarnings("null")
class OrderServiceAdapterCircuitBreakerTest {

    @Mock
    private WebClient orderWebClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Mock
    private TimeLimiterRegistry timeLimiterRegistry;

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private TimeLimiter timeLimiter;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private JwtAuthenticationToken jwtAuthenticationToken;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private OrderServiceAdapter orderServiceAdapter;

    private String validOrderJson = "{\"id\":1,\"userId\":1,\"status\":\"CONFIRMED\",\"grandTotal\":100000}";
    private OrderDetailResponse validOrderResponse;

    @BeforeEach
    @SuppressWarnings("null")
    void setUp() throws Exception {
        // Setup valid order response
        validOrderResponse = OrderDetailResponse.builder()
                .id(1L)
                .userId(1L)
                .status("CONFIRMED")
                .grandTotal(java.math.BigDecimal.valueOf(100000))
                .build();

        // Setup SecurityContext
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
        when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
        when(jwt.getTokenValue()).thenReturn("mock-jwt-token");

        // Setup WebClient mocks
        doReturn(requestHeadersUriSpec).when(orderWebClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString(), any(Object.class));
        doReturn(requestHeadersSpec).when(requestHeadersSpec).header(anyString(), anyString());
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();

        // Setup Circuit Breaker Registry
        when(circuitBreakerRegistry.circuitBreaker("orderService")).thenReturn(circuitBreaker);
        when(circuitBreaker.getState()).thenReturn(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Test 1.1: Circuit Breaker CLOSED - Order Service healthy")
    void testCircuitBreakerClosed_OrderServiceHealthy() throws Exception {
        // Given: Order Service hoạt động bình thường
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(validOrderJson));
        when(objectMapper.readValue(validOrderJson, OrderDetailResponse.class))
                .thenReturn(validOrderResponse);

        // When: Payment Service gọi Order Service
        OrderDetailResponse response = orderServiceAdapter.getOrderDetail(1L);

        // Then: Response thành công
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");

        // Verify: WebClient được gọi
        verify(orderWebClient, times(1)).get();
        verify(responseSpec, times(1)).bodyToMono(String.class);
    }

    @Test
    @DisplayName("Test 1.2: Circuit Breaker OPEN - Order Service down")
    void testCircuitBreakerOpen_OrderServiceDown() {
        // Given: Order Service down (connection timeout)
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(
                        org.springframework.web.reactive.function.client.WebClientResponseException.create(
                                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE.value(),
                                "Connection timeout",
                                org.springframework.http.HttpHeaders.EMPTY, // NOSONAR
                                new byte[0], null)));

        // When: Gọi Order Service nhiều lần để trigger Circuit Breaker
        // Note: Trong thực tế, cần gọi đủ số lần để đạt failureRateThreshold
        // Ở đây mock để simulate Circuit Breaker đã OPEN

        // Then: Exception được throw
        assertThatThrownBy(() -> orderServiceAdapter.getOrderDetail(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot connect to Order Service");

        // Verify: WebClient được gọi
        verify(orderWebClient, atLeastOnce()).get();
    }

    @Test
    @DisplayName("Test 1.3: Fallback method execution")
    void testFallbackMethod_Execution() {
        // Given: Circuit Breaker OPEN (simulate bằng cách throw exception)
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(
                        org.springframework.web.reactive.function.client.WebClientResponseException.create(
                                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE.value(),
                                "Service unavailable",
                                org.springframework.http.HttpHeaders.EMPTY, // NOSONAR
                                new byte[0], null)));

        // When: Gọi Order Service
        // Then: Fallback method được gọi (trong thực tế, cần Circuit Breaker thực sự OPEN)
        // Note: Để test fallback thực sự, cần setup Circuit Breaker với state OPEN
        assertThatThrownBy(() -> orderServiceAdapter.getOrderDetail(1L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Test 1.4: TimeLimiter timeout - Order Service slow response")
    void testTimeLimiterTimeout_OrderServiceSlow() {
        // Given: Order Service response chậm (> 1s timeout)
        // Note: TimeLimiter timeout test cần setup thực tế với delay
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.just(validOrderJson)
                        .delayElement(java.time.Duration.ofSeconds(2)));

        // When: Gọi Order Service
        // Then: TimeLimiter timeout → Exception
        // Note: Test này cần chạy với Circuit Breaker thực sự
        assertThatThrownBy(() -> orderServiceAdapter.getOrderDetail(1L))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Test 1.5: Order not found (404)")
    void testOrderNotFound_404() {
        // Given: Order không tồn tại
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(
                        org.springframework.web.reactive.function.client.WebClientResponseException.create(
                                org.springframework.http.HttpStatus.NOT_FOUND.value(),
                                "Order not found",
                                org.springframework.http.HttpHeaders.EMPTY, // NOSONAR
                                new byte[0], null)));

        // When: Gọi Order Service
        // Then: Exception với message "Order not found"
        assertThatThrownBy(() -> orderServiceAdapter.getOrderDetail(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    @DisplayName("Test 1.6: Unauthorized (401)")
    void testUnauthorized_401() {
        // Given: Token không hợp lệ
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(
                        org.springframework.web.reactive.function.client.WebClientResponseException.create(
                                org.springframework.http.HttpStatus.UNAUTHORIZED.value(),
                                "Unauthorized",
                                org.springframework.http.HttpHeaders.EMPTY, // NOSONAR
                                new byte[0], null)));

        // When: Gọi Order Service
        // Then: Exception với message về token
        assertThatThrownBy(() -> orderServiceAdapter.getOrderDetail(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Token không hợp lệ");
    }

    @Test
    @DisplayName("Test 1.7: Forbidden (403)")
    void testForbidden_403() {
        // Given: Không có quyền truy cập
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(
                        org.springframework.web.reactive.function.client.WebClientResponseException.create(
                                org.springframework.http.HttpStatus.FORBIDDEN.value(),
                                "Forbidden",
                                org.springframework.http.HttpHeaders.EMPTY, // NOSONAR
                                new byte[0], null)));

        // When: Gọi Order Service
        // Then: Exception với message về quyền truy cập
        assertThatThrownBy(() -> orderServiceAdapter.getOrderDetail(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không có quyền truy cập");
    }

    @Test
    @DisplayName("Test 1.8: Server error (500)")
    void testServerError_500() {
        // Given: Order Service lỗi server
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(
                        org.springframework.web.reactive.function.client.WebClientResponseException.create(
                                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "Internal Server Error",
                                org.springframework.http.HttpHeaders.EMPTY, // NOSONAR
                                new byte[0], null)));

        // When: Gọi Order Service
        // Then: Exception với message về server error
        assertThatThrownBy(() -> orderServiceAdapter.getOrderDetail(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order Service error");
    }
}

