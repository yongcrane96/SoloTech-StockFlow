package SoloTech.StockFlow.stock.service;

import SoloTech.StockFlow.common.annotations.RedissonLock;
import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.stock.dto.StockDto;
import SoloTech.StockFlow.stock.entity.Stock;
import SoloTech.StockFlow.stock.repository.StockRepository;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    final StockRepository stockRepository;
    final ObjectMapper mapper;
    final RedisTemplate redisTemplate;

    private static final String STOCK_KEY_PREFIX = "stock:";

    // 로컬 캐시 (Caffeine)
    private final Cache<String, Object> localCache;

    // 메시지 발행 (Pub/Sub) 컴포넌트
    private final CachePublisher cachePublisher;

    @Transactional
    public Stock createStock(StockDto dto) {
        log.info("createStock dto : ", dto);
        Stock stock = mapper.convertValue(dto, Stock.class);

        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

        stock.setStockId(String.valueOf(snowflakeId));
        Stock savedStock = stockRepository.saveAndFlush(stock);

        String cacheKey = STOCK_KEY_PREFIX + savedStock.getStockId();
        redisTemplate.opsForValue().set(cacheKey, savedStock, Duration.ofMinutes(10));

        // 로컬 캐시 저장
        localCache.put(cacheKey, savedStock);
        cachePublisher.publish("stock_update", cacheKey);

        log.info("Created order: {}", cacheKey);
        return savedStock;
    }

    public Stock getStock(String stockId) {
        String cacheKey = STOCK_KEY_PREFIX + stockId;

        Stock cachedStock = (Stock) localCache.getIfPresent(cacheKey);
        if(cachedStock != null){
            log.info("[LocalCache] Hit for key={}", cacheKey);
            return cachedStock;
        }

        // Redis 캐시 확인
        cachedStock = (Stock) redisTemplate.opsForValue().get(cacheKey);
        if (cachedStock != null) {
            log.info("[RedisCache] Hit for key={}", cacheKey);
            // 로컬 캐시에 다시 저장
            localCache.put(cacheKey, cachedStock);
            return cachedStock;
        }

        // DB 조회
        Stock dbStock = stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new RuntimeException("StockId not found : " + stockId));

        // 캐시에 저장
        redisTemplate.opsForValue().set(cacheKey, dbStock);
        localCache.put(cacheKey, dbStock);

        return dbStock;
    }

    @Transactional
    public Stock updateStock(String stockId, StockDto dto) throws JsonMappingException {
        // Fetch stock from repository
        Stock stock = stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new EntityNotFoundException("Stock not found: " + stockId));

        log.info("Before update: stock={}, dto={}", stock.getStock(), dto.getStock());

        // Update stock using mapper
        mapper.updateValue(stock, dto);

        log.info("After mapping: stock={}", stock.getStock());

        // Save updated stock in repository
        Stock savedStock = stockRepository.save(stock);
        String cacheKey = STOCK_KEY_PREFIX + savedStock.getStockId();

        // Always update cache regardless of condition
        log.info("Updating stock in Redis and local cache");
        redisTemplate.opsForValue().set(cacheKey, savedStock);
        localCache.put(cacheKey, savedStock);

        // Publish sync message
        String message = "Updated stock-" + cacheKey;
        cachePublisher.publish("cache-sync", message);

        log.info("Updated stock: {}, published message: {}", cacheKey, message);

        return savedStock;
    }

    /**
     * 재고 감소
     *  - 동시성 제어(@RedissonLock) + DB 수정
     *  - 캐시 갱신
     *  - Pub/Sub 메시지 발행
     */
    @Transactional
    @RedissonLock(value = "stock-{stockId}", transactional = true)
    public Stock decreaseStock(String stockId, Long quantity) {
        Stock stock = stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new RuntimeException("Stock not found: " + stockId));

        // 수량 검사
        if (!stock.decrease(quantity)) throw new RuntimeException("The quantity is larger than the stock: " + stockId);

        Stock updatedStock = stockRepository.save(stock);

        // 캐시 갱신 및 Pub/Sub 메시지 발행
        // 캐시 키 생성
        String cacheKey = STOCK_KEY_PREFIX + stockId;
        redisTemplate.convertAndSend("cache-sync", "Updated stock-" + stockId);
        redisTemplate.opsForValue().set(cacheKey, updatedStock);
        localCache.put(cacheKey, updatedStock);

        // 3) 다른 서버들도 캐시를 무효화하도록 메시지 발행
        String message = "Updated stock-" + cacheKey;
        cachePublisher.publish("cache-sync", message);

        log.info("Updated stock: {}, published message: {}", cacheKey, message);

        return updatedStock;
    }

    public void deleteStock(String stockId) {
        Stock stock = stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new RuntimeException("StockId not found : " + stockId));
        stock.setDeleted(true);
        stockRepository.save(stock);
        stockRepository.delete(stock);

        // 캐시 무효화 대상 key
        String cacheKey = STOCK_KEY_PREFIX + stockId;

        // 현재 서버(로컬 캐시 + Redis)에서도 삭제
        localCache.invalidate(cacheKey);
        redisTemplate.delete(cacheKey);

        // 다른 서버들도 캐시를 무효화하도록 메시지 발행
        // 메시지 형식: "Deleted stock-stock:xxxx" 로 가정
        String message = "Deleted stock-" + cacheKey;
        cachePublisher.publish("cache-sync", message);

        log.info("Deleted order: {}, published message: {}", cacheKey, message);
    }
}
