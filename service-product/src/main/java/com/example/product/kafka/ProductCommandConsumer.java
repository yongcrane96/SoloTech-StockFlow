package com.example.product.kafka;


import com.example.kafka.*;
import com.example.product.entity.Product;
import com.example.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCommandConsumer {

    private final ProductService productService;
    private final ProductEventProducer eventProducer;

    @KafkaListener(topics = "product-command", groupId = "Product-group")
    public void onCommandEvent(ConsumerRecord<String, Event> record) {
        log.info("Received record: {}", record);
        Object event = record.value().getEvent();

        if (event instanceof CreateProductEvent) {
            handleCreateProduct((CreateProductEvent) event);
        } else if (event instanceof UpdateProductEvent) {
            handleUpdateProduct((UpdateProductEvent) event);
        } else if (event instanceof DeleteProductEvent) {
            handleDeleteProduct((DeleteProductEvent) event);
        } else {
            log.warn("Unknown command event: {}", record);
        }
    }

    private void handleCreateProduct(CreateProductEvent event) {
        try {
            log.info("[CommandConsumer] Creating Product: {}", event);
            Product product = productService.createProduct(event);

            // 결과 이벤트
            ProductCreatedEvent result = new ProductCreatedEvent(
                    product.getId(),
                    product.getProductId(),
                    product.getName()
            );
            eventProducer.sendResultEvent(result);

        } catch (Exception e) {
            log.error("[CommandConsumer] Error in handleCreateProduct: ", e);
            // 실패 시 별도 실패 이벤트 발행 가능
        }
    }

    private void handleUpdateProduct(UpdateProductEvent event) {
        try {
            log.info("[CommandConsumer] Updating Product: {}", event);

            Product product = productService.updateProduct(event);

            // 결과 이벤트
            ProductUpdatedEvent result = new ProductUpdatedEvent(
                    product.getId(),
                    product.getProductId(),
                    product.getName()
            );
            eventProducer.sendResultEvent(result);

        } catch (Exception e) {
            log.error("[CommandConsumer] Error in handleUpdateProduct: ", e);
        }
    }

    private void handleDeleteProduct(DeleteProductEvent event) {
        try {
            log.info("[CommandConsumer] Deleting Product: {}", event);
            productService.deleteProduct(event.getProductId());

            // 결과 이벤트
            ProductDeletedEvent result = new ProductDeletedEvent(
                    event.getProductId()
            );
            eventProducer.sendResultEvent(result);

        } catch (Exception e) {
            log.error("[CommandConsumer] Error in handleDeleteProduct: ", e);
        }
    }
}
