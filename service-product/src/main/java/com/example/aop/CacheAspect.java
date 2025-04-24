package com.example.aop;

import com.example.annotations.Cached;
import com.example.cache.CachePublisher;
import com.example.cache.CacheType;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.time.Duration;

import static com.example.util.CacheKeyUtil.*;
import static com.example.util.CacheKeyUtil.buildEventMessage;
import static com.example.util.CacheKeyUtil.buildFullKey;
import static com.example.util.CacheKeyUtil.getDefaultChannel;
import static com.example.util.SpELKeyGenerator.*;
import static com.example.util.SpELKeyGenerator.generateKey;

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

        final long finalTtl = ttl;

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
                if (shouldCache(result, cached)) {
                    try {
                        redisTemplate.opsForValue().set(cacheKey, result, Duration.ofSeconds(finalTtl));
                    } catch (Exception e) {
                        log.error("[READ] Redis 캐시 저장 실패 - key: {}, error: {}", cacheKey, e.getMessage(), e);
                    }
                    localCache.put(cacheKey, result);
                    log.info("[READ] Cached: {}", cacheKey);
                }
                return result;
            }

            case WRITE -> {
                result = joinPoint.proceed();


                if (shouldCache(result, cached) && isTxActive()) {
                    Object finalResult = result;

                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                redisTemplate.opsForValue().set(cacheKey, finalResult, Duration.ofSeconds(finalTtl));
                            } catch (Exception e) {
                                log.error("[WRITE] Redis 캐시 저장 실패 (TX afterCommit) - key: {}, error: {}", cacheKey, e.getMessage(), e);
                            }
                            localCache.put(cacheKey, finalResult);

                            String message = buildEventMessage(CacheType.WRITE, prefix, key);
                            cachePublisher.publish(getDefaultChannel(), message);
                            log.info("[WRITE] Cached and published: {}, message: {}", cacheKey, message);
                        }
                    });
                } else {
                    try {
                        redisTemplate.opsForValue().set(cacheKey, result, Duration.ofSeconds(finalTtl));
                    } catch (Exception e) {
                        log.error("[WRITE] Redis 캐시 저장 실패 (no TX) - key: {}, error: {}", cacheKey, e.getMessage(), e);
                    }
                    localCache.put(cacheKey, result);

                    String message = buildEventMessage(CacheType.WRITE, prefix, key);
                    cachePublisher.publish(getDefaultChannel(), message);
                    log.info("[WRITE] Cached immediately (no TX) and published: {}, message: {}", cacheKey, message);
                }
                return result;
            }

            case DELETE -> {
                result = joinPoint.proceed();
                try {
                    redisTemplate.delete(cacheKey);
                } catch (Exception e) {
                    log.error("[DELETE] Redis 캐시 삭제 실패 - key: {}, error: {}", cacheKey, e.getMessage(), e);
                }
                localCache.invalidate(cacheKey);

                String message = buildEventMessage(CacheType.DELETE, prefix, key);
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

    private boolean shouldCache(Object result, Cached cached) {
        return result != null || cached.cacheNull();
    }
    private boolean isTxActive() {
        return TransactionSynchronizationManager.isSynchronizationActive();
    }
}
