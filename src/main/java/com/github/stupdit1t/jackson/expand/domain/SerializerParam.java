package com.github.stupdit1t.jackson.expand.domain;

/**
 * 注解处理结果
 */
public class SerializerParam {

    /**
     * 远程调用额外参数
     */
    private Object[] remoteParams;

    /**
     * 写入字段
     */
    private String writeField;

    /**
     * 值缓存时间
     */
    private Integer cacheTime;

    /**
     * 是否展开
     */
    private Boolean expand;

    public Object[] getRemoteParams() {
        return remoteParams;
    }

    public void setRemoteParams(Object[] remoteParams) {
        this.remoteParams = remoteParams;
    }

    public String getWriteField() {
        return writeField;
    }

    public void setWriteField(String writeField) {
        this.writeField = writeField;
    }

    public Integer getCacheTime() {
        return cacheTime;
    }

    public void setCacheTime(Integer cacheTime) {
        this.cacheTime = cacheTime;
    }

    public Boolean isOpen() {
        return expand;
    }

    public void setExpand(Boolean expand) {
        this.expand = expand;
    }
}
