package com.example.order.exception;

import SoloTech.StockFlow.common.util.BusinessException;

public class OrderCreationException extends BusinessException {
    public OrderCreationException(String message) {
        super("Order creation failed: " + message);
    }
}
