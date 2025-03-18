package SoloTech.StockFlow.order.controller;

import SoloTech.StockFlow.order.dto.OrderDto;
import SoloTech.StockFlow.order.entity.Order;
import SoloTech.StockFlow.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 컨트롤러
 *
 * @since   2025-03-18
 * @author  yhkim
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "주문 생성", description = "주문을 생성하고 생성된 주문 객체를 반환합니다.")
    public Order createOrder(@RequestBody OrderDto dto) {
        return orderService.createOrder(dto);
    }

}
