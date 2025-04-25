package com.example.product;

import com.example.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductFeignClient productFeignClient;

    public ProductResponse getProduct(String productId) {
        return productFeignClient.getProduct(productId);
    }
}