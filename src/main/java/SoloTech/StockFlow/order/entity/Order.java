package SoloTech.StockFlow.order.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 주문 객체
 *
 * @since   2025-03-18
 * @author  yhkim
 */
@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderId;
    private String storeId;
    private String productId;
    private String stockId;
    private Long quantity;

    // 🔽 결제 관련 필드 추가
    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private String paymentMethod;

    @Column(nullable = false)
    private String paymentStatus; // "PENDING", "SUCCESS", "FAILED"
}
