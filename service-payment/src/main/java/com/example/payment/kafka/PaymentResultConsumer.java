package com.example.payment.kafka;

import com.example.events.EventEntity;
import com.example.events.EventRepository;
import com.example.kafka.Event;
import com.example.kafka.PaymentCreatedEvent;
import com.example.kafka.PaymentDeletedEvent;
import com.example.kafka.PaymentUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentResultConsumer {

    private final EventRepository eventRepository;

    @KafkaListener(topics = "payment-result", groupId = "Payment-group")
    public void onCommandEvent(ConsumerRecord<String, Event> record) {
        log.info("Received command event: {}", record.value());;
        Object event = record.value().getEvent();

        if (event instanceof PaymentCreatedEvent) {
            handlePaymentCreated((PaymentCreatedEvent) event);
        } else if (event instanceof PaymentUpdatedEvent) {
            handlePaymentUpdated((PaymentUpdatedEvent) event);
        } else if (event instanceof PaymentDeletedEvent) {
            handlePaymentDeleted((PaymentDeletedEvent) event);
        } else {
            log.warn("Unknown command event: {}", record);
        }
    }

    private void handlePaymentCreated(PaymentCreatedEvent evt) {
        log.info("[ResultConsumer] PaymentCreated: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("CreatePaymentEvent")
                .payload(String.format("PaymentId=%s, OrderId=%s, Amount=%d, PaymentMethod=%s, Status=%s",
                        evt.getPaymentId(),
                        evt.getOrderId(),
                        evt.getAmount(),
                        evt.getPaymentMethod(),
                        evt.getPaymentStatus()))
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }

    private void handlePaymentUpdated(PaymentUpdatedEvent evt) {
        log.info("[ResultConsumer] PaymentUpdated: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("PaymentUpdatedEvent")
                .payload(String.format(
                        "PaymentId=%s, PaymentMethod=%s, Amount=%d, PaymentStatus=%s",
                        evt.getPaymentId(),
                        evt.getPaymentMethod(),
                        evt.getAmount(),
                        evt.getPaymentStatus().name() // Enum 값을 문자열로 변환
                ))
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }

    private void handlePaymentDeleted(PaymentDeletedEvent evt) {
        log.info("[ResultConsumer] PaymentDeleted: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("PaymentDeletedEvent")
                .payload("PaymentId=" + evt.getPaymentId())
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }
}
