package com.example.kafka.config;

import com.example.entity.Event;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.List;

/**
 * Kafka 토픽에서 메시지를 보내는 역할  Producer 동작 설정하는 클래스
 *
 * @since   2025-04-23
 * @author  yhkim
 */

@EnableKafka
@Configuration
public class KafkaProducerConfig {
    @Bean // 객체를 직접 등록하겠다는 선언 (메서드에서 리턴하는 객체를 스프링이 관리하게 해달라는)
    public ProducerFactory<String, Event> producerFactory(){
        HashMap<String, Object> config = new HashMap<>();
        // Kafka 브로커 주소 (프로듀서가 메세지를 전송할 대상 카프카 서버)
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:10000");

        // 메시지 Key를 직렬화할 방식 ( 문자열 그대로 직렬화)
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // 메시지 value를 Avro포맷으로 직렬화
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);

        // Avro 스키마를 등록/조회할 스키마 레지스트리 서버 주소
        config.put("schema.registry.url", "http://localhost:9001");

        // 위 설정을 기반으로 카프카 ProducerFactory 생성
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Event> kafkaTemplate() {return new KafkaTemplate<>(producerFactory());}

    @Bean
    public List<NewTopic> defaultTopic() { // 카프카 토픽을 애플리케이션 실행 시 자동 생성
        return List.of(
                // 이름이 topic인 카프카 토픽 생성, 파티션 1개 복제 수 1
                new NewTopic("topic", 1, (short) 1),
                new NewTopic("topic-a", 1, (short) 1),
                new NewTopic("topic-b", 1, (short) 1)
        );
    }
    // 카프카는 토픽이 존재하지 않으면 메시지 보낼 수 X
    // 수동 생성이 필요없음
}
