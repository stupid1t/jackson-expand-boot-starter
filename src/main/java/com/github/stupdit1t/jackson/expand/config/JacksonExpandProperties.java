package com.github.stupdit1t.jackson.expand.config;

import com.github.stupdit1t.jackson.expand.domain.ExpandStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(JacksonExpandProperties.class)
@ConfigurationProperties(
        prefix = "spring.jackson.expand"
)
public class JacksonExpandProperties {

    /**
     * 缓存Key前缀
     */
    private String cachePrefix = "Expand";

    /**
     * 缓存时间
     * <p>
     * 单位秒
     */
    private Integer cacheTimeout = 300;

    /**
     * 是否要动态展开，如果true。则通过接口url传参进行展开，默认不展开。
     * 如果代码里设置不展开，动态展开也不生效
     * <p>
     * 如传参 /api/user?expand=userId,father.id
     * <p>
     * 则会展开
     */
    private boolean dynamicExpand;

    /**
     * 动态展开参数名字, URL 接受的参数
     */
    private String dynamicExpandParameterName = "expand";


    /**
     * 动态展开 统一数据的Path前缀，比如前缀是 data.body. 如果配置 expand=userId, 相当于是expnad=data.body.userId, 默认无
     */
    private String dynamicExpandCommonPrefix;

    /**
     * 展开策略, 默认覆盖
     */
    private ExpandStrategy expandStrategy = ExpandStrategy.COVER;

    /**
     * 复制字段策略，格式化
     */
    private String copyStrategyFormat = "$%s";

    /**
     * 可以扩展到不存在的字段
     */
    private boolean canExpandToNotExistField = true;

    public String getCachePrefix() {
        return cachePrefix;
    }

    public void setCachePrefix(String cachePrefix) {
        this.cachePrefix = cachePrefix;
    }

    public Integer getCacheTimeout() {
        return cacheTimeout;
    }

    public void setCacheTimeout(Integer cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }

    public boolean isDynamicExpand() {
        return dynamicExpand;
    }

    public void setDynamicExpand(boolean dynamicExpand) {
        this.dynamicExpand = dynamicExpand;
    }

    public String getDynamicExpandParameterName() {
        return dynamicExpandParameterName;
    }

    public void setDynamicExpandParameterName(String dynamicExpandParameterName) {
        this.dynamicExpandParameterName = dynamicExpandParameterName;
    }

    public ExpandStrategy getExpandStrategy() {
        return expandStrategy;
    }

    public void setExpandStrategy(ExpandStrategy expandStrategy) {
        this.expandStrategy = expandStrategy;
    }

    public String getCopyStrategyFormat() {
        return copyStrategyFormat;
    }

    public void setCopyStrategyFormat(String copyStrategyFormat) {
        this.copyStrategyFormat = copyStrategyFormat;
    }

    public boolean isCanExpandToNotExistField() {
        return canExpandToNotExistField;
    }

    public void setCanExpandToNotExistField(boolean canExpandToNotExistField) {
        this.canExpandToNotExistField = canExpandToNotExistField;
    }

    public String getDynamicExpandCommonPrefix() {
        return dynamicExpandCommonPrefix;
    }

    public void setDynamicExpandCommonPrefix(String dynamicExpandCommonPrefix) {
        this.dynamicExpandCommonPrefix = dynamicExpandCommonPrefix;
    }
}
