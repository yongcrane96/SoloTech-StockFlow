package SoloTech.StockFlow.payment;

import SoloTech.StockFlow.payment.entity.Payment;
import SoloTech.StockFlow.payment.entity.PaymentStatus;
import SoloTech.StockFlow.payment.repository.PaymentRepository;
import SoloTech.StockFlow.payment.service.PaymentService;
import SoloTech.StockFlow.store.dto.StoreDto;
import SoloTech.StockFlow.store.entity.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.payment.dto.PaymentDto;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

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

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private static final String PAYMENT_KEY_PREFIX = "payment:";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doNothing().when(valueOperations).set(anyString(), any());
        doNothing().when(cachePublisher).publish(anyString(), anyString());
        doNothing().when(valueOperations).set(anyString(), any(), any());
    }


    @Test
    void getPaymentTest() {
        // Payment 객체 준비
        Payment mockPayment = new Payment();
        mockPayment.setPaymentId("P12345");
        mockPayment.setOrderId("O12345");
        mockPayment.setAmount(10000L);
        mockPayment.setPaymentMethod("Credit Card");
        mockPayment.setPaymentStatus(PaymentStatus.Success);

        // PaymentRepository의 findByPaymentId 메서드가 mockPayment 반환하도록 설정
        Mockito.when(paymentRepository.findByPaymentId(any(String.class)))
                .thenReturn(java.util.Optional.of(mockPayment));

        // 테스트 실행
        Payment result = paymentService.readPayment("P12345");

        // 검증
        assertNotNull(result);
        assertEquals("P12345", result.getPaymentId());
        assertEquals("O12345", result.getOrderId());
        assertEquals(10000L, result.getAmount());
        assertEquals("Credit Card", result.getPaymentMethod());
        assertEquals(PaymentStatus.Success, result.getPaymentStatus());

    }

    @Test
    void createPaymentTest() {
        // Given
        PaymentDto paymentDto = new PaymentDto(
                "order123", 15000L, "카드", "완료" // 한글 상태값
        );

        Payment mappedPayment = Payment.builder()
                .orderId(paymentDto.getOrderId())
                .amount(paymentDto.getAmount())
                .paymentMethod(paymentDto.getPaymentMethod())
                // 테스트에서는 하드코딩으로 enum 설정 (한글 매핑 로직은 서비스 로직에 있다고 가정)
                .paymentStatus(PaymentStatus.Success)
                .build();

        long fakeSnowflakeId = 123456789L;
        String expectedPaymentId = String.valueOf(fakeSnowflakeId);
        String cacheKey = PAYMENT_KEY_PREFIX + expectedPaymentId;

        Payment savedPayment = Payment.builder()
                .id(1L)
                .paymentId(expectedPaymentId)
                .orderId("order123")
                .amount(15000L)
                .paymentMethod("카드")
                .paymentStatus(PaymentStatus.Success)
                .build();

        when(mapper.convertValue(eq(paymentDto), eq(Payment.class))).thenReturn(mappedPayment);
        when(snowflake.nextId()).thenReturn(fakeSnowflakeId);
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);

        // When
        Payment result = paymentService.createPayment(paymentDto);

        // Then
        assertNotNull(result);
        assertEquals(expectedPaymentId, result.getPaymentId());
        assertEquals(paymentDto.getOrderId(), result.getOrderId());
        assertEquals(paymentDto.getAmount(), result.getAmount());
        assertEquals(paymentDto.getPaymentMethod(), result.getPaymentMethod());
        assertEquals(PaymentStatus.Success, result.getPaymentStatus());

        verify(mapper).convertValue(eq(paymentDto), eq(Payment.class));
        verify(snowflake).nextId();
        verify(paymentRepository).saveAndFlush(any(Payment.class));
        verify(redisTemplate.opsForValue(), times(1)).set(eq(cacheKey), eq(savedPayment), eq(Duration.ofHours(1)));
        verify(localCache, times(1)).put(eq(cacheKey), eq(savedPayment));
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
                .paymentStatus(PaymentStatus.PAID)
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
    void updatePaymentTest() throws Exception {
        // Given
        String paymentId = "P12345";
        Payment mockPayment = Payment.builder()
                .id(1L)
                .paymentId(paymentId)
                .orderId("O12345")
                .amount(10000L)
                .paymentMethod("Credit Card")
                .paymentStatus(PaymentStatus.Success)
                .build();

        PaymentDto updateDto = new PaymentDto("O12345", 12000L, "Debit Card", "PAID");

        String cacheKey = PAYMENT_KEY_PREFIX + paymentId;
        String expectedMessage = "Updated payment-" + cacheKey;

        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            PaymentDto dto = invocation.getArgument(1);
            payment.setAmount(dto.getAmount());
            payment.setPaymentMethod(dto.getPaymentMethod());
            payment.setPaymentStatus(PaymentStatus.valueOf(dto.getPaymentStatus()));
            return null;
        }).when(mapper).updateValue(any(Payment.class), any(PaymentDto.class));

        // 캐시 저장 Mock
        doNothing().when(valueOperations).set(eq(cacheKey), any(Payment.class), eq(Duration.ofHours(1)));
        doNothing().when(cachePublisher).publish(eq("cache-sync"), eq(expectedMessage));

        // When
        Payment updatedPayment = paymentService.updatePayment(paymentId, updateDto);

        // Then
        assertNotNull(updatedPayment);
        assertEquals(12000L, updatedPayment.getAmount());
        assertEquals("Debit Card", updatedPayment.getPaymentMethod());
        assertEquals(PaymentStatus.PAID, updatedPayment.getPaymentStatus());

        verify(valueOperations, times(1)).set(eq(cacheKey), eq(updatedPayment), eq(Duration.ofHours(1)));
        verify(cachePublisher, times(1)).publish(eq("cache-sync"), eq(expectedMessage));
        verify(paymentRepository, times(1)).save(any(Payment.class));
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
                .paymentStatus(PaymentStatus.PAID)
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
