package com.example.kafka;
import com.example.controller.KafkaController;
import com.example.entity.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KafkaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private KafkaController kafkaController;

    // 공통 설정을 위한 변수들
    private Event event;
    private String topic;
    private String key;
    private String content;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(kafkaController).build();
        // 공통 초기화 메서드 호출
        event = setUpEvent(1, "testType", "testPayload");
        topic = "testTopic";
        key = "testKey";
        content = setUpContent(event);  // 공통 content 설정
    }

    private Event setUpEvent(int id, String type, String payload) {
        return new Event(id, type, payload);
    }

    private String setUpContent(Event event) {
        return "{\"id\":" + event.getId() + ", \"type\":\"" + event.getType() + "\", \"payload\":\"" + event.getPayload() + "\"}";
    }

    // 공통 mockMvc 요청 메서드
    private void performPostRequest(String url, String content, Object... urlVariables) throws Exception {
        mockMvc.perform(post(url, urlVariables)
                .contentType("application/json")
                .content(content))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Kafka에 메시지를 전송하는 기능을 테스트")
    void sendMessageTest() throws Exception {
        performPostRequest("/api/kafka/send", content);

        verify(kafkaProducer, times(1)).sendMessage("topic", event);
    }

    @Test
    @DisplayName("주어진 topic과 key로 메시지 전송 테스트")
    void sendKeyMessageTest() throws Exception {
        performPostRequest("/api/kafka/send/topic/{topic}/key/{key}", content, topic, key);
        verify(kafkaProducer, times(1)).sendKeyMessage(topic, key, event);
    }

    @Test
    @DisplayName("하나의 topic으로 여러 메시지 전송 테스트")
    void sendManyMessageTest() throws Exception {
        performPostRequest("/api/kafka/send/many/{topic}", content, topic);

        Mockito.verify(kafkaProducer, times(10000)).sendMessage(eq(topic), any(Event.class));

    }

    @Test
    @DisplayName("주어진 topic과 key로 여러 메시지 전송 테스트")
    void sendKeyWithMessageTest() throws Exception {
        performPostRequest("/api/kafka/send/many/{topic}/key/{key}", content, topic, key);
        verify(kafkaProducer, times(100)).sendKeyMessage(eq(topic), eq(key), any(Event.class));
    }

    //eq Mockito의 매처 기능을 활용 : Equality Matcher => 특정 값이 일치하는지 확인하는데
    // 매개변수로 전달된 값이 기대한 값과 정확히 동일한지
    // any()는 어떤 값이든 상관없이 해당 매개변수에 전달된 값이 무엇이든 간에 매칭

}