package SoloTech.StockFlow.order;

import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.order.dto.OrderDto;
import SoloTech.StockFlow.order.entity.Order;
import SoloTech.StockFlow.order.repository.OrderRepository;
import SoloTech.StockFlow.order.service.OrderService;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private Cache<String, Object> localCache;

    @Mock
    private CachePublisher cachePublisher;

    @Mock
    private ObjectMapper mapper; // ✅ ObjectMapper 추가

    @InjectMocks
    private OrderService orderService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private static final String ORDER_KEY_PREFIX = "order:";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void createOrderTest() {
        // Given
        OrderDto dto = new OrderDto("S12345", "P56789", "STK98765", 2L);
        Order mockOrder = Order.builder()
                .id(1L)
                .orderId("1907728879506821120") // ❌ 하드코딩 제거
                .storeId("S12345")
                .productId("P56789")
                .stockId("STK98765")
                .quantity(2L)
                .build();

        // ✅ ObjectMapper 동작 모킹 추가
        when(mapper.convertValue(any(OrderDto.class), eq(Order.class))).thenReturn(mockOrder);
        when(orderRepository.saveAndFlush(any(Order.class))).thenReturn(mockOrder);

        // When
        Order createdOrder = orderService.createOrder(dto);

        // Then
        assertNotNull(createdOrder);
        assertEquals("S12345", createdOrder.getStoreId());

        String cacheKey = "order:" + createdOrder.getOrderId();

        // Redis 및 로컬 캐시 저장 검증
        verify(redisTemplate.opsForValue(), times(1)).set(eq(cacheKey), eq(createdOrder), any());
        verify(localCache, times(1)).put(eq(cacheKey), eq(createdOrder));
    }

    @Test
    void readOrderTest() {
        // Given
        String orderId = "1234567890";
        String cacheKey = ORDER_KEY_PREFIX + orderId;
        Order mockOrder = Order.builder()
                .id(1L)
                .orderId(orderId)
                .storeId("S12345")
                .productId("P56789")
                .stockId("STK98765")
                .quantity(2L)
                .build();

        when(localCache.getIfPresent(cacheKey)).thenReturn(null);
        when(redisTemplate.opsForValue().get(cacheKey)).thenReturn(null);
        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(mockOrder));

        // When
        Order result = orderService.readOrder(orderId);

        // Then
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());

        verify(localCache, times(1)).getIfPresent(cacheKey);
        verify(redisTemplate.opsForValue(), times(1)).get(cacheKey);
        verify(orderRepository, times(1)).findByOrderId(orderId);
        verify(redisTemplate.opsForValue(), times(1)).set(eq(cacheKey), eq(mockOrder));
        verify(localCache, times(1)).put(eq(cacheKey), eq(mockOrder));
    }

    @Test
    void updateOrderTest() throws JsonMappingException {
        // Given
        String orderId = "order123";
        OrderDto dto = new OrderDto("store1", "product1", "stock1", 10L);
        Order existingOrder = Order.builder()
                .orderId(orderId)
                .storeId("storeOld")
                .productId("productOld")
                .stockId("stockOld")
                .quantity(5L)
                .build();
        Order updatedOrder = Order.builder()
                .orderId(orderId)
                .storeId(dto.getStoreId())
                .productId(dto.getProductId())
                .stockId(dto.getStockId())
                .quantity(dto.getQuantity())
                .build();
        String cacheKey = ORDER_KEY_PREFIX + orderId;
        String message = "Updated order-" + cacheKey;

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingOrder));
        // ObjectMapper의 updateValue를 모킹하여 예외 없이 작동하도록 설정
        doAnswer(invocation -> {
            Order target = invocation.getArgument(0); // 첫 번째 매개변수
            OrderDto source = invocation.getArgument(1); // 두 번째 매개변수
            target.setStoreId(source.getStoreId());
            target.setProductId(source.getProductId());
            target.setStockId(source.getStockId());
            target.setQuantity(source.getQuantity());
            return null; // updateValue는 void이므로 null 반환
        }).when(mapper).updateValue(existingOrder, dto);

        when(orderRepository.save(existingOrder)).thenReturn(updatedOrder);
        when(localCache.getIfPresent(cacheKey)).thenReturn(null);

        // When
        Order result = orderService.updateOrder(orderId, dto);

        // Then
        assertEquals(updatedOrder, result);
        verify(localCache).put(cacheKey, updatedOrder);
        verify(cachePublisher).publish("cache-sync", message);
        verify(orderRepository).save(existingOrder);
        verify(mapper).updateValue(existingOrder, dto);
    }


    @Test
    void deleteOrderTest() {
        // Given
        String orderId = "1234567890";
        String cacheKey = ORDER_KEY_PREFIX + orderId;
        Order mockOrder = Order.builder()
                .id(1L)
                .orderId(orderId)
                .storeId("S12345")
                .productId("P56789")
                .stockId("STK98765")
                .quantity(2L)
                .build();

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(mockOrder));

        // When
        orderService.deleteOrder(orderId);

        // Then
        verify(orderRepository, times(1)).delete(mockOrder);
        verify(localCache, times(1)).invalidate(cacheKey);
        verify(redisTemplate, times(1)).delete(cacheKey);
        verify(cachePublisher, times(1)).publish(eq("cache-sync"), contains("Deleted order-order:"));
    }
}
