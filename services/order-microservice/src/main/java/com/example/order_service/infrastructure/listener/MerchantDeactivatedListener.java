package com.example.order_service.infrastructure.listener;

import com.example.order_service.application.usecase.CancelOrdersDueToMerchantDeactivatedUseCase;
import com.example.order_service.infrastructure.config.RabbitMQConfig;
import com.example.order_service.infrastructure.event.MerchantDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MerchantDeactivatedListener {

    private final CancelOrdersDueToMerchantDeactivatedUseCase cancelOrdersUseCase;

    @RabbitListener(queues = RabbitMQConfig.ORDER_MERCHANT_DEACTIVATED_QUEUE)
    @Transactional
    public void handleMerchantDeactivated(MerchantDeactivatedEvent event) {
        Long merchantId = event.getMerchantId();
        if (merchantId == null) {
            log.warn("MerchantDeactivatedEvent missing merchantId: {}", event);
            return;
        }
        log.info("Received MerchantDeactivatedEvent for merchant {}. Cancelling pending orders.", merchantId);
        cancelOrdersUseCase.execute(merchantId);
    }
}

