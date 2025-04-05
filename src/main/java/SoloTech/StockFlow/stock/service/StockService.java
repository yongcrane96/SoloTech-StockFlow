package SoloTech.StockFlow.stock.service;

import SoloTech.StockFlow.common.annotations.RedissonLock;
import SoloTech.StockFlow.stock.dto.StockDto;
import SoloTech.StockFlow.stock.entity.Stock;
import SoloTech.StockFlow.stock.repository.StockRepository;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    final StockRepository stockRepository;
    final ObjectMapper mapper;
    final RedisTemplate redisTemplate;

    @Transactional
    public Stock createStock(StockDto dto) {
        log.info("createStock dto : ", dto);
        Stock stock = mapper.convertValue(dto, Stock.class);

        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

        stock.setStockId(String.valueOf(snowflakeId));
        return stockRepository.saveAndFlush(stock);
    }

    public Stock getStock(String stockId) {
        return stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new RuntimeException("StockId not found : " + stockId));
    }

    @Transactional
    public Stock updateStock(String stockId, StockDto dto) throws JsonMappingException {
        Stock stock = this.getStock(stockId);
        mapper.updateValue(stock, dto);
        return stockRepository.save(stock);
    }

    /**
     * 재고 감소
     *  - 동시성 제어(@RedissonLock) + DB 수정
     *  - 캐시 갱신
     *  - Pub/Sub 메시지 발행
     */
    @Transactional
    @RedissonLock(value = "#{'stock-' + stockId}")
    public Stock decreaseStock(String stockId, Long quantity) {
        Stock stock = stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new RuntimeException("Stock not found: " + stockId));

        // 수량 검사
        if (!stock.decrease(quantity)) throw new RuntimeException("The quantity is larger than the stock: " + stockId);

        Stock updatedStock = stockRepository.save(stock);

        // 캐시 갱신 및 Pub/Sub 메시지 발행
        String cacheKey = "stock-" + stockId;
        redisTemplate.opsForValue().set(cacheKey, updatedStock);
        redisTemplate.convertAndSend("cache-sync", "Updated stock-" + stockId);

        return updatedStock;
    }

    public void deleteStock(String stockId) {
        Stock stock = stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new RuntimeException("StockId not found : " + stockId));
        stockRepository.delete(stock);
    }
}
