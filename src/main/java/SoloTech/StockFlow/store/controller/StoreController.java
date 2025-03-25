package SoloTech.StockFlow.store.controller;

import SoloTech.StockFlow.store.dto.StoreDto;
import SoloTech.StockFlow.store.entity.Store;
import SoloTech.StockFlow.store.service.StoreService;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 상점 컨트롤러
 *
 * @since   2025-03-25
 * @author  yhkim
 */
@Slf4j
@RestController
@RequestMapping("api/store")
@RequiredArgsConstructor
public class StoreController {
    final StoreService storeService;

    @PostMapping
    public Store createStore(@RequestBody StoreDto dto){
        return storeService.createStore(dto);
    }

    @GetMapping("/{storeId}")
    public Store getStore(@PathVariable String storeId) {
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
