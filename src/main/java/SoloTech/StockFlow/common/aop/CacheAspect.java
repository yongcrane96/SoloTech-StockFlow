package SoloTech.StockFlow.common.aop;
import SoloTech.StockFlow.cache.CachePublisher;
import static SoloTech.StockFlow.common.util.SpELKeyGenerator.generateKey;
import SoloTech.StockFlow.common.annotations.Cached;
import SoloTech.StockFlow.common.cache.CacheType;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import static SoloTech.StockFlow.common.util.CacheKeyUtil.*;
import static SoloTech.StockFlow.common.cache.CacheType.*;
import java.lang.reflect.Method;
import java.time.Duration;

/**
 * 캐시관점 : 코드 중복 줄이고, 유지보수성 높이며, 공통 관심사를 관리
 */

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CacheAspect {
    private final Cache<String,Object> localCache;
    private final RedisTemplate<String,Object> redisTemplate;
    private final CachePublisher cachePublisher;
    private static final long DEFAULT_TTL = 60;

    @Around("@annotation(cached)")
    public Object handleCaching(ProceedingJoinPoint joinPoint, Cached cached) throws Throwable{
        String prefix = cached.prefix();
        String keyExpression = cached.key();
        long ttl = cached.ttl();
        CacheType type = cached.type();

        // TTL 검증 및 기본값 설정
        if (ttl <= 0) {
            log.warn("Invalid TTL value: {}. Using default TTL.", ttl);
            ttl = DEFAULT_TTL;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String key = generateKey(keyExpression, method, joinPoint.getArgs());

        String cacheKey = buildFullKey(prefix, key);

        Object result;

        switch (type) {
            case READ -> {
                Object localValue = localCache.getIfPresent(cacheKey);
                if (localValue != null) return localValue;

                Object redisValue = redisTemplate.opsForValue().get(cacheKey);
                if (redisValue != null) {
                    localCache.put(cacheKey, redisValue);
                    return redisValue;
                }

                result = joinPoint.proceed();
                if (result != null || cached.cacheNull()) { // null 값도 캐싱 처리
                    redisTemplate.opsForValue().set(cacheKey, result, Duration.ofSeconds(ttl));
                    localCache.put(cacheKey, result);
                    log.info("[READ] Cached: {}", cacheKey);
                }
                return result;
            }

            case WRITE -> {
                result = joinPoint.proceed();
                if (result != null || cached.cacheNull()) {
                    redisTemplate.opsForValue().set(cacheKey, result, Duration.ofSeconds(ttl));
                    localCache.put(cacheKey, result);

                    String message = buildEventMessage(WRITE, prefix, key);
                    cachePublisher.publish(getDefaultChannel(), message);

                    log.info("[WRITE] Cached and published: {}, message: {}", cacheKey, message);
                }
                return result;
            }

            case DELETE -> {
                result = joinPoint.proceed();

                redisTemplate.delete(cacheKey);
                localCache.invalidate(cacheKey);

                String message = buildEventMessage(DELETE, prefix, key);
                cachePublisher.publish(getDefaultChannel(), message);

                log.info("[DELETE] Cache invalidated and published: {}, message: {}", cacheKey, message);
                return result;
            }
            default -> {
                log.warn("Unsupported cache type: {}", type);
                return joinPoint.proceed();
            }
        }
    }
}
