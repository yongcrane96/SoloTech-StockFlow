package com.example.util;

import com.example.cache.CacheType;

public class CacheKeyUtil {
    /**
     * prefix + key 조합
     */
    public static String buildFullKey(String prefix, String key){
        return prefix + key;
    }

    /**
     * 캐시 이벤트 메시지 생성
     * 예: "UPDATE order-user:1"
     */
    public static String buildEventMessage(CacheType eventType, String prefix, String key) {
        return eventType.getLabel() + " " + prefix + key;
    }

    /**
     * 예: "cache-sync"
     */
    public static String getDefaultChannel() {
        return "cache-sync";
    }
}
