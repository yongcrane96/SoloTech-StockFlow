package com.example.store.controller;

import com.example.store.dto.StoreDto;
import com.example.store.entity.Store;
import com.example.store.service.StoreService;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("api/store")
@RequiredArgsConstructor
public class StoreController {
    final StoreService storeService;

    @PostMapping
    public Store create(@RequestBody StoreDto dto){
        return storeService.createStore(dto);
    }

    @GetMapping("/{storeId}")
    public Store getStore(@PathVariable String storeId){
        log.info("getStore storeId : ", storeId);
        return storeService.getStore(storeId);
    }

    @PutMapping("/{storeId}")
    public Store updateStore(@PathVariable String storeId,
                             @RequestBody StoreDto dto) throws JsonMappingException {
        return storeService.updateStore(storeId, dto);
    }

    @DeleteMapping("/{storeId}")
    public boolean deleteStore(@PathVariable String storeId) {
        storeService.deleteStore(storeId);
        return true;
    }
}
