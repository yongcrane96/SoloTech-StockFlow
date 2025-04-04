package SoloTech.StockFlow.product.service;

import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.order.entity.Order;
import SoloTech.StockFlow.order.service.OrderService;
import SoloTech.StockFlow.product.dto.ProductDto;
import SoloTech.StockFlow.product.entity.Product;
import SoloTech.StockFlow.product.repository.ProductRepository;
import cn.hutool.core.lang.Snowflake;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ObjectMapper mapper;
    private static final String PRODUCT_KEY_PREFIX = "product:";

    // 로컬 캐시 (Caffeine)
    private final Cache<String, Object> localCache;

    private final RedisTemplate<String, Object> redisTemplate;

    // 메시지 발행 (Pub/Sub) 컴포넌트
    private final CachePublisher cachePublisher;

    @Transactional
    public Product createProduct(ProductDto dto) {
        Product product = mapper.convertValue(dto, Product.class);

        Snowflake snowflake = new Snowflake(1,1);
        long snowflakeId = snowflake.nextId();

        product.setProductId(String.valueOf(snowflakeId));
        Product savedProduct = productRepository.saveAndFlush(product);

        // 캐시 키
        String cacheKey = PRODUCT_KEY_PREFIX + savedProduct.getProductId();

        redisTemplate.opsForValue().set(cacheKey, savedProduct, Duration.ofHours(1));

        // 로컬 캐시 저장
        localCache.put(cacheKey, savedProduct);
        cachePublisher.publish("product_update", cacheKey);

        log.info("Created product: {}", cacheKey);
        return savedProduct;
    }

    public Product getProduct(String productId) {
        String cacheKey = PRODUCT_KEY_PREFIX + productId;

        // 1) 로컬 캐시 확인
        Product cachedProduct = (Product) localCache.getIfPresent(cacheKey);
        if(cachedProduct != null){
            log.info("[LocalCache] Hit for key={}", cacheKey);
            return cachedProduct;
        }

        // 2) Redis 캐시 확인
        cachedProduct = (Product) redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            log.info("[RedisCache] Hit for key={}", cacheKey);
            // 로컬 캐시에 다시 저장
            localCache.put(cacheKey, cachedProduct);
            return cachedProduct;
        }

        Product dbProduct = productRepository.findByProductId(productId)
            .orElseThrow(()-> new RuntimeException("Product not found : " + productId));

        // 조회 후 캐시 저장
        redisTemplate.opsForValue().set(cacheKey, dbProduct, Duration.ofHours(1));
        localCache.put(cacheKey, dbProduct);

        return dbProduct;
    }

    @Transactional
    public Product updateProduct(String productId, ProductDto dto) throws JsonMappingException {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));

        log.info("Update product by id: {}", productId);

        mapper.updateValue(product, dto);
        Product savedProduct = productRepository.save(product);
        String cacheKey = PRODUCT_KEY_PREFIX + savedProduct.getProductId();

        // Redis, 로컬 캐시에 갱신
        redisTemplate.opsForValue().set(cacheKey, savedProduct);
        localCache.put(cacheKey, savedProduct);

        String message = "Updated product-" + cacheKey;
        cachePublisher.publish("cache-sync", message);

        log.info("Updated product: {}, published message: {}", cacheKey, message);

        return savedProduct;
    }

    public void deleteProduct(String productId) {
        Product product = productRepository.findByProductId(productId)
                .orElseThrow(()-> new RuntimeException("Product not found : " + productId));
        productRepository.delete(product);

        // 캐시 무효화 대상 key
        String cacheKey = PRODUCT_KEY_PREFIX + productId;

        // 현재 서버(로컬 캐시 + Redis)에서도 삭제
        localCache.invalidate(cacheKey);
        redisTemplate.delete(cacheKey);

        // 다른 서버들도 캐시를 무효화하도록 메시지 발행
        // 메시지 형식: "Deleted product-product:xxxx" 로 가정
        String message = "Deleted product-" + cacheKey;
        cachePublisher.publish("cache-sync", message);

        log.info("Deleted product: {}, published message: {}", cacheKey, message);
    }
}
