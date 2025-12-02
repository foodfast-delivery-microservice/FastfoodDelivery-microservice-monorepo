package com.example.order_service.load;

import com.example.order_service.application.dto.CreateOrderRequest;
import com.example.order_service.application.dto.OrderResponse;
import com.example.order_service.application.dto.ProductValidationResponse;
import com.example.order_service.application.dto.UserValidationResponse;
import com.example.order_service.application.usecase.CreateOrderUseCase;
import com.example.order_service.domain.repository.ProductServicePort;
import com.example.order_service.domain.repository.UserServicePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Load Test cho CreateOrder API
 * Test với database thật (H2 in-memory) và Spring context đầy đủ
 * 
 * Lưu ý: Test này sẽ tạo 1000 đơn hàng thật trong database
 * Có thể mất vài phút để chạy xong
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("CreateOrder Load Integration Tests - 1000 Orders")
class CreateOrderLoadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @MockBean
    private ProductServicePort productServicePort;

    @MockBean
    private UserServicePort userServicePort;

    private static final int TOTAL_ORDERS = 1000;
    private static final int THREAD_POOL_SIZE = 50;
    private static final int TIMEOUT_SECONDS = 300; // 5 phút timeout

    @BeforeEach
    void setUp() {
        // Mock Product Service
        when(productServicePort.validateProducts(any())).thenAnswer(invocation -> {
            List<?> requests = invocation.getArgument(0);
            return requests.stream()
                    .map(req -> new ProductValidationResponse(
                            1L,
                            true,
                            "Load Test Product",
                            BigDecimal.valueOf(100000),
                            10L
                    ))
                    .toList();
        });

        // Mock User Service
        when(userServicePort.validateUser(anyLong())).thenReturn(
                new UserValidationResponse(1L, true, true, "load-test-user")
        );
    }

    /**
     * Test tạo 1000 đơn hàng qua UseCase trực tiếp (nhanh hơn)
     */
    @Test
    @DisplayName("Load Test: 1000 orders via UseCase")
    void testCreate1000Orders_ViaUseCase() throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("LOAD TEST: 1000 Orders via UseCase");
        System.out.println("=".repeat(60));

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch latch = new CountDownLatch(TOTAL_ORDERS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> responseTimes = new CopyOnWriteArrayList<>();
        List<String> errors = new CopyOnWriteArrayList<>();

        long startTime = System.currentTimeMillis();

        // Submit 1000 tasks
        IntStream.range(0, TOTAL_ORDERS).forEach(i -> {
            executor.submit(() -> {
                long requestStart = System.currentTimeMillis();
                try {
                    CreateOrderRequest request = createTestRequest(i);
                    String idempotencyKey = "load-test-idem-" + i;
                    String jti = "load-test-jti-" + i;

                    OrderResponse response = createOrderUseCase.execute(request, idempotencyKey, jti);

                    long requestEnd = System.currentTimeMillis();
                    responseTimes.add(requestEnd - requestStart);

                    successCount.incrementAndGet();

                    if ((i + 1) % 100 == 0) {
                        System.out.println("✓ Completed: " + (i + 1) + " / " + TOTAL_ORDERS + " orders");
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    String error = "Order " + i + ": " + e.getClass().getSimpleName() + " - " + e.getMessage();
                    errors.add(error);
                    if (errors.size() <= 10) { // Chỉ log 10 lỗi đầu tiên
                        System.err.println("✗ " + error);
                    }
                } finally {
                    latch.countDown();
                }
            });
        });

        // Wait for completion
        boolean completed = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;

        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Calculate statistics
        printStatistics(successCount.get(), failureCount.get(), completed, totalDuration, responseTimes, errors);

        // Assertions
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isGreaterThanOrEqualTo(TOTAL_ORDERS * 90 / 100); // Cho phép 10% lỗi
        System.out.println("\n✅ Load test completed!");
    }

    /**
     * Test tạo 1000 đơn hàng qua REST API (giống production)
     */
    @Test
    @DisplayName("Load Test: 1000 orders via REST API")
    void testCreate1000Orders_ViaRestAPI() throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("LOAD TEST: 1000 Orders via REST API");
        System.out.println("=".repeat(60));

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch latch = new CountDownLatch(TOTAL_ORDERS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> responseTimes = new CopyOnWriteArrayList<>();
        List<String> errors = new CopyOnWriteArrayList<>();

        long startTime = System.currentTimeMillis();

        // Submit 1000 tasks
        IntStream.range(0, TOTAL_ORDERS).forEach(i -> {
            executor.submit(() -> {
                long requestStart = System.currentTimeMillis();
                try {
                    CreateOrderRequest request = createTestRequest(i);
                    String idempotencyKey = "load-test-api-idem-" + i;

                    mockMvc.perform(post("/api/v1/orders")
                                    .header("Idempotency-Key", idempotencyKey)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andExpect(status().isCreated());

                    long requestEnd = System.currentTimeMillis();
                    responseTimes.add(requestEnd - requestStart);

                    successCount.incrementAndGet();

                    if ((i + 1) % 100 == 0) {
                        System.out.println("✓ Completed: " + (i + 1) + " / " + TOTAL_ORDERS + " orders");
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    String error = "Order " + i + ": " + e.getClass().getSimpleName() + " - " + e.getMessage();
                    errors.add(error);
                    if (errors.size() <= 10) {
                        System.err.println("✗ " + error);
                    }
                } finally {
                    latch.countDown();
                }
            });
        });

        // Wait for completion
        boolean completed = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;

        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Calculate statistics
        printStatistics(successCount.get(), failureCount.get(), completed, totalDuration, responseTimes, errors);

        // Assertions
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isGreaterThanOrEqualTo(TOTAL_ORDERS * 90 / 100);
        System.out.println("\n✅ Load test completed!");
    }

    /**
     * Test với số lượng nhỏ hơn để verify trước
     */
    @Test
    @DisplayName("Quick Test: 100 orders to verify setup")
    void testCreate100Orders_QuickTest() throws Exception {
        System.out.println("\n=== QUICK TEST: 100 Orders ===");

        int quickTestOrders = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(quickTestOrders);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        IntStream.range(0, quickTestOrders).forEach(i -> {
            executor.submit(() -> {
                try {
                    CreateOrderRequest request = createTestRequest(i);
                    createOrderUseCase.execute(request, "quick-test-" + i, "jti-" + i);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        });

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Success: " + successCount.get());
        System.out.println("Failed: " + failureCount.get());
        System.out.println("Duration: " + duration + " ms");
        System.out.println("Throughput: " + (successCount.get() * 1000.0 / duration) + " orders/second");

        assertThat(successCount.get()).isEqualTo(quickTestOrders);
    }

    private void printStatistics(int success, int failure, boolean completed, long totalDuration,
                                 List<Long> responseTimes, List<String> errors) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("LOAD TEST RESULTS");
        System.out.println("=".repeat(60));
        System.out.println("Total Orders: " + TOTAL_ORDERS);
        System.out.println("Success: " + success + " (" + (success * 100 / TOTAL_ORDERS) + "%)");
        System.out.println("Failed: " + failure + " (" + (failure * 100 / TOTAL_ORDERS) + "%)");
        System.out.println("Completed: " + completed);
        System.out.println("Total Duration: " + totalDuration + " ms (" + (totalDuration / 1000.0) + " seconds)");
        System.out.println("Throughput: " + String.format("%.2f", success * 1000.0 / totalDuration) + " orders/second");

        if (!responseTimes.isEmpty()) {
            List<Long> sortedTimes = new java.util.ArrayList<>(responseTimes);
            sortedTimes.sort(Long::compareTo);

            long min = sortedTimes.get(0);
            long max = sortedTimes.get(sortedTimes.size() - 1);
            double avg = sortedTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            long p50 = sortedTimes.get(sortedTimes.size() / 2);
            long p95 = sortedTimes.get((int) (sortedTimes.size() * 0.95));
            long p99 = sortedTimes.get((int) (sortedTimes.size() * 0.99));

            System.out.println("\n--- Response Time Statistics ---");
            System.out.println("Min: " + min + " ms");
            System.out.println("Max: " + max + " ms");
            System.out.println("Average: " + String.format("%.2f", avg) + " ms");
            System.out.println("P50 (Median): " + p50 + " ms");
            System.out.println("P95: " + p95 + " ms");
            System.out.println("P99: " + p99 + " ms");
        }

        if (!errors.isEmpty()) {
            System.out.println("\n--- Error Summary (first 10) ---");
            errors.stream().limit(10).forEach(System.err::println);
            if (errors.size() > 10) {
                System.out.println("... and " + (errors.size() - 10) + " more errors");
            }
        }
        System.out.println("=".repeat(60));
    }

    private CreateOrderRequest createTestRequest(int index) {
        return CreateOrderRequest.builder()
                .userId((long) (index % 100) + 1)
                .orderItems(List.of(
                        CreateOrderRequest.OrderItemRequest.builder()
                                .productId(1L)
                                .quantity((index % 5) + 1)
                                .build()
                ))
                .discount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.valueOf(10000))
                .deliveryAddress(CreateOrderRequest.DeliveryAddressRequest.builder()
                        .receiverName("Load Test User " + index)
                        .receiverPhone("09" + String.format("%08d", index % 100000000))
                        .addressLine1("Test Address " + index)
                        .ward("Ward " + (index % 10 + 1))
                        .district("District " + (index % 20 + 1))
                        .city("City " + (index % 5 + 1))
                        .build())
                .note("Load test order #" + index)
                .build();
    }
}


















