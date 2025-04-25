package com.example.stock.controller;

import cn.hutool.core.lang.Snowflake;
import com.example.controller.BaseRestController;
import com.example.kafka.CreateStockEvent;
import com.example.kafka.DecreaseStockEvent;
import com.example.kafka.UpdateStockEvent;
import com.example.stock.dto.StockDto;
import com.example.stock.entity.Stock;
import com.example.stock.kafka.StockEventProducer;
import com.example.stock.service.StockService;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.message.UpdateFeaturesResponseData;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController extends BaseRestController {

    final StockService stockService;
    final StockEventProducer eventProducer;

    @PostMapping
    public long createStock(@RequestBody StockDto dto){
        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();
        CreateStockEvent event = new CreateStockEvent(
                snowflakeId,
                dto.getStoreId(),
                dto.getStoreId(),
                dto.getProductId(),
                dto.getStock()
        );

        eventProducer.sendCommandEvent(event);

        return snowflakeId;
    }

    @GetMapping("/product/{productId}")
    public Stock getStockByProductId(@PathVariable String productId) {
        return stockService.getStockByProductId(productId);
    }

    @GetMapping("{stockId}")
    public ResponseEntity<?> getStock(@PathVariable String stockId){
        log.info("Get stock by id: {}", stockId);
            Stock stock = stockService.getStock(stockId);

            if(stock == null){
                return getErrorResponse("재고를 찾을 수 없습니다: " + stockId);
            }
            return getOkResponse(stock);
    }

    @PutMapping("{stockId}")
    public boolean updateStock(@PathVariable String stockId, @RequestBody StockDto dto) throws JsonMappingException{
        UpdateStockEvent event = new UpdateStockEvent(
                stockId,
                dto.getStoreId(),
                dto.getProductId(),
                dto.getStock()
        );

        eventProducer.sendCommandEvent(event);
        return true;
    }

    // 재고 감소 로직 구성
    @PutMapping("{stockId}/decrease/{quantity}")
    public boolean decreaseStock(@PathVariable String stockId, @PathVariable Long quantity){
        DecreaseStockEvent event = new DecreaseStockEvent(
                stockId,
                quantity
        );
        eventProducer.sendCommandEvent(event);

        return true;
    }

    @DeleteMapping("{stockId}")
    public boolean deleteStock(@PathVariable String stockId){
        stockService.deleteStock(stockId);
        return true;
    }
}
