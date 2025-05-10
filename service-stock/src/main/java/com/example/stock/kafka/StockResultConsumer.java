package com.example.stock.kafka;

import com.example.events.EventEntity;
import com.example.events.EventRepository;
import com.example.kafka.*;
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
public class StockResultConsumer {

    private final EventRepository eventRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @KafkaListener(topics = "stock-result", groupId = "stock-group")
    public void onCommandEvent(ConsumerRecord<String, Event> record) {
        log.info("Received command event: {}", record.value());;
        Object event = record.value().getEvent();

        if (event instanceof StockCreatedEvent) {
            handleStockCreated((StockCreatedEvent) event);
        } else if (event instanceof StockUpdatedEvent) {
            handleStockUpdated((StockUpdatedEvent) event);
        } else if (event instanceof StockDecreasedEvent) {
            handleStockDecreased((StockDecreasedEvent) event);
        } else if (event instanceof StockDeletedEvent) {
            handleStockDeleted((StockDeletedEvent) event);
        } else {
            log.warn("Unknown command event: {}", record);
        }

    }

    private void handleStockCreated(StockCreatedEvent evt) {
        log.info("[ResultConsumer] StockCreated: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("StockCreatedEvent")
                .payload("id=" + evt.getId() + ", storeId=" + evt.getStoreId())
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }

    private void handleStockUpdated(StockUpdatedEvent evt) {
        log.info("[ResultConsumer] StockUpdated: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("StockUpdatedEvent")
                .payload("stockId=" + evt.getId() + ", storeId=" + evt.getStoreId())
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }

    private void handleStockDecreased(StockDecreasedEvent evt) {
        log.info("[ResultConsumer] StockDecreased: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("StockUpdatedEvent")
                .payload(evt.toString())
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }

    private void handleStockDeleted(StockDeletedEvent evt) {
        log.info("[ResultConsumer] StockDeleted: {}", evt);
        EventEntity entity = EventEntity.builder()
                .eventType("StockDeletedEvent")
                .payload("stockId=" + evt.getStockId())
                .eventTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();
        eventRepository.save(entity);
    }
}
