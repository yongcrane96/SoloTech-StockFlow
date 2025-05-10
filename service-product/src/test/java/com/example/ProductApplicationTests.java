//package com.example;
//
//import com.example.kafka.CreateProductEvent;
//import com.example.product.dto.ProductDto;
//import com.example.product.kafka.ProductEventProducer;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.verify;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//class ProductApplicationTests {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockitoBean
//    private ProductEventProducer eventProducer;
//
//    @Test
//    void create_shouldSendKafkaEvent() throws Exception {
//        ProductDto dto = new ProductDto(
//                "P001", "가방", 10000L, "실용성 높은 백팩"
//        );
//        ObjectMapper mapper = new ObjectMapper();
//        // MockMvc를 사용한 요청 실행
//        mockMvc.perform(post("/api/product")
//                .contentType("application/json")
//                .content(mapper.writeValueAsString(dto)))
//                .andExpect(status().isOk());  // 상태 코드 확인
//
//        // Kafka 이벤트 Producer가 호출되었는지 확인
//        verify(eventProducer).sendCommandEvent(any(CreateProductEvent.class));
//    }
//
//}
