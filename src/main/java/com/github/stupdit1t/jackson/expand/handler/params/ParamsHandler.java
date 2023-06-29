package com.github.stupdit1t.jackson.expand.handler.params;

import com.fasterxml.jackson.databind.BeanProperty;
import com.github.stupdit1t.jackson.expand.domain.SerializerParam;

import java.util.StringJoiner;

/**
 * 参数处理类
 */
public interface ParamsHandler {

    /**
     * 处理注解对象的值
     *
     * @param val
     * @return
     */
    Object handleVal(Object val);

    /**
     * 处理注解上的参数
     *
     * @param property
     * @return
     */
    SerializerParam handleAnnotation(BeanProperty property);

    /**
     * 获取缓存的key
     *
     * @param val           当前值
     * @param annotationVal 注解值
     * @return
     */
    default String getCacheKey(Object val, Object[] annotationVal) {
        if (annotationVal == null) {
            return val.toString();
        }
        StringJoiner key = new StringJoiner("-");
        key.add(String.valueOf(val));
        for (Object subVal : annotationVal) {
            key.add(String.valueOf(subVal));
        }
        return key.toString();
    }
}
