package com.example.kafka;

import com.example.order.OrderFeignClient;
import com.example.stock.service.StockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import com.example.entity.Event;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import io.github.resilience4j.retry.annotation.Retry;


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
    private final StockService stockService;
    private final ObjectMapper objectMapper;
    private final OrderFeignClient orderFeignClient;
    private final KafkaMessageHandler kafkaMessageHandler;

    @KafkaListener(topics = "order-events", groupId = "order-consumer-group")
    public void consumeOrderEvent(String message) {
        try {
            Event event = objectMapper.readValue(message, Event.class);
            log.info("Kafka 메시지 수신: {}", event);

            kafkaMessageHandler.handleEventAsync(event); // ✅ 비동기로 처리

        } catch (Exception e) {
            log.error("Kafka 메시지 처리 실패", e);
        }
    }

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
            updateOrderStatusWithRetry(event.getOrderId(), "SUCCESS");
            log.info("결제 성공 처리 완료 - orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("결제 성공 처리 실패", e);
        }
    }

    private void handlePaymentFail(Event event) {
        try {
            // 결제 실패 → 주문 취소 + 재고 복구
            updateOrderStatusWithRetry(event.getOrderId(), "CANCELED");

            // 재고 복구
            IncreaseStockEvent increaseStockEvent = new IncreaseStockEvent(event.getStockId(), event.getQuantity());
            stockService.increaseStock(increaseStockEvent);  // 재고 복구 처리

            log.info("결제 실패 처리 및 재고 복구 완료 - orderId: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("결제 실패 처리 중 예외", e);
        }
    }

    @Retry(name = "orderApi", fallbackMethod = "fallbackUpdateOrderStatus")
    public void updateOrderStatusWithRetry(String orderId, String status) {
        orderFeignClient.updateOrderStatus(orderId, status);
    }

    public void fallbackUpdateOrderStatus(String orderId, String status, Exception ex) {
        log.error("Order 상태 업데이트 실패. 재시도 모두 실패 - orderId: {}, status: {}, 이유: {}",
                orderId, status, ex.getMessage());
        // DLQ로 보내거나, DB에 실패 기록 저장 등 후처리 가능
    }
}