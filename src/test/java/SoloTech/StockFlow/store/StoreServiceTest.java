package SoloTech.StockFlow.store;

import SoloTech.StockFlow.store.dto.StoreDto;
import SoloTech.StockFlow.store.entity.Store;
import SoloTech.StockFlow.store.repository.StoreRepository;
import SoloTech.StockFlow.store.service.StoreService;
import SoloTech.StockFlow.cache.CachePublisher;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    private static final String STORE_KEY_PREFIX = "store:";


    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getStoreTest() {
        String storeId = "W001";
        String cacheKey = STORE_KEY_PREFIX + storeId;
        Store mockStore = new Store(1L, storeId, "상점", "서울시 압구정");

        when(localCache.getIfPresent(any(String.class))).thenReturn(null);
        when(valueOperations.get(any(String.class))).thenReturn(null);
        when(storeRepository.findByStoreId(any(String.class))).thenReturn(Optional.of(mockStore));

        Store result = storeService.getStore("W001");

        assertNotNull(result);
        assertEquals("W001", result.getStoreId());
        assertEquals("상점", result.getStoreName());
        assertEquals("서울시 압구정", result.getAddress());

        verify(valueOperations, times(1)).set(eq(cacheKey), eq(mockStore));
        verify(localCache, times(1)).put(eq(cacheKey), eq(mockStore));
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

        String cacheKey = STORE_KEY_PREFIX + result.getStoreId();

        verify(storeRepository).saveAndFlush(any(Store.class));
        // Redis 및 로컬 캐시 저장 검증
        verify(redisTemplate.opsForValue(), times(1)).set(eq(cacheKey), eq(result), any());
        verify(localCache, times(1)).put(eq(cacheKey), eq(result));

    }

    @Test
    void updateStoreTest() throws JsonMappingException {
        // Given
        String storeId = "W002";
        Store existingStore = new Store(3L, storeId, "기존 상점", "서울 강남");
        StoreDto updateDto = new StoreDto("업데이트된 상점", "서울 강남구");
        Store updatedStore = new Store(3L, storeId, "업데이트된 상점", "서울 강남구");
        String cacheKey = STORE_KEY_PREFIX + storeId;
        String message = "Updated store-" + cacheKey;

        // RedisTemplate과 ValueOperations 모의 객체 설정
        when(storeRepository.findByStoreId(storeId)).thenReturn(Optional.of(existingStore));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(eq(cacheKey), any(Store.class));
        doNothing().when(cachePublisher).publish(eq("cache-sync"), eq(message));
        when(storeRepository.save(any(Store.class))).thenReturn(updatedStore);

        // When
        Store result = storeService.updateStore(storeId, updateDto);

        // Then
        assertNotNull(result);
        assertEquals("업데이트된 상점", result.getStoreName());
        assertEquals("서울 강남구", result.getAddress());

        verify(storeRepository).save(any(Store.class));
        verify(valueOperations).set(eq(cacheKey), eq(updatedStore));
        verify(cachePublisher).publish(eq("cache-sync"), eq(message));
    }

    @Test
    void deleteStoreTest() {
        String storeId = "W003";
        String cacheKey = STORE_KEY_PREFIX + storeId;
        Store existingStore = new Store(4L, storeId, "삭제될 상점", "서울 마포");

        when(storeRepository.findByStoreId(storeId)).thenReturn(Optional.of(existingStore));
        doNothing().when(storeRepository).delete(any(Store.class));

        storeService.deleteStore(storeId);

        verify(storeRepository, times(1)).delete(existingStore);
        verify(localCache, times(1)).invalidate(cacheKey);
        verify(redisTemplate, times(1)).delete(cacheKey);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(cachePublisher, times(1)).publish(eq("cache-sync"), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Deleted store-store:"));

    }
}
