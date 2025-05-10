package com.example.cache;

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

        assert body != null;
        if (body.contains("Updated stock-") || body.contains("Deleted stock-")) {
            String cachedKey = body.split("-", 2)[1];

            log.info(cachedKey);
            localCache.invalidate(cachedKey);
            redisTemplate.delete(cachedKey);
            log.info("Invalidated local cache for store: [{}]", cachedKey);
        }
    }
}
