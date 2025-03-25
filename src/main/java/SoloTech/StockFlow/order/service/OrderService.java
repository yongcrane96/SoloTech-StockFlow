package SoloTech.StockFlow.order.service;

import SoloTech.StockFlow.order.dto.OrderDto;
import SoloTech.StockFlow.order.entity.Order;
import SoloTech.StockFlow.order.repository.OrderRepository;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 주문 서비스
 *
 * @since   2025-03-18
 * @author  yhkim
 */

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ObjectMapper mapper;

    @Transactional
    public Order createOrder(OrderDto dto) {
        Order order = mapper.convertValue(dto, Order.class);

        // Snowflake ID 생성
        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

//       String uuid = UUID.randomUUID().toString();
//       order.setOrderId(uuid);
        order.setOrderId(String.valueOf(snowflakeId));
        return orderRepository.saveAndFlush(order);
    }

    public Order readOrder(String orderId) {
        return  orderRepository.findByOrderId(orderId)
                .orElseThrow(()->new RuntimeException("Order not found: " + orderId));
    }

    @Transactional
    public Order updateOrder(String orderId, OrderDto dto) throws JsonMappingException {
        Order order = this.readOrder(orderId);
        mapper.updateValue(order, dto);
        return orderRepository.save(order);

    }

    public void deleteOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(()-> new RuntimeException("Order not found: " + orderId));
        orderRepository.delete(order);
    }
}
