package com.example.payment;

import cn.hutool.core.lang.Snowflake;
import com.example.kafka.CreatePaymentEvent;
import com.example.kafka.Status;
import com.example.kafka.UpdatePaymentEvent;
import com.example.payment.entity.Payment;
import com.example.payment.entity.PaymentStatus;
import com.example.payment.exception.PaymentFailedException;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class paymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment defaultPayment;
    private CreatePaymentEvent defaultCreateEvent;
    private UpdatePaymentEvent defaultUpdateEvent;
    private String paymentId = "P001";

    @BeforeEach
    void setUp(){
        Snowflake snowflake = new Snowflake(1, 1);
        long snowflakeId = snowflake.nextId();

        defaultPayment = Payment.builder()
                .id(snowflakeId)
                .paymentId(paymentId)
                .orderId("O12345")
                .amount(10000L)
                .paymentMethod("Credit Card")
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        defaultCreateEvent = new CreatePaymentEvent(
                snowflakeId,
                paymentId,
                "O001",
                1L,
                "CARD",
                Status.SUCCESS
        );
        defaultUpdateEvent = new UpdatePaymentEvent(
                paymentId,
                1L,
                "CARD",
                Status.PAID
        );
    }

    @Test
    @DisplayName("결제 생성")
    void createPaymentTest() {
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(defaultPayment);
        Payment result = paymentService.createPayment(defaultCreateEvent);
        assertNotNull(result);
        assertEquals(defaultPayment.getPaymentId(), result.getPaymentId());
        assertEquals(defaultPayment.getOrderId(), result.getOrderId());
        assertEquals(defaultPayment.getAmount(), result.getAmount());
        assertEquals(defaultPayment.getPaymentMethod(), result.getPaymentMethod());
        assertEquals(PaymentStatus.SUCCESS, result.getPaymentStatus());
        verify(paymentRepository, times(1)).saveAndFlush(any(Payment.class));
    }

    @Test
    @DisplayName("결제 조회")
    void getPaymentTest() {
        when(paymentRepository.findByPaymentId(paymentId))
                .thenReturn(Optional.of(defaultPayment));
        // 테스트 실행
        Payment result = paymentService.readPayment(paymentId);
        // 검증
        assertNotNull(result);
        assertEquals(defaultPayment.getPaymentId(), result.getPaymentId());
        assertEquals(defaultPayment.getOrderId(), result.getOrderId());
        assertEquals(defaultPayment.getAmount(), result.getAmount());
        assertEquals(defaultPayment.getPaymentMethod(), result.getPaymentMethod());
        assertEquals(PaymentStatus.SUCCESS, result.getPaymentStatus());
        verify(paymentRepository, times(1)).findByPaymentId(paymentId);
    }

    @Test
    @DisplayName("결제 수정 시")
    void updatePaymentTest() {
        when(paymentRepository.findByPaymentId(defaultUpdateEvent.getPaymentId())).thenReturn(Optional.of(defaultPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Payment updatedPayment = paymentService.updatePayment(defaultUpdateEvent);

        // Then
        assertNotNull(updatedPayment);
        assertEquals(1L, updatedPayment.getAmount());
        assertEquals("CARD", updatedPayment.getPaymentMethod());
        assertEquals(Status.PAID, updatedPayment.getPaymentStatus());

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("결제가 없는 경우 수정할 경우")
    void updatePayment_NoPayment() {
        String paymentId = "NOT_FOUND";
        defaultUpdateEvent = new UpdatePaymentEvent(
                paymentId,
                1L,
                "CARD",
                Status.PAID
        );

        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());

        Exception exception = assertThrows(EntityNotFoundException.class,
                () -> paymentService.updatePayment(defaultUpdateEvent));

        assertTrue(exception.getMessage().contains("Payment not found"));
        verify(paymentRepository).findByPaymentId(paymentId);
    }


    @Test
    @DisplayName("결제 삭제")
    void deletePaymentTest() {
        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(defaultPayment));
        paymentService.deletePayment(paymentId);
        verify(paymentRepository, times(1)).delete(defaultPayment);
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
