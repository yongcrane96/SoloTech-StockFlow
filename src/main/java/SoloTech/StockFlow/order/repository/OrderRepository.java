package SoloTech.StockFlow.order.repository;


import SoloTech.StockFlow.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 주문 레퍼지토리
 *
 * @since   2025-03-18
 * @author  yhkim
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

}
