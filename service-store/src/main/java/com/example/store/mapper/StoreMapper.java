package com.example.store.mapper;

import com.example.kafka.CreateStoreEvent;
import com.example.store.dto.StoreDto;
import com.example.store.entity.Store;

public class StoreMapper {
    // CreateStoreEvent를 StoreDto로 변환하는 메서드
    public static StoreDto toDto(CreateStoreEvent event) {
        return StoreDto.builder()
                .storeName(event.getStoreName())  // Avro 이벤트에서 storeName 매핑
                .address(event.getAddress())      // Avro 이벤트에서 address 매핑
                .build();
    }

    // StoreDto를 Store로 변환하는 메서드 (필요시)
    public static Store toEntity(StoreDto dto) {
        return Store.builder()
                .storeName(dto.getStoreName())
                .address(dto.getAddress())
                .build();
    }
}