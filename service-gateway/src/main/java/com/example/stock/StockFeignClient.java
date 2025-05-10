package com.example.stock;

import com.example.stock.dto.StockRequest;
import com.example.stock.dto.StockResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "stockClient", url = "http://localhost:8083")
public interface StockFeignClient {

    // [C] Create
    @PostMapping("/api/stock")
    String createStock(@RequestBody StockRequest request);

    // [R1] 단건 조회
    @GetMapping("/api/stock/{stockId}")
    StockResponse getStock(@PathVariable("stockId") String stockId);


    // [R2] 단건 품목 조회
    @GetMapping("/api/stock/product/{productId}")
    StockResponse getStockByProductId(@PathVariable("productId") String productId);

    // [U] Update
    @PutMapping("/api/stock/{stockId}")
    boolean updateStocks(
            @PathVariable("stockId") String stockId,
            @RequestBody StockRequest request
    );

    // [U] Update
    @PutMapping("/api/stock/{stockId}/decrease/{quantity}")
    boolean decreaseStock(
            @PathVariable("stockId") String stockId,
            @PathVariable("quantity") long quantity
    );

    @PutMapping("/api/stock/{stockId}/increase/{quantity}")
    boolean increaseStock(
            @PathVariable("stockId") String stockId,
            @PathVariable("quantity") long quantity
    );

    // [D] Delete
    @DeleteMapping("/api/stock/{stockId}")
    boolean deleteStock(@PathVariable("stockId") String stockId);
}
