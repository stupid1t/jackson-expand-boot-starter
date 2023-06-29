package com.github.stupdit1t.jackson.expand.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.github.stupdit1t.jackson.expand.annotation.Expand;
import com.github.stupdit1t.jackson.expand.cache.ExpandCache;
import com.github.stupdit1t.jackson.expand.cache.LocalCache;
import com.github.stupdit1t.jackson.expand.config.JacksonExpandProperties;
import com.github.stupdit1t.jackson.expand.domain.ExpandStrategy;
import com.github.stupdit1t.jackson.expand.domain.SerializerParam;
import com.github.stupdit1t.jackson.expand.handler.params.ParamsHandler;
import com.github.stupdit1t.jackson.expand.handler.rsp.ResponseHandler;
import com.github.stupdit1t.jackson.expand.util.ReflectUtil;
import com.github.stupdit1t.jackson.expand.util.SpringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletRequest;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

public class ExpandSerializer extends JsonSerializer<Object> implements ContextualSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(ExpandSerializer.class);

    /**
     * 成功数据
     */
    public static final String OK = "OK";

    /**
     * 失败数据
     */
    public static final String FAIL = "FAIL";

    /**
     * 缓存
     */
    private static ExpandCache cache;

    /**
     * 配置
     */
    private static JacksonExpandProperties jacksonExpandProperties;

    /**
     * 本地锁缓存，防止同时查询
     */
    private static final LocalCache lockCache = new LocalCache();

    /**
     * 远程调用服务
     */
    private Object loadService;

    /**
     * 方法
     */
    private String method;

    /**
     * 注解参数处理
     */
    private SerializerParam params;

    /**
     * 返回结果处理类
     */
    private ParamsHandler paramsHandler;

    /**
     * 返回结果处理类
     */
    private ResponseHandler responseHandler;

    /**
     * bean名称
     */
    private String beanName;

    public ExpandSerializer() {
        super();
        if (cache == null) {
            synchronized (ExpandSerializer.class) {
                if (cache == null) {
                    cache = SpringUtil.getBean(ExpandCache.class);
                    jacksonExpandProperties = SpringUtil.getBean(JacksonExpandProperties.class);
                }
            }
        }
    }

    public ExpandSerializer(String beanName, String method, SerializerParam params, ParamsHandler paramsHandler, ResponseHandler otherResponseHandler) {
        this();
        this.loadService = SpringUtil.getBean(beanName);
        this.method = method;
        this.params = params;
        this.responseHandler = otherResponseHandler;
        this.paramsHandler = paramsHandler;
        this.beanName = beanName;
    }

    @Override
    public void serialize(Object bindData, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String writeFieldPath = getFieldPath(gen.getOutputContext());

        // 统一path替换
        String dynamicExpandCommonPrefix = jacksonExpandProperties.getDynamicExpandCommonPrefix();
        if (StringUtils.hasText(dynamicExpandCommonPrefix) && writeFieldPath.startsWith(dynamicExpandCommonPrefix)) {
            writeFieldPath = writeFieldPath.substring(dynamicExpandCommonPrefix.length() + 1);
        }

        // 是否展开
        boolean expand;
        // 动态展开开启，判断是否展开
        boolean dynamicExpand = jacksonExpandProperties.isDynamicExpand();
        if (dynamicExpand) {
            Set<String> needExpandField = getParam(jacksonExpandProperties.getDynamicExpandParameterName());
            // 如果代码里设置不展开，动态展开也不生效
            expand = needExpandField.contains(writeFieldPath) && params.isOpen();
        } else {
            expand = params.isOpen();
        }
        if (!expand) {
            gen.writeObject(bindData);
            return;
        }

        // 判断要写入的字段
        String writeField = gen.getOutputContext().getCurrentName();
        if (jacksonExpandProperties.getExpandStrategy() == ExpandStrategy.COVER) {
            writeField = gen.getOutputContext().getCurrentName();
        } else if (jacksonExpandProperties.getExpandStrategy() == ExpandStrategy.COPY) {
            writeField = String.format(jacksonExpandProperties.getCopyStrategyFormat(), gen.getOutputContext().getCurrentName());
        }

        // 自定义要写入的优先级最高
        if (StringUtils.hasText(params.getWriteField())) {
            writeField = params.getWriteField();
        }

        // 设置理论上的响应类型，要不要使用取决于 ResponseHandler 要不要处理，比如只能写入数据对象存在的对象，默认是忽略存不存在
        Class<?> writeClass = null;
        if (params.getWriteField() != null && StringUtils.hasText(params.getWriteField())) {
            Field field = ReflectionUtils.findField(gen.getCurrentValue().getClass(), params.getWriteField());
            if (field != null) {
                writeClass = field.getType();
            }
        }

        // 关闭不存在字段扩展，被写入的字段类型找不到，不扩展
        if (!jacksonExpandProperties.isCanExpandToNotExistField() && writeClass == null) {
            gen.writeObject(bindData);
            return;
        }


        // 翻译为非当前字段，先写入当前字段值再翻译
        boolean currField = gen.getOutputContext().getCurrentName().equals(writeField);
        if (!currField) {
            gen.writeObject(bindData);
            gen.writeFieldName(writeField);
        }
        if (bindData == null || loadService == null) {
            gen.writeObject(bindData);
            return;
        }

        // 获取缓存KEY
        Object[] args = params.getRemoteParams();
        int argsLength = args == null ? 0 : args.length;
        String cacheKey = jacksonExpandProperties.getCachePrefix() + ":" + beanName + ":" + method + ":%s:" + paramsHandler.getCacheKey(bindData, args);
        Object result = getCacheInfo(cacheKey);
        if (result != null) {
            LOG.info("{} Expand cache 命中: {}", beanName, result);
            gen.writeObject(result);
            return;
        }

        StampedLock lock = lockCache.get(cacheKey, new StampedLock(), Duration.ofSeconds(300));
        // 写锁避免同一业务ID重复查询
        long stamp = lock.writeLock();
        Integer cacheTime = params.getCacheTime();
        try {
            // 多参数组装
            Object[] objectParams = new Object[argsLength + 1];
            objectParams[0] = paramsHandler.handleVal(bindData);
            if(objectParams.length > 1){
                System.arraycopy(args, 0, objectParams, 1, argsLength);
            }
            // 请求翻译结果
            Object loadResult = ReflectUtil.invoke(loadService, method, objectParams);
            if (loadResult != null) {
                result = this.responseHandler.handle(this.beanName, method, loadResult, writeClass, objectParams);
                cache.put(String.format(cacheKey, OK), result, Duration.ofSeconds(cacheTime));
            } else {
                LOG.error("【{}】 Expand失败，未找到：{}", beanName, bindData);
                cache.put(String.format(cacheKey, FAIL), bindData, Duration.ofSeconds(cacheTime));
                result = bindData;
            }

        } catch (Exception e) {
            LOG.error("【{}】 Expand异常：", beanName, e);
            cache.put(String.format(cacheKey, FAIL), bindData, Duration.ofSeconds(cacheTime));
            result = bindData;
        } finally {
            lock.unlockWrite(stamp);
        }
        gen.writeObject(result);
    }

    /**
     * 获取当前字段的path路径
     *
     * @param outputContext
     * @return
     */
    private String getFieldPath(JsonStreamContext outputContext) {
        List<String> path = new ArrayList<>(4);
        while (outputContext != null) {
            String currentName = outputContext.getCurrentName();
            if (StringUtils.hasText(currentName)) {
                path.add(currentName);
            }
            outputContext = outputContext.getParent();
        }
        Collections.reverse(path);
        return String.join(".", path);
    }

    /**
     * 获取厍信息
     *
     * @param cacheKey 缓存的KEY
     * @return
     */
    private Object getCacheInfo(String cacheKey) {
        Object result = cache.get(String.format(cacheKey, OK));
        if (result == null) {
            result = cache.get(String.format(cacheKey, FAIL));
        }
        return result;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        if (property != null) {
            Expand load = property.getAnnotation(Expand.class);
            if (load == null) {
                throw new RuntimeException("未注解相关 @Expand 注解");
            }
            String bean = load.bean();
            Class<? extends ParamsHandler> paramsHandlerClass = load.paramsHandler();
            Class<? extends ResponseHandler> responseHandlerClass = load.responseHandler();
            String method = load.method();
            try {
                ParamsHandler paramsHandler = paramsHandlerClass.getDeclaredConstructor().newInstance();
                ResponseHandler responseHandler = responseHandlerClass.getDeclaredConstructor().newInstance();
                int cacheTime = load.cacheTime();
                // 额外参数处理
                SerializerParam params = paramsHandler.handleAnnotation(property);
                // 参数处理器没设置，且父注设置了，以父注解为主
                if (params.getCacheTime() == null && cacheTime != -1) {
                    params.setCacheTime(cacheTime);
                }
                // 缓存时间未设置，取默认
                if (params.getCacheTime() == null) {
                    params.setCacheTime(jacksonExpandProperties.getCacheTimeout());
                }
                if (params.isOpen() == null) {
                    params.setExpand(load.expand());
                }
                return new ExpandSerializer(bean, method, params, paramsHandler, responseHandler);
            } catch (Exception e) {
                LOG.error("@Expand error: ", e);
            }
        }
        return prov.findNullValueSerializer(null);
    }

    /**
     * 获取展开参数
     *
     * @param key
     * @return
     */
    private Set<String> getParam(String key) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Collections.emptySet();
        }
        ServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        String[] parameterValues = request.getParameterValues(key);
        if (parameterValues == null) {
            return Collections.emptySet();
        }
        return Arrays.stream(parameterValues).flatMap(o -> Arrays.stream(o.split(",")))
                .collect(Collectors.toSet());
    }
}


