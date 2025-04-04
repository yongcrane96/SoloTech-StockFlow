package SoloTech.StockFlow.common.annotations;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedissonLock {
    String value();
    long waitTime() default 5000L;
    long leaseTime() default 2000L;
}
