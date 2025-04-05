package SoloTech.StockFlow.stock;

import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.stock.dto.StockDto;
import SoloTech.StockFlow.stock.entity.Stock;
import SoloTech.StockFlow.stock.repository.StockRepository;
import SoloTech.StockFlow.stock.service.StockService;
import cn.hutool.core.lang.Snowflake;
import java.time.Duration;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class StockServiceTest {
    @Mock
    private StockRepository stockRepository;

    @Mock
    private Cache<String, Object> localCache;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private CachePublisher cachePublisher;

    @Mock
    private Snowflake snowflake;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private StockService stockService;

    private static final String STOCK_KEY_PREFIX = "stock:";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // ✅ Redis Mock 설정
        doNothing().when(valueOperations).set(anyString(), any());
        doNothing().when(cachePublisher).publish(anyString(), anyString());

    }

    @Test
    void createStockTest() {
        // Arrange
        StockDto stockDto = new StockDto("store123", "product456", 100L);
        Stock stock = new Stock();
        stock.setStoreId(stockDto.getStoreId());
        stock.setProductId(stockDto.getProductId());
        stock.setStock(stockDto.getStock());

        Snowflake snowflake = new Snowflake(1, 1);
        stock.setStockId(String.valueOf(snowflake.nextId()));

        when(objectMapper.convertValue(stockDto, Stock.class)).thenReturn(stock);
        when(stockRepository.saveAndFlush(any(Stock.class))).thenReturn(stock);
        doNothing().when(cachePublisher).publish(anyString(), anyString());

        // Act
        Stock savedStock = stockService.createStock(stockDto);

        // Assert
        assertNotNull(savedStock);
        assertEquals(stockDto.getStoreId(), savedStock.getStoreId());
        assertEquals(stockDto.getProductId(), savedStock.getProductId());
        assertEquals(stockDto.getStock(), savedStock.getStock());

        String cacheKey = STOCK_KEY_PREFIX + savedStock.getStockId();
        // Redis 및 로컬 캐시 저장 검증
        verify(redisTemplate.opsForValue(), times(1)).set(eq(cacheKey), eq(savedStock), any(Duration.class)); // Duration 추가
        verify(localCache, times(1)).put(eq(cacheKey), eq(savedStock));
        verify(cachePublisher).publish(anyString(), anyString());
    }

    @Test
    void getStockTest() {
        String stockId = "S001";
        String cacheKey = STOCK_KEY_PREFIX + stockId;
        Stock mockStock = new Stock(1L, stockId, "W001", "P001", 100L);

        when(localCache.getIfPresent(eq(cacheKey))).thenReturn(null);
        when(valueOperations.get(eq(cacheKey))).thenReturn(null);
        when(stockRepository.findByStockId(eq(stockId))).thenReturn(Optional.of(mockStock));

        Stock result = stockService.getStock(stockId);

        assertNotNull(result);
        assertEquals(stockId, result.getStockId());
        assertEquals("W001", result.getStoreId());

        verify(valueOperations, times(1)).set(eq(cacheKey), eq(mockStock));
        verify(localCache, times(1)).put(eq(cacheKey), eq(mockStock));
    }

    @Test
    void decreaseStockSuccessTest() {
        String stockId = "S001";
        Stock mockStock = new Stock(1L, stockId, "W001", "P001", 100L);

        when(stockRepository.findByStockId(stockId)).thenReturn(Optional.of(mockStock));
        when(stockRepository.save(any(Stock.class))).thenReturn(mockStock);

        Stock result = stockService.decreaseStock(stockId, 50L);

        assertNotNull(result);
        assertEquals(50L, result.getStock());

        String cacheKey = STOCK_KEY_PREFIX + stockId;
        verify(valueOperations, times(1)).set(eq(cacheKey), eq(mockStock));
        verify(localCache, times(1)).put(eq(cacheKey), eq(mockStock));
    }

    @Test
    void decreaseStockInsufficientTest() {
        String stockId = "S001";
        Stock mockStock = new Stock(1L, stockId, "W001", "P001", 100L);

        when(stockRepository.findByStockId(stockId)).thenReturn(Optional.of(mockStock));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            stockService.decreaseStock(stockId, 150L);
        });

        assertEquals("The quantity is larger than the stock: S001", exception.getMessage());

        verify(valueOperations, never()).set(anyString(), any());
        verify(localCache, never()).put(anyString(), any());
    }

    @Test
    void updateStockTest() throws Exception {
        // Given
        String stockId = "1234";
        Stock existingStock = new Stock(1L, stockId, "W001", "P001", 30L); // Existing stock 30
        StockDto updatedDto = new StockDto();
        updatedDto.setStock(50L); // Updated stock (30 → 50)

        String cacheKey = STOCK_KEY_PREFIX + stockId;
        String expectedMessage = "Updated stock-" + cacheKey;

        // Mock setup
        when(stockRepository.findByStockId(stockId)).thenReturn(Optional.of(existingStock));
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doAnswer(invocation -> {
            Stock stock = invocation.getArgument(0);
            StockDto dto = invocation.getArgument(1);
            stock.setStock(dto.getStock()); // Apply updated stock value
            return null;
        }).when(objectMapper).updateValue(any(Stock.class), any(StockDto.class));

        doNothing().when(valueOperations).set(eq(cacheKey), any(Stock.class));
        doNothing().when(cachePublisher).publish(eq("cache-sync"), eq(expectedMessage));

        // When
        Stock result = stockService.updateStock(stockId, updatedDto);

        // Then
        assertNotNull(result);
        assertEquals(50L, result.getStock());

        // Verify mock interactions
        verify(valueOperations, times(1)).set(eq(cacheKey), any(Stock.class));
        verify(cachePublisher, times(1)).publish(eq("cache-sync"), eq(expectedMessage));
        verify(stockRepository, times(1)).save(any(Stock.class));
    }

    @Test
    void deleteStockTest() {
        String stockId = "S001";
        String cacheKey = STOCK_KEY_PREFIX + stockId;
        Stock mockStock = new Stock(1L, stockId, "W001", "P001", 100L);

        when(stockRepository.findByStockId(stockId)).thenReturn(Optional.of(mockStock));
        doNothing().when(stockRepository).delete(mockStock);

        stockService.deleteStock(stockId);

        verify(stockRepository, times(1)).delete(mockStock);
        verify(localCache, times(1)).invalidate(cacheKey);
        verify(redisTemplate, times(1)).delete(cacheKey);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(cachePublisher, times(1)).publish(eq("cache-sync"), messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains("Deleted stock-stock:"));
    }
}
