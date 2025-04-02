package SoloTech.StockFlow.payment.repository;

import SoloTech.StockFlow.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 결제 레퍼지토리
 *
 * @since   2025-04-02
 * @author  yhkim
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 결제 ID로 결제 조회
    Optional<Payment> findByPaymentId(String paymentId);

    // 주문 ID로 결제 조회
    Optional<Payment> findByOrderId(String orderId);
}
