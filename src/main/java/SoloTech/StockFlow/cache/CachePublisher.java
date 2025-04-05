package SoloTech.StockFlow.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CachePublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(String channel, String message){
        redisTemplate.convertAndSend(channel, message);
        log.info("Published message: {}", message);
    }
}
