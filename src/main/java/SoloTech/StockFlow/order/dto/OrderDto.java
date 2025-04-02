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

    @Schema(description = "주문 ID", example = "O12345")
    private String orderId;  // 추가!

    @Schema(description = "상점 ID", example = "")
    private String storeId;

    @Schema(description = "제품 ID", example = "")
    private String productId;

    @Schema(description = "재고 ID", example = "")
    private String stockId;

    @Schema(description = "수량", example = "")
    private Long quantity;

    // 🔽 결제 관련 필드 추가
    @Schema(description = "결제 금액", example = "20000")
    private Long amount;

    @Schema(description = "결제 방식", example = "CARD")
    private String paymentMethod;
}
