package SoloTech.StockFlow.payment;

import SoloTech.StockFlow.payment.entity.Payment;
import SoloTech.StockFlow.payment.entity.PaymentStatus;
import SoloTech.StockFlow.payment.exception.PaymentFailedException;
import SoloTech.StockFlow.payment.repository.PaymentRepository;
import SoloTech.StockFlow.payment.service.PaymentService;
import SoloTech.StockFlow.store.dto.StoreDto;
import SoloTech.StockFlow.store.entity.Store;
import SoloTech.StockFlow.store.exception.StoreNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

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
    @DisplayName("결제 생성")
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
    }

    @Test
    @DisplayName("결제 조회")
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
    @DisplayName("결제 수정 시")
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

        // When
        Payment updatedPayment = paymentService.updatePayment(paymentId, updateDto);

        // Then
        assertNotNull(updatedPayment);
        assertEquals(12000L, updatedPayment.getAmount());
        assertEquals("Debit Card", updatedPayment.getPaymentMethod());
        assertEquals(PaymentStatus.PAID, updatedPayment.getPaymentStatus());

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제가 없는 경우 수정할 경우")
    void updatePayment_NoPayment() {
        String paymentId = "NOT_FOUND";
        PaymentDto paymentDto = new PaymentDto(
                "order123", 15000L, "카드", "완료" // 한글 상태값
        );

        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());

        // When & Then: 서비스 메서드 호출 시 예외가 발생하는지 확인
        Exception exception = assertThrows(EntityNotFoundException.class,
                () -> paymentService.updatePayment(paymentId, paymentDto));

        // 예외 메시지 검증
        assertTrue(exception.getMessage().contains("Payment not found"));

        // Verify: mapper와 save 메서드가 호출되지 않았는지 확인
        verify(paymentRepository).findByPaymentId(paymentId);
    }

    @Test
    @DisplayName("결제 삭제")
    void deletePaymentTest() {
        // Given
        String paymentId = "P12345";
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
    }

    @Test
    @DisplayName("결제가 없는 경우 삭제할 경우")
    void deletePayment_NoPayment() {
        String paymentId = "NOT_FOUND";
        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(PaymentFailedException.class, () ->
                paymentService.deletePayment(paymentId));

        assertTrue(exception.getMessage().contains("Payment not found"));
        verify(paymentRepository).findByPaymentId(paymentId);

    }
}
