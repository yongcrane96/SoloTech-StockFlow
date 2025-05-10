package com.example.product;

import com.example.product.dto.ProductRequest;
import com.example.product.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductFeignClient productFeignClient;

    @PostMapping
    public String createProduct(@RequestBody ProductRequest request) {
        return productFeignClient.createProduct(request);
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable String productId) {
        return productFeignClient.getProduct(productId);
    }

    @PutMapping("/{productId}")
    public boolean updateProduct(
            @PathVariable String productId,
            @RequestBody ProductRequest request
    ) {
        return productFeignClient.updateProducts(productId, request);
    }

    @DeleteMapping("/{productId}")
    public boolean deleteStore(@PathVariable String productId) {
        return productFeignClient.deleteProduct(productId);
    }
}
