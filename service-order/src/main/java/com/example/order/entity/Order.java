package com.example.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String paymentId;
    private Long quantity;
    private OrderStatus orderStatus;

    // 🔽 결제 관련 필드 추가
    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private String paymentMethod;

    @Column(nullable = false)
    private Enum paymentStatus; // "PENDING", "SUCCESS", "FAILED"

    public void cancel(){
        this.orderStatus = OrderStatus.CANCELED;
    }

    public void updateStatus(OrderStatus newStatus) {
        if (this.orderStatus == OrderStatus.CANCELED || this.orderStatus == OrderStatus.SUCCESS) {
            throw new IllegalStateException("이미 처리 완료된 주문은 상태를 변경할 수 없습니다.");
        }
        this.orderStatus = newStatus;
    }

}
