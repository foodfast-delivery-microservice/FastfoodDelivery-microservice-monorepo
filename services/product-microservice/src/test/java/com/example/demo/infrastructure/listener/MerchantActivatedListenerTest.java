package com.example.demo.infrastructure.listener;

import com.example.demo.domain.repository.ProductRepository;
import com.example.demo.infrastructure.event.MerchantActivatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantActivatedListenerTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private MerchantActivatedListener listener;

    @Test
    @DisplayName("Should reactivate products for merchant when event contains ID")
    void handleMerchantActivated() {
        MerchantActivatedEvent event = new MerchantActivatedEvent(5L, Instant.now(), "reactivated", "admin");
        when(productRepository.reactivateProductsByMerchantId(5L)).thenReturn(3);

        listener.handleMerchantActivated(event);

        verify(productRepository).reactivateProductsByMerchantId(5L);
    }

    @Test
    @DisplayName("Should skip processing when merchantId is missing")
    void skipWhenMerchantIdMissing() {
        MerchantActivatedEvent event = new MerchantActivatedEvent(null, Instant.now(), "reactivated", "admin");

        listener.handleMerchantActivated(event);

        verifyNoInteractions(productRepository);
    }
}

