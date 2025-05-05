package com.example.kafka.config;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import jdk.jfr.Enabled;
import jdk.jfr.Event;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 토픽에서 메시지를 읽어오는 역할  Consumer 동작 설정하는 클래스
 *
 * @since   2025-04-23
 * @author  yhkim
 */

@Configuration
public class KafkaConsumerConfig {
    @Bean
    public ConsumerFactory<String, Event> consumerFactory(){
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:10000");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        config.put("schema.registry.url", "http://localhost:9001"); // Schema Registry 설정
        config.put("specific.avro.reader", true); // Avro Specific 클래스를 사용
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Event> kafkaListenerContainerFactory(){
        ConcurrentKafkaListenerContainerFactory<String, Event> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // ✅ 동시 소비 스레드 수 지정
        factory.setConcurrency(3); // 예: 파티션 수나 처리량에 따라 조절

        return factory;
    }
}
