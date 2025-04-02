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

        // 로컬 캐시 저장
        localCache.put(cacheKey, savedStore);

        log.info("Created store: {}", cacheKey);
        return savedStore;
    }

    public Store getStore(String storeId) {
        // 1) 로컬 캐시 확인
        Store cachedStore = (Store) localCache.getIfPresent(STORE_KEY_PREFIX);

        if(cachedStore != null){
            log.info("[LocalCache] Hit for key={}", STORE_KEY_PREFIX);
            return cachedStore;
        }
        Store dbStore = storeRepository.findByStoreId(storeId)
                .orElseThrow(()->new RuntimeException("Store not found: " + storeId));

        // 캐시에 저장
        localCache.put(STORE_KEY_PREFIX, dbStore);

        return dbStore;
    }

    @Transactional
    public Store updateStore(String storeId, StoreDto dto) throws JsonMappingException {
        Store store = this.getStore(storeId);
        mapper.updateValue(store,dto);
        Store savedStore = storeRepository.save(store);
        String cacheKey = STORE_KEY_PREFIX + savedStore.getStoreId();

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

        // 다른 서버들도 캐시를 무효화하도록 메시지 발행
        // 메시지 형식: "Deleted store-store:xxxx" 로 가정
        String message = "Deleted store-" + cacheKey;
        cachePublisher.publish("cache-sync", message);

        log.info("Deleted store: {}, published message: {}", cacheKey, message);
    }
}
