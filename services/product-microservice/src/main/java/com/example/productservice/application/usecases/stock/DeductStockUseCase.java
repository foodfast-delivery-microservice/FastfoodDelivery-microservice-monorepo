package com.example.productservice.application.usecases.stock;

import com.example.productservice.domain.entities.Product;
import com.example.productservice.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class DeductStockUseCase {
    private final ProductRepository productRepository;

    public void execute(Long productId, Integer quantity, Long merchantId) {
        log.info("[STOCK_DEDUCTION] Deducting stock - productId: {}, quantity: {}, merchantId: {}",
                productId, quantity, merchantId);

        // Tìm product với pessimistic lock để tránh race condition
        Optional<Product> productOpt = productRepository.findByIdAndMerchantIdWithLock(productId, merchantId);

        if (productOpt.isEmpty()) {
            log.error("[STOCK_DEDUCTION] Product not found - productId: {}, merchantId: {}", productId, merchantId);
            throw new IllegalStateException(
                    String.format("Product not found with id: %d and merchantId: %d", productId, merchantId)
            );
        }

        Product product = productOpt.get();

        // Validate product is active
        if (!product.isActive()) {
            log.error("[STOCK_DEDUCTION] Product is not active - productId: {}, merchantId: {}", productId, merchantId);
            throw new IllegalStateException(
                    String.format("Product with id: %d is not active. Cannot deduct stock.", productId)
            );
        }

        // Deduct stock using domain business logic (đã được lock, không có race condition)
        int oldStock = product.getStock() != null ? product.getStock().getQuantity() : 0;
        product.deductStock(quantity);
        productRepository.save(product);

        log.info("[STOCK_DEDUCTION] Stock deducted successfully - productId: {}, merchantId: {}, oldStock: {}, deductedQuantity: {}, newStock: {}",
                productId, merchantId, oldStock, quantity, product.getStock().getQuantity());
    }
}
