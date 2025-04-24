package com.example.payment.exception;

import com.example.util.BusinessException;

public class PaymentFailedException extends BusinessException {
    public PaymentFailedException(String paymentId) {
        super("Payment failed for paymentId: " + paymentId);
    }
}