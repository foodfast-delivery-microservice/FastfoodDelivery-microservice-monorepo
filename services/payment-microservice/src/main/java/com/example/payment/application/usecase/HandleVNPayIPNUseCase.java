package com.example.payment.application.usecase;

import com.example.payment.domain.model.Payment;
import com.example.payment.domain.repository.PaymentRepository;
import com.example.payment.infrastructure.config.VNPayConfig;
import com.example.payment.infrastructure.vnpay.VNPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class HandleVNPayIPNUseCase {

    private final PaymentRepository paymentRepository;
    private final VNPayConfig config;

    @Transactional
    public String execute(Map<String, String> params) {
        // Verify signature
        String receivedHash = params.remove("vnp_SecureHash");
        String query = buildSortedQuery(params);
        String calculated = VNPayUtil.hmacSHA512(config.hashSecret, query);
        if (!calculated.equalsIgnoreCase(receivedHash)) {
            return "97"; // Invalid signature
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");

        Payment payment = paymentRepository.findByVnpTxnRef(txnRef)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if ("00".equals(responseCode)) {
            payment.setStatus(Payment.Status.SUCCESS);
        } else {
            payment.setStatus(Payment.Status.FAILED);
        }
        payment.setVnpTransactionNo(transactionNo);
        payment.setVnpBankCode(bankCode);
        paymentRepository.save(payment);

        return "00"; // OK
    }

    private String buildSortedQuery(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }
}



