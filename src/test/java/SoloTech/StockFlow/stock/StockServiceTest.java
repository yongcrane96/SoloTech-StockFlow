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

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StockServiceTest {
    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockService stockService;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this); // Mock 객체 초기화
    }

    @Test
    void getStockTest(){
        Stock mockStock = new Stock(1L, "S001", "W001", "P001", 100L);
        Mockito.when(stockRepository.findByStockIdAndDeletedFalse(any(String.class))).thenReturn(java.util.Optional.of(mockStock));

        Stock result = stockService.getStock("S001");

        assertNotNull(result);
        assertEquals("S001", result.getStockId());
        assertEquals("W001", result.getStoreId());
    }

    @Test
    void decreaseStockSuccessTest(){
        // 재고가 충분히 있을 때 감소하는지 확인하는 테스트
        Stock mockStock = new Stock(1L, "S001", "W001", "P001", 100L); // 초기 재고 100
        Mockito.when(stockRepository.findByStockIdAndDeletedFalse("S001")).thenReturn(java.util.Optional.of(mockStock));
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
        Mockito.when(stockRepository.findByStockIdAndDeletedFalse("S001")).thenReturn(java.util.Optional.of(mockStock));

        // 테스트 실행 & 예외 발생 확인
        RuntimeException exception = assertThrows(RuntimeException.class, () ->{
            stockService.decreaseStock("S001", 150L);
        });

        // 검증
        assertEquals("The quantity is larger than the stock: S001", exception.getMessage());
    }
}
