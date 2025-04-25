package com.example.order.exception;


import com.example.util.BusinessException;

public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(String orderId){
        super("Order not found: " + orderId);
    }
}
