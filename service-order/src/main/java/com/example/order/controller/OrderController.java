package com.example.order.controller;

import cn.hutool.core.lang.Snowflake;
import com.example.kafka.CreateOrderEvent;
import com.example.kafka.Status;
import com.example.kafka.UpdateOrderEvent;
import com.example.order.dto.OrderDto;
import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.kafka.OrderEventProducer;
import com.example.order.service.OrderService;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    final OrderEventProducer eventProducer;
    // C - R - U - D 형태로 작성

    @PostMapping
    @Operation(summary = "주문 생성", description = "주문을 생성하고 생성된 주문 객체를 반환합니다.")
    public long createOrder(@RequestBody OrderDto dto)
    {
        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();
        CreateOrderEvent event = new CreateOrderEvent(
                snowflakeId,
                dto.getOrderId(),
                dto.getStoreId(),
                dto.getProductId(),
                dto.getStockId(),
                dto.getPaymentId(),
                dto.getQuantity(),
                dto.getAmount(),
                dto.getPaymentMethod(),
                (Status) dto.getPaymentStatus()
        );

        eventProducer.sendCommandEvent(event);

        return snowflakeId;
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "주문 조회", description = "주문 정보를 1건 조회합니다.")
    public Order readOrder(@PathVariable String orderId) {
        return orderService.readOrder(orderId);
    }

    @PutMapping("{orderId}")
    @Operation(summary = "주문 수정", description = "주문 정보를 수정합니다.")
    public boolean updateOrder(@PathVariable String orderId, @RequestBody OrderDto dto) throws JsonMappingException {

        UpdateOrderEvent event = new UpdateOrderEvent(
                orderId,
                dto.getStoreId(),
                dto.getProductId(),
                dto.getStockId(),
                dto.getQuantity()
        );

        eventProducer.sendCommandEvent(event);

        return true;
    }

    @DeleteMapping("{orderId}")
    @Operation(summary = "주문 삭제", description = "주문 정보를 삭제합니다.")
    public boolean deleteOrder(@PathVariable String orderId){
        orderService.deleteOrder(orderId);
        return true;
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(@PathVariable String orderId, @RequestParam OrderStatus status){
        orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok().build();
    }

}
