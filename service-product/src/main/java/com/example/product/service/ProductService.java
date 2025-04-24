package com.example.product.service;

import cn.hutool.core.lang.Snowflake;
import com.example.annotations.Cached;
import com.example.cache.CacheType;
import com.example.kafka.CreateProductEvent;
import com.example.kafka.UpdateProductEvent;
import com.example.product.dto.ProductDto;
import com.example.product.entity.Product;
import com.example.product.exception.ProductNotFoundException;
import com.example.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    @Cached(prefix = "product:", key = "#result.productId", ttl = 3600, type = CacheType.WRITE, cacheNull = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Product createProduct(CreateProductEvent event) {
        Product product = Product.builder()
                    .id(event.getId())
                    .productId(event.getProductId())
                    .name(event.getName())
                    .price(event.getPrice())
                    .content(event.getContent())
                    .build();
        try {
            Product savedProduct = productRepository.saveAndFlush(product);
            return savedProduct;
        }catch (Exception e){
            log.error("Product 생성 또는 후속 작업 실패. storeId: {}", product.getProductId(), e);

            // 사가 보상 로직 예시 (간단하게 delete 처리)
            try {
                productRepository.deleteById(product.getId());
                log.info("Product 보상 처리 완료 (삭제). storeId: {}", product.getProductId());
            } catch (Exception compensationEx) {
                log.error("Product 보상 처리 실패. 수동 개입 필요. storeId: {}", product.getProductId(), compensationEx);
            }

            throw new ProductNotFoundException(product.getProductId());
        }
    }

    @Cached(prefix = "product:", key = "#productId", ttl = 3600, type = CacheType.READ, cacheNull = true)
    public Product getProduct(String productId) {
        Product dbProduct = productRepository.findByProductId(productId)
            .orElseThrow(()-> new ProductNotFoundException("Product not found : " + productId));

        return dbProduct;
    }

    @Cached(prefix = "product:", key = "#result.productId", ttl = 3600, type = CacheType.WRITE, cacheNull = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Product updateProduct(UpdateProductEvent event){
        String productId = event.getProductId();
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        log.info("Update product by id: {}", productId);

        product.setContent(event.getContent());
        product.setPrice(event.getPrice());
        product.setName(event.getName());

        Product savedProduct = productRepository.save(product);

        return savedProduct;
    }

    @Cached(prefix = "product:", key = "#productId", ttl = 3600, type = CacheType.DELETE, cacheNull = true)
    public void deleteProduct(String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(()-> new ProductNotFoundException("Product not found : " + productId));
        productRepository.delete(product);
    }
}
