package com.example.store.service;

import cn.hutool.core.lang.Snowflake;
import com.example.annotations.Cached;
import com.example.cache.CacheType;
import com.example.store.dto.StoreDto;
import com.example.store.entity.Store;
import com.example.store.exception.StoreNotFoundException;
import com.example.store.repository.StoreRepository;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
    final StoreRepository storeRepository;

    final ObjectMapper mapper;

    @Cached(prefix = "store:", key = "#result.storeId", ttl = 3600, type = CacheType.WRITE, cacheNull = true)
    @Transactional
    public Store createStore(StoreDto dto) {
        Store store = mapper.convertValue(dto, Store.class);
        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

        store.setStoreId(String.valueOf(snowflakeId));
        Store savedStore = storeRepository.saveAndFlush(store);

        return savedStore;
    }

    @Cached(prefix = "store:", key = "#storeId", ttl = 3600, type = CacheType.READ, cacheNull = true)
    public Store getStore(String storeId) {
        Store dbStore = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new StoreNotFoundException("Store not found: " + storeId));

        return dbStore;
    }

    @Cached(prefix = "store:", key = "#result.storeId", ttl = 3600, type = CacheType.WRITE, cacheNull = true)
    @Transactional
    public Store updateStore(String storeId, StoreDto dto) throws JsonMappingException {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(()-> new EntityNotFoundException("Store not found: " + storeId));

        mapper.updateValue(store,dto);
        Store savedStore = storeRepository.save(store);

        return savedStore;
    }

    @Cached(prefix = "store:", key = "#storeId", ttl = 3600, type = CacheType.DELETE, cacheNull = true)
    public void deleteStore(String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(()->new StoreNotFoundException("Store not found: " + storeId));
        storeRepository.delete(store);
}
}
