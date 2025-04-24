package com.example.store.service;

import com.example.annotations.Cached;
import com.example.cache.CacheType;
import com.example.kafka.CreateStoreEvent;
import com.example.kafka.UpdateStoreEvent;
import com.example.store.entity.Store;
import com.example.store.exception.StoreNotFoundException;
import com.example.store.repository.StoreRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

/**
 * 상점 서비스
 *
 * @since   2025-03-25
 * @author  yhkim
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {
    final StoreRepository storeRepository;

    final ObjectMapper mapper;

    @Cached(prefix = "store:", key = "#result.storeId", ttl = 3600, type = CacheType.WRITE, cacheNull = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Store createStore(CreateStoreEvent event) {
        Store store = Store.builder()
                .id(event.getId())
                .storeId(event.getStoreId())
                .storeName(event.getStoreName())
                .address(event.getAddress())
                .build();
        try {
            Store savedStore = storeRepository.saveAndFlush(store);

            return savedStore;
        } catch (Exception e) {
            log.error("Store 생성 또는 후속 작업 실패. storeId: {}", store.getStoreId(), e);

            // 사가 보상 로직 예시 (간단하게 delete 처리)
            try {
                storeRepository.deleteById(store.getId());
                log.info("Store 보상 처리 완료 (삭제). storeId: {}", store.getStoreId());
            } catch (Exception compensationEx) {
                log.error("Store 보상 처리 실패. 수동 개입 필요. storeId: {}", store.getStoreId(), compensationEx);
            }

            throw new StoreNotFoundException(store.getStoreId());
        }
    }

    @Cached(prefix = "store:", key = "#storeId", ttl = 3600, type = CacheType.READ, cacheNull = true)
    public Store getStore(String storeId) {
        Store dbStore = storeRepository.findByStoreId(storeId)
                .orElseThrow(() -> new StoreNotFoundException("Store not found: " + storeId));

        return dbStore;
    }

    @Cached(prefix = "store:", key = "#result.storeId", ttl = 3600, type = CacheType.WRITE, cacheNull = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Store updateStore(UpdateStoreEvent event) {
        String storeId = event.getStoreId();
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(()-> new EntityNotFoundException("Store not found: " + storeId));

        store.setStoreName(event.getStoreName());
        store.setAddress(event.getAddress());

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
