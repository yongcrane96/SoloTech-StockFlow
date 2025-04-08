package SoloTech.StockFlow.payment.service;

import SoloTech.StockFlow.common.annotations.Cached;
import SoloTech.StockFlow.common.cache.CacheType;
import SoloTech.StockFlow.payment.dto.PaymentDto;
import SoloTech.StockFlow.payment.entity.Payment;
import SoloTech.StockFlow.payment.entity.PaymentStatus;
import SoloTech.StockFlow.payment.repository.PaymentRepository;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final Snowflake snowflake;

    @Cached(prefix = "payment:", key = "#result.paymentId", ttl = 3600, type = CacheType.WRITE)
    @Transactional
    public Payment createPayment(PaymentDto dto) {
        Payment payment = mapper.convertValue(dto, Payment.class);

        // Snowflake ID 생성
        long snowflakeId = snowflake.nextId();
        payment.setPaymentId(String.valueOf(snowflakeId));

        Payment savedPayment = paymentRepository.saveAndFlush(payment);
        return savedPayment;
    }

    @Cached(prefix = "payment:", key = "#paymentId", ttl = 3600, type = CacheType.READ)
    public Payment readPayment(String paymentId) {
        Payment dbPayment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        return dbPayment;
    }

    @Cached(prefix = "payment:", key = "#result.paymentId", ttl = 3600, type = CacheType.WRITE)
    @Transactional
    public Payment updatePayment(String paymentId, PaymentDto dto) throws JsonMappingException {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));

        mapper.updateValue(payment, dto);

        Payment savedPayment = paymentRepository.save(payment);
        return savedPayment;
    }

    @Cached(prefix = "payment:", key = "#paymentId", ttl = 3600, type = CacheType.DELETE)
    public void deletePayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        paymentRepository.delete(payment);
    }
}
