package com.example.order.repository;

import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 주문 레퍼지토리
 *
 * @since   2025-03-18
 * @author  yhkim
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderId(String orderId); // Optional 클래스는 Order가 null이여도 처리 가능

    @Modifying
    @Query("UPDATE Order o SET o.orderStatus = :status WHERE o.orderId = :orderId")
    void updateOrderStatus(@Param("orderId") String orderId, @Param("status") OrderStatus status);
}
