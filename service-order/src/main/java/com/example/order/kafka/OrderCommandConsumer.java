//package com.example.order.kafka;
//
//
//import com.example.order.entity.Order;
//import com.example.order.service.OrderService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.kafka.clients.consumer.ConsumerRecord;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Service;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class OrderCommandConsumer {
//
//    private final OrderService OrderService;
//    private final OrderEventProducer eventProducer;
//
//    @KafkaListener(topics = "order-command", groupId = "order-group")
//    public void onCommandEvent(ConsumerRecord<String, Event> record) {
//        log.info("Received record: {}", record);
//        Object event = record.value().getEvent();
//
//        if (event instanceof CreateOrderEvent) {
//            handleCreateOrder((CreateOrderEvent) event);
//        } else if (event instanceof UpdateOrderEvent) {
//            handleUpdateOrder((UpdateOrderEvent) event);
//        } else if (event instanceof DeleteOrderEvent) {
//            handleDeleteOrder((DeleteOrderEvent) event);
//        } else {
//            log.warn("Unknown command event: {}", record);
//        }
//    }
//
//    private void handleCreateOrder(CreateOrderEvent event) {
//        try {
//            log.info("[CommandConsumer] Creating Order: {}", event);
//            Order order = OrderService.createOrder(event);
//
//            // 결과 이벤트
//            OrderCreatedEvent result = new OrderCreatedEvent(
//                    order.getId(),
//                    order.getOrderId(),
//                    order.getStoreId(),
//                    order.getProductId(),
//                    order.getStockId(),
//                    order.getQuantity()
//            );
//            eventProducer.sendResultEvent(result);
//
//        } catch (Exception e) {
//            log.error("[CommandConsumer] Error in handleCreateOrder: ", e);
//            // 실패 시 별도 실패 이벤트 발행 가능
//        }
//    }
//
//    private void handleUpdateOrder(UpdateOrderEvent event) {
//        try {
//            log.info("[CommandConsumer] Updating Order: {}", event);
//
//            Order order = OrderService.updateOrder(event);
//
//            // 결과 이벤트
//            OrderUpdatedEvent result = new OrderUpdatedEvent(
//                    order.getId(),
//                    order.getOrderId(),
//                    order.getStoreId(),
//                    order.getProductId(),
//                    order.getStockId(),
//                    order.getQuantity()
//            );
//            eventProducer.sendResultEvent(result);
//
//        } catch (Exception e) {
//            log.error("[CommandConsumer] Error in handleUpdateOrder: ", e);
//        }
//    }
//
//    private void handleDeleteOrder(DeleteOrderEvent event) {
//        try {
//            log.info("[CommandConsumer] Deleting Order: {}", event);
//            OrderService.deleteOrder(event.getOrderId());
//
//            // 결과 이벤트
//            OrderDeletedEvent result = new OrderDeletedEvent(
//                    event.getOrderId()
//            );
//            eventProducer.sendResultEvent(result);
//
//        } catch (Exception e) {
//            log.error("[CommandConsumer] Error in handleDeleteOrder: ", e);
//        }
//    }
//}
