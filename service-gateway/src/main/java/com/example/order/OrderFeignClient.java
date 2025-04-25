package com.example.order;

import com.example.order.dto.OrderRequest;
import com.example.order.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "orderClient", url = "http://localhost:8086")
public interface OrderFeignClient {

    @PostMapping("/api/order")
    String createOrder(@RequestBody OrderRequest request);

    @GetMapping("api/order/{orderId}")
    OrderResponse getOrder(@PathVariable("orderId") String orderId);

    @PutMapping("api/order/{orderId}")
    boolean updateOrders(
            @PathVariable("orderId") String orderId,
            @RequestBody OrderRequest request
    );

    @DeleteMapping("/api/order/{orderId}")
    boolean deleteOrder(@PathVariable("orderId") String orderId);

}
