package com.example.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor // 이거 있으면 자동 생성됨
@NoArgsConstructor
public class PaymentRequest {
    private String paymentId;
    private String orderId;
    private Long amount;
    private String paymentMethod;
    private PaymentStatus paymentStatus;
}
