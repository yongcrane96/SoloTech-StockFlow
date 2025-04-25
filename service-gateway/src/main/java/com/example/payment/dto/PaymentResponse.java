package com.example.payment.dto;

import lombok.Data;

@Data
public class PaymentResponse {
    private long id;
    private String paymentId;
    private String orderId;
    private Long amount;
    private String paymentMethod;
    private Enum paymentStatus;
}
