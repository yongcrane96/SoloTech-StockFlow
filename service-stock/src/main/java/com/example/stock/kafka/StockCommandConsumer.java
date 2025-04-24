package com.example.stock.kafka;


import com.example.kafka.*;
import com.example.stock.entity.Stock;
import com.example.stock.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockCommandConsumer {

    private final StockService stockService;
    private final StockEventProducer eventProducer;

    @KafkaListener(topics = "stock-command", groupId = "stock-group")
    public void onCommandEvent(ConsumerRecord<String, Event> record) {
        log.info("Received record: {}", record);
        Object event = record.value().getEvent();

        if (event instanceof CreateStockEvent) {
            handleCreateStock((CreateStockEvent) event);
        } else if (event instanceof UpdateStockEvent) {
            handleUpdateStock((UpdateStockEvent) event);
        } else if (event instanceof DecreaseStockEvent) {
            handleDeceaseStock((DecreaseStockEvent) event);
        } else if (event instanceof DeleteStockEvent) {
            handleDeleteStock((DeleteStockEvent) event);
        } else {
            log.warn("Unknown command event: {}", record);
        }
    }

    private void handleCreateStock(CreateStockEvent event) {
        try {
            log.info("[CommandConsumer] Creating stock: {}", event);
            Stock stock = stockService.createStock(event);

            // 결과 이벤트
            StockCreatedEvent result = new StockCreatedEvent(
                    stock.getId(),
                    stock.getStockId(),
                    stock.getStoreId(),
                    stock.getProductId(),
                    stock.getStock()
            );
            eventProducer.sendResultEvent(result);

        } catch (Exception e) {
            log.error("[CommandConsumer] Error in handleCreateStock: ", e);
            // 실패 시 별도 실패 이벤트 발행 가능
        }
    }

    private void handleUpdateStock(UpdateStockEvent event) {
        try {
            log.info("[CommandConsumer] Updating stock: {}", event);

            Stock stock = stockService.updateStock(event);

            // 결과 이벤트
            StockUpdatedEvent result = new StockUpdatedEvent(
                    stock.getId(),
                    stock.getStockId(),
                    stock.getStoreId(),
                    stock.getProductId(),
                    stock.getStock()
            );
            eventProducer.sendResultEvent(result);

        } catch (Exception e) {
            log.error("[CommandConsumer] Error in handleUpdateStock: ", e);
        }
    }

    private void handleDeceaseStock(DecreaseStockEvent event) {
        try {
            log.info("[CommandConsumer] Decreasing stock: {}", event);

            Stock stock = stockService.decreaseStock(event);

            // 결과 이벤트
            StockUpdatedEvent result = new StockUpdatedEvent(
                    stock.getId(),
                    stock.getStockId(),
                    stock.getStoreId(),
                    stock.getProductId(),
                    stock.getStock()
            );
            eventProducer.sendResultEvent(result);

        } catch (Exception e) {
            log.error("[CommandConsumer] Error in handleUpdateStock: ", e);
        }
    }

    private void handleDeleteStock(DeleteStockEvent event) {
        try {
            log.info("[CommandConsumer] Deleting stock: {}", event);
            stockService.deleteStock(event.getStockId());

            // 결과 이벤트
            StockDeletedEvent result = new StockDeletedEvent(
                    event.getStockId()
            );
            eventProducer.sendResultEvent(result);

        } catch (Exception e) {
            log.error("[CommandConsumer] Error in handleDeleteStock: ", e);
        }
    }
}
