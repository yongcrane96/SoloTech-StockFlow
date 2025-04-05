package SoloTech.StockFlow.payment;

import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.order.dto.OrderDto;
import SoloTech.StockFlow.order.entity.Order;
import SoloTech.StockFlow.payment.dto.PaymentDto;
import SoloTech.StockFlow.payment.entity.Payment;
import SoloTech.StockFlow.payment.repository.PaymentRepository;
import SoloTech.StockFlow.payment.service.PaymentService;
import SoloTech.StockFlow.product.entity.Product;
import cn.hutool.core.lang.Snowflake;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private Cache<String, Object> localCache;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private CachePublisher cachePublisher;

    @Mock
    private Snowflake snowflake;

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private static final String PAYMENT_KEY_PREFIX = "payment:";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), any());
        doNothing().when(cachePublisher).publish(anyString(), anyString());
    }

    @Test
    void createPaymentTest() {
        // Given
        PaymentDto dto = new PaymentDto("O12345", 10000L, "Credit Card", "Success");

        Payment mockPayment = Payment.builder()
                .id(1L)
                .paymentId("12345") // ✅ Mock된 Snowflake ID 적용
                .orderId("O12345")
                .amount(10000L)
                .paymentMethod("Credit Card")
                .paymentStatus("Success")
                .build();
        when(mapper.convertValue(dto, Payment.class)).thenReturn(mockPayment);
        when(snowflake.nextId()).thenReturn(12345L);
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(mockPayment);

        String cacheKey = PAYMENT_KEY_PREFIX + mockPayment.getOrderId();

        // When
        Payment result = paymentService.createPayment(dto);

        // Then
        assertNotNull(result);
        assertEquals(mockPayment.getPaymentId(), result.getPaymentId());
        assertEquals(mockPayment.getOrderId(), result.getOrderId());
        assertEquals(mockPayment.getAmount(), result.getAmount());
        assertEquals(mockPayment.getPaymentMethod(), result.getPaymentMethod());
        assertEquals(mockPayment.getPaymentStatus(), result.getPaymentStatus());

        // Verify interactions
        verify(paymentRepository, times(1)).saveAndFlush(any(Payment.class));
        verify(redisTemplate.opsForValue(), times(1)).set(eq(cacheKey), eq(mockPayment), eq(Duration.ofHours(1)));
        verify(localCache, times(1)).put(eq(cacheKey), eq(mockPayment));
        verify(cachePublisher, times(1)).publish(eq("payment_update"), eq(cacheKey));
    }

    @Test
    void readPaymentTest() {
        // Given
        String paymentId = "P12345";
        String cacheKey = PAYMENT_KEY_PREFIX + paymentId;
        Payment mockPayment = Payment.builder()
                .id(1L)
                .paymentId(paymentId)
                .orderId("O12345")
                .amount(10000L)
                .paymentMethod("Credit Card")
                .paymentStatus("Success")
                .build();

        when(localCache.getIfPresent(cacheKey)).thenReturn(null);
        when(redisTemplate.opsForValue().get(cacheKey)).thenReturn(null);
        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(mockPayment));

        // When
        Payment result = paymentService.readPayment(paymentId);

        // Then
        assertNotNull(result);
        assertEquals(paymentId, result.getPaymentId());

        verify(localCache, times(1)).getIfPresent(cacheKey);
        verify(redisTemplate.opsForValue(), times(1)).get(cacheKey);
        verify(paymentRepository, times(1)).findByPaymentId(paymentId);
        verify(redisTemplate.opsForValue(), times(1)).set(eq(cacheKey), eq(mockPayment));
        verify(localCache, times(1)).put(eq(cacheKey), eq(mockPayment));
    }

    @Test
    void updatePaymentTest() throws JsonMappingException {
        // Given
        String paymentId = "P12345";
        Payment mockPayment = Payment.builder()
                .id(1L)
                .paymentId(paymentId)
                .orderId("O12345")
                .amount(10000L)
                .paymentMethod("Credit Card")
                .paymentStatus("Success")
                .build();
        PaymentDto updateDto = new PaymentDto("O12345", 12000L, "Debit Card", "Completed");

        String cacheKey = PAYMENT_KEY_PREFIX + paymentId;
        String expectedMessage = "Updated payment-" + cacheKey;

        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);


        // 캐시 저장 Mock
        doNothing().when(valueOperations).set(eq(cacheKey), any(Payment.class));
        doNothing().when(cachePublisher).publish(eq("cache-sync"), eq(expectedMessage));


        // When
        Payment updatedPayment = paymentService.updatePayment(paymentId, updateDto);

        // Then
        assertNotNull(updatedPayment);
        assertEquals("O12345", updatedPayment.getOrderId());
        assertEquals(12000L, updatedPayment.getAmount());
        assertEquals("Debit Card", updatedPayment.getPaymentMethod());

        verify(redisTemplate.opsForValue(), times(1)).set(eq(cacheKey), eq(updatedPayment));
        verify(localCache, times(1)).put(eq(cacheKey), eq(updatedPayment));
        verify(cachePublisher, times(1)).publish(eq("cache-sync"), contains("Updated payment-payment:"));
    }

    @Test
    void deletePaymentTest() {
        // Given
        String paymentId = "P12345";
        String cacheKey = PAYMENT_KEY_PREFIX + paymentId;
        Payment mockPayment = Payment.builder()
                .id(1L)
                .paymentId(paymentId)
                .orderId("O12345")
                .amount(10000L)
                .paymentMethod("Credit Card")
                .paymentStatus("Success")
                .build();

        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(mockPayment));

        // When
        paymentService.deletePayment(paymentId);

        // Then
        verify(paymentRepository, times(1)).delete(mockPayment);
        verify(localCache, times(1)).invalidate(cacheKey);
        verify(redisTemplate, times(1)).delete(cacheKey);
        verify(cachePublisher, times(1)).publish(eq("cache-sync"), contains("Deleted payment-payment:"));
    }
}
