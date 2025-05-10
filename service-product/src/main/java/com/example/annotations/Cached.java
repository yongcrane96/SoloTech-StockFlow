package com.example.annotations;

import com.example.cache.CacheType;

import java.lang.annotation.*;

/**
 * 캐시 관련 적용하는 어노테이션
 */
@Documented
@Target(ElementType.METHOD) // 메서드에만 적용될 수록있도록 제한
@Retention(RetentionPolicy.RUNTIME) // 런타임동안 유지
public @interface Cached {
    String prefix(); // 캐시 키 prefix;
    String key();
    long ttl() default 3600; // 초 단위 TTL
    CacheType type() default CacheType.READ;
    boolean cacheNull() default false;
}
