package com.example.kafka;

import com.example.order.OrderFeignClient;
import com.example.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.example.entity.Event;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaMessageHandler {

    private final OutboxEventRepository outboxEventRepository;
    private final OrderFeignClient orderFeignClient;

    @Async("kafkaEventExecutor")
    public void handleEventAsync(Event event) {
        try {
            // ✅ 멱등성 체크
            if (outboxEventRepository.existsByAggregateIdAndPublishedTrue(event.getOrderId(), true)) {
                log.warn("이미 처리된 이벤트 - id: {}", event.getOrderId());
                return;
            }

            // ✅ 실제 처리 로직 예: 주문 상태 변경
            orderFeignClient.updateOrderStatus(event.getOrderId(), "CONFIRMED");

            log.info("이벤트 처리 완료 - id: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("비동기 이벤트 처리 실패", e);
        }
    }
}
