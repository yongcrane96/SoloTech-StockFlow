package com.example.product.controller;

import cn.hutool.core.lang.Snowflake;
import com.example.controller.BaseRestController;
import com.example.kafka.CreateProductEvent;
import com.example.kafka.UpdateProductEvent;
import com.example.product.dto.ProductDto;
import com.example.product.entity.Product;
import com.example.product.kafka.ProductEventProducer;
import com.example.product.service.ProductService;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 상품 컨트롤러
 *
 * @since   2025-03-25
 * @author  yhkim
 */

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController extends BaseRestController {

    final ProductEventProducer eventProducer;
    final ProductService productService;

    // 상품 등록
    @PostMapping
    @Operation(summary = "상품 등록", description = "상픔을 생성하고 생성된 상품 객체를 반환합니다.")
    public long createProduct(@RequestBody ProductDto dto){
        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();
        CreateProductEvent event = new CreateProductEvent(
                snowflakeId,
                dto.getProductId(),
                dto.getName(),
                dto.getPrice(),
                dto.getContent()
        );

        eventProducer.sendCommandEvent(event);

        return snowflakeId;
    }

    // 상품 조회
    @GetMapping("{productId}")
    @Operation(summary = "상품 조회", description = "상품 정보를 1건 조회합니다.")
    public ResponseEntity<?> getProduct(@PathVariable String productId){
        Product product = productService.getProduct(productId);

        if (product == null) {
            return getErrorResponse("상품을 찾을 수 없습니다: " + productId);
        }

        return getOkResponse(product);
    }

    // 상품 수정
    @PutMapping("{productId}")
    @Operation(summary = "상품 수정", description = "상품 정보를 수정합니다.")
    public boolean updateProduct(@PathVariable String productId, @RequestBody ProductDto dto) throws JsonMappingException {

        UpdateProductEvent event = new UpdateProductEvent(
                dto.getProductId(),
                dto.getName(),
                dto.getPrice(),
                dto.getContent()
        );
        eventProducer.sendCommandEvent(event);
        return true;
    }

    // 상품 삭제
    @DeleteMapping("{productId}")
    @Operation(summary = "상품 삭제", description = "상품 정보를 삭제합니다.")
    public boolean deleteProduct(@PathVariable String productId){
        productService.deleteProduct(productId);
        return true;
    }
}
