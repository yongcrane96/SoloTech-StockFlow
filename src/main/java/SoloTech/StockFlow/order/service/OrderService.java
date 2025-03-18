package SoloTech.StockFlow.order.service;

import SoloTech.StockFlow.order.dto.OrderDto;
import SoloTech.StockFlow.order.entity.Order;
import SoloTech.StockFlow.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public Order createOrder(OrderDto dto) {
        Order order = mapper.convertValue(dto, Order.class);
        String uuid = UUID.randomUUID().toString();
        order.setOrderId(uuid);
        return orderRepository.saveAndFlush(order);
    }

}
