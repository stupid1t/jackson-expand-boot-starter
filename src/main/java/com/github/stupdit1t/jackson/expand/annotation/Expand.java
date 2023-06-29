package com.github.stupdit1t.jackson.expand.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.stupdit1t.jackson.expand.handler.params.DefaultParamsHandler;
import com.github.stupdit1t.jackson.expand.handler.params.ParamsHandler;
import com.github.stupdit1t.jackson.expand.handler.rsp.DefaultResponseHandler;
import com.github.stupdit1t.jackson.expand.handler.rsp.ResponseHandler;
import com.github.stupdit1t.jackson.expand.serializer.ExpandSerializer;

import java.lang.annotation.*;

/**
 * 展开注解
 *
 * @author 625
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Inherited
@JacksonAnnotationsInside
@JsonSerialize(using = ExpandSerializer.class)
public @interface Expand {

    /**
     * spring操的Bean-name
     *
     * @return
     */
    String bean();

    /**
     * 数据展开方法
     *
     * @return
     */
    String method() default "expand";

    /**
     * 回显到字段, 填写了已填写的为准, 否则自动填充当前字段
     *
     * @return
     */
    String to() default "";

    /**
     * 值缓存时间， 单位秒
     *
     * @return
     */
    int cacheTime() default -1;

    /**
     * 是否要展开
     *
     * @return
     */
    boolean expand() default true;

    /**
     * 参数处理器
     *
     * @return
     */
    Class<? extends ParamsHandler> paramsHandler() default DefaultParamsHandler.class;

    /**
     * 返回结果处理类
     *
     * @return
     */
    Class<? extends ResponseHandler> responseHandler() default DefaultResponseHandler.class;
}
