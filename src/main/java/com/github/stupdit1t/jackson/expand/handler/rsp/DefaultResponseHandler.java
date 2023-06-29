package com.github.stupdit1t.jackson.expand.handler.rsp;

/**
 * 默认返回值处理，不处理
 */
public class DefaultResponseHandler implements ResponseHandler {

    /**
     * 响应数据处理
     *
     * @param bean   当前bean
     * @param method 当前方法
     * @param rsp    当前返回值
     * @param toClass    要填充字段的类型
     * @param params 当前方法参数
     * @return
     */
    @Override
    public Object handle(String bean, String method, Object rsp, Class<?> toClass, Object... params) {
        return rsp;
    }
}
