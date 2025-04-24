package com.example.aop;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.transaction.support.TransactionSynchronization;

@Slf4j
public class TransactionSync implements TransactionSynchronization {
    private final RLock lock;
    private final String lockKey;

    public TransactionSync(RLock lock, String lockKey){
        this.lock = lock;
        this.lockKey = lockKey;
    }

    @Override
    public void afterCompletion(int status) {
        if (lock.isHeldByCurrentThread()) { // 현재 쓰레드가 락을 소유하고 있는지 확인
            lock.unlock();
            log.info("[TransactionSync] 트랜잭션 종료 후 락 해제: {}", lockKey);
        }
    }

}
