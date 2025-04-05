package SoloTech.StockFlow.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheSubscriber implements MessageListener {

    private final Cache<String, Object> localCache;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        StringRedisSerializer serializer = new StringRedisSerializer();
        String body = serializer.deserialize(message.getBody());

        log.info("Received message: {}", body);

        if(body == null){
            log.warn("Received null message from Redis Pub/Sub");
            return;
        }

        log.info("Received message : {}", body);

        String[] parts = body.split("-", 2);
        if(parts.length < 2){
            log.warn("Invalid cache update message format: {}", body);
            return;
        }


        String cachedKey = parts[1];

        redisTemplate.delete(cachedKey);
        localCache.invalidate(cachedKey);

        log.info("Invalidated caches for stock: [{}]", cachedKey);
    }
}
