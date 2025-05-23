package com.example.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 재고 DTO
 *
 * @since   2025-03-25
 * @author  yhkim
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockDto {
    // 재고에 필요한 것들
    // 재고 수량, 창고(상점) ID, 제품 ID

    @Schema(description = "상점 ID", example = "")
    private String storeId;

    @Schema(description = "재고 ID", example = "")
    private String stockId;

    @Schema(description = "제품 ID", example = "")
    private String productId;

    @Schema(description = "재고", example = "")
    private Long stock;
}
