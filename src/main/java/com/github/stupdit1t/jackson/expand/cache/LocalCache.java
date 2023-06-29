package com.github.stupdit1t.jackson.expand.cache;

import com.github.stupdit1t.jackson.expand.serializer.ExpandSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 简单的本地缓存实现
 */
public class LocalCache implements ExpandCache {

    private static final Logger LOG = LoggerFactory.getLogger(ExpandSerializer.class);

    private static final Timer timer = new Timer();

    private final Map<String, Object> cacheMap;

    public LocalCache() {
        this.cacheMap = new ConcurrentHashMap<>();
    }

    public LocalCache(Map<String, Object> cacheMap) {
        this.cacheMap = cacheMap;
    }

    @Override
    public <T> void put(String key, T value, Duration timeout) {
        cacheMap.put(key, value);
        scheduleExpiration(key, timeout.toMillis());
    }

    @Override
    public <T> T get(String key) {
        return (T) cacheMap.get(key);
    }

    public <T> T get(String cacheKey, T value, Duration timeout) {
        T val = (T) cacheMap.computeIfAbsent(cacheKey, (key) -> {
            scheduleExpiration(cacheKey, timeout.toMillis());
            return value;
        });
        return val;
    }

    @Override
    public Set<String> keys(String pattern) {
        return cacheMap.keySet().stream()
                .filter(key -> matchKey(pattern, key))
                .collect(Collectors.toSet());
    }

    @Override
    public void clear() {
        this.cacheMap.clear();
        timer.purge();
    }

    @Override
    public void delete(String key) {
        this.cacheMap.remove(key);
    }

    /**
     * 计时到期
     *
     * @param key
     * @param expirationTimeMillis
     */
    private void scheduleExpiration(String key, long expirationTimeMillis) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                LOG.info("缓存KEY失效:{}", key);
                cacheMap.remove(key);
            }
        }, expirationTimeMillis);
    }
}
