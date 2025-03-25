package SoloTech.StockFlow.store;

import SoloTech.StockFlow.store.entity.Store;
import SoloTech.StockFlow.store.repository.StoreRepository;
import SoloTech.StockFlow.store.service.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

public class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private StoreService storeService;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this); // Mock 객체 초기화
    }

    @Test
    void getStoreTest(){
        Store mockStore = new Store(1L, "W001", "상점", "서울시 압구정");
        Mockito.when(storeRepository.findByStoreId(any(String.class))).thenReturn(java.util.Optional.of(mockStore));

        Store result = storeService.getStore("W001");

        assertNotNull(result);
        assertEquals("W001", result.getStoreId());
        assertEquals("상점", result.getStoreName());
        assertEquals("서울시 압구정", result.getAddress());
    }

}
