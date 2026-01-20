package com.example.productservice.infrastructure.messaging.listener;

import com.example.productservice.domain.repository.ProductRepository;
import com.example.productservice.infrastructure.config.RabbitMQConfig;
import com.example.productservice.infrastructure.messaging.event.MerchantActivatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MerchantActivatedListener {

    private final ProductRepository productRepository;

    @RabbitListener(queues = RabbitMQConfig.MERCHANT_ACTIVATED_QUEUE)
    @Transactional
    public void handleMerchantActivated(MerchantActivatedEvent event) {
        Long merchantId = event.getMerchantId();
        if (merchantId == null) {
            log.warn("Received MerchantActivatedEvent without merchantId: {}", event);
            return;
        }
        int updated = productRepository.reactivateProductsByMerchantId(merchantId);
        log.info("MerchantActivatedListener - merchantId={} reactivatedProducts={}", merchantId, updated);
    }
}
