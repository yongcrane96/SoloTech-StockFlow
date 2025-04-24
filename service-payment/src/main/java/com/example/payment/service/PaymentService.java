package com.example.payment.service;

import cn.hutool.core.lang.Snowflake;
import com.example.annotations.Cached;
import com.example.cache.CacheType;
import com.example.kafka.CreatePaymentEvent;
import com.example.kafka.UpdatePaymentEvent;
import com.example.payment.dto.PaymentDto;
import com.example.payment.entity.Payment;
import com.example.payment.exception.PaymentFailedException;
import com.example.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 서비스
 *
 * @since   2025-04-02
 * @author  yhkim
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ObjectMapper mapper;
    private final Snowflake snowflake;

    @Cached(prefix = "payment:", key = "#result.paymentId", ttl = 3600, type = CacheType.WRITE, cacheNull = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment createPayment(CreatePaymentEvent event) {
        Payment payment = Payment.builder()
                .id(event.getId())
                .paymentId(event.getPaymentId())
                .orderId(event.getOrderId())
                .amount(event.getAmount())
                .paymentMethod(event.getPaymentMethod())
                .paymentStatus(event.getPaymentStatus())
                .build();
        try{
            Payment savedPayment = paymentRepository.saveAndFlush(payment);
            return savedPayment;
        }catch (Exception e){
            log.error("Payment 생성 또는 후속 작업 실패. storeId: {}", payment.getPaymentId(), e);

            // 사가 보상 로직 예시 (간단하게 delete 처리)
            try {
                paymentRepository.deleteById(payment.getId());
                log.info("Payment 보상 처리 완료 (삭제). paymentId: {}", payment.getPaymentId());
            } catch (Exception compensationEx) {
                log.error("Payment 보상 처리 실패. 수동 개입 필요. paymentId: {}", payment.getPaymentId(), compensationEx);
            }

            throw new PaymentFailedException(payment.getPaymentId());
        }
    }

    @Cached(prefix = "payment:", key = "#paymentId", ttl = 3600, type = CacheType.READ, cacheNull = true)
    public Payment readPayment(String paymentId) {
        Payment dbPayment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        return dbPayment;
    }

    @Cached(prefix = "payment:", key = "#result.paymentId", ttl = 3600, type = CacheType.WRITE, cacheNull = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment updatePayment(UpdatePaymentEvent event) {
        String paymentId = event.getPaymentId();
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));

        payment.setPaymentStatus(event.getPaymentStatus());
        payment.setPaymentMethod(event.getPaymentMethod());
        payment.setAmount(event.getAmount());

        Payment savedPayment = paymentRepository.save(payment);
        return savedPayment;
    }

    @Cached(prefix = "payment:", key = "#paymentId", ttl = 3600, type = CacheType.DELETE, cacheNull = true)
    public void deletePayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentFailedException("Payment not found: " + paymentId));
        paymentRepository.delete(payment);
    }
}
