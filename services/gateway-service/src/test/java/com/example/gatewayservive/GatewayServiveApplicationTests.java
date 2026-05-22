package com.example.gatewayservive;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = com.example.gatewayservice.GatewayServiceApplication.class,
    properties = {
        "app.jwt.base64-secretkey=dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2VzLW9ubHk="
    }
)
class GatewayServiveApplicationTests {

    @Test
    void contextLoads() {
    }

}
