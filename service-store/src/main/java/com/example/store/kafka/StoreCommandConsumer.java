package com.example.store.kafka;

import com.example.store.entity.Store;
import com.example.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreCommandConsumer {
    private final StoreService storeService;
    private final StoreEventProducer storeEventProducer;

    @KafkaListener(topics = "store-command", groupId = "store-gourp")
    private void onCommandEvent(ConsumerRecord<String, Event> record){
        log.info("Received record: {}", record);
        Object event = record.value().getEvent();

        if (event instanceof CreateStoreEvent) {
            handleCreateStore((CreateStoreEvent) event);
        } else if (event instanceof UpdateStoreEvent) {
            handleUpdateStore((UpdateStoreEvent) event);
        } else if (event instanceof DeleteStoreEvent) {
            handleDeleteStore((DeleteStoreEvent) event);
        } else {
            log.warn("Unknown command event: {}", record);
        }
    }

    private void handleCreateStore(CreateStoreEvent event) {
        try {
            log.info("[CommandConsumer] Creating store: {}", event);
            Store store = storeService.createStore(event);

            // 결과 이벤트
            StoreCreatedEvent result = new StoreCreatedEvent(
                    store.getId(),
                    store.getStoreId(),
                    store.getStoreName()
            );
            eventProducer.sendResultEvent(result);

        } catch (Exception e) {
            log.error("[CommandConsumer] Error in handleCreateStore: ", e);
            // 실패 시 별도 실패 이벤트 발행 가능
        }
    }

    private void handleUpdateStore(UpdateStoreEvent event) {
        try {
            log.info("[CommandConsumer] Updating store: {}", event);

            Store store = storeService.updateStore(event);

            // 결과 이벤트
            StoreUpdatedEvent result = new StoreUpdatedEvent(
                    store.getId(),
                    store.getStoreId(),
                    store.getStoreName()
            );
            eventProducer.sendResultEvent(result);

        } catch (Exception e) {
            log.error("[CommandConsumer] Error in handleUpdateStore: ", e);
        }
    }

    private void handleDeleteStore(DeleteStoreEvent event) {
        try {
            log.info("[CommandConsumer] Deleting store: {}", event);
            storeService.deleteStore(event.getStoreId());

            // 결과 이벤트
            StoreDeletedEvent result = new StoreDeletedEvent(
                    event.getStoreId()
            );
            eventProducer.sendResultEvent(result);

        } catch (Exception e) {
            log.error("[CommandConsumer] Error in handleDeleteStore: ", e);
        }
    }
}
