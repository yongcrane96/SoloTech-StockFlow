package com.example.product;

import com.example.product.dto.ProductRequest;
import com.example.product.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "productClient", url = "http://localhost:8082")
public interface ProductFeignClient {

    @PostMapping("/api/product")
    String createProduct(@RequestBody ProductRequest request);

    @GetMapping("/api/product/{productId}")
    ProductResponse getProduct(@PathVariable("productId") String productId);

    @PutMapping("/api/product/{productId}")
    boolean updateProducts(
            @PathVariable("productId") String productId,
            @RequestBody ProductRequest request
    );

    @DeleteMapping("/api/product/{productId}")
    boolean deleteProduct(@PathVariable("productId") String productId);

}
