package com.github.stupdit1t.jackson.expand.util;

import org.springframework.util.ObjectUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectUtil {

    /**
     * 包装类型为Key，原始类型为Value，例如： Integer.class =》 int.class.
     */
    public static final Map<Class<?>, Class<?>> WRAPPER_PRIMITIVE_MAP = new ConcurrentHashMap<>(8);

    static {
        WRAPPER_PRIMITIVE_MAP.put(Boolean.class, boolean.class);
        WRAPPER_PRIMITIVE_MAP.put(Byte.class, byte.class);
        WRAPPER_PRIMITIVE_MAP.put(Character.class, char.class);
        WRAPPER_PRIMITIVE_MAP.put(Double.class, double.class);
        WRAPPER_PRIMITIVE_MAP.put(Float.class, float.class);
        WRAPPER_PRIMITIVE_MAP.put(Integer.class, int.class);
        WRAPPER_PRIMITIVE_MAP.put(Long.class, long.class);
        WRAPPER_PRIMITIVE_MAP.put(Short.class, short.class);
    }

    /**
     * 反射方法缓存
     */
    private static final Map<String, Method> METHODS_CACHE = new ConcurrentHashMap<>();

    /**
     * 反射调用
     *
     * @param obj
     * @param methodName
     * @param args
     * @return
     */
    public static Object invoke(Object obj, String methodName, Object[] args) throws InvocationTargetException, IllegalAccessException {
        String cacheKey = obj.getClass().getName() + methodName;
        final Method method = METHODS_CACHE.computeIfAbsent(cacheKey, (key) -> getMethod(obj.getClass(), methodName, args));
        if (null == method) {
            throw new UnsupportedOperationException("No such method: [" + methodName + "] from [" + obj.getClass() + "]");
        }
        return method.invoke(obj, args);
    }

    /**
     * 获取反射方法
     *
     * @param beanClass
     * @param methodName
     * @param args
     * @return
     */
    private static Method getMethod(Class<?> beanClass, String methodName, Object[] args) {
        List<Method> allMethods = new ArrayList<>();
        Class<?> searchType = beanClass;
        Class<?>[] argsClasses = getClasses(args);
        while (searchType != null) {
            allMethods.addAll(new ArrayList<>(Arrays.asList(searchType.getDeclaredMethods())));
            searchType = searchType.getSuperclass();
        }

        for (Method method : allMethods) {
            if (methodName.equals(method.getName()) && isAllAssignableFrom(method.getParameterTypes(), argsClasses)
                    //排除桥接方法
                    && !method.isBridge()) {
                return method;
            }
        }

        return null;
    }

    /**
     * 获得对象数组的类数组
     *
     * @param objects 对象数组，如果数组中存在{@code null}元素，则此元素被认为是Object类型
     * @return 类数组
     */
    public static Class<?>[] getClasses(Object... objects) {
        Class<?>[] classes = new Class<?>[objects.length];
        Object obj;
        for (int i = 0; i < objects.length; i++) {
            obj = objects[i];
            if (null == obj) {
                classes[i] = Object.class;
            } else {
                classes[i] = obj.getClass();
            }
        }
        return classes;
    }

    /**
     * 比较判断types1和types2两组类，如果types1中所有的类都与types2对应位置的类相同，或者是其父类或接口，则返回{@code true}
     *
     * @param types1 类组1
     * @param types2 类组2
     * @return 是否相同、父类或接口
     */
    private static boolean isAllAssignableFrom(Class<?>[] types1, Class<?>[] types2) {
        if (ObjectUtils.isEmpty(types1) && ObjectUtils.isEmpty(types2)) {
            return true;
        }
        if (null == types1 || null == types2) {
            // 任何一个为null不相等（之前已判断两个都为null的情况）
            return false;
        }
        if (types1.length != types2.length) {
            return false;
        }

        Class<?> type1;
        Class<?> type2;
        for (int i = 0; i < types1.length; i++) {
            type1 = types1[i];
            type2 = types2[i];
            if (isBasicType(type1) && isBasicType(type2)) {
                // 原始类型和包装类型存在不一致情况
                if (unWrap(type1) != unWrap(type2)) {
                    return false;
                }
            } else if (!type1.isAssignableFrom(type2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 是否为基本类型（包括包装类和原始类）
     *
     * @param clazz 类
     * @return 是否为基本类型
     */
    private static boolean isBasicType(Class<?> clazz) {
        if (null == clazz) {
            return false;
        }
        return (clazz.isPrimitive() || WRAPPER_PRIMITIVE_MAP.containsKey(clazz));
    }

    /**
     * 包装类转为原始类，非包装类返回原类
     *
     * @param clazz 包装类
     * @return 原始类
     */
    private static Class<?> unWrap(Class<?> clazz) {
        if (null == clazz || clazz.isPrimitive()) {
            return clazz;
        }
        Class<?> result = WRAPPER_PRIMITIVE_MAP.get(clazz);
        return (null == result) ? clazz : result;
    }
}
