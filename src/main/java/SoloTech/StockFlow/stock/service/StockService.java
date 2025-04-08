package SoloTech.StockFlow.stock.service;

import SoloTech.StockFlow.common.annotations.Cached;
import SoloTech.StockFlow.common.annotations.RedissonLock;
import SoloTech.StockFlow.common.cache.CacheType;
import SoloTech.StockFlow.stock.dto.StockDto;
import SoloTech.StockFlow.stock.entity.Stock;
import SoloTech.StockFlow.stock.repository.StockRepository;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    final StockRepository stockRepository;
    final ObjectMapper mapper;
    final RedisTemplate redisTemplate;

    @Cached(prefix = "stock:", key = "#result.stockId", ttl = 3600, type = CacheType.WRITE)
    @Transactional
    public Stock createStock(StockDto dto) {
        log.info("createStock dto : ", dto);
        Stock stock = mapper.convertValue(dto, Stock.class);

        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

        stock.setStockId(String.valueOf(snowflakeId));
        Stock savedStock = stockRepository.saveAndFlush(stock);

        return savedStock;
    }

    @Cached(prefix = "stock:", key = "#stockId", ttl = 3600, type = CacheType.READ)
    public Stock getStock(String stockId) {
        Stock dbStock = stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new RuntimeException("StockId not found : " + stockId));

        return dbStock;
    }

    @Cached(prefix = "stock:", key = "#result.stockId", ttl = 3600, type = CacheType.WRITE)
    @Transactional
    public Stock updateStock(String stockId, StockDto dto) throws JsonMappingException {
        Stock stock = stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found: " + stockId));

        log.info("Before update: stock={}, dto={}", stock.getStock(), dto.getStock());

        // Update stock using mapper
        mapper.updateValue(stock, dto);

        log.info("After mapping: stock={}", stock.getStock());

        // Save updated stock in repository
        Stock savedStock = stockRepository.save(stock);
        return savedStock;
    }

    /**
     * 재고 감소
     *  - 동시성 제어(@RedissonLock) + DB 수정
     *  - 캐시 갱신
     *  - Pub/Sub 메시지 발행
     */
    @Transactional
    @Cached(prefix = "stock:", key = "#result.stockId", ttl = 3600, type = CacheType.WRITE)
    @RedissonLock(value = "#{'stock-' + stockId}")
    public Stock decreaseStock(String stockId, Long quantity) {
        Stock stock = stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new RuntimeException("Stock not found: " + stockId));

        // 수량 검사
        if (!stock.decrease(quantity)) throw new RuntimeException("The quantity is larger than the stock: " + stockId);

        Stock updatedStock = stockRepository.save(stock);

        return updatedStock;
    }


    @Cached(prefix = "stock:", key = "#stockId", ttl = 3600, type = CacheType.DELETE)
    public void deleteStock(String stockId) {
        Stock stock = stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new RuntimeException("StockId not found : " + stockId));
        stock.setDeleted(true);
        stockRepository.save(stock);
        stockRepository.delete(stock);
       }
}
