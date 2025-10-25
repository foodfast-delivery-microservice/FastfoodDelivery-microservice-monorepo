package com.example.payment.interfaces.rest;

import com.example.payment.application.usecase.HandleVNPayIPNUseCase;
import com.example.payment.application.usecase.HandleVNPayReturnUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/vnpay")
@RequiredArgsConstructor
public class VNPayCallbackController {

    private final HandleVNPayReturnUseCase handleVNPayReturnUseCase;
    private final HandleVNPayIPNUseCase handleVNPayIPNUseCase;

    @GetMapping("/return")
    public ResponseEntity<String> handleReturn(@RequestParam Map<String, String> all) {
        handleVNPayReturnUseCase.execute(new HashMap<>(all));
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/ipn")
    public ResponseEntity<String> handleIpn(@RequestParam Map<String, String> all) {
        String code = handleVNPayIPNUseCase.execute(new HashMap<>(all));
        return ResponseEntity.ok(code);
    }
}



