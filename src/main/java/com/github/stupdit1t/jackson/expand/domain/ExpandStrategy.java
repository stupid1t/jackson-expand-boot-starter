package com.github.stupdit1t.jackson.expand.domain;

/**
 * 展开策略
 */
public enum ExpandStrategy {

    /**
     * 覆盖当前字段
     */
    COVER,

    /**
     * 复制字段
     */
    COPY,
}
