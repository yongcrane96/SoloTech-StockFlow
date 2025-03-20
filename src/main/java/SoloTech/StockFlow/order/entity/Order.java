package SoloTech.StockFlow.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.stereotype.Service;

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
}
