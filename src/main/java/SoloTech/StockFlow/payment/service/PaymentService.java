package SoloTech.StockFlow.payment.service;

import SoloTech.StockFlow.payment.dto.PaymentDto;
import SoloTech.StockFlow.payment.entity.Payment;
import SoloTech.StockFlow.payment.repository.PaymentRepository;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 결제 서비스
 *
 * @since   2025-04-02
 * @author  yhkim
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ObjectMapper mapper;

    @Transactional
    public Payment createPayment(PaymentDto dto) {
        Payment payment = mapper.convertValue(dto, Payment.class);

        // Snowflake ID 생성
        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

        payment.setPaymentId(String.valueOf(snowflakeId));
        return paymentRepository.saveAndFlush(payment);
    }

    public Payment readPayment(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    @Transactional
    public Payment updatePayment(String paymentId, PaymentDto dto) throws JsonMappingException {
        Payment payment = this.readPayment(paymentId);
        mapper.updateValue(payment, dto);
        return paymentRepository.save(payment);
    }

    public void deletePayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        paymentRepository.delete(payment);
    }
}
