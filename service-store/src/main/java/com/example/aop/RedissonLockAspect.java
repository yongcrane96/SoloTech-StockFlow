package com.example.aop;

import com.example.util.KeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.example.annotations.RedissonLock; // 이 import만 남깁니다.

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RedissonLockAspect {

    private final RedissonClient redissonClient;

    @Around("@annotation(com.example.annotations.RedissonLock)")
    public Object redissonLock(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RedissonLock annotation = method.getAnnotation(RedissonLock.class);

        String lockKey = KeyResolver.resolve(annotation.value(), signature.getParameterNames(), joinPoint.getArgs());
        RLock lock = redissonClient.getLock(lockKey);

        boolean lockable = false;
        boolean txLock = false;
        try {
            // 락 획득 시도
            lockable = lock.tryLock(annotation.waitTime(), annotation.leaseTime(), TimeUnit.MILLISECONDS);
            log.info("[RedissonLock] 획득 시도 - name: {}, locked: {}, lockable: {}", lock.getName(), lock.isLocked(), lockable);

            if (!lockable) {
                throw new IllegalStateException("락 획득 실패: " + lockKey);
            }
            log.info("[RedissonLock]  락 획득 성공: {}", lockKey);

            // 트랜잭션 종료 후 락 해제를 플래그가 true이고 트랜잭션이 활성화된 경우라면
            if (annotation.transactional() && TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSync(lock, lockKey));
                txLock = true;
            }

            // 로직 수행
            return joinPoint.proceed();

        } catch (Exception e) {
            log.info("[RedissonLock] 락 획득 실패: {}", lockKey);
            log.info("[RedissonLock] 예외 발생 - : {}", e.getMessage(), e);
            throw e;

        } finally {
            // 트랜잭션 종료 후 해제 설정이 아닐 경우, 직접 해제
            boolean shouldUnlockNow = lockable &&
                    (!annotation.transactional() || !TransactionSynchronizationManager.isSynchronizationActive() && txLock);

                if(shouldUnlockNow && lock.isHeldByCurrentThread()){
                    lock.unlock();
                    log.info("[RedissonLock] 즉시 락 해제 : {}", lockKey);
                }
        }
    }
}
