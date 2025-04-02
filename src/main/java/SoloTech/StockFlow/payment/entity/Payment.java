package SoloTech.StockFlow.payment.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 결제 객체
 *
 * @since   2025-04-02
 * @author  yhkim
 */
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String paymentId;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private String paymentMethod;

    @Column(nullable = false)
    private String paymentStatus;
}
