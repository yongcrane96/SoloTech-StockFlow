package com.example.order.exception;

import com.example.util.BusinessException;

public class StockNotFoundException extends BusinessException {
    public StockNotFoundException(String stockId) {
        super("Stock not found: " + stockId);
    }
}