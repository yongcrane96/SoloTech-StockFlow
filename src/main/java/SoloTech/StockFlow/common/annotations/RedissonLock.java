package SoloTech.StockFlow.common.annotations;

import java.lang.annotation.*;

/**
 * Redis 분산 락을 적용하는 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedissonLock {
    String value(); // Redis Lock Key 값
    long waitTime() default 5000L; // 락 획득 대기 시간 (기본값: 5000ms)
    long leaseTime() default 2000L; // 락 점유 시간 (기본값: 2000ms, -1 설정 시 자동 연장)
}
