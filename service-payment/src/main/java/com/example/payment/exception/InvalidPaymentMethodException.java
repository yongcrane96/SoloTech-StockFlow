package com.example.payment.exception;

import com.example.util.BusinessException;

public class InvalidPaymentMethodException extends BusinessException {
    public InvalidPaymentMethodException(String method) {
        super("Invalid payment method: " + method);
    }
}