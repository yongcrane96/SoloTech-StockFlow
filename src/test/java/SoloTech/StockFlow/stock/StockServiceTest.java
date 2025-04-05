package SoloTech.StockFlow.stock;

import SoloTech.StockFlow.stock.entity.Stock;
import SoloTech.StockFlow.stock.repository.StockRepository;
import SoloTech.StockFlow.stock.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test; // ✅ JUnit 5
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.Mockito.*;


import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StockServiceTest {
    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockService stockService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this); // Mock 객체 초기화

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getStockTest(){
        Stock mockStock = new Stock(1L, "S001", "W001", "P001", 100L);
        Mockito.when(stockRepository.findByStockId(any(String.class))).thenReturn(java.util.Optional.of(mockStock));

        Stock result = stockService.getStock("S001");

        assertNotNull(result);
        assertEquals("S001", result.getStockId());
        assertEquals("W001", result.getStoreId());
    }

    @Test
    void decreaseStockSuccessTest(){
        // 재고가 충분히 있을 때 감소하는지 확인하는 테스트
        Stock mockStock = new Stock(1L, "S001", "W001", "P001", 100L); // 초기 재고 100
        Mockito.when(stockRepository.findByStockId("S001")).thenReturn(java.util.Optional.of(mockStock));
        Mockito.when(stockRepository.save(any(Stock.class))).thenReturn(mockStock); // 저장 후 mockStock 반환

        // 테스트 실행
        Stock result = stockService.decreaseStock("S001", 50L); // 재고 50 감소

        // 검증
        assertNotNull(result);
        assertEquals(50L, result.getStock()); // 100에서 50이 감소해서 50이어야 함
    }
    @Test
    void decreaseStockInsufficientTest(){
        // 재고가 부족할 때 예외 발생 확인
        Stock mockStock = new Stock(1L, "S001", "W001", "P001", 100L); // 초기 재고 100
        Mockito.when(stockRepository.findByStockId("S001")).thenReturn(java.util.Optional.of(mockStock));

        // 테스트 실행 & 예외 발생 확인
        RuntimeException exception = assertThrows(RuntimeException.class, () ->{
            stockService.decreaseStock("S001", 150L);
        });

        // 검증
        assertEquals("The quantity is larger than the stock: S001", exception.getMessage());
    }

    /**
     * ✅ 동시성 테스트 (멀티스레드 환경에서 `decreaseStock` 호출)
     */
    @Test
    void decreaseStockConcurrencyTest() throws InterruptedException {
        Stock mockStock = new Stock(1L, "S001", "W001", "P001", 100L);
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
        Stock mockStock = new Stock(1L, "S001", "W001", "P001", 100L);

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
