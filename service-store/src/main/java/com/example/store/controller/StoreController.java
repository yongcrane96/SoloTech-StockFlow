package com.example.store.controller;

import cn.hutool.core.lang.Snowflake;
import com.example.kafka.CreateStoreEvent;
import com.example.kafka.UpdateStoreEvent;
import com.example.store.dto.StoreDto;
import com.example.store.entity.Store;
import com.example.store.kafka.StoreEventProducer;
import com.example.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("api/store")
@RequiredArgsConstructor
public class StoreController {
    final StoreEventProducer eventProducer;
    final StoreService storeService;

    @PostMapping
    public long create(@RequestBody StoreDto dto){
        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();
        CreateStoreEvent event = new CreateStoreEvent(
                snowflakeId,
                dto.getStoreId(),
                dto.getStoreName(),
                dto.getAddress()
        );
        eventProducer.sendCommandEvent(event);

        return snowflakeId;
    }

    @GetMapping("/{storeId}")
    public Store getStore(@PathVariable String storeId){
        log.info("getStore storeId : ", storeId);
        return storeService.getStore(storeId);
    }

    @PutMapping("/{storeId}")
    public boolean updateStore(@PathVariable String storeId,
                             @RequestBody StoreDto dto) {
        UpdateStoreEvent event = new UpdateStoreEvent(
                storeId,
                dto.getStoreName(),
                dto.getAddress()
        );
        eventProducer.sendCommandEvent(event);
        return true;
    }

    @DeleteMapping("/{storeId}")
    public boolean deleteStore(@PathVariable String storeId) {
        storeService.deleteStore(storeId);
        return true;
    }
}
