package com.example.payment.kafka;


import com.example.kafka.*;
import com.example.payment.entity.Payment;
import com.example.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCommandConsumer {

    private final PaymentService paymentService;
    private final PaymentEventProducer eventProducer;

    @KafkaListener(topics = "payment-command", groupId = "Payment-group")
    public void onCommandEvent(ConsumerRecord<String, Event> record) {
        log.info("Received record: {}", record);
        Object event = record.value().getEvent();

        if (event instanceof CreatePaymentEvent) {
            handleCreatePayment((CreatePaymentEvent) event);
        } else if (event instanceof UpdatePaymentEvent) {
            handleUpdatePayment((UpdatePaymentEvent) event);
        } else if (event instanceof DeletePaymentEvent) {
            handleDeletePayment((DeletePaymentEvent) event);
        } else {
            log.warn("Unknown command event: {}", record);
        }
    }

    private void handleCreatePayment(CreatePaymentEvent event) {
        try {
            log.info("[CommandConsumer] Creating Payment: {}", event);
            Payment payment = paymentService.createPayment(event);

            // 결과 이벤트
            PaymentCreatedEvent result = new PaymentCreatedEvent(
                    payment.getId(),
                    payment.getPaymentId(),
                    payment.getOrderId(),  // 추가된 필드 매핑
                    payment.getAmount(),
                    payment.getPaymentMethod(),
                    Status.valueOf(payment.getPaymentStatus().name())
            );
            eventProducer.sendResultEvent(result);

        } catch (Exception e) {
            log.error("[CommandConsumer] Error in handleCreatePayment: ", e);
            // 실패 시 별도 실패 이벤트 발행 가능
        }
    }

    private void handleUpdatePayment(UpdatePaymentEvent event) {
        try {
            log.info("[CommandConsumer] Updating Payment: {}", event);

            Payment payment = paymentService.updatePayment(event);

            // 결과 이벤트
            PaymentUpdatedEvent result = new PaymentUpdatedEvent(
                    payment.getPaymentId(),
                    payment.getAmount(),
                    payment.getPaymentMethod(),
                    Status.valueOf(payment.getPaymentStatus().name())
            );
            eventProducer.sendResultEvent(result);

        } catch (Exception e) {
            log.error("[CommandConsumer] Error in handleUpdatePayment: ", e);
        }
    }

    private void handleDeletePayment(DeletePaymentEvent event) {
        try {
            log.info("[CommandConsumer] Deleting Payment: {}", event);
            paymentService.deletePayment(event.getPaymentId());

            // 결과 이벤트
            PaymentDeletedEvent result = new PaymentDeletedEvent(
                    event.getPaymentId()
            );
            eventProducer.sendResultEvent(result);

        } catch (Exception e) {
            log.error("[CommandConsumer] Error in handleDeletePayment: ", e);
        }
    }
}
