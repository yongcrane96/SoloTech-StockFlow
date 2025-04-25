package com.example.controller;

import com.example.entity.Event;
import com.example.kafka.KafkaProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * KafkaController
 *
 * @since   2025-04-23
 * @author  yhkim
 */

@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class KafkaController {
    private final KafkaProducer kafkaProducer;

    @PostMapping("/send")
    public String sendMessage(@RequestBody Event message){
        kafkaProducer.sendMessage("topic", message);
        return "Message sent " + message;
    }

    @PostMapping("/send/topic/{topic}/key/{key}")
    public String sendKeyMessage(@PathVariable String topic, @PathVariable String key, @RequestBody Event message){
        kafkaProducer.sendKeyMessage(topic, key, message);
        return "Key = " + key + " Message sent: " + message;
    }

    // 처리 성능을 테스트를 위한 시나리오.
    @PostMapping("/send/many/{topic}")
    public String sendManyMessage(@PathVariable String topic, @RequestBody Event message){
        for (int i = 0; i < 10000; i++) {
            message.setId(i);
            kafkaProducer.sendMessage(topic, message);
        }
        return "Message sent: " + message;
    }

    // 처리 성능을 테스트를 위한 시나리오.
    @PostMapping("/send/many/{topic}/key/{key}")
    public String sendKeyWithMessage(@PathVariable String topic, @PathVariable String key, @RequestBody Event message){
        for(int i = 0; i < 100; i++){
            message.setId(i);
            kafkaProducer.sendKeyMessage(topic, key, message);
        }
        return "Key = " + key + " Message sent: " + message;
    }
}
