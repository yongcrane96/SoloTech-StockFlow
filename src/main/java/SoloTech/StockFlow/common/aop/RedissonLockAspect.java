package SoloTech.StockFlow.common.aop;

import SoloTech.StockFlow.common.annotations.RedissonLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RedissonLockAspect {

    private final RedissonClient redissonClient;

    @Around("@annotation(com.example.annotations.RedissonLock)")
    public Object redissonLock(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("redissonLock");
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RedissonLock annotation = method.getAnnotation(RedissonLock.class);
        String lockKey = annotation.value();

        RLock lock = redissonClient.getLock(lockKey);

        boolean lockable = false;
        try {
            // 락 획득 시도
            lockable = lock.tryLock(annotation.waitTime(), annotation.leaseTime(), TimeUnit.MILLISECONDS);
            log.info("name: {}, locked: {}, lockable: {}", lock.getName(), lock.isLocked(), lockable);
            if (!lockable) {
                throw new IllegalStateException("Could not acquire lock for key: " + lockKey);
            }
            log.info("락 획득 성공: {}", lockKey);

            // 트랜잭션 종료 후 락 해제를 등록
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                            log.info("트랜잭션 종료 후 락 해제: {}", lockKey);
                        }
                    }
                });
            }

            // 로직 수행
            return joinPoint.proceed();
        } catch (IllegalStateException e) {
            log.info("락 획득 실패: {}", lockKey);
            throw e;
        } finally {
            if (lockable)
                lock.unlock();
        }
    }
}
