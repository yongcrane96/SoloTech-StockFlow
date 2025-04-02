package SoloTech.StockFlow.payment.service;

import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.order.entity.Order;
import SoloTech.StockFlow.order.service.OrderService;
import SoloTech.StockFlow.payment.dto.PaymentDto;
import SoloTech.StockFlow.payment.entity.Payment;
import SoloTech.StockFlow.payment.repository.PaymentRepository;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final ObjectMapper mapper;

    private static final String PAYMENT_KEY_PREFIX = "payment:";

    // 로컬 캐시 (Caffeine)
    private final Cache<String, Object> localCache;

    // 메시지 발행 (Pub/Sub) 컴포넌트
    private final CachePublisher cachePublisher;

    @Transactional
    public Payment createPayment(PaymentDto dto) {
        Payment payment = mapper.convertValue(dto, Payment.class);

        // Snowflake ID 생성
        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

        payment.setPaymentId(String.valueOf(snowflakeId));
        Payment savedPayment = paymentRepository.saveAndFlush(payment);
        String cacheKey = PAYMENT_KEY_PREFIX + savedPayment.getOrderId();

        // 로컬 캐시 저장
        localCache.put(cacheKey, savedPayment);

        log.info("Created payment: {}", cacheKey);
        return savedPayment;
    }

    public Payment readPayment(String paymentId) {

        Payment cachedPayment = (Payment) localCache.getIfPresent(PAYMENT_KEY_PREFIX);

        if(cachedPayment != null){
            log.info("[LocalCache] Hit for key={}", PAYMENT_KEY_PREFIX);
            return cachedPayment;
        }


        Payment dbPayment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        // 캐시에 저장
        localCache.put(PAYMENT_KEY_PREFIX, dbPayment);

        return dbPayment;
    }

    @Transactional
    public Payment updatePayment(String paymentId, PaymentDto dto) throws JsonMappingException {
        Payment payment = this.readPayment(paymentId);
        mapper.updateValue(payment, dto);
        Payment savedPayment = paymentRepository.save(payment);
        String cacheKey = PAYMENT_KEY_PREFIX + savedPayment.getPaymentId();

        localCache.put(cacheKey, savedPayment);

        // 다른 서버 인스턴스 캐시 무효화를 위해 메시지 발행
        // 메시지 형식: "Updated payment-payment:xxxx" 로 가정
        String message = "Updated payment-" + cacheKey;
        cachePublisher.publish("cache-sync", message);

        log.info("Updated payment: {}, published message: {}", cacheKey, message);

        return savedPayment;
    }

    public void deletePayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        paymentRepository.delete(payment);

        // 캐시 무효화 대상 key
        String cacheKey = PAYMENT_KEY_PREFIX + paymentId;

        // 현재 서버(로컬 캐시 + Redis)에서도 삭제
        localCache.invalidate(cacheKey);

        // 다른 서버들도 캐시를 무효화하도록 메시지 발행
        // 메시지 형식: "Deleted payment-payment:xxxx" 로 가정
        String message = "Deleted payment-" + cacheKey;
        cachePublisher.publish("cache-sync", message);

        log.info("Deleted payment: {}, published message: {}", cacheKey, message);
    }
}
