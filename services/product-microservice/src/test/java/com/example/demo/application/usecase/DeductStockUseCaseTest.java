package com.example.demo.application.usecase;

import com.example.demo.domain.model.Product;
import com.example.demo.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test cho DeductStockUseCase
 * Mục đích: Test logic nghiệp vụ deduct stock KHÔNG CẦN DB thật
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeductStockUseCase Unit Tests")
class DeductStockUseCaseTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private DeductStockUseCase deductStockUseCase;

    private Product validProduct;
    private Long productId;
    private Long merchantId;
    private Integer quantity;

    @BeforeEach
    void setUp() {
        productId = 1L;
        merchantId = 10L;
        quantity = 5;

        validProduct = Product.builder()
                .id(productId)
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(100000))
                .stock(10)
                .category(Product.Category.FOOD)
                .merchantId(merchantId)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("✅ Should deduct stock successfully with sufficient stock")
    void testExecute_Success() {
        // Given
        when(productRepository.findByIdAndMerchantIdWithLock(productId, merchantId))
                .thenReturn(Optional.of(validProduct));

        // When
        deductStockUseCase.execute(productId, quantity, merchantId);

        // Then
        assertThat(validProduct.getStock()).isEqualTo(5); // 10 - 5 = 5
        verify(productRepository, times(1)).findByIdAndMerchantIdWithLock(productId, merchantId);
        verify(productRepository, times(1)).save(validProduct);
    }

    @Test
    @DisplayName("❌ Should throw IllegalStateException when product not found")
    void testExecute_ProductNotFound() {
        // Given
        when(productRepository.findByIdAndMerchantIdWithLock(productId, merchantId))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> deductStockUseCase.execute(productId, quantity, merchantId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Product not found");

        verify(productRepository, times(1)).findByIdAndMerchantIdWithLock(productId, merchantId);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Should throw IllegalStateException when product is not active")
    void testExecute_ProductNotActive() {
        // Given
        validProduct.setActive(false);
        when(productRepository.findByIdAndMerchantIdWithLock(productId, merchantId))
                .thenReturn(Optional.of(validProduct));

        // When & Then
        assertThatThrownBy(() -> deductStockUseCase.execute(productId, quantity, merchantId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is not active");

        verify(productRepository, times(1)).findByIdAndMerchantIdWithLock(productId, merchantId);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Should throw IllegalArgumentException when insufficient stock")
    void testExecute_InsufficientStock() {
        // Given
        validProduct.setStock(3); // Less than quantity (5)
        when(productRepository.findByIdAndMerchantIdWithLock(productId, merchantId))
                .thenReturn(Optional.of(validProduct));

        // When & Then
        assertThatThrownBy(() -> deductStockUseCase.execute(productId, quantity, merchantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock");

        verify(productRepository, times(1)).findByIdAndMerchantIdWithLock(productId, merchantId);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("✅ Should deduct exact quantity when stock equals quantity")
    void testExecute_ExactStockMatch() {
        // Given
        validProduct.setStock(5); // Exactly equals quantity
        when(productRepository.findByIdAndMerchantIdWithLock(productId, merchantId))
                .thenReturn(Optional.of(validProduct));

        // When
        deductStockUseCase.execute(productId, quantity, merchantId);

        // Then
        assertThat(validProduct.getStock()).isEqualTo(0); // 5 - 5 = 0
        verify(productRepository, times(1)).findByIdAndMerchantIdWithLock(productId, merchantId);
        verify(productRepository, times(1)).save(validProduct);
    }

    @Test
    @DisplayName("✅ Should use pessimistic lock method")
    void testExecute_UsesPessimisticLock() {
        // Given
        when(productRepository.findByIdAndMerchantIdWithLock(productId, merchantId))
                .thenReturn(Optional.of(validProduct));

        // When
        deductStockUseCase.execute(productId, quantity, merchantId);

        // Then
        verify(productRepository, times(1)).findByIdAndMerchantIdWithLock(productId, merchantId);
        verify(productRepository, never()).findByIdAndMerchantId(any(), any());
    }
}



