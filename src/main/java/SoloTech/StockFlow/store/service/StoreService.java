package SoloTech.StockFlow.store.service;

import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.common.annotations.Cached;
import SoloTech.StockFlow.common.cache.CacheType;
import SoloTech.StockFlow.order.entity.Order;
import SoloTech.StockFlow.order.service.OrderService;
import SoloTech.StockFlow.store.dto.StoreDto;
import SoloTech.StockFlow.store.entity.Store;
import SoloTech.StockFlow.store.repository.StoreRepository;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Cached(prefix = "store:", key = "#result.storeId", ttl = 3600, type = CacheType.WRITE)
    @Transactional
    public Store createStore(StoreDto dto) {
        Store store = mapper.convertValue(dto, Store.class);
        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

        store.setStoreId(String.valueOf(snowflakeId));
        Store savedStore = storeRepository.saveAndFlush(store);

        return savedStore;
    }

    @Cached(prefix = "store:", key = "#storeId", ttl = 3600, type = CacheType.READ)
    public Store getStore(String storeId) {
        Store dbStore = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));

        return dbStore;
    }

    @Cached(prefix = "store:", key = "#result.storeId", ttl = 3600, type = CacheType.WRITE)
    @Transactional
    public Store updateStore(String storeId, StoreDto dto) throws JsonMappingException {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(()-> new EntityNotFoundException("Store not found: " + storeId));

        mapper.updateValue(store,dto);
        Store savedStore = storeRepository.save(store);

        return savedStore;
    }

    @Cached(prefix = "store:", key = "#storeId", ttl = 3600, type = CacheType.DELETE)
    public void deleteStore(String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(()->new RuntimeException("Store not found: " + storeId));
        storeRepository.delete(store);
}
}
