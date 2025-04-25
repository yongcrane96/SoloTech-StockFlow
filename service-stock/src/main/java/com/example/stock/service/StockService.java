package com.example.stock.service;

import com.example.annotations.Cached;
import com.example.annotations.RedissonLock;
import com.example.cache.CacheType;
import com.example.kafka.CreateStockEvent;
import com.example.kafka.DecreaseStockEvent;
import com.example.kafka.UpdateStockEvent;
import com.example.stock.dto.StockDto;
import com.example.stock.entity.Stock;
import com.example.stock.exception.StockNotFoundException;
import com.example.stock.repository.StockRepository;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    final StockRepository stockRepository;
    final ObjectMapper mapper;
    final RedisTemplate redisTemplate;

    @Cached(prefix = "stock:", key = "#result.stockId", ttl = 3600, type = CacheType.WRITE, cacheNull = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Stock createStock(CreateStockEvent event) {
        Stock stock = Stock.builder()
                .id(event.getId())
                .stockId(event.getStockId())
                .storeId(event.getStoreId())
                .productId(event.getProductId())
                .stock(event.getStock())
                .build();

        try {
            Stock savedStock = stockRepository.saveAndFlush(stock);
            return savedStock;
        }catch (Exception e){
            log.error("Stock 생성 또는 후속 작업 실패. stockId: {}", stock.getStockId(), e);

            // 사가 보상 로직 예시 (간단하게 delete 처리)
            try {
                stockRepository.deleteById(stock.getId());
                log.info("Stock 보상 처리 완료 (삭제). stockId: {}", stock.getStockId());
            } catch (Exception compensationEx) {
                log.error("Stock 보상 처리 실패. 수동 개입 필요. stockId: {}", stock.getStockId(), compensationEx);
            }

            throw new StockNotFoundException(stock.getStockId());
        }

    }

    @Cached(prefix = "stock:", key = "#stockId", ttl = 3600, type = CacheType.READ, cacheNull = true)
    public Stock getStock(String stockId) {
        Stock dbStock = stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new StockNotFoundException("StockId not found : " + stockId));

        return dbStock;
    }

    @Cached(prefix = "stock:", key = "#stockId", ttl = 3600, type = CacheType.READ, cacheNull = true)
    public Stock getStockByProductId(String productId){
        return stockRepository.findByProductId(productId)
                .orElseThrow(() -> new StockNotFoundException("No stock for product: " + productId));
    }

    @Cached(prefix = "stock:", key = "#result.stockId", ttl = 3600, type = CacheType.WRITE, cacheNull = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Stock updateStock(UpdateStockEvent event) {
        String stockId = event.getStockId();
        Stock stock = stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found: " + stockId));

        log.info("After mapping: stock={}", stock.getStock());

        stock.setStock(event.getStock());

        Stock savedStock = stockRepository.save(stock);
        return savedStock;
    }

    /**
     * 재고 감소
     *  - 동시성 제어(@RedissonLock) + DB 수정
     *  - 캐시 갱신
     *  - Pub/Sub 메시지 발행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Cached(prefix = "stock:", key = "#result.stockId", ttl = 3600, type = CacheType.WRITE, cacheNull = true)
    @RedissonLock(value = "#{'stock-' + stockId}")
    public Stock decreaseStock(DecreaseStockEvent event) {
        String stockId = event.getStockId();
        Stock stock = stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new RuntimeException("Stock not found: " + stockId));

        // 수량 검사
        if (!stock.decrease(event.getQuantity())) throw new StockNotFoundException("The quantity is larger than the stock: " + stockId);

        Stock updatedStock = stockRepository.save(stock);

        return updatedStock;
    }


    @Cached(prefix = "stock:", key = "#stockId", ttl = 3600, type = CacheType.DELETE, cacheNull = true)
    public void deleteStock(String stockId) {
        Stock stock = stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new StockNotFoundException("StockId not found : " + stockId));
        stock.setDeleted(true);
        stockRepository.save(stock);
        stockRepository.delete(stock);
       }
}
