package com.example.kafka;

import com.example.entity.Event;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Producer 서비스
 *
 * @since   2025-04-23
 * @author  yhkim
 */

@Service
public class KafkaProducer {
    private final KafkaTemplate<String, Event> kafkaTemplate;

    public KafkaProducer(KafkaTemplate<String, Event> kafkaTemplate) { this.kafkaTemplate = kafkaTemplate; }

    public void sendMessage(String topic, Event message){
        kafkaTemplate.send(topic, message);
    }

    public void sendKeyMessage(String topic, String key, Event message){
        kafkaTemplate.send(topic, key, message);
    }
}