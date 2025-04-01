package SoloTech.StockFlow.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 주문 DTO
 *
 * @since   2025-03-18
 * @author  yhkim
 */
@Data
@AllArgsConstructor
public class OrderDto {

    @Schema(description = "상점 ID", example = "")
    private String storeId;

    @Schema(description = "제품 ID", example = "")
    private String productId;

    @Schema(description = "재고 ID", example = "")
    private String stockId;

    @Schema(description = "수량", example = "")
    private Long quantity;
}
