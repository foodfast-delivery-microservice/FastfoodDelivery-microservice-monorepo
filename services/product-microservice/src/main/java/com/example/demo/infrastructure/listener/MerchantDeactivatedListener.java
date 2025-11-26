package com.example.demo.infrastructure.listener;

import com.example.demo.domain.repository.ProductRepository;
import com.example.demo.infrastructure.config.RabbitMQConfig;
import com.example.demo.infrastructure.event.MerchantDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MerchantDeactivatedListener {

    private final ProductRepository productRepository;

    @RabbitListener(queues = RabbitMQConfig.MERCHANT_DEACTIVATED_QUEUE)
    @Transactional
    public void handleMerchantDeactivated(MerchantDeactivatedEvent event) {
        Long merchantId = event.getMerchantId();
        if (merchantId == null) {
            log.warn("Received MerchantDeactivatedEvent without merchantId: {}", event);
            return;
        }
        log.info("Processing MerchantDeactivatedEvent for merchantId: {}", merchantId);
        productRepository.deactivateProductsByMerchantId(merchantId);
    }
}

