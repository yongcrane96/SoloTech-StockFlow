package SoloTech.StockFlow.stock.service;

import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.order.service.OrderService;
import SoloTech.StockFlow.stock.dto.StockDto;
import SoloTech.StockFlow.stock.entity.Stock;
import SoloTech.StockFlow.stock.repository.StockRepository;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    final StockRepository stockRepository;
    final ObjectMapper mapper;

    private static final String STOCK_KEY_PREFIX = "stock:";

    // 로컬 캐시 (Caffeine)
    private final Cache<String, Object> localCache;

    private final RedisTemplate<String, Object> redisTemplate;

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

        redisTemplate.opsForValue().set(cacheKey, savedStock);
        // 로컬 캐시 저장
        localCache.put(cacheKey, savedStock);

        log.info("Created order: {}", cacheKey);
        return savedStock;
    }

    public Stock getStock(String stockId) {
        String cacheKey = STOCK_KEY_PREFIX + stockId;

        Stock cachedStock = (Stock) localCache.getIfPresent(STOCK_KEY_PREFIX);
        if(cachedStock != null){
            log.info("[LocalCache] Hit for key={}", STOCK_KEY_PREFIX);
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
        localCache.put(STOCK_KEY_PREFIX, dbStock);

        return dbStock;
    }

    @Transactional
    public Stock updateStock(String stockId, StockDto dto) throws JsonMappingException {
        Stock stock = this.getStock(stockId);
        mapper.updateValue(stock, dto);
        Stock savedStock = stockRepository.save(stock);

        String cacheKey = STOCK_KEY_PREFIX + savedStock.getStockId();

        // Redis, 로컬 캐시에 갱신
        redisTemplate.opsForValue().set(cacheKey, savedStock);
        localCache.put(cacheKey, savedStock);

        // 다른 서버 인스턴스 캐시 무효화를 위해 메시지 발행
        // 메시지 형식: "Updated stock-stock:xxxx" 로 가정
        String message = "Updated stock-" + cacheKey;
        cachePublisher.publish("cache-sync", message);

        log.info("Updated stock: {}, published message: {}", cacheKey, message);

        return savedStock;
    }

    @Transactional
    public Stock decreaseStock(String stockId, Long quantity) {
        Stock stock = stockRepository.findByStockId(stockId)
                .orElseThrow(() -> new RuntimeException("Stock not found: " + stockId));
        if (!stock.decrease(quantity)) throw new RuntimeException("The quantity is larger than the stock: " + stockId);

        Stock updatedStock = stockRepository.save(stock);

        // 캐시 키 생성
        String cacheKey = STOCK_KEY_PREFIX + stockId;
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
