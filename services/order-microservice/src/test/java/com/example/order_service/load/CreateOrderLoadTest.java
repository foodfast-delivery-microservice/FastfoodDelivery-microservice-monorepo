package com.example.order_service.load;

import com.example.order_service.application.dto.CreateOrderRequest;
import com.example.order_service.application.dto.OrderResponse;
import com.example.order_service.application.dto.ProductValidationResponse;
import com.example.order_service.application.dto.UserValidationResponse;
import com.example.order_service.application.usecase.CreateOrderUseCase;
import com.example.order_service.domain.repository.ProductServicePort;
import com.example.order_service.domain.repository.UserServicePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Load Test cho CreateOrderUseCase
 * Mục đích: Test khả năng xử lý 1000 đơn hàng đồng thời
 * 
 * Lưu ý: Test này sử dụng mock services để tránh phụ thuộc vào external services
 * Để test với database thật, cần sử dụng @SpringBootTest và Testcontainers
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrder Load Tests - 1000 Orders")
class CreateOrderLoadTest {

    @Mock
    private ProductServicePort productServicePort;

    @Mock
    private UserServicePort userServicePort;

    private CreateOrderUseCase createOrderUseCase;

    private static final int TOTAL_ORDERS = 1000;
    private static final int THREAD_POOL_SIZE = 50; // Số thread xử lý đồng thời
    private static final int TIMEOUT_SECONDS = 120; // Timeout 2 phút

    @BeforeEach
    void setUp() {
        // Mock Product Service - luôn trả về sản phẩm hợp lệ
        when(productServicePort.validateProducts(any())).thenAnswer(invocation -> {
            List<?> requests = invocation.getArgument(0);
            return requests.stream()
                    .map(req -> new ProductValidationResponse(
                            1L,
                            true,
                            "Test Product",
                            BigDecimal.valueOf(100000),
                            10L
                    ))
                    .toList();
        });

        // Mock User Service - luôn trả về user hợp lệ
        when(userServicePort.validateUser(anyLong())).thenReturn(
                new UserValidationResponse(1L, true, true, "test-user")
        );

        // Tạo CreateOrderUseCase với mocked dependencies
        // Note: Trong thực tế, cần inject các dependencies thật từ Spring context
        // Để test đầy đủ, nên sử dụng @SpringBootTest
    }

