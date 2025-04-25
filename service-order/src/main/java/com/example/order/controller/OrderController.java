package com.example.order.controller;

import com.example.order.dto.OrderDto;
import com.example.order.entity.Order;
import com.example.order.service.OrderService;
import com.fasterxml.jackson.databind.JsonMappingException;
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
    
    // C - R - U - D 형태로 작성
    
    @PostMapping
    @Operation(summary = "주문 생성", description = "주문을 생성하고 생성된 주문 객체를 반환합니다.")
    public Order createOrder(@RequestBody OrderDto dto) {
        return orderService.createOrder(dto);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "주문 조회", description = "주문 정보를 1건 조회합니다.")
    public Order readOrder(@PathVariable String orderId) {
        return orderService.readOrder(orderId);
    }
    
    @PutMapping("{orderId}")
    @Operation(summary = "주문 수정", description = "주문 정보를 수정합니다.")
    public Order updateOrder(@PathVariable String orderId, @RequestBody OrderDto dto) throws JsonMappingException {
        return orderService.updateOrder(orderId, dto);
    }

    @DeleteMapping("{orderId}")
    @Operation(summary = "주문 삭제", description = "주문 정보를 삭제합니다.")
    public boolean deleteOrder(@PathVariable String orderId){
        orderService.deleteOrder(orderId);
        return true;
    }
}
