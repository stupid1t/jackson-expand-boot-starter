package com.github.stupdit1t.jackson.expand.handler.rsp;

/**
 * 返回处理第三方响应
 */
public interface ResponseHandler {
    Object handle(String bean, String method, Object rsp, Class<?> writeClass, Object... params);
}