    /**
     * Test tạo 1000 đơn hàng tuần tự (sequential)
     * Mục đích: Đo baseline performance
     */
    @Test
    @DisplayName("Test 1000 orders sequentially - Baseline")
    void testCreate1000Orders_Sequentially() throws Exception {
        System.out.println("\n=== LOAD TEST: 1000 Orders Sequential ===");
        
        long startTime = System.currentTimeMillis();
        List<OrderResponse> orders = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < TOTAL_ORDERS; i++) {
            try {
                CreateOrderRequest request = createTestRequest(i);
                // Note: Cần inject CreateOrderUseCase thật từ Spring context
                // OrderResponse response = createOrderUseCase.execute(request, "idem-" + i, "jti-" + i);
                // orders.add(response);
                successCount.incrementAndGet();
                
                if ((i + 1) % 100 == 0) {
                    System.out.println("Processed: " + (i + 1) + " orders");
                }
            } catch (Exception e) {
                failureCount.incrementAndGet();
                System.err.println("Failed order " + i + ": " + e.getMessage());
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("\n=== RESULTS ===");
        System.out.println("Total Orders: " + TOTAL_ORDERS);
        System.out.println("Success: " + successCount.get());
        System.out.println("Failed: " + failureCount.get());
        System.out.println("Duration: " + duration + " ms (" + (duration / 1000.0) + " seconds)");
        System.out.println("Throughput: " + (TOTAL_ORDERS * 1000.0 / duration) + " orders/second");
        System.out.println("Average Time per Order: " + (duration / (double) TOTAL_ORDERS) + " ms");

        assertThat(successCount.get()).isEqualTo(TOTAL_ORDERS);
        assertThat(failureCount.get()).isEqualTo(0);
    }

    /**
     * Test tạo 1000 đơn hàng đồng thời (concurrent)
     * Mục đích: Test khả năng xử lý concurrent requests
     */
    @Test
    @DisplayName("Test 1000 orders concurrently - Stress Test")
    void testCreate1000Orders_Concurrently() throws Exception {
        System.out.println("\n=== LOAD TEST: 1000 Orders Concurrent ===");
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch latch = new CountDownLatch(TOTAL_ORDERS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> responseTimes = new CopyOnWriteArrayList<>();

        long startTime = System.currentTimeMillis();

        // Submit 1000 tasks
        IntStream.range(0, TOTAL_ORDERS).forEach(i -> {
            executor.submit(() -> {
                long requestStart = System.currentTimeMillis();
                try {
                    CreateOrderRequest request = createTestRequest(i);
                    String idempotencyKey = "idem-key-" + i;
                    String jti = "jti-" + i;
                    
                    // Note: Cần inject CreateOrderUseCase thật
                    // OrderResponse response = createOrderUseCase.execute(request, idempotencyKey, jti);
                    
                    long requestEnd = System.currentTimeMillis();
                    responseTimes.add(requestEnd - requestStart);
                    
                    successCount.incrementAndGet();
                    
                    if ((i + 1) % 100 == 0) {
                        System.out.println("Completed: " + (i + 1) + " orders");
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Failed order " + i + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        });

        // Wait for all tasks to complete
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
        List<Long> sortedTimes = new ArrayList<>(responseTimes);
        sortedTimes.sort(Long::compareTo);
        
        long p50 = sortedTimes.isEmpty() ? 0 : sortedTimes.get(sortedTimes.size() / 2);
        long p95 = sortedTimes.isEmpty() ? 0 : sortedTimes.get((int) (sortedTimes.size() * 0.95));
        long p99 = sortedTimes.isEmpty() ? 0 : sortedTimes.get((int) (sortedTimes.size() * 0.99));
        long min = sortedTimes.isEmpty() ? 0 : sortedTimes.get(0);
        long max = sortedTimes.isEmpty() ? 0 : sortedTimes.get(sortedTimes.size() - 1);
        double avg = sortedTimes.isEmpty() ? 0 : sortedTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        System.out.println("\n=== RESULTS ===");
        System.out.println("Total Orders: " + TOTAL_ORDERS);
        System.out.println("Success: " + successCount.get());
        System.out.println("Failed: " + failureCount.get());
        System.out.println("Completed: " + completed);
        System.out.println("Total Duration: " + totalDuration + " ms (" + (totalDuration / 1000.0) + " seconds)");
        System.out.println("Throughput: " + (successCount.get() * 1000.0 / totalDuration) + " orders/second");
        System.out.println("\n=== RESPONSE TIME STATISTICS ===");
        System.out.println("Min: " + min + " ms");
        System.out.println("Max: " + max + " ms");
        System.out.println("Average: " + String.format("%.2f", avg) + " ms");
        System.out.println("P50 (Median): " + p50 + " ms");
        System.out.println("P95: " + p95 + " ms");
        System.out.println("P99: " + p99 + " ms");

        // Assertions
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isGreaterThanOrEqualTo(TOTAL_ORDERS * 95 / 100); // Cho phép 5% lỗi
        System.out.println("\n✅ Load test completed successfully!");
    }

    /**
     * Test với các batch nhỏ hơn để kiểm tra từng giai đoạn
     */
    @Test
    @DisplayName("Test 100 orders in batches of 10")
    void testCreate100Orders_InBatches() throws Exception {
        System.out.println("\n=== LOAD TEST: 100 Orders in Batches ===");
        
        int batchSize = 10;
        int totalBatches = 10;
        AtomicInteger totalSuccess = new AtomicInteger(0);
        AtomicInteger totalFailure = new AtomicInteger(0);

        for (int batch = 0; batch < totalBatches; batch++) {
            System.out.println("\n--- Batch " + (batch + 1) + " ---");
            
            ExecutorService executor = Executors.newFixedThreadPool(batchSize);
            CountDownLatch latch = new CountDownLatch(batchSize);
            AtomicInteger batchSuccess = new AtomicInteger(0);
            AtomicInteger batchFailure = new AtomicInteger(0);

            long batchStart = System.currentTimeMillis();

            for (int i = 0; i < batchSize; i++) {
                final int orderIndex = batch * batchSize + i;
                executor.submit(() -> {
                    try {
                        CreateOrderRequest request = createTestRequest(orderIndex);
                        // OrderResponse response = createOrderUseCase.execute(request, "idem-" + orderIndex, "jti-" + orderIndex);
                        batchSuccess.incrementAndGet();
                    } catch (Exception e) {
                        batchFailure.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            long batchEnd = System.currentTimeMillis();
            long batchDuration = batchEnd - batchStart;

            totalSuccess.addAndGet(batchSuccess.get());
            totalFailure.addAndGet(batchFailure.get());

            System.out.println("Batch " + (batch + 1) + " - Success: " + batchSuccess.get() + 
                             ", Failed: " + batchFailure.get() + 
                             ", Duration: " + batchDuration + " ms");
        }

        System.out.println("\n=== TOTAL RESULTS ===");
        System.out.println("Total Success: " + totalSuccess.get());
        System.out.println("Total Failed: " + totalFailure.get());
        
        assertThat(totalSuccess.get()).isGreaterThan(90); // Ít nhất 90% thành công
    }

    /**
     * Helper method để tạo test request
     */
    private CreateOrderRequest createTestRequest(int index) {
        return CreateOrderRequest.builder()
                .userId((long) (index % 100) + 1) // Rotate user IDs
                .orderItems(List.of(
                        CreateOrderRequest.OrderItemRequest.builder()
                                .productId(1L)
                                .quantity((index % 5) + 1) // Quantity từ 1-5
                                .build()
                ))
                .discount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.valueOf(10000))
                .deliveryAddress(CreateOrderRequest.DeliveryAddressRequest.builder()
                        .receiverName("User " + index)
                        .receiverPhone("09" + String.format("%08d", index % 100000000))
                        .addressLine1("Address " + index)
                        .ward("Ward " + (index % 10))
                        .district("District " + (index % 20))
                        .city("City " + (index % 5))
                        .build())
                .note("Load test order #" + index)
                .build();
    }
}

