package com.example.order.kafka;

import com.example.events.EventEntity;
import com.example.events.EventRepository;
import com.example.kafka.Event;
import com.example.kafka.OrderCreatedEvent;
import com.example.kafka.OrderDeletedEvent;
import com.example.kafka.OrderUpdatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderResultConsumer {

    private final EventRepository eventRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @KafkaListener(topics = "order-result", groupId = "order-group")
    public void onCommandEvent(ConsumerRecord<String, Event> record) {
        log.info("Received command event: {}", record.value());;
        Object event = record.value().getEvent();

        if (event instanceof OrderCreatedEvent) {
            handleOrderCreated((OrderCreatedEvent) event);
        } else if (event instanceof OrderUpdatedEvent) {
            handleOrderUpdated((OrderUpdatedEvent) event);
        } else if (event instanceof OrderDeletedEvent) {
            handleOrderDeleted((OrderDeletedEvent) event);
        } else {
            log.warn("Unknown command event: {}", record);
        }
    }

    private void handleOrderCreated(OrderCreatedEvent evt) {
        log.info("[ResultConsumer] OrderCreated: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("OrderCreatedEvent")
                .payload(evt.toString())
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }

    private void handleOrderUpdated(OrderUpdatedEvent evt) {
        log.info("[ResultConsumer] OrderUpdated: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("OrderUpdatedEvent")
                .payload("id=" + evt.getId() + ", storeId=" + evt.getStoreId())
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }

    private void handleOrderDeleted(OrderDeletedEvent evt) {
        log.info("[ResultConsumer] OrderDeleted: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("OrderDeletedEvent")
                .payload("orderId=" + evt.getOrderId())
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }
}
