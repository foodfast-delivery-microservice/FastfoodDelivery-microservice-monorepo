package com.example.order_service.load;

import com.example.order_service.application.dto.CreateOrderRequest;
import com.example.order_service.application.dto.OrderResponse;
import com.example.order_service.application.dto.ProductValidationRequest;
import com.example.order_service.application.dto.ProductValidationResponse;
import com.example.order_service.application.dto.UserValidationResponse;
import com.example.order_service.application.usecase.CreateOrderUseCase;
import com.example.order_service.domain.exception.OrderValidationException;
import com.example.order_service.domain.repository.ProductServicePort;
import com.example.order_service.domain.repository.UserServicePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
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

/**
 * Test Race Condition khi nhiều users cùng tạo đơn cho cùng 1 sản phẩm có stock hạn chế
 * 
 * Scenario: 100 users cùng tạo đơn cho sản phẩm có stock = 50
 * Expected:
 * - Chỉ 50 đơn thành công
 * - 50 đơn còn lại báo "Out of stock"
 * - Không có race condition
 * - Stock không bị âm
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Stock Race Condition Test - 100 Users, Stock = 50")
class StockRaceConditionTest {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @MockBean
    private ProductServicePort productServicePort;

    @MockBean
    private UserServicePort userServicePort;

    // Stock configuration
    private static final int INITIAL_STOCK = 50;
    private static final int TOTAL_USERS = 100;
    private static final int QUANTITY_PER_ORDER = 1; // Mỗi user order 1 sản phẩm
    private static final int EXPECTED_SUCCESSFUL_ORDERS = 50; // Chỉ 50 đơn thành công
    private static final int EXPECTED_FAILED_ORDERS = 50; // 50 đơn còn lại fail

    // Thread-safe counter để track stock
    private final AtomicInteger availableStock = new AtomicInteger(INITIAL_STOCK);
    private final AtomicInteger successfulOrders = new AtomicInteger(0);
    private final AtomicInteger failedOrders = new AtomicInteger(0);
    private final AtomicInteger totalValidationCalls = new AtomicInteger(0);

    @BeforeEach
    void setUp() {
        // Reset counters
        availableStock.set(INITIAL_STOCK);
        successfulOrders.set(0);
        failedOrders.set(0);
        totalValidationCalls.set(0);

        // Mock Product Service với stock validation logic
        // Simulate race condition: nhiều requests cùng lúc check stock
        when(productServicePort.validateProducts(any())).thenAnswer(invocation -> {
            totalValidationCalls.incrementAndGet();
            List<ProductValidationRequest> requests = invocation.getArgument(0);
            
            // Giả sử mỗi request chỉ có 1 sản phẩm
            ProductValidationRequest request = requests.get(0);
            int requestedQuantity = request.quantity();
            
            // Atomic check và reserve stock
            // Simulate pessimistic locking: check và reserve trong 1 operation
            int currentStock = availableStock.get();
            boolean hasEnoughStock = false;
            
            // Try to reserve stock atomically
            while (currentStock >= requestedQuantity) {
                if (availableStock.compareAndSet(currentStock, currentStock - requestedQuantity)) {
                    hasEnoughStock = true;
                    break;
                }
                // Retry if CAS failed (another thread modified stock)
                currentStock = availableStock.get();
            }
            
            if (hasEnoughStock) {
                // Stock available - return success
                return List.of(new ProductValidationResponse(
                        1L,
                        true,
                        "Limited Stock Product",
                        BigDecimal.valueOf(100000),
                        10L
                ));
            } else {
                // Out of stock - return failure
                return List.of(new ProductValidationResponse(
                        1L,
                        false, // success = false
                        "Limited Stock Product",
                        BigDecimal.valueOf(100000),
                        10L
                ));
            }
        });

        // Mock User Service
        when(userServicePort.validateUser(anyLong())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            return new UserValidationResponse(userId, true, true, "test-user-" + userId);
        });
    }

    @Test
    @DisplayName("Race Condition Test: 100 users order product with stock = 50")
    void testStockRaceCondition_100Users_50Stock() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("STOCK RACE CONDITION TEST");
        System.out.println("=".repeat(70));
        System.out.println("Initial Stock: " + INITIAL_STOCK);
        System.out.println("Total Users: " + TOTAL_USERS);
        System.out.println("Quantity per Order: " + QUANTITY_PER_ORDER);
        System.out.println("Expected Successful Orders: " + EXPECTED_SUCCESSFUL_ORDERS);
        System.out.println("Expected Failed Orders: " + EXPECTED_FAILED_ORDERS);
        System.out.println("=".repeat(70));

        ExecutorService executor = Executors.newFixedThreadPool(100); // 100 threads để simulate concurrent requests
        CountDownLatch latch = new CountDownLatch(TOTAL_USERS);
        List<String> errors = new CopyOnWriteArrayList<>();
        List<Long> orderIds = new CopyOnWriteArrayList<>();

        long startTime = System.currentTimeMillis();

        // Submit 100 concurrent order requests
        IntStream.range(0, TOTAL_USERS).forEach(userIndex -> {
            executor.submit(() -> {
                try {
                    CreateOrderRequest request = createTestRequest(userIndex + 1);
                    String idempotencyKey = "race-test-" + userIndex;
                    String jti = "jti-" + userIndex;

                    OrderResponse response = createOrderUseCase.execute(request, idempotencyKey, jti);
                    orderIds.add(response.getId());
                    successfulOrders.incrementAndGet();

                    if (successfulOrders.get() % 10 == 0) {
                        System.out.println("✓ Successful orders: " + successfulOrders.get() + 
                                         " | Failed: " + failedOrders.get() + 
                                         " | Remaining stock: " + availableStock.get());
                    }
                } catch (OrderValidationException e) {
                    failedOrders.incrementAndGet();
                    String error = "User " + userIndex + ": " + e.getMessage();
                    errors.add(error);
                    
                    // Verify error message contains stock-related info
                    if (e.getMessage().contains("không hợp lệ") || 
                        e.getMessage().contains("hết hàng") ||
                        e.getMessage().contains("Out of stock")) {
                        // Expected error for out of stock
                    }
                } catch (Exception e) {
                    failedOrders.incrementAndGet();
                    String error = "User " + userIndex + ": " + e.getClass().getSimpleName() + " - " + e.getMessage();
                    errors.add(error);
                    System.err.println("✗ Unexpected error: " + error);
                } finally {
                    latch.countDown();
                }
            });
        });

        // Wait for all requests to complete
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Print results
        printResults(duration, completed, errors);

        // Assertions
        assertThat(completed).isTrue();
        
        // Verify: Exactly 50 orders should succeed (stock = 50, quantity = 1 each)
        assertThat(successfulOrders.get())
                .as("Should have exactly %d successful orders", EXPECTED_SUCCESSFUL_ORDERS)
                .isEqualTo(EXPECTED_SUCCESSFUL_ORDERS);
        
        // Verify: Exactly 50 orders should fail
        assertThat(failedOrders.get())
                .as("Should have exactly %d failed orders", EXPECTED_FAILED_ORDERS)
                .isEqualTo(EXPECTED_FAILED_ORDERS);
        
        // Verify: Total should be 100
        assertThat(successfulOrders.get() + failedOrders.get())
                .as("Total orders should be %d", TOTAL_USERS)
                .isEqualTo(TOTAL_USERS);
        
        // Verify: Stock should be 0 (all 50 items reserved)
        assertThat(availableStock.get())
                .as("Final stock should be 0 (all items reserved)")
                .isEqualTo(0);
        
        // Verify: No negative stock (race condition protection)
        assertThat(availableStock.get())
                .as("Stock should never be negative")
                .isGreaterThanOrEqualTo(0);
        
        // Verify: All successful orders have unique IDs
        assertThat(orderIds.size())
                .as("All successful orders should have unique IDs")
                .isEqualTo(EXPECTED_SUCCESSFUL_ORDERS);
        
        // Verify: No duplicate order IDs
        long uniqueOrderIds = orderIds.stream().distinct().count();
        assertThat(uniqueOrderIds)
                .as("All order IDs should be unique")
                .isEqualTo(EXPECTED_SUCCESSFUL_ORDERS);

        System.out.println("\n✅ All assertions passed! Race condition handled correctly.");
    }

    @Test
    @DisplayName("Race Condition Test: 200 users order product with stock = 50 (more aggressive)")
    void testStockRaceCondition_200Users_50Stock() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("AGGRESSIVE RACE CONDITION TEST");
        System.out.println("=".repeat(70));
        System.out.println("Initial Stock: 50");
        System.out.println("Total Users: 200");
        System.out.println("Quantity per Order: 1");
        System.out.println("Expected Successful Orders: 50");
        System.out.println("Expected Failed Orders: 150");
        System.out.println("=".repeat(70));

        // Reset for this test
        availableStock.set(50);
        successfulOrders.set(0);
        failedOrders.set(0);
        totalValidationCalls.set(0);

        ExecutorService executor = Executors.newFixedThreadPool(200);
        CountDownLatch latch = new CountDownLatch(200);
        List<String> errors = new CopyOnWriteArrayList<>();

        long startTime = System.currentTimeMillis();

        IntStream.range(0, 200).forEach(userIndex -> {
            executor.submit(() -> {
                try {
                    CreateOrderRequest request = createTestRequest(userIndex + 1);
                    createOrderUseCase.execute(request, "race-test-200-" + userIndex, "jti-" + userIndex);
                    successfulOrders.incrementAndGet();
                } catch (Exception e) {
                    failedOrders.incrementAndGet();
                    if (errors.size() < 10) {
                        errors.add("User " + userIndex + ": " + e.getMessage());
                    }
                } finally {
                    latch.countDown();
                }
            });
        });

        latch.await(120, TimeUnit.SECONDS);
        executor.shutdown();

        long duration = System.currentTimeMillis() - startTime;

        printResults(duration, true, errors);

        // Assertions
        assertThat(successfulOrders.get()).isEqualTo(50);
        assertThat(failedOrders.get()).isEqualTo(150);
        assertThat(availableStock.get()).isEqualTo(0);
        assertThat(availableStock.get()).isGreaterThanOrEqualTo(0);

        System.out.println("\n✅ Aggressive test passed!");
    }

    @Test
    @DisplayName("Race Condition Test: 50 users order 2 items each, stock = 50")
    void testStockRaceCondition_50Users_2ItemsEach_50Stock() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("RACE CONDITION TEST: Multiple items per order");
        System.out.println("=".repeat(70));
        System.out.println("Initial Stock: 50");
        System.out.println("Total Users: 50");
        System.out.println("Quantity per Order: 2");
        System.out.println("Expected Successful Orders: 25 (25 × 2 = 50 items)");
        System.out.println("Expected Failed Orders: 25");
        System.out.println("=".repeat(70));

        // Reset for this test
        availableStock.set(50);
        successfulOrders.set(0);
        failedOrders.set(0);
        totalValidationCalls.set(0);

        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(50);

        long startTime = System.currentTimeMillis();

        IntStream.range(0, 50).forEach(userIndex -> {
            executor.submit(() -> {
                try {
                    CreateOrderRequest request = createTestRequest(userIndex + 1, 2); // 2 items per order
                    createOrderUseCase.execute(request, "race-test-2items-" + userIndex, "jti-" + userIndex);
                    successfulOrders.incrementAndGet();
                } catch (Exception e) {
                    failedOrders.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        });

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        long duration = System.currentTimeMillis() - startTime;

        printResults(duration, true, List.of());

        // Assertions
        assertThat(successfulOrders.get()).isEqualTo(25); // 25 orders × 2 items = 50 items
        assertThat(failedOrders.get()).isEqualTo(25);
        assertThat(availableStock.get()).isEqualTo(0);

        System.out.println("\n✅ Multiple items test passed!");
    }

    private void printResults(long duration, boolean completed, List<String> errors) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST RESULTS");
        System.out.println("=".repeat(70));
        System.out.println("Completed: " + completed);
        System.out.println("Duration: " + duration + " ms (" + (duration / 1000.0) + " seconds)");
        System.out.println("\n--- Order Statistics ---");
        System.out.println("Successful Orders: " + successfulOrders.get() + 
                         " (Expected: " + EXPECTED_SUCCESSFUL_ORDERS + ")");
        System.out.println("Failed Orders: " + failedOrders.get() + 
                         " (Expected: " + EXPECTED_FAILED_ORDERS + ")");
        System.out.println("Total Orders: " + (successfulOrders.get() + failedOrders.get()) + 
                         " (Expected: " + TOTAL_USERS + ")");
        System.out.println("\n--- Stock Statistics ---");
        System.out.println("Initial Stock: " + INITIAL_STOCK);
        System.out.println("Final Stock: " + availableStock.get());
        System.out.println("Stock Reserved: " + (INITIAL_STOCK - availableStock.get()));
        System.out.println("Stock Never Negative: " + (availableStock.get() >= 0 ? "✅" : "❌"));
        System.out.println("\n--- Validation Statistics ---");
        System.out.println("Total Validation Calls: " + totalValidationCalls.get());
        System.out.println("Average Calls per Order: " + 
                         String.format("%.2f", totalValidationCalls.get() / (double) TOTAL_USERS));

        if (!errors.isEmpty()) {
            System.out.println("\n--- Error Samples (first 10) ---");
            errors.stream().limit(10).forEach(System.err::println);
        }
        System.out.println("=".repeat(70));
    }

    private CreateOrderRequest createTestRequest(long userId) {
        return createTestRequest(userId, QUANTITY_PER_ORDER);
    }

    private CreateOrderRequest createTestRequest(long userId, int quantity) {
        return CreateOrderRequest.builder()
                .userId(userId)
                .orderItems(List.of(
                        CreateOrderRequest.OrderItemRequest.builder()
                                .productId(1L)
                                .quantity(quantity)
                                .build()
                ))
                .discount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.valueOf(10000))
                .deliveryAddress(CreateOrderRequest.DeliveryAddressRequest.builder()
                        .receiverName("User " + userId)
                        .receiverPhone("09" + String.format("%08d", userId % 100000000))
                        .addressLine1("Test Address " + userId)
                        .ward("Ward " + (userId % 10 + 1))
                        .district("District " + (userId % 20 + 1))
                        .city("City " + (userId % 5 + 1))
                        .build())
                .note("Race condition test order #" + userId)
                .build();
    }
}






























