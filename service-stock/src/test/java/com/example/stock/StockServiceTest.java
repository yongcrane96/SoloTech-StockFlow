package com.example.stock;


import cn.hutool.core.lang.Snowflake;
import com.example.kafka.CreateStockEvent;
import com.example.kafka.DecreaseStockEvent;
import com.example.kafka.UpdateStockEvent;
import com.example.stock.entity.Stock;
import com.example.stock.exception.StockNotFoundException;
import com.example.stock.repository.StockRepository;
import com.example.stock.service.StockService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StockServiceTest {
    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockService stockService;

    private Stock defaultStock;
    private CreateStockEvent defaultCreateEvent;
    private UpdateStockEvent defaultUpdateEvent;
    private DecreaseStockEvent defaultDecreaseStockEvent;
    private String stockId = "S001";

    @BeforeEach
    void setUp(){
        Snowflake snowflake = new Snowflake(1, 1);
        long snowflakeId = snowflake.nextId();

        defaultStock = Stock.builder()
                .id(snowflakeId)
                .stockId(stockId)
                .storeId("W001")
                .productId("P001")
                .stock(1000L)
                .build();

        defaultCreateEvent = new CreateStockEvent(
                snowflakeId, stockId, "W001", "P001", 1000L
        );

        defaultUpdateEvent = new UpdateStockEvent(
                stockId, "W001","P001",2000L
        );
        defaultDecreaseStockEvent = new DecreaseStockEvent(
                stockId, 300L
        );
    }

    @Test
    @DisplayName("재고 생성")
    void createStockTest() {
        when(stockRepository.saveAndFlush(any(Stock.class))).thenReturn(defaultStock);

        Stock savedStock = stockService.createStock(defaultCreateEvent);

        assertNotNull(savedStock);
        assertEquals(defaultStock.getStoreId(), savedStock.getStoreId());
        assertEquals(defaultStock.getProductId(), savedStock.getProductId());
        assertEquals(defaultStock.getStock(), savedStock.getStock());

        verify(stockRepository).saveAndFlush(any(Stock.class));
    }

    @Test
    @DisplayName("재고 조회")
    void getStockTest() {
        when(stockRepository.findByStockId(eq(stockId))).thenReturn(Optional.of(defaultStock));

        Stock result = stockService.getStock(stockId);

        assertNotNull(result);
        assertEquals(stockId, result.getStockId());
        assertEquals(defaultStock.getStoreId(), result.getStoreId());
        verify(stockRepository, times(1)).findByStockId(stockId);
    }

    @Test
    @DisplayName("재고 감소 성공 시")
    void decreaseStockSuccessTest() {
        defaultStock = defaultStock.toBuilder().stock(600L).build();

        when(stockRepository.findByStockId(stockId)).thenReturn(Optional.of(defaultStock));
        when(stockRepository.save(any(Stock.class))).thenReturn(defaultStock);

        // when
        Stock result = stockService.decreaseStock(defaultDecreaseStockEvent);

        // then
        assertNotNull(result);
        assertEquals(300L, result.getStock()); // 600 - 300 = 300
        verify(stockRepository, times(1)).findByStockId(stockId);
        verify(stockRepository, times(1)).save(any(Stock.class));
    }

    @Test
    @DisplayName("재고 부족 시 예외 발생")
    void decreaseStockInsufficientTest() {
        // given
        defaultStock.setStock(100L); // 300L 요청인데 현재 100L라 부족하게 만듦
        when(stockRepository.findByStockId(stockId)).thenReturn(Optional.of(defaultStock));

        // when
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            stockService.decreaseStock(defaultDecreaseStockEvent);
        });

        // then
        assertTrue(exception.getMessage().contains("The quantity is larger than the stock"));
        verify(stockRepository, times(1)).findByStockId(stockId);
    }

    @Test
    @DisplayName("재고 수정 시")
    void updateStockTest() {
        when(stockRepository.findByStockId(defaultUpdateEvent.getStockId())).thenReturn(Optional.of(defaultStock));
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Stock result = stockService.updateStock(defaultUpdateEvent);

        assertNotNull(result);
        assertEquals(2000L, result.getStock());
        assertEquals(defaultStock.getStockId(), result.getStockId());

        verify(stockRepository, times(1)).save(any(Stock.class));
    }

    @Test
    @DisplayName("재고가 없는 경우 수정할 경우")
    void updateStock_NoStock() {
        String stockId = "NOT_FOUND";
        defaultUpdateEvent = new UpdateStockEvent(
                stockId, "W001","P001",2000L
        );
        when(stockRepository.findByStockId(stockId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(EntityNotFoundException.class, () ->
                stockService.updateStock(defaultUpdateEvent));

        // 예외 메시지 검증
        assertTrue(exception.getMessage().contains("Stock not found"));
        verify(stockRepository).findByStockId(stockId);
        verify(stockRepository, never()).save(any());

    }

    @Test
    @DisplayName("재고 삭제")
    void deleteStockTest() {
        when(stockRepository.findByStockId(stockId)).thenReturn(Optional.of(defaultStock));
        doNothing().when(stockRepository).delete(defaultStock);

        stockService.deleteStock(stockId);

        verify(stockRepository, times(1)).delete(defaultStock);
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
    @DisplayName("동시성 재고 감소 테스트 - 모든 요청 성공")
    void decreaseStockConcurrencySuccessOnlyTest() throws InterruptedException {
        long initialStock = 1000L;
        int totalRequests = 100; // 요청 수
        long decreasePerRequest = 5L;
        defaultStock.setStock(initialStock);

        defaultDecreaseStockEvent = new DecreaseStockEvent(
                stockId, 5L
        );
        when(stockRepository.findByStockId(stockId)).thenAnswer(invocation -> Optional.of(defaultStock));
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            executorService.execute(() -> {
                try {
                    synchronized (defaultStock){
                        stockService.decreaseStock(defaultDecreaseStockEvent);
                    }
                    successCount.incrementAndGet();
                }catch (Exception e){
                    System.err.println("에러 발생: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long expectedStock = initialStock - (decreasePerRequest * totalRequests);

        System.out.println("성공 수량: " + successCount.get());
        System.out.println("남은 재고: " + defaultStock.getStock());

        // Assert
        assertEquals(totalRequests, successCount.get());
        assertEquals(expectedStock, defaultStock.getStock());
    }

    @Test
    @DisplayName("동시성 재고 감소 테스트 - 일부 실패 발생")
    void decreaseStockConcurrencyWithFailureTest() throws InterruptedException {
        long initialStock = 300L; // 재고: 300
        int totalRequests = 50; // 요청 수: 50
        long decreasePerRequest = 10L; // 요청당 차감량: 10

        defaultStock.setStock(initialStock);

        defaultDecreaseStockEvent = new DecreaseStockEvent(
                stockId, decreasePerRequest
        );
        when(stockRepository.findByStockId(stockId)).thenAnswer(invocation -> Optional.of(defaultStock));
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExecutorService executorService = Executors.newFixedThreadPool(16);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            executorService.execute(() -> {
                try {
                    stockService.decreaseStock(defaultDecreaseStockEvent);
                    successCount.incrementAndGet();
                } catch (RuntimeException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long expectedSuccess = initialStock / decreasePerRequest;
        long expectedStock = initialStock - (expectedSuccess * decreasePerRequest);

        System.out.println("총 요청 수: " + totalRequests);
        System.out.println("성공 수량: " + successCount.get());
        System.out.println("실패 수량: " + failureCount.get());
        System.out.println("남은 재고: " + defaultStock.getStock());

        // Assert
        assertEquals(expectedSuccess, successCount.get());
        assertEquals(totalRequests - expectedSuccess, failureCount.get());
        assertEquals(expectedStock, defaultStock.getStock());
    }

    @Test
    @DisplayName("동시성 재고 감소 테스트 - 최대 성공 수 확인")
    void decreaseStockConcurrency_MaxSuccessTest() throws InterruptedException {
        long initialStock = 100L; // 초기 재고
        int totalRequests = 100;
        long decreasePerRequest = 1L; // 각 요청당 20L 감소

        defaultStock.setStock(initialStock);

        defaultDecreaseStockEvent = new DecreaseStockEvent(
                stockId, decreasePerRequest
        );

        when(stockRepository.findByStockId(stockId)).thenAnswer(invocation -> Optional.of(defaultStock));
        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CountDownLatch latch = new CountDownLatch(totalRequests);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            executorService.execute(() -> {
                try {
                    stockService.decreaseStock(defaultDecreaseStockEvent);
                    successCount.incrementAndGet();
                } catch (RuntimeException e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        System.out.println("요청 수: " + totalRequests);
        System.out.println("성공 수: " + successCount.get());
        System.out.println("실패 수: " + failureCount.get());
        System.out.println("남은 재고: " + defaultStock.getStock());

        // assertEquals 추가
        assertEquals(totalRequests, successCount.get(), "모든 요청이 성공해야 합니다."); // 성공 수가 총 요청 수와 일치해야 함
        assertEquals(0, failureCount.get(), "실패한 요청이 없어야 합니다."); // 실패 수가 0이어야 함
        assertEquals(initialStock - totalRequests * decreasePerRequest, defaultStock.getStock(), "남은 재고가 예상과 일치해야 합니다.");
    }
    @Test
    @DisplayName("Redisson 분산락 테스트 - 최대 요청량 제한")
    void decreaseStockConcurrencyWithRedissonLockTest() throws InterruptedException {
        // Given
        String stockId = "S001";
        long initialStock = 300L;
        int totalRequests = 1000;
        long decreasePerRequest = 10L;

        Stock stock = Stock.builder()
                .id(1L)
                .stockId(stockId)
                .storeId("W001")
                .productId("P001")
                .stock(initialStock)
                .deleted(false)
                .build();

        defaultDecreaseStockEvent = new DecreaseStockEvent(
                stockId, decreasePerRequest
        );

        // mock 객체를 항상 새로운 인스턴스로 반환
        when(stockRepository.findByStockId(stockId))
                .thenAnswer(invocation -> Optional.of(stock));

        when(stockRepository.save(any(Stock.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExecutorService executor = Executors.newFixedThreadPool(64);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    stockService.decreaseStock(defaultDecreaseStockEvent);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long expectedMaxSuccess = initialStock / decreasePerRequest;
        long expectedRemainingStock = initialStock - (successCount.get() * decreasePerRequest);

        System.out.println("요청 수: " + totalRequests);
        System.out.println("성공 수: " + successCount.get());
        System.out.println("실패 수: " + failureCount.get());
        System.out.println("남은 재고: " + stock.getStock());

        assertEquals(expectedMaxSuccess, successCount.get(), "락이 정상이면 성공 수는 초과하지 않아야 함");
        assertEquals(0, stock.getStock(), "재고는 0이어야 함");
    }


//    /**
//     * ✅ 캐시 갱신 & Pub/Sub 메시지 발행 확인
//     */
//    @Test
//    void decreaseStockCacheUpdateTest() {
//        // given
//        Stock mockStock = Stock.builder()
//                .id(1L)
//                .stockId("S001")
//                .storeId("W001")
//                .productId("P001")
//                .stock(100L)
//                .deleted(false) // ✅ 소프트 삭제 고려
//                .build();
//
//        when(stockRepository.findByStockId("S001")).thenReturn(Optional.of(mockStock));
//        when(stockRepository.save(any(Stock.class))).thenReturn(mockStock);
//        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
//        doNothing().when(valueOperations).set(anyString(), any());
//
//        // when
//        stockService.decreaseStock("S001", 20L);
//
//        // then
//        verify(valueOperations, times(1)).set(eq("stock-S001"), any(Stock.class));  // ✅ 핵심 검증
//        verify(redisTemplate, times(1)).convertAndSend(eq("cache-sync"), eq("Updated stock-S001"));
//    }



}
