package com.example.kafka;

import com.example.order.entity.OrderStatus;
import com.example.order.repository.OrderRepository;
import com.example.payment.entity.Payment;
import com.example.payment.service.PaymentService;
import com.example.stock.service.StockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import com.example.entity.Event;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumer 서비스
 *
 * @since   2025-04-23
 * @author  yhkim
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final StockService stockService;
    private final ObjectMapper objectMapper;

    // Choreography 기반 Saga + Kafka consumer
    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void listen(ConsumerRecord<String, Event> record) {
        Event event = record.value();

        switch (event.getType()) {
            case "PaymentSuccess":
                handlePaymentSuccess(event);
                break;
            case "PaymentFail":
                handlePaymentFail(event);
                break;
        }
    }

    private void handlePaymentSuccess(Event event) {
        try {
            Payment payload = objectMapper.readValue(event.getPayload(), Payment.class);
            orderRepository.updateOrderStatus(payload.getOrderId(), OrderStatus.SUCCESS);
            log.info("결제 성공 처리 완료 - orderId: {}", payload.getOrderId());
        } catch (Exception e) {
            log.error("결제 성공 처리 실패", e);
        }
    }

    private void handlePaymentFail(Event event) {
        try {
            Payment payload = objectMapper.readValue(event.getPayload(), Payment.class);

            // 결제 실패 → 주문 취소 + 재고 복구
            orderRepository.updateOrderStatus(payload.getOrderId(), OrderStatus.CANCELED);

            // 재고 복구
            IncreaseStockEvent increaseStockEvent = new IncreaseStockEvent(event.getStockId(), event.getQuantity());
            stockService.increaseStock(increaseStockEvent);  // 재고 복구 처리

            log.info("결제 실패 처리 및 재고 복구 완료 - orderId: {}", payload.getOrderId());
        } catch (Exception e) {
            log.error("결제 실패 처리 중 예외", e);
        }
    }
}