package com.example.stock;

import com.example.stock.dto.StockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockFeignClient stockFeignClient;

    public StockResponse getStockProduct(String stockId){
        return stockFeignClient.getStockByProductId(stockId);
    }

    public boolean decreaseStock(String stockId, long quantity){
        return stockFeignClient.decreaseStock(stockId, quantity);
    }
}
