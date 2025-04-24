package com.example.kafka;

import jdk.jfr.Event;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumer 서비스
 *
 * @since   2025-04-23
 * @author  yhkim
 */

@Service
public class KafkaConsumer {
    @KafkaListener(topics = "topic", groupId = "my-group")
    public void listen(ConsumerRecord<String, Event> record){
        System.out.println("Consumed message: " + record.value());
    }
}
