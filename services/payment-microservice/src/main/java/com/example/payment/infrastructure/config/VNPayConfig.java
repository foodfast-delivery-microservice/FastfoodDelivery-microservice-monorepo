package com.example.payment.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VNPayConfig {

    @Value("${vnpay.tmn-code}")
    public String tmnCode;

    @Value("${vnpay.hash-secret}")
    public String hashSecret;

    @Value("${vnpay.url}")
    public String vnpUrl;

    @Value("${vnpay.return-url}")
    public String returnUrl;

    @Value("${vnpay.ipn-url}")
    public String ipnUrl;
}



