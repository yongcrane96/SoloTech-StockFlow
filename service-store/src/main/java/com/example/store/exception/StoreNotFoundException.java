package com.example.store.exception;

import com.example.util.BusinessException;

public class StoreNotFoundException extends BusinessException {
    public StoreNotFoundException(String storeId) {
        super("Store not found: " + storeId);
    }
}
