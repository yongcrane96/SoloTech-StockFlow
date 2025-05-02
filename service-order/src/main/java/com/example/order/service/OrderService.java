package com.example.order.service;

import com.example.annotations.Cached;
import com.example.annotations.RedissonLock;
import com.example.cache.CacheType;
import com.example.kafka.CreateOrderEvent;
import com.example.kafka.DecreaseStockEvent;
import com.example.kafka.Event;
import com.example.kafka.UpdateOrderEvent;
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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 서비스
 *
 * @since   2025-03-18
 * @author  yhkim
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final KafkaTemplate<String, Event> kafkaTemplate;
    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final StockService stockService;
    private final PaymentService paymentService;

    @RedissonLock(value = "stock-{productId}", transactional = true)
    @Cached(prefix = "order:", key = "#result.orderId", ttl = 3600, type = CacheType.WRITE, cacheNull = true)
    @Transactional
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

        //3. 주문 생성 ( 상태 : PENDING )
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

        orderRepository.saveAndFlush(order);

        try{
            // 4. 재고 차감 처리 (예외 발생시 자동 롤백)
            stockService.decreaseStock(event.getStockId(), event.getQuantity());

            PaymentRequest paymentRequest = new PaymentRequest(
                    event.getPaymentId(),
                    event.getOrderId(),
                    event.getAmount(),
                    event.getPaymentMethod(),
                    PaymentStatus.PENDING
            );
            kafkaTemplate.send("payment-events", new Event("RequestPayment", paymentRequest));
        } catch (Exception e) {
            log.error("재고 차감 실패로 주문 취소 처리", e);
            order.cancel(); // 상태만 변경하거나 DB에서 제거
            orderRepository.save(order);
            throw new OrderCreationException("재고 차감 실패로 주문 생성 중단");
        }
        return order;
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
        Order savedOrder =  orderRepository.saveAndFlush(order); // 바로 return 해주는 부분이라 save가 아닌 saveAndFlush 사용

       return savedOrder;
    }


    @Cached(prefix = "order:", key = "#orderId", ttl = 3600, type = CacheType.DELETE, cacheNull = true)
    public void deleteOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(()-> new OrderNotFoundException("Order not found: " + orderId));
        orderRepository.delete(order);
    }
}