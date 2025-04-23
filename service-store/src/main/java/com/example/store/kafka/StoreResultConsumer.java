package com.example.store.kafka;

import com.example.events.EventEntity;
import com.example.events.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreResultConsumer {
    private final EventRepository eventRepository;

    @KafkaListener(topics = "store-result", groupId = "store-group")
    public void onCommandEvent(ConsumerRecord<String, Event> record) {
        log.info("Received command event: {}", record.value());;
        Object event = record.value().getEvent();

        if (event instanceof StoreCreatedEvent) {
            handleStoreCreated((StoreCreatedEvent) event);
        } else if (event instanceof StoreUpdatedEvent) {
            handleStoreUpdated((StoreUpdatedEvent) event);
        } else if (event instanceof StoreDeletedEvent) {
            handleStoreDeleted((StoreDeletedEvent) event);
        } else {
            log.warn("Unknown command event: {}", record);
        }
    }

    private void handleStoreCreated(StoreCreatedEvent evt) {
        log.info("[ResultConsumer] StoreCreated: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("StoreCreatedEvent")
                .payload("storeId=" + evt.getStoreId() + ", storeName=" + evt.getStoreName())
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }

    private void handleStoreUpdated(StoreUpdatedEvent evt) {
        log.info("[ResultConsumer] StoreUpdated: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("StoreUpdatedEvent")
                .payload("storeId=" + evt.getStoreId() + ", storeName=" + evt.getStoreName())
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }

    private void handleStoreDeleted(StoreDeletedEvent evt) {
        log.info("[ResultConsumer] StoreDeleted: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("StoreDeletedEvent")
                .payload("storeId=" + evt.getStoreId())
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }
}
