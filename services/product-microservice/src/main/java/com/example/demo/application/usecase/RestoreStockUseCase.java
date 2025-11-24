package com.example.demo.application.usecase;

import com.example.demo.domain.model.Product;
import com.example.demo.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class RestoreStockUseCase {
    private final ProductRepository productRepository;

    public void execute(Long productId, Integer quantity, Long merchantId) {
        log.info("[STOCK_RESTORE] Restoring stock - productId: {}, quantity: {}, merchantId: {}", 
                productId, quantity, merchantId);

        // Tìm product với pessimistic lock để tránh race condition
        Optional<Product> productOpt = productRepository.findByIdAndMerchantIdWithLock(productId, merchantId);

        if (productOpt.isEmpty()) {
            log.error("[STOCK_RESTORE] Product not found - productId: {}, merchantId: {}", productId, merchantId);
            throw new IllegalStateException(
                    String.format("Product not found with id: %d and merchantId: %d", productId, merchantId)
            );
        }

        Product product = productOpt.get();

        // Validate product is active
        if (!product.isActive()) {
            log.error("[STOCK_RESTORE] Product is not active - productId: {}, merchantId: {}", productId, merchantId);
            throw new IllegalStateException(
                    String.format("Product with id: %d is not active. Cannot restore stock.", productId)
            );
        }

        // Restore stock (đã được lock, không có race condition)
        int oldStock = product.getStock();
        product.setStock(product.getStock() + quantity);
        productRepository.save(product);

        log.info("[STOCK_RESTORE] Stock restored successfully - productId: {}, merchantId: {}, oldStock: {}, restoredQuantity: {}, newStock: {}", 
                productId, merchantId, oldStock, quantity, product.getStock());
    }
}


