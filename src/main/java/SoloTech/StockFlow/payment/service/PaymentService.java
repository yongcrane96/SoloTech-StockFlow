package SoloTech.StockFlow.payment.service;

import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.payment.dto.PaymentDto;
import SoloTech.StockFlow.payment.entity.Payment;
import SoloTech.StockFlow.payment.repository.PaymentRepository;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;


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
    private final RedisTemplate<String, Object> redisTemplate;
    // 메시지 발행 (Pub/Sub) 컴포넌트
    private final CachePublisher cachePublisher;

    @Transactional
    public Payment createPayment(PaymentDto dto) {
        Payment payment = mapper.convertValue(dto, Payment.class);

        // Snowflake ID 생성
        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

        payment.setPaymentId(String.valueOf(snowflakeId));

        payment.setPaymentStatus("SUCCESS");
        return paymentRepository.saveAndFlush(payment);
    }

    public Payment readPayment(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        String cacheKey = PAYMENT_KEY_PREFIX + savedPayment.getOrderId();

        redisTemplate.opsForValue().set(cacheKey, savedPayment, Duration.ofHours(1));

        // 로컬 캐시 저장
        localCache.put(cacheKey, savedPayment);
        cachePublisher.publish("payment_update", cacheKey);

        log.info("Created payment: {}", cacheKey);
        return savedPayment;
    }

    public Payment readPayment(String paymentId) {
        String cacheKey = PAYMENT_KEY_PREFIX + paymentId;

        Payment cachedPayment = (Payment) localCache.getIfPresent(cacheKey);
        if(cachedPayment != null){
            log.info("[LocalCache] Hit for key={}", cacheKey);
            return cachedPayment;
        }

        cachedPayment = (Payment) redisTemplate.opsForValue().get(cacheKey);
        if(cachedPayment != null){
            localCache.put(cacheKey, cachedPayment);
            return cachedPayment;
        }

        Payment dbPayment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        // 캐시에 저장
        redisTemplate.opsForValue().set(cacheKey,dbPayment);
        localCache.put(cacheKey, dbPayment);

        return dbPayment;
    }

    @Transactional
    public Payment updatePayment(String paymentId, PaymentDto dto) throws JsonMappingException {
        Payment payment = this.readPayment(paymentId);
        mapper.updateValue(payment, dto);
        return paymentRepository.save(payment);
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));

        mapper.updateValue(payment, dto);
        Payment savedPayment = paymentRepository.save(payment);
        // 캐시 키
        String cacheKey = PAYMENT_KEY_PREFIX + savedPayment.getPaymentId();

        redisTemplate.opsForValue().set(cacheKey, savedPayment);
        localCache.put(cacheKey, savedPayment);

        String message = "Updated payment-" + cacheKey;
        cachePublisher.publish("cache-sync", message);

        return savedPayment;
    }

    public void deletePayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
        paymentRepository.delete(payment);

        // 캐시 무효화 대상 key
        String cacheKey = PAYMENT_KEY_PREFIX + paymentId;

        // 현재 서버(로컬 캐시 + Redis)에서도 삭제
        redisTemplate.delete(cacheKey);
        localCache.invalidate(cacheKey);

        // 다른 서버들도 캐시를 무효화하도록 메시지 발행
        // 메시지 형식: "Deleted payment-payment:xxxx" 로 가정
        String message = "Deleted payment-" + cacheKey;
        cachePublisher.publish("cache-sync", message);

        log.info("Deleted payment: {}, published message: {}", cacheKey, message);
    }
}
