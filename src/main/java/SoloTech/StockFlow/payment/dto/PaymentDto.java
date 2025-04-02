package SoloTech.StockFlow.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 결제 DTO
 *
 * @since   2025-04-02
 * @author  yhkim
 */
@Data
@AllArgsConstructor
public class PaymentDto {

    @Schema(description = "주문 ID", example = "order123")
    private String orderId;

    @Schema(description = "결제 금액", example = "15000")
    private Long amount;

    @Schema(description = "결제 방법 (예: 카드, 현금)", example = "카드")
    private String paymentMethod;

    @Schema(description = "결제 상태 (예: 완료, 실패, 대기)", example = "완료")
    private String paymentStatus;
}
