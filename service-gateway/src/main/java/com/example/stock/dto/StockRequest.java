package com.example.stock.dto;

import lombok.Data;

@Data
public class StockRequest {
    private String storeId;
    private String productId;
    private long stock;
}
