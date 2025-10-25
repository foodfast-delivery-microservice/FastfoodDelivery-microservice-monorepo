package com.example.payment.application.usecase;

import com.example.payment.domain.model.Payment;
import com.example.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class HandleVNPayReturnUseCase {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment execute(Map<String, String> params) {
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
        return paymentRepository.save(payment);
    }
}



