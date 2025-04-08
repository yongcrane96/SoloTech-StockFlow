package SoloTech.StockFlow.order.service;

import SoloTech.StockFlow.common.annotations.Cached;
import SoloTech.StockFlow.common.cache.CacheType;
import SoloTech.StockFlow.order.dto.OrderDto;
import SoloTech.StockFlow.order.entity.Order;
import SoloTech.StockFlow.order.repository.OrderRepository;
import SoloTech.StockFlow.payment.dto.PaymentDto;
import SoloTech.StockFlow.payment.entity.Payment;
import SoloTech.StockFlow.payment.entity.PaymentStatus;
import SoloTech.StockFlow.payment.service.PaymentService;
import SoloTech.StockFlow.product.entity.Product;
import SoloTech.StockFlow.product.service.ProductService;
import SoloTech.StockFlow.stock.entity.Stock;
import SoloTech.StockFlow.stock.service.StockService;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

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

    @Cached(prefix = "order:", key = "#result.orderId", ttl = 3600, type = CacheType.WRITE)
    @Transactional
    public Order createOrder(OrderDto dto) {
        // 1. 상품 조회
        Product product = productService.getProduct(dto.getProductId());
        if (product == null) {
            throw new RuntimeException("Product not found: " + dto.getProductId());
        }

        // 2. 재고 확인
        Stock stock = stockService.getStock(dto.getProductId());
        if (stock.getStock() < dto.getQuantity()) {
            throw new RuntimeException("Insufficient stock for product: " + dto.getProductId());
        }

        // 3. 결제 처리
        String initialPaymentStatus = "PENDING";
        PaymentDto paymentDto = new PaymentDto(dto.getOrderId(), dto.getAmount(), dto.getPaymentMethod(), initialPaymentStatus);
        Payment payment = paymentService.createPayment(paymentDto);
        if (!PaymentStatus.Success.equals(payment.getPaymentStatus())) {
            throw new RuntimeException("Payment failed for order: " + dto.getOrderId());
        }

        stockService.decreaseStock(dto.getProductId(), dto.getQuantity());

        Order order = mapper.convertValue(dto, Order.class);

        // Snowflake ID 생성
        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

        order.setOrderId(String.valueOf(snowflakeId));
        Order savedOrder = orderRepository.saveAndFlush(order);

        return savedOrder;
    }


    @Cached(prefix = "order:", key = "#orderId", ttl = 3600, type = CacheType.READ)
    public Order readOrder(String orderId) {
        // DB 조회
         Order dbOrder = orderRepository.findByOrderId(orderId)
                .orElseThrow(()->new RuntimeException("Order not found: " + orderId));
        // 캐시에 저장
        return dbOrder;
    }

    @Cached(prefix = "order:", key = "#result.orderId", ttl = 3600, type = CacheType.WRITE)
    @Transactional
    public Order updateOrder(String orderId, OrderDto dto) throws JsonMappingException {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(()-> new EntityNotFoundException("Order not found: " + orderId));

        mapper.updateValue(order, dto);
        Order savedOrder =  orderRepository.save(order);

        return savedOrder;
    }


    @Cached(prefix = "order:", key = "#orderId", ttl = 3600, type = CacheType.DELETE)
    public void deleteOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(()-> new RuntimeException("Order not found: " + orderId));
        orderRepository.delete(order);
    }
}