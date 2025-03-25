package SoloTech.StockFlow.store.service;

import SoloTech.StockFlow.store.dto.StoreDto;
import SoloTech.StockFlow.store.entity.Store;
import SoloTech.StockFlow.store.repository.StoreRepository;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Transactional
    public Store createStore(StoreDto dto) {
        Store store = mapper.convertValue(dto, Store.class);
        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

        store.setStoreId(String.valueOf(snowflakeId));
        return storeRepository.saveAndFlush(store);
    }

    public Store getStore(String storeId) {
        return storeRepository.findByStoreId(storeId)
                .orElseThrow(()->new RuntimeException("Store not found: " + storeId));
    }

    @Transactional
    public Store updateStore(String storeId, StoreDto dto) throws JsonMappingException {
        Store store = this.getStore(storeId);
        mapper.updateValue(store,dto);
        return storeRepository.save(store);
    }

    public void deleteStore(String storeId) {
        Store store = storeRepository.findByStoreId(storeId)
                .orElseThrow(()->new RuntimeException("Store not found: " + storeId));
        storeRepository.delete(store);
    }
}
