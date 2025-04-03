package SoloTech.StockFlow.store.service;

import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.order.service.OrderService;
import SoloTech.StockFlow.store.dto.StoreDto;
import SoloTech.StockFlow.store.entity.Store;
import SoloTech.StockFlow.store.repository.StoreRepository;
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

/**
 * 상점 서비스
 *
 * @since   2025-03-25
 * @author  yhkim
 */

@Service
@RequiredArgsConstructor
public class StoreService {

    private static final Logger log = LoggerFactory.getLogger(StoreService.class);

    final StoreRepository storeRepository;

    final ObjectMapper mapper;

    private static final String STORE_KEY_PREFIX = "store:";

    // 로컬 캐시 (Caffeine)
    private final Cache<String, Object> localCache;
    private final RedisTemplate<String, Object> redisTemplate;

    // 메시지 발행 (Pub/Sub) 컴포넌트
    private final CachePublisher cachePublisher;


    @Transactional
    public Store createStore(StoreDto dto) {
        Store store = mapper.convertValue(dto, Store.class);
        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

        store.setStoreId(String.valueOf(snowflakeId));
        Store savedStore = storeRepository.saveAndFlush(store);

        String cacheKey = STORE_KEY_PREFIX + savedStore.getStoreId();
        redisTemplate.opsForValue().set(cacheKey, savedStore);

        // 로컬 캐시 저장
        localCache.put(cacheKey, savedStore);

        log.info("Created store: {}", cacheKey);
        return savedStore;
    }

    public Store getStore(String storeId) {
        // 1) 로컬 캐시 확인
        String cacheKey = STORE_KEY_PREFIX + storeId;

        // 1) 로컬 캐시 확인
        Store cachedStore = (Store) localCache.getIfPresent(cacheKey);
        if (cachedStore != null) {
            return cachedStore;
        }

        // 2) Redis 확인
        cachedStore = (Store) redisTemplate.opsForValue().get(cacheKey);
        if (cachedStore != null) {
            localCache.put(cacheKey, cachedStore);
            return cachedStore;
        }

        // 3) DB 조회
        Store dbStore = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));

        // 캐시에 저장
        redisTemplate.opsForValue().set(cacheKey, dbStore);
        localCache.put(cacheKey, dbStore);

        return dbStore;    }

    @Transactional
    public Store updateStore(String storeId, StoreDto dto) throws JsonMappingException {
        Store store = this.getStore(storeId);
        mapper.updateValue(store,dto);
        Store savedStore = storeRepository.save(store);
        String cacheKey = STORE_KEY_PREFIX + savedStore.getStoreId();

        // Redis, 로컬 캐시에 갱신
        redisTemplate.opsForValue().set(cacheKey, savedStore);
        localCache.put(cacheKey, savedStore);

        // 다른 서버 인스턴스 캐시 무효화를 위해 메시지 발행
        // 메시지 형식: "Updated store-store:xxxx" 로 가정
        String message = "Updated store-" + cacheKey;
        cachePublisher.publish("cache-sync", message);

        log.info("Updated store: {}, published message: {}", cacheKey, message);

        return savedStore;
    }

    public void deleteStore(String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(()->new RuntimeException("Store not found: " + storeId));
        storeRepository.delete(store);

        // 캐시 무효화 대상 key
        String cacheKey = STORE_KEY_PREFIX + storeId;

        // 현재 서버(로컬 캐시 + Redis)에서도 삭제
        localCache.invalidate(cacheKey);
        redisTemplate.delete(cacheKey);
        // 다른 서버들도 캐시를 무효화하도록 메시지 발행
        // 메시지 형식: "Deleted store-store:xxxx" 로 가정
        String message = "Deleted store-" + cacheKey;
        cachePublisher.publish("cache-sync", message);

        log.info("Deleted store: {}, published message: {}", cacheKey, message);
    }
}
