package com.example.publisher;

import com.example.entity.Event;
import com.example.outbox.OutboxEvent;
import com.example.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher {
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Event> kafkaTemplate;
    private final ObjectMapper objectMapper; // JSON 역직렬화에 사용

    @Transactional
    @Scheduled(fixedRate = 5000)
    public void publishOutboxEvents(){
        List<OutboxEvent> unpublishedEvents = outboxEventRepository.findByPublishedFalse();

        for(OutboxEvent outboxEvent : unpublishedEvents){
            try {
                Event event = objectMapper.readValue(outboxEvent.getPayload(), Event.class);
                kafkaTemplate.send("order-events", event).get();  // 메시지가 브로커에 정상적으로 전달될 때까지 기다림

                // ✅ Kafka 전송이 성공한 후에만 published 상태 변경
                outboxEvent.published();
                outboxEventRepository.save(outboxEvent);

                log.info("Kafka 전송 성공 및 Outbox 업데이트 - id: {}", outboxEvent.getId());
            } catch (Exception  e) {
                e.printStackTrace();
            }
        }
    }
}
