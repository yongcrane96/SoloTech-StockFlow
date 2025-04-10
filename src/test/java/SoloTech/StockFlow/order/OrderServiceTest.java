package SoloTech.StockFlow.order;

import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.order.dto.OrderDto;
import SoloTech.StockFlow.order.entity.Order;
import SoloTech.StockFlow.order.repository.OrderRepository;
import SoloTech.StockFlow.order.service.OrderService;
import SoloTech.StockFlow.payment.dto.PaymentDto;
import SoloTech.StockFlow.payment.entity.Payment;
import SoloTech.StockFlow.payment.entity.PaymentStatus;
import SoloTech.StockFlow.payment.service.PaymentService;
import SoloTech.StockFlow.product.entity.Product;
import SoloTech.StockFlow.product.service.ProductService;
import SoloTech.StockFlow.stock.entity.Stock;
import SoloTech.StockFlow.stock.service.StockService;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
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

    @Mock
    private ProductService productService;

    @Mock
    private StockService stockService;

    @Mock
    private PaymentService paymentService;

    private static final String ORDER_KEY_PREFIX = "order:";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void createOrderTest() {
        // Given
        OrderDto orderDto = new OrderDto(
                "O12345",      // orderId
                "S12345",      // storeId
                "P001",        // productId (올바른 값으로 수정)
                "STK98765",    // stockId
                2L,            // quantity
                20000L,        // amount
                "CARD"         // paymentMethod
        );

        Product mockProduct = new Product(1L, "P001", "백팩", 10000L, "튼튼한 가방");
        Stock mockStock = Stock.builder()
                .id(1L)
                .stockId("S001")
                .storeId("W001")
                .productId("P001")
                .stock(100L)  // 충분한 재고 설정
                .deleted(false)
                .build();

        Payment mockPayment = Payment.builder()
                .id(1L)
                .paymentId("PAY123")
                .orderId(orderDto.getOrderId())
                .amount(20000L)
                .paymentMethod("CARD")
                .paymentStatus(PaymentStatus.Success)  // "Success"로 설정
                .build();

        Order mockOrder = new Order();
        mockOrder.setProductId("P001");
        mockOrder.setQuantity(2L);
        mockOrder.setAmount(20000L);
        mockOrder.setPaymentMethod("CARD");

        // Mocking
        when(productService.getProduct("P001")).thenReturn(mockProduct);
        when(stockService.getStock("P001")).thenReturn(mockStock);
        when(paymentService.createPayment(any(PaymentDto.class))).thenReturn(mockPayment);
        when(mapper.convertValue(eq(orderDto), eq(Order.class))).thenReturn(mockOrder);
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId("111122223333"); // Snowflake ID mock
            return order;
        });

        String cacheKey = ORDER_KEY_PREFIX + "111122223333";
        doNothing().when(valueOperations).set(eq(cacheKey), any(Order.class), eq(Duration.ofHours(1)));
        doNothing().when(localCache).put(eq(cacheKey), any(Order.class));

        // When
        Order createdOrder = orderService.createOrder(orderDto);

        // Then
        assertNotNull(createdOrder);
        assertEquals("P001", createdOrder.getProductId());
        assertEquals(2L, createdOrder.getQuantity());
        assertEquals(20000L, createdOrder.getAmount());
        assertEquals("111122223333", createdOrder.getOrderId());

        // Verification
        verify(productService, times(1)).getProduct("P001");
        verify(stockService, times(1)).getStock("P001");
        verify(paymentService, times(1)).createPayment(any(PaymentDto.class));
        verify(stockService, times(1)).decreaseStock("P001", 2L);
        verify(orderRepository, times(1)).saveAndFlush(any(Order.class));
    }

    @Test
    void readOrderTest() {
        // given
        String orderId = "ORD123";
        Order mockOrder = Order.builder()
                .id(1L)
                .orderId(orderId)
                .storeId("S001")
                .productId("P001")
                .stockId("ST001")
                .quantity(3L)
                .build();

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(mockOrder));

        // when
        Order result = orderService.readOrder(orderId);

        // then
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals("P001", result.getProductId());

        verify(orderRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    void updateOrderTest() throws JsonMappingException {
        // given
        String orderId = "ORD1001";
        Order existingOrder = Order.builder()
                .id(1L)
                .orderId(orderId)
                .storeId("S001")
                .productId("P001")
                .stockId("ST001")
                .quantity(2L)
                .build();

        OrderDto updateDto = OrderDto.builder()
                .quantity(5L)
                .build();

        Order updatedOrder = Order.builder()
                .id(1L)
                .orderId(orderId)
                .storeId("S001")
                .productId("P001")
                .stockId("ST001")
                .quantity(5L)
                .build();

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingOrder));
        doNothing().when(mapper).updateValue(existingOrder, updateDto);
        when(orderRepository.save(existingOrder)).thenReturn(updatedOrder);

        // when
        Order result = orderService.updateOrder(orderId, updateDto);

        // then
        assertNotNull(result);
        assertEquals(5L, result.getQuantity());
        verify(orderRepository).findByOrderId(orderId);
        verify(mapper).updateValue(existingOrder, updateDto);
        verify(orderRepository).save(existingOrder);
    }

    @Test
    void updateOrder_NoOrder() {
        // given
        String orderId = "NOT_FOUND";
        OrderDto dto = OrderDto.builder().quantity(3L).build();

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // when & then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                orderService.updateOrder(orderId, dto));

        assertTrue(exception.getMessage().contains("Order not found"));
        verify(orderRepository).findByOrderId(orderId);
    }

    @Test
    void deleteOrderTest() {
        // given
        String orderId = "ORD2001";
        Order existingOrder = Order.builder()
                .id(1L)
                .orderId(orderId)
                .build();

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingOrder));
        doNothing().when(orderRepository).delete(existingOrder);

        // when
        orderService.deleteOrder(orderId);

        // then
        verify(orderRepository).findByOrderId(orderId);
        verify(orderRepository).delete(existingOrder);
    }

    @Test
    void deleteOrder_NoOrder() {
        // given
        String orderId = "NON_EXISTENT";

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                orderService.deleteOrder(orderId));

        assertTrue(exception.getMessage().contains("Order not found"));
        verify(orderRepository).findByOrderId(orderId);
    }

}