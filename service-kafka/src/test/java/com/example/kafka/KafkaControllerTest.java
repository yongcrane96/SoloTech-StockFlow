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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;class KafkaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private KafkaController kafkaController;

    private Event event;
    private String topic;
    private String key;
    private String content;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(kafkaController).build();

        event = setUpEvent(1, "testType", "testPayload", "P001", 10L, "S001", "PAY001", "ORD001");
        topic = "testTopic";
        key = "testKey";
        content = setUpContent(event); // 모든 필드를 포함한 JSON 문자열 생성
    }

    private Event setUpEvent(int id, String type, String payload, String productId, long quantity, String stockId, String paymentId, String orderId) {
        return new Event(id, type, payload, productId, quantity, stockId, paymentId, orderId);
    }

    private String setUpContent(Event event) {
        return "{"
                + "\"id\":" + event.getId()
                + ",\"type\":\"" + event.getType() + "\""
                + ",\"payload\":\"" + event.getPayload() + "\""
                + ",\"productId\":\"" + event.getProductId() + "\""
                + ",\"quantity\":" + event.getQuantity()
                + ",\"stockId\":\"" + event.getStockId() + "\""
                + ",\"paymentId\":\"" + event.getPaymentId() + "\""
                + ",\"orderId\":\"" + event.getOrderId() + "\""
                + "}";
    }

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

        verify(kafkaProducer, times(1)).sendMessage(eq("topic"), any(Event.class));
    }

    @Test
    @DisplayName("주어진 topic과 key로 메시지 전송 테스트")
    void sendKeyMessageTest() throws Exception {
        performPostRequest("/api/kafka/send/topic/{topic}/key/{key}", content, topic, key);

        verify(kafkaProducer, times(1)).sendKeyMessage(eq(topic), eq(key), any(Event.class));
    }

    @Test
    @DisplayName("하나의 topic으로 여러 메시지 전송 테스트")
    void sendManyMessageTest() throws Exception {
        performPostRequest("/api/kafka/send/many/{topic}", content, topic);

        verify(kafkaProducer, times(10000)).sendMessage(eq(topic), any(Event.class));
    }

    @Test
    @DisplayName("주어진 topic과 key로 여러 메시지 전송 테스트")
    void sendKeyWithMessageTest() throws Exception {
        performPostRequest("/api/kafka/send/many/{topic}/key/{key}", content, topic, key);

        verify(kafkaProducer, times(100)).sendKeyMessage(eq(topic), eq(key), any(Event.class));
    }
}
