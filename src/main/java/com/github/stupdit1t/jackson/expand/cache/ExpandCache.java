package com.github.stupdit1t.jackson.expand.cache;

import com.github.stupdit1t.jackson.expand.config.JacksonExpandProperties;
import com.github.stupdit1t.jackson.expand.util.SpringUtil;
import com.github.stupdit1t.jackson.expand.serializer.ExpandSerializer;
import org.springframework.util.AntPathMatcher;

import java.time.Duration;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 缓存抽象
 */
public interface ExpandCache {

    /**
     * 放入缓存
     *
     * @param key
     * @param value
     * @param <T>
     */
    <T> void put(String key, T value, Duration timeout);

    /**
     * 获取缓存
     *
     * @param key
     * @return
     */
    <T> T get(String key);

    /**
     * 列出匹配的的key
     *
     * @param pattern
     * @return
     */
    Set<String> keys(String pattern);

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 删除缓存
     *
     * @param key
     */
    void delete(String key);

    /**
     * 按照bean删除缓存
     */
    default void delete(String beanName, String method, Object bindData, Object... annotationVal) {
        JacksonExpandProperties properties = SpringUtil.getBean(JacksonExpandProperties.class);
        StringJoiner key = new StringJoiner("-");
        key.add(String.valueOf(bindData));
        for (Object subVal : annotationVal) {
            key.add(String.valueOf(subVal));
        }
        String cacheKey = properties.getCachePrefix() + ":" + beanName + ":" + method + ":%s:" + key.toString();
        delete(String.format(cacheKey, ExpandSerializer.OK));
        delete(String.format(cacheKey, ExpandSerializer.FAIL));
    }


    /**
     * 模糊匹配key
     *
     * @param pattern
     * @param key
     */
    default boolean matchKey(String pattern, String key) {
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        // *
        if ("*".equals(pattern)) {
            return true;
        }
        // h?llo
        if (pattern.contains("?")) {
            if (antPathMatcher.match(pattern, key)) {
                return true;
            }
        }
        // h*llo
        if (pattern.contains("*")) {
            if (antPathMatcher.match(pattern, key)) {
                return true;
            }
        }
        // h[ae]llo
        if (pattern.contains("[") && pattern.contains("]")) {
            return key.matches(pattern);
        }
        return false;
    }

}
