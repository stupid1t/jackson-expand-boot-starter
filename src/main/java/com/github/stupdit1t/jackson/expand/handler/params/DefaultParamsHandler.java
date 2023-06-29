package com.github.stupdit1t.jackson.expand.handler.params;

import com.fasterxml.jackson.databind.BeanProperty;
import com.github.stupdit1t.jackson.expand.annotation.Expand;
import com.github.stupdit1t.jackson.expand.domain.SerializerParam;
import org.springframework.util.StringUtils;

/**
 * 单值long参数处理器
 * <p>
 * 如你的字段类型是Long，且直接就是参数，不需要处理，用这个
 */
public class DefaultParamsHandler implements ParamsHandler {

    @Override
    public Object handleVal(Object val) {
        return val;
    }

    @Override
    public SerializerParam handleAnnotation(BeanProperty property) {
        SerializerParam params = new SerializerParam();
        // 用户注解值处理
        Expand expand = property.getAnnotation(Expand.class);
        if (expand != null) {
            if (StringUtils.hasText(expand.to())) {
                params.setWriteField(expand.to());
            }
            params.setExpand(expand.expand());
        }
        return params;
    }
}
