package SoloTech.StockFlow.payment;

import SoloTech.StockFlow.payment.entity.Payment;
import SoloTech.StockFlow.payment.repository.PaymentRepository;
import SoloTech.StockFlow.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this); // Mock 객체 초기화
    }

    @Test
    void getPaymentTest() {
        // Payment 객체 준비
        Payment mockPayment = new Payment();
        mockPayment.setPaymentId("P12345");
        mockPayment.setOrderId("O12345");
        mockPayment.setAmount(10000L);
        mockPayment.setPaymentMethod("Credit Card");
        mockPayment.setPaymentStatus("Success");

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
        assertEquals("Success", result.getPaymentStatus());
    }

}
