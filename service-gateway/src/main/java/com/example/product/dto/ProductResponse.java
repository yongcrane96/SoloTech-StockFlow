package com.example.product.dto;

import lombok.Data;

@Data
public class ProductResponse {
    private Long id;
    private String name;
    private int price;
}
