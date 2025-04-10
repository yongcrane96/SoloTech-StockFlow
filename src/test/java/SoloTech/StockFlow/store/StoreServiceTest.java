package SoloTech.StockFlow.store;

import SoloTech.StockFlow.store.dto.StoreDto;
import SoloTech.StockFlow.store.entity.Store;
import SoloTech.StockFlow.store.exception.StoreNotFoundException;
import SoloTech.StockFlow.store.repository.StoreRepository;
import SoloTech.StockFlow.store.service.StoreService;
import cn.hutool.core.lang.Snowflake;
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

@ExtendWith(MockitoExtension.class) // Mockito 확장 적용
public class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private StoreService storeService;

    @Mock
    private ValueOperations<String, Object> valueOperations; // Redis 값 조작 인터페이스 Mock

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("스토어 생성")
    void createStoreTest() {
        StoreDto storeDto = new StoreDto("새로운 상점", "서울 강남");
        Store newStore = new Store(2L, "123456789", "새로운 상점", "서울 강남");

        Snowflake snowflake = new Snowflake(1, 1);
        newStore.setStoreId(String.valueOf(snowflake.nextId()));

        when(mapper.convertValue(any(StoreDto.class), eq(Store.class))).thenReturn(newStore);
        when(storeRepository.saveAndFlush(any(Store.class))).thenReturn(newStore);

        Store result = storeService.createStore(storeDto);

        assertNotNull(result);
        assertEquals("새로운 상점", result.getStoreName());
        assertEquals("서울 강남", result.getAddress());

        verify(storeRepository).saveAndFlush(any(Store.class));
    }

    @Test
    @DisplayName("스토어 조회")
    void getStoreTest() {
        String storeId = "W001";
        Store mockStore = new Store(1L, storeId, "상점", "서울시 압구정");

        when(storeRepository.findByStoreId(any(String.class))).thenReturn(Optional.of(mockStore));

        Store result = storeService.getStore("W001");

        assertNotNull(result);
        assertEquals("W001", result.getStoreId());
        assertEquals("상점", result.getStoreName());
        assertEquals("서울시 압구정", result.getAddress());

        verify(storeRepository, times(1)).findByStoreId(storeId);
    }

    @Test
    @DisplayName("스토어 수정 시")
    void updateStoreTest() throws JsonMappingException {
        // Given
        String storeId = "W002";
        Store existingStore = new Store(3L, storeId, "기존 상점", "서울 강남");
        StoreDto updateDto = new StoreDto("업데이트된 상점", "서울 강남구");
        Store updatedStore = new Store(3L, storeId, "업데이트된 상점", "서울 강남구");

        when(storeRepository.findByStoreId(storeId)).thenReturn(Optional.of(existingStore));

        // doNothing() -> void 메서드에서만 사용 가능
        // doAnswer() -> 메서드가 수행하는 동작을 직접 정의
        doAnswer(invocation -> {
            Store storeArg = invocation.getArgument(0);
            StoreDto dtoArg = invocation.getArgument(1);
            storeArg.setStoreName(dtoArg.getStoreName());
            storeArg.setAddress(dtoArg.getAddress());
            return null;
        }).when(mapper).updateValue(existingStore, updateDto);

        when(storeRepository.save(any(Store.class))).thenReturn(updatedStore);

        // When
        Store result = storeService.updateStore(storeId, updateDto);

        // Then
        assertNotNull(result);
        assertEquals("업데이트된 상점", result.getStoreName());
        assertEquals("서울 강남구", result.getAddress());

        verify(storeRepository).save(any(Store.class));
        verify(mapper).updateValue(existingStore, updateDto);
    }

    @Test
    @DisplayName("스토어가 없는 경우 수정할 경우")
    void updateStore_NoStore() {
        // Given: 테스트를 위한 초기 설정
        String storeId = "NOT_FOUND";
        StoreDto updateDto = new StoreDto("업데이트된 상점", "서울 강남구");

        when(storeRepository.findByStoreId(storeId)).thenReturn(Optional.empty());

        // When & Then: 서비스 메서드 호출 시 예외가 발생하는지 확인
        Exception exception = assertThrows(EntityNotFoundException.class,
                () -> storeService.updateStore(storeId, updateDto));

        // 예외 메시지 검증
        assertTrue(exception.getMessage().contains("Store not found"));

        // Verify: mapper와 save 메서드가 호출되지 않았는지 확인
        verify(storeRepository).findByStoreId(storeId);
    }

    @Test
    @DisplayName("스토어 삭제")
    void deleteStoreTest() {
        String storeId = "W003";
        Store existingStore = new Store(4L, storeId, "삭제될 상점", "서울 마포");

        when(storeRepository.findByStoreId(storeId)).thenReturn(Optional.of(existingStore));
        doNothing().when(storeRepository).delete(existingStore);

        storeService.deleteStore(storeId);

        verify(storeRepository).delete(existingStore);
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
