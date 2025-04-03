package SoloTech.StockFlow.store;

import SoloTech.StockFlow.store.dto.StoreDto;
import SoloTech.StockFlow.store.entity.Store;
import SoloTech.StockFlow.store.repository.StoreRepository;
import SoloTech.StockFlow.store.service.StoreService;
import SoloTech.StockFlow.cache.CachePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
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
    private Cache<String, Object> localCache;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private CachePublisher cachePublisher;

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
    void getStoreTest() {
        Store mockStore = new Store(1L, "W001", "상점", "서울시 압구정");

        when(localCache.getIfPresent(any(String.class))).thenReturn(null);
        when(valueOperations.get(any(String.class))).thenReturn(null);
        when(storeRepository.findByStoreId(any(String.class))).thenReturn(Optional.of(mockStore));

        Store result = storeService.getStore("W001");

        assertNotNull(result);
        assertEquals("W001", result.getStoreId());
        assertEquals("상점", result.getStoreName());
        assertEquals("서울시 압구정", result.getAddress());
    }

    @Test
    void createStoreTest() {
        StoreDto storeDto = new StoreDto("새로운 상점", "서울 강남");
        Store newStore = new Store(2L, "123456789", "새로운 상점", "서울 강남");

        when(mapper.convertValue(any(StoreDto.class), eq(Store.class))).thenReturn(newStore);
        when(storeRepository.saveAndFlush(any(Store.class))).thenReturn(newStore);

        Store result = storeService.createStore(storeDto);

        assertNotNull(result);
        assertEquals("새로운 상점", result.getStoreName());
        assertEquals("서울 강남", result.getAddress());

        verify(storeRepository).saveAndFlush(any(Store.class));
    }

    @Test
    void updateStoreTest() throws Exception {
        String storeId = "W002";
        Store existingStore = new Store(3L, storeId, "기존 상점", "서울 강남");
        StoreDto updateDto = new StoreDto("업데이트된 상점", "서울 강남구");
        Store updatedStore = new Store(3L, storeId, "업데이트된 상점", "서울 강남구");

        when(storeRepository.findByStoreId(storeId)).thenReturn(Optional.of(existingStore));
        when(mapper.updateValue(any(Store.class), any(StoreDto.class))).thenReturn(updatedStore);
        when(storeRepository.save(any(Store.class))).thenReturn(updatedStore);

        Store result = storeService.updateStore(storeId, updateDto);

        assertNotNull(result);
        assertEquals("업데이트된 상점", result.getStoreName());
        assertEquals("서울 강남구", result.getAddress());

        verify(storeRepository).save(any(Store.class));
    }

    @Test
    void deleteStoreTest() {
        String storeId = "W003";
        Store existingStore = new Store(4L, storeId, "삭제될 상점", "서울 마포");

        when(storeRepository.findByStoreId(storeId)).thenReturn(Optional.of(existingStore));
        doNothing().when(storeRepository).delete(any(Store.class));

        storeService.deleteStore(storeId);

        verify(storeRepository).delete(any(Store.class));
        verify(localCache).invalidate(any(String.class));
        verify(redisTemplate).delete(any(String.class));
    }
}
