package com.example.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 상품 DTO
 *
 * @since   2025-03-25
 * @author  yhkim
 */
@Data
@AllArgsConstructor
public class ProductDto {
    @Schema(description = "상품코드", example = "")
    private String productId;

    @Schema(description = "상품명", example = "")
    private String name;

    @Schema(description = "상품 가격", example = "")
    private Long price;

    @Schema(description = "상품 상세설명", example = "")
    private String content;
}
