package SoloTech.StockFlow.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useClusterServers()
                .addNodeAddress(
                        "redis://127.0.0.1:7001",
                        "redis://127.0.0.1:7002",
                        "redis://127.0.0.1:7003",
                        "redis://127.0.0.1:7004",
                        "redis://127.0.0.1:7005",
                        "redis://127.0.0.1:7006"
                )
                .setScanInterval(2000); // 2초 간격으로 연결 재시도 설정
        config.setLockWatchdogTimeout(30000); // 락 자동 해제 시간 설정
        
        return Redisson.create(config);
    }

}
