package com.example.order.service;

import cn.hutool.core.lang.Snowflake;
import com.example.annotations.Cached;
import com.example.annotations.RedissonLock;
import com.example.cache.CacheType;
import com.example.kafka.CreateOrderEvent;
import com.example.kafka.UpdateOrderEvent;
import com.example.order.dto.OrderDto;
import com.example.order.entity.Order;
import com.example.order.exception.OrderCreationException;
import com.example.order.exception.OrderNotFoundException;
import com.example.order.exception.PaymentFailedException;
import com.example.order.exception.StockNotFoundException;
import com.example.order.repository.OrderRepository;
import com.example.payment.PaymentService;
import com.example.payment.dto.PaymentRequest;
import com.example.payment.dto.PaymentResponse;
import com.example.payment.dto.PaymentStatus;
import com.example.product.ProductService;
import com.example.product.dto.ProductResponse;
import com.example.stock.StockService;
import com.example.stock.dto.StockResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


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
    private final ProductService productService;
    private final StockService stockService;
    private final PaymentService paymentService;

    private final ObjectMapper mapper;

    @RedissonLock(value = "stock-{productId}", transactional = true)
    @Cached(prefix = "order:", key = "#result.orderId", ttl = 3600, type = CacheType.WRITE, cacheNull = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order createOrder(CreateOrderEvent event) {
        // 1. 상품 조회
        ProductResponse product = productService.getProduct(event.getProductId());
        if (product == null) {
            throw new OrderCreationException("Product not found: " + event.getProductId());
        }

        // 2. 재고 확인
        StockResponse stock = stockService.getStockProduct(event.getProductId());
        if (stock.getStock() < event.getQuantity()) {
            throw new StockNotFoundException("Insufficient stock for product: " + event.getProductId());
        }

        // 3. 결제 처리
        PaymentStatus initialPaymentStatus = PaymentStatus.PENDING;
        PaymentRequest paymentDto = new PaymentRequest(event.getPaymentId(),event.getOrderId(), event.getAmount(), event.getPaymentMethod(), initialPaymentStatus);
        PaymentResponse payment = paymentService.createPayment(paymentDto);
        if (!PaymentStatus.SUCCESS.equals(payment.getPaymentStatus())) {
            throw new PaymentFailedException("Payment failed for order: " + event.getOrderId());
        }

        stockService.decreaseStock(event.getProductId(), event.getQuantity());

        Order order = Order.builder()
                .id(event.getId())
                .orderId(event.getOrderId())
                .storeId(event.getStoreId())
                .productId(event.getProductId())
                .stockId(event.getStockId())
                .paymentId(event.getPaymentId())
                .quantity(event.getQuantity())
                .amount(event.getAmount())
                .paymentMethod(event.getPaymentMethod())
                .paymentStatus(PaymentStatus.valueOf(event.getPaymentStatus().name())) // enum 변환
                .build();

        Order savedOrder = orderRepository.saveAndFlush(order);

        return savedOrder;
    }


    @Cached(prefix = "order:", key = "#orderId", ttl = 3600, type = CacheType.READ, cacheNull = true)
    public Order readOrder(String orderId) {
        // DB 조회
         Order dbOrder = orderRepository.findByOrderId(orderId)
                .orElseThrow(()->new RuntimeException("Order not found: " + orderId));
        // 캐시에 저장
        return dbOrder;
    }

    @Cached(prefix = "order:", key = "#result.orderId", ttl = 3600, type = CacheType.WRITE, cacheNull = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order updateOrder(UpdateOrderEvent event) {
        String orderId = event.getOrderId();
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(()-> new EntityNotFoundException("Order not found: " + orderId));

        order.setQuantity(event.getQuantity());
        Order savedOrder =  orderRepository.save(order);
        return savedOrder;
    }


    @Cached(prefix = "order:", key = "#orderId", ttl = 3600, type = CacheType.DELETE, cacheNull = true)
    public void deleteOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(()-> new OrderNotFoundException("Order not found: " + orderId));
        orderRepository.delete(order);
    }
}