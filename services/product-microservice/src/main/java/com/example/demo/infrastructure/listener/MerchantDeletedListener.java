package com.example.demo.infrastructure.listener;

import com.example.demo.domain.repository.ProductRepository;
import com.example.demo.infrastructure.config.RabbitMQConfig;
import com.example.demo.infrastructure.event.MerchantDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MerchantDeletedListener {

    private final ProductRepository productRepository;

    @RabbitListener(queues = RabbitMQConfig.MERCHANT_DELETED_QUEUE)
    @Transactional
    public void handleMerchantDeleted(MerchantDeletedEvent event) {
        Long merchantId = event.getMerchantId();
        if (merchantId == null) {
            log.warn("Received MerchantDeletedEvent without merchantId: {}", event);
            return;
        }
        log.info("Processing MerchantDeletedEvent for merchantId: {}, deleting all products", merchantId);
        try {
            productRepository.deleteProductsByMerchantId(merchantId);
            log.info("Successfully deleted all products for merchantId: {}", merchantId);
        } catch (Exception e) {
            log.error("Failed to delete products for merchantId: {}", merchantId, e);
            throw e; // Re-throw to trigger retry mechanism
        }
    }
}

