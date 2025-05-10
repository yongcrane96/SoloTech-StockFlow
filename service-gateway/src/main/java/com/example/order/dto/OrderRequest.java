package com.example.order.dto;

import lombok.Data;

@Data
public class OrderRequest {
    private String storeId;
    private String productId;
    private String stockId;
    private long quantity;
}
