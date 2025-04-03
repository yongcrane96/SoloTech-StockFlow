package SoloTech.StockFlow.order.service;

import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.order.dto.OrderDto;
import SoloTech.StockFlow.order.entity.Order;
import SoloTech.StockFlow.order.repository.OrderRepository;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 주문 서비스
 *
 * @since   2025-03-18
 * @author  yhkim
 */

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;

    private final ObjectMapper mapper;

    private static final String ORDER_KEY_PREFIX = "order:";

    private final RedisTemplate redisTemplate;
    // 로컬 캐시 (Caffeine)
    private final Cache<String, Object> localCache;

    // 메시지 발행 (Pub/Sub) 컴포넌트
    private final CachePublisher cachePublisher;

    @Transactional
    public Order createOrder(OrderDto dto) {
        Order order = mapper.convertValue(dto, Order.class);

        // Snowflake ID 생성
        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

        order.setOrderId(String.valueOf(snowflakeId));
        Order savedOrder = orderRepository.saveAndFlush(order);

        //캐시 키
        String cacheKey = ORDER_KEY_PREFIX + savedOrder.getOrderId();

        // Redis 캐시 저장
        redisTemplate.opsForValue().set(cacheKey, savedOrder, Duration.ofHours(1));

        // 로컬 캐시 저장
        localCache.put(cacheKey, savedOrder);

        log.info("Created order: {}", cacheKey);
        return savedOrder;
    }

    public Order readOrder(String orderId) {
        String cacheKey = ORDER_KEY_PREFIX + orderId;

        // 1) 로컬 캐시 확인
        Order cachedOrder = (Order) localCache.getIfPresent(cacheKey);
        if(cachedOrder != null){
            log.info("[LocalCache] Hit for key={}", cacheKey);
            return cachedOrder;
        }

        // 2) Redis 캐시 확인
        cachedOrder = (Order) redisTemplate.opsForValue().get(cacheKey);
        if(cachedOrder != null){
            log.info("[RedisCache] Hit for key={}", cacheKey);
            localCache.put(cacheKey, cachedOrder);
            return cachedOrder;
        }

        // DB 조회
         Order dbOrder = orderRepository.findByOrderId(orderId)
                .orElseThrow(()->new RuntimeException("Order not found: " + orderId));

        // 캐시에 저장
        redisTemplate.opsForValue().set(cacheKey, dbOrder);
        localCache.put(cacheKey, dbOrder);
        return dbOrder;
    }

    @Transactional
    public Order updateOrder(String orderId, OrderDto dto) throws JsonMappingException {
        Order order = this.readOrder(orderId);
        mapper.updateValue(order, dto);
        Order savedOrder =  orderRepository.save(order);
        String cacheKey = ORDER_KEY_PREFIX + savedOrder.getOrderId();

        localCache.put(cacheKey, savedOrder);

        // 다른 서버 인스턴스 캐시 무효화를 위해 메시지 발행
        // 메시지 형식: "Updated order-order:xxxx" 로 가정
        String message = "Updated order-" + cacheKey;
        cachePublisher.publish("cache-sync", message);

        log.info("Updated order: {}, published message: {}", cacheKey, message);

        return savedOrder;
    }

    public void deleteOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(()-> new RuntimeException("Order not found: " + orderId));
        orderRepository.delete(order);

        // 캐시 무효화 대상 key
        String cacheKey = ORDER_KEY_PREFIX + orderId;

        // 현재 서버(로컬 캐시 + Redis)에서도 삭제
        localCache.invalidate(cacheKey);
        redisTemplate.delete(cacheKey);

        // 다른 서버들도 캐시를 무효화하도록 메시지 발행
        // 메시지 형식: "Deleted order-order:xxxx" 로 가정
        String message = "Deleted order-" + cacheKey;
        cachePublisher.publish("cache-sync", message);

        log.info("Deleted order: {}, published message: {}", cacheKey, message);
    }
}