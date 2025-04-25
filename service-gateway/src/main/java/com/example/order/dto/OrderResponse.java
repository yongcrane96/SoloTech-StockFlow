package com.example.order.dto;

import lombok.Data;

@Data
public class OrderResponse {
    private Long id;
    private String orderId;
    private String storeId;
    private String productId;
    private String stockId;
    private long quantity;
}
