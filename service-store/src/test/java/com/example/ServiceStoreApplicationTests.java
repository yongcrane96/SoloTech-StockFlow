//package com.example;
//
//import com.example.kafka.CreateStoreEvent;
//import com.example.store.dto.StoreDto;
//import com.example.store.kafka.StoreEventProducer;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@SpringBootTest
//@AutoConfigureMockMvc // MockMvc 자동 설정
//class ServiceStoreApplicationTests {
//
//    @Autowired
//    private MockMvc mockMvc;  // MockMvc 주입
//
//    @MockitoBean
//    private StoreEventProducer eventProducer;  // Mockito로 Mocking된 Producer
//
//    @Test
//    void create_shouldSendKafkaEvent() throws Exception {
//        StoreDto dto = new StoreDto(3L, "S002", "업데이트된 상점", "서울 강남구");
//        ObjectMapper mapper = new ObjectMapper();
//
//        // MockMvc를 사용한 요청 실행
//        mockMvc.perform(post("/api/store")
//                .contentType("application/json")
//                .content(mapper.writeValueAsString(dto)))
//                .andExpect(status().isOk());  // 상태 코드 확인
//
//        // Kafka 이벤트 Producer가 호출되었는지 확인
//        verify(eventProducer).sendCommandEvent(any(CreateStoreEvent.class));
//    }
//}
