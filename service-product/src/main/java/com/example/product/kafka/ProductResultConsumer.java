package com.example.product.kafka;

import com.example.events.EventEntity;
import com.example.events.EventRepository;
import com.example.kafka.Event;
import com.example.kafka.ProductCreatedEvent;
import com.example.kafka.ProductDeletedEvent;
import com.example.kafka.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductResultConsumer {

    private final EventRepository eventRepository;

    @KafkaListener(topics = "product-result", groupId = "Product-group")
    public void onCommandEvent(ConsumerRecord<String, Event> record) {
        log.info("Received command event: {}", record.value());;
        Object event = record.value().getEvent();

        if (event instanceof ProductCreatedEvent) {
            handleProductCreated((ProductCreatedEvent) event);
        } else if (event instanceof ProductUpdatedEvent) {
            handleProductUpdated((ProductUpdatedEvent) event);
        } else if (event instanceof ProductDeletedEvent) {
            handleProductDeleted((ProductDeletedEvent) event);
        } else {
            log.warn("Unknown command event: {}", record);
        }
    }

    private void handleProductCreated(ProductCreatedEvent evt) {
        log.info("[ResultConsumer] ProductCreated: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("ProductCreatedEvent")
                .payload("ProductId=" + evt.getId() + ", ProductName=" + evt.getName())
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }

    private void handleProductUpdated(ProductUpdatedEvent evt) {
        log.info("[ResultConsumer] ProductUpdated: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("ProductUpdatedEvent")
                .payload("ProductId=" + evt.getId() + ", ProductName=" + evt.getName())
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }

    private void handleProductDeleted(ProductDeletedEvent evt) {
        log.info("[ResultConsumer] ProductDeleted: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("ProductDeletedEvent")
                .payload("ProductId=" + evt.getProductId())
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }
}
