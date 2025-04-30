package com.example.kafka;

import com.example.order.entity.OrderStatus;
import com.example.order.repository.OrderRepository;
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
    @KafkaListener(topics = "order-events", groupId = "order-service")
    public void listen(ConsumerRecord<String, Event> record) {
        Event event = record.value();

        switch (event.getType()) {
            case "DecreaseStock":
                handleDecreaseStockEvent(event);
                break;
        }
    }

    private void handleDecreaseStockEvent(Event event){
        try{
            DecreaseStockEvent decreaseStockEvent = objectMapper.readValue(event.getPayload(), DecreaseStockEvent.class);
            // 재고 감소 처리
            stockService.decreaseStock(decreaseStockEvent);

            // 재고 감소 성공 후, 결제 성공 이벤트 발행
            paymentService.confirmPayment(event.getPaymentId());
            // 결제 성공 시 주문 상태 변경
            orderRepository.updateOrderStatus(event.getOrderId(), OrderStatus.SUCCESS);

        }catch (Exception e){
            log.error("재고 감소 처리 중 예외 발생" , e);
            paymentService.cancelPayment(event.getPayload());
            orderRepository.updateOrderStatus(event.getOrderId(), OrderStatus.CANCELED);
        }
    }
}
