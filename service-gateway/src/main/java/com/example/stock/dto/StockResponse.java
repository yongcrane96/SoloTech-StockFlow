package com.example.stock.dto;

import lombok.Data;

@Data
public class StockResponse {
    private long id;
    private String stockId;
    private String storeId;
    private String productId;
    private long stock;
}
