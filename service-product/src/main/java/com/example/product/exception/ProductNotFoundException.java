package com.example.product.exception;

import com.example.util.BusinessException;

public class ProductNotFoundException extends BusinessException {
    public ProductNotFoundException(String productId) {
        super("Product not found: " + productId);
    }
}
