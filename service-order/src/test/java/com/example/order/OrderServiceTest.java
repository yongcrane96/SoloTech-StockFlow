package com.example.order;


import cn.hutool.core.lang.Snowflake;
import com.example.kafka.CreateOrderEvent;
import com.example.kafka.UpdateOrderEvent;
import com.example.order.entity.Order;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
import com.example.payment.dto.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order defaultOrder;
    private CreateOrderEvent defaultCreateEvent;
    private UpdateOrderEvent defaultUpdateEvent;
    private String orderId = "P001";

    @BeforeEach
    void setUp(){
        Snowflake snowflake = new Snowflake(1, 1);
        long snowflakeId = snowflake.nextId();

        defaultOrder = Order.builder()
                .id(snowflakeId)
                .orderId(orderId)
                .storeId("S001")
                .productId("P001")
                .stockId("ST01")
                .quantity(2L)
                .amount(3L)
                .paymentMethod("CARD")
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        defaultUpdateEvent = new UpdateOrderEvent(orderId,"S001", "P001", "ST01", 2L);

    }

//    @Test
//    @DisplayName("주문 수정 시")
//    void updateOrderTest() {
//        when(orderRepository.findByOrderId(defaultUpdateEvent.getOrderId())).thenReturn(Optional.of(defaultOrder));
//        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
//        // when
//        Order result = orderService.updateOrder(defaultUpdateEvent);
//        // then
//        assertNotNull(result);
//        assertEquals(2L, result.getQuantity());
//        verify(orderRepository).findByOrderId(orderId);
//        verify(orderRepository).save(defaultOrder);
//    }


    @Test
    @DisplayName("주문 조회")
    void readOrderTest() {
        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(defaultOrder));
        Order result = orderService.readOrder(orderId);
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals("P001", result.getProductId());
        verify(orderRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    @DisplayName("주문 삭제")
    void deleteOrderTest() {
        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(defaultOrder));
        orderService.deleteOrder(orderId);
        verify(orderRepository, times(1)).delete(defaultOrder);
    }

    @Test
    @DisplayName("주문가 없는 경우 삭제할 경우")
    void deleteOrder_NoPayment() {
        String orderId = "NOT_FOUND";
        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(OrderNotFoundException.class, () ->
                orderService.deleteOrder(orderId));

        assertTrue(exception.getMessage().contains("Order not found"));
        verify(orderRepository).findByOrderId(orderId);

    }
}
