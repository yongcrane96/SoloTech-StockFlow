package com.example.stock.exception;

import com.example.util.BusinessException;

public class OutOfStockException extends BusinessException {
    public OutOfStockException(String productId) {
        super("Insufficient stock for product: " + productId);
    }
}
