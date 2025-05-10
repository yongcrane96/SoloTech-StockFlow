package com.example.store;

import cn.hutool.core.lang.Snowflake;
import com.example.kafka.CreateStoreEvent;
import com.example.kafka.UpdateStoreEvent;
import com.example.store.dto.StoreDto;
import com.example.store.entity.Store;
import com.example.store.exception.StoreNotFoundException;
import com.example.store.kafka.StoreEventProducer;
import com.example.store.repository.StoreRepository;
import com.example.store.service.StoreService;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StoreServiceTest {
    @Mock
    private StoreRepository storeRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private StoreService storeService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private Store defaultStore;
    private String storeId = "S001";
    private CreateStoreEvent defaultCreateEvent;
    private UpdateStoreEvent defaultUpdateEvent;

    @BeforeEach
    void setUp(){
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Snowflake ID 생성
        Snowflake snowflake = new Snowflake(1, 1);
        long snowflakeId = snowflake.nextId();

        //공통 데이터 초기화
        defaultStore = Store.builder()
                .id(snowflakeId)
                .storeId("S001")
                .storeName("기본 상점")
                .address("서울 강남")
                .build();

        defaultCreateEvent = new CreateStoreEvent(snowflakeId, "S001", "기본 상점", "서울 강남");
        defaultUpdateEvent = new UpdateStoreEvent("S001", "업데이트된 상점", "서울 강남구");
    }

    @Test
    @DisplayName("스토어 생성 테스트 - Snowflake ID 활용")
    void createStoreTest() {
        when(storeRepository.saveAndFlush(any())).thenReturn(defaultStore);
        Store result = storeService.createStore(defaultCreateEvent);

        // Assert: 결과 검증
        assertNotNull(result);
        assertEquals("S001", result.getStoreId());
        verify(storeRepository).saveAndFlush(any());
      }

    @Test
    @DisplayName("스토어 생성 시 예외 및 보상 처리")
    void createStoreException() {
        when(storeRepository.saveAndFlush(any())).thenThrow(RuntimeException.class);

        assertThrows(StoreNotFoundException.class, () -> storeService.createStore(defaultCreateEvent));
        verify(storeRepository).deleteById(defaultCreateEvent.getId());
    }

    @Test
    @DisplayName("스토어 조회")
    void getStoreTest() {
        when(storeRepository.findByStoreId(storeId)).thenReturn(Optional.of(defaultStore));

        Store result = storeService.getStore(storeId);

        assertNotNull(result);
        assertEquals("S001", result.getStoreId());
        assertEquals("기본 상점", result.getStoreName());
        assertEquals("서울 강남", result.getAddress());
        verify(storeRepository).findByStoreId(storeId);
    }

    @Test
    @DisplayName("스토어 수정 시 UpdateStoreEvent를 이용한 테스트")
    void updateStoreTest() {
        when(storeRepository.findByStoreId(defaultUpdateEvent.getStoreId())).thenReturn(Optional.of(defaultStore));
        when(storeRepository.save(any(Store.class))).thenReturn(
                Store.builder()
                        .id(1L)
                        .storeId("S001")
                        .storeName("업데이트된 상점")
                        .address("서울 강남구")
                        .build()
        );

        // Act
        Store result = storeService.updateStore(defaultUpdateEvent);

        // Assert
        assertNotNull(result);
        assertEquals("업데이트된 상점", result.getStoreName());
        assertEquals("서울 강남구", result.getAddress());
        verify(storeRepository).save(any(Store.class));
    }

    @Test
    @DisplayName("스토어가 없는 경우 수정할 경우")
    void updateStore_NoStore() {
        // Given
        String storeId = "NOT_FOUND";
        UpdateStoreEvent updateEvent = new UpdateStoreEvent(storeId, "업데이트된 상점", "서울 강남구");

        when(storeRepository.findByStoreId(storeId)).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(EntityNotFoundException.class,
                () -> storeService.updateStore(updateEvent));

        assertTrue(exception.getMessage().contains("Store not found"));
        verify(storeRepository).findByStoreId(storeId);
        verify(storeRepository, never()).save(any());
    }

    @Test
    @DisplayName("스토어 삭제")
    void deleteStoreTest() {
        when(storeRepository.findByStoreId(storeId)).thenReturn(Optional.of(defaultStore));
        doNothing().when(storeRepository).delete(defaultStore);

        storeService.deleteStore(storeId);

        verify(storeRepository).delete(defaultStore);
        verify(storeRepository).findByStoreId(storeId);
    }

    @Test
    @DisplayName("스토어가 없는 경우 삭제할 경우")
    void deleteStore_NoStore() {
        String storeId = "NOT_FOUND";
        when(storeRepository.findByStoreId(storeId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(StoreNotFoundException.class, () ->
                storeService.deleteStore(storeId));

        assertTrue(exception.getMessage().contains("Store not found"));
        verify(storeRepository).findByStoreId(storeId);

    }
}
