package com.example.paymentservice.infrastructure.listener;

import com.example.paymentservice.application.usecase.FailPendingPaymentsDueToMerchantDeactivatedUseCase;
import com.example.paymentservice.infrastructure.config.RabbitMQConfig;
import com.example.paymentservice.infrastructure.event.MerchantDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class MerchantDeactivatedListener {

    private final FailPendingPaymentsDueToMerchantDeactivatedUseCase failPaymentsUseCase;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_MERCHANT_DEACTIVATED_QUEUE)
    @Transactional
    public void handleMerchantDeactivated(MerchantDeactivatedEvent event) {
        Long merchantId = event.getMerchantId();
        if (merchantId == null) {
            log.warn("Received MerchantDeactivatedEvent without merchantId: {}", event);
            return;
        }
        log.info("Failing pending payments for merchant {}", merchantId);
        failPaymentsUseCase.execute(merchantId);
    }
}

