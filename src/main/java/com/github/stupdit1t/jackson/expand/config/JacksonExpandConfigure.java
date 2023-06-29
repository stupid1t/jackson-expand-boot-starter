package com.github.stupdit1t.jackson.expand.config;

import com.github.stupdit1t.jackson.expand.cache.ExpandCache;
import com.github.stupdit1t.jackson.expand.cache.LocalCache;
import com.github.stupdit1t.jackson.expand.util.SpringUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

public class JacksonExpandConfigure {

    @Bean
    public SpringUtil springUtil() {
        return new SpringUtil();
    }

    /**
     * jackson  配置
     *
     * @return
     */
    @Bean
    public JacksonExpandProperties jacksonExpandProperties() {
        return new JacksonExpandProperties();
    }

    /**
     * 默认缓存机制， 本地缓存
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public ExpandCache coverCache() {
        return new LocalCache();
    }
}
