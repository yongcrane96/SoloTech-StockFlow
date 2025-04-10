package SoloTech.StockFlow.stock;

import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.stock.dto.StockDto;
import SoloTech.StockFlow.stock.entity.Stock;
import SoloTech.StockFlow.stock.exception.StockNotFoundException;
import SoloTech.StockFlow.stock.repository.StockRepository;
import SoloTech.StockFlow.stock.service.StockService;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.*;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.*;

public class StockServiceTest {
    @Mock
    private StockRepository stockRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private CachePublisher cachePublisher;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private StockService stockService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // ✅ Redis Mock 설정
        doNothing().when(valueOperations).set(anyString(), any());
        doNothing().when(cachePublisher).publish(anyString(), anyString());

    }

    @Test
    @DisplayName("재고 생성")
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

        // Act
        Stock savedStock = stockService.createStock(stockDto);

        // Assert
        assertNotNull(savedStock);
        assertEquals(stockDto.getStoreId(), savedStock.getStoreId());
        assertEquals(stockDto.getProductId(), savedStock.getProductId());
        assertEquals(stockDto.getStock(), savedStock.getStock());

        verify(stockRepository).saveAndFlush(any(Stock.class));
    }

    @Test
    @DisplayName("재고 조회")
    void getStockTest() {
        String stockId = "S001";
        Stock mockStock = Stock.builder()
                .id(1L)
                .stockId(stockId)
                .storeId("W001")
                .productId("P001")
                .stock(100L)
                .deleted(false) // ✅ 소프트 삭제 고려
                .build();


        when(stockRepository.findByStockId(eq(stockId))).thenReturn(Optional.of(mockStock));

        Stock result = stockService.getStock(stockId);

        assertNotNull(result);
        assertEquals(stockId, result.getStockId());
        assertEquals("W001", result.getStoreId());

        verify(stockRepository, times(1)).findByStockId(stockId);
    }

    @Test
    @DisplayName("재고 부족 성공 시")
    void decreaseStockSuccessTest() {
        String stockId = "S001";
        Stock mockStock = Stock.builder()
                .id(1L)
                .stockId(stockId)
                .storeId("W001")
                .productId("P001")
                .stock(100L)
                .deleted(false) // ✅ 소프트 삭제 고려
                .build();


        when(stockRepository.findByStockId(stockId)).thenReturn(Optional.of(mockStock));
        when(stockRepository.save(any(Stock.class))).thenReturn(mockStock);

        Stock result = stockService.decreaseStock(stockId, 50L);

        assertNotNull(result);
        assertEquals(50L, result.getStock());

        verify(stockRepository, times(1)).findByStockId(stockId);
    }

    @Test
    @DisplayName("재고 부족 시 예외 발생")
    void decreaseStockInsufficientTest() {
        String stockId = "S001";
        Stock mockStock = Stock.builder()
                .id(1L)
                .stockId(stockId)
                .storeId("W001")
                .productId("P001")
                .stock(100L)
                .deleted(false) // ✅ 소프트 삭제 고려
                .build();


        when(stockRepository.findByStockId(stockId)).thenReturn(Optional.of(mockStock));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            stockService.decreaseStock(stockId, 150L);
        });

        assertEquals("The quantity is larger than the stock: S001", exception.getMessage());

        verify(stockRepository, times(1)).findByStockId(stockId);
    }

    @Test
    @DisplayName("재고 수정 시")
    void updateStockTest() throws Exception {
        // Given
        String stockId = "1234";
        Stock existingStock = Stock.builder()
                .id(1L)
                .stockId(stockId)
                .storeId("W001")
                .productId("P001")
                .stock(100L)
                .deleted(false) // ✅ 소프트 삭제 고려
                .build(); // Existing stock 30
        StockDto updatedDto = new StockDto();
        updatedDto.setStock(50L); // Updated stock (30 → 50)


        // Mock setup
        when(stockRepository.findByStockId(stockId)).thenReturn(Optional.of(existingStock));
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doAnswer(invocation -> {
            Stock stock = invocation.getArgument(0);
            StockDto dto = invocation.getArgument(1);
            stock.setStock(dto.getStock()); // Apply updated stock value
            return null;
        }).when(objectMapper).updateValue(any(Stock.class), any(StockDto.class));

        // When
        Stock result = stockService.updateStock(stockId, updatedDto);

        // Then
        assertNotNull(result);
        assertEquals(50L, result.getStock());

        // Verify mock interactions
        verify(stockRepository, times(1)).save(any(Stock.class));
    }

    @Test
    @DisplayName("재고가 없는 경우 수정할 경우")
    void updateStock_NoStock() {
        String stockId = "NOT_FOUND";
        StockDto stockDto = new StockDto("store123", "product456", 100L);

        when(stockRepository.findByStockId(stockId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(EntityNotFoundException.class, () ->
                stockService.updateStock(stockId, stockDto));

        // 예외 메시지 검증
        assertTrue(exception.getMessage().contains("Stock not found"));

        // Verify: mapper와 save 메서드가 호출되지 않았는지 확인
        verify(stockRepository).findByStockId(stockId);

    }

    @Test
    @DisplayName("재고 삭제")
    void deleteStockTest() {
        String stockId = "S001";
        Stock mockStock = Stock.builder()
                .id(1L)
                .stockId(stockId)
                .storeId("W001")
                .productId("P001")
                .stock(100L)
                .deleted(false) // ✅ 소프트 삭제 고려
                .build();

        when(stockRepository.findByStockId(stockId)).thenReturn(Optional.of(mockStock));
        doNothing().when(stockRepository).delete(mockStock);

        stockService.deleteStock(stockId);

        verify(stockRepository, times(1)).delete(mockStock);
    }

    @Test
    @DisplayName("재고가 없는 경우 삭제할 경우")
    void deleteStock_NoStock() {
        String stockId = "NOT_FOUND";
        when(stockRepository.findByStockId(stockId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(StockNotFoundException.class, () ->
                stockService.deleteStock(stockId));

        assertTrue(exception.getMessage().contains("Stock not found"));

        verify(stockRepository).findByStockId(stockId);
    }

    /**
     * ✅ 동시성 테스트 (멀티스레드 환경에서 `decreaseStock` 호출)
     */
    @Test
    void decreaseStockConcurrencyTest() throws InterruptedException {
        String stockId = "S001";
        Stock mockStock = Stock.builder()
                .id(1L)
                .stockId(stockId)
                .storeId("W001")
                .productId("P001")
                .stock(100L)
                .deleted(false) // ✅ 소프트 삭제 고려
                .build();
        when(stockRepository.findByStockId("S001")).thenReturn(Optional.of(mockStock));
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> invocation.getArgument(0)); // 저장된 객체 반환

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    stockService.decreaseStock("S001", 10L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 종료될 때까지 대기

        assertEquals(0L, mockStock.getStock()); // 100 - (10 * 10) = 0
    }

    /**
     * ✅ 캐시 갱신 & Pub/Sub 메시지 발행 확인
     */
    @Test
    void decreaseStockCacheUpdateTest() {
        // given
        Stock mockStock = Stock.builder()
                .id(1L)
                .stockId("S001")
                .storeId("W001")
                .productId("P001")
                .stock(100L)
                .deleted(false) // ✅ 소프트 삭제 고려
                .build();

        when(stockRepository.findByStockId("S001")).thenReturn(Optional.of(mockStock));
        when(stockRepository.save(any(Stock.class))).thenReturn(mockStock);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), any());

        // when
        stockService.decreaseStock("S001", 20L);

        // then
        verify(valueOperations, times(1)).set(eq("stock-S001"), any(Stock.class));  // ✅ 핵심 검증
        verify(redisTemplate, times(1)).convertAndSend(eq("cache-sync"), eq("Updated stock-S001"));
    }

}
