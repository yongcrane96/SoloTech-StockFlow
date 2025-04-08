package SoloTech.StockFlow.common.aop;
import SoloTech.StockFlow.cache.CachePublisher;
import SoloTech.StockFlow.cache.CacheSubscriber;
import SoloTech.StockFlow.common.annotations.Cached;
import SoloTech.StockFlow.common.cache.CacheType;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

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
    private final SpelExpressionParser parser = new SpelExpressionParser();
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
        String key = buildKeyFromSpEL(keyExpression, method, joinPoint.getArgs());

        String cacheKey = prefix + key;

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
                if (result != null) {
                    redisTemplate.opsForValue().set(cacheKey, result, Duration.ofSeconds(ttl));
                    localCache.put(cacheKey, result);
                    log.info("[READ] Cached: {}", cacheKey);
                }
                return result;
            }

            case WRITE -> {
                result = joinPoint.proceed();
                if (result != null) {
                    redisTemplate.opsForValue().set(cacheKey, result, Duration.ofSeconds(ttl));
                    localCache.put(cacheKey, result);

                    String message = "Updated " + prefix+ "-" + cacheKey;
                    cachePublisher.publish("cache-sync", message);

                    log.info("[WRITE] Cached and published: {}, message: {}", cacheKey, message);
                }
                return result;
            }

            case DELETE -> {
                result = joinPoint.proceed();

                redisTemplate.delete(cacheKey);
                localCache.invalidate(cacheKey);

                String message = "DELETE  " + prefix+ "-" + cacheKey;
                cachePublisher.publish("cache-sync", message);

                log.info("[DELETE] Cache invalidated and published: {}, message: {}", cacheKey, message);
                return result;
            }
            default -> {
                log.warn("Unsupported cache type: {}", type);
                return joinPoint.proceed();
            }
        }
    }

    private String buildKeyFromSpEL(String keySpEL, Method method, Object[] args){
        EvaluationContext evalContext = new StandardEvaluationContext();

        ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();
        String[] paramNames = discoverer.getParameterNames(method);

        if(paramNames != null){
            for (int i = 0; i < paramNames.length; i++) {
                evalContext.setVariable(paramNames[i], args[i]);
            }
        }
        try{
            return  parser.parseExpression(keySpEL).getValue(evalContext, String.class);
        } catch (Exception e){
            log.error("SpEL 파싱 오류 - key: {}, error: {}", keySpEL, e.getMessage());
            throw new RuntimeException("캐시 키 생성 실패", e);
        }
    }
}
