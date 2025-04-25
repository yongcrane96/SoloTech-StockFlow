package com.example.stock;

import com.example.stock.dto.StockRequest;
import com.example.stock.dto.StockResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockFeignClient stockFeignClient;

    @PostMapping
    public String createStock(@RequestBody StockRequest request) {
        log.info("Create stock request: {}", request);
        return stockFeignClient.createStock(request);
    }

    @GetMapping("/{stockId}")
    public StockResponse getStock(@PathVariable String stockId) {
        log.info("Get stock by id {}", stockId);
        return stockFeignClient.getStock(stockId);
    }

    @PutMapping("/{stockId}")
    public boolean updateStock(
            @PathVariable String stockId,
            @RequestBody StockRequest request
    ) {
        return stockFeignClient.updateStocks(stockId, request);
    }

    @PutMapping("/{stockId}/decrease/{quantity}")
    public boolean decreaseStock(
            @PathVariable String stockId,
            @PathVariable Long quantity
    ) {
        return stockFeignClient.decreaseStock(stockId, quantity);
    }

    @DeleteMapping("/{stockId}")
    public boolean deleteStore(@PathVariable String stockId) {
        return stockFeignClient.deleteStock(stockId);
    }
}
