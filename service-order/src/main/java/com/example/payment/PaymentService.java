package com.example.payment;

import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentFeignClient paymentFeignClient;

    public PaymentResponse createPayment(PaymentRequest paymentId){
        return paymentFeignClient.createPayment(paymentId);
    }
}
