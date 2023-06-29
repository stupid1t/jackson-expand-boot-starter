# jackson-expand

<img alt="GitHub code size in bytes" src="https://img.shields.io/github/languages/code-size/stupdit1t/jackson-expand-boot-starter">
<a target="_blank" href="LICENSE"><img src="https://img.shields.io/:license-MIT-blue.svg"></a>
<a target="_blank" href="https://www.oracle.com/technetwork/java/javase/downloads/index.html"><img src="https://img.shields.io/badge/JDK-1.8+-green.svg" /></a>
<a target="_blank" href='https://github.com/stupdit1t/jackson-expand-boot-starter'><img src="https://img.shields.io/github/stars/stupdit1t/jackson-expand-boot-starter.svg?style=social"/>
<a href='https://gitee.com/stupid1t/jackson-expand/stargazers'><img src='https://gitee.com/stupid1t/jackson-expand/badge/star.svg?theme=white' alt='star'></img></a>

基于jackson，接口层的数据展开操作，使用环境是spring, 不依赖任何第三方包

* 使用场景

> 减少对于实时性不高的数据的关联查询，比如创建人姓名，字典翻译等功能。

1. 无代码侵入，引入无感知，只需要在展开的字段上打jackson注解
2. 支持自定义扩展展开行为，如字典，枚举，RPC，feign等等，只要能在spring中的容器做就可以
3. 支持缓存扩展，默认使用内存缓存，可自行扩展redis、caffeine等其它缓存。缓存配置采用Spring的SPI机制

* maven 坐标

```xml
 <!-- 翻译模块  -->
<dependency>
    <groupId>com.github.stupdit1t</groupId>
    <artifactId>jackson-expand-boot-starter</artifactId>
    <version>1.1.1</version>
</dependency>
```

# 快速入门
1. 引入maven依赖
2. 在需要展开 or 翻译的字段上打@Expand注解
> 原理是序列化的时候会带着`creater`的值，去请求`sysUserServiceImpl`的`expand`方法。
```java
@Data
public class SysCodeRuleInfoVO implements Serializable {

    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("创建人")
    @Expand(bean = "sysUserServiceImpl")
    private Long creater;
    
    @ApiModelProperty("删除标志")
    private String deleted;
}
```
原始json

```json
{
  "id": 1,
  "creater": 1,
  "deleted": 1
}
```

展开后的json

```json
{
  "id": 1,
  "creater": {
    "id": 1,
    "name": "张三"
  },
  "deleted": 1
}

```


# 进阶使用
1. 减少每次需要展开参数的代码量，实现自定义 `@ExpandUser` 注解。
```java
/**
 * 翻译用户ID注解自定义
 *
 * @author 625
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
@JacksonAnnotationsInside
@Expand(bean = "sysUserServiceImpl", paramsHandler = UserParamsHandler.class)
public @interface ExpandUser {

   /**
    * 回显到字段, 填写了已填写的为准
    *
    * @return
    */
   String to() default "";

   /**
    * 是否包含多个值。比如字段值为1,2,3 ，如果mul=true， 则展开为集合
    *
    * @return
    */
   boolean mul() default false;
}
```

2.自定义参数处理类
```java
/**
 * 参数处理类
 */
public class UserParamsHandler implements ParamsHandler {

    /**
     * 处理被注解标注的字段当前值
     *
     * @param params
     * @return
     */
    @Override
    public Object handleVal(Object params) {
        if (ObjectUtil.isEmpty(params)) {
            return null;
        }
        String paramsStr = String.valueOf(params);
        return paramsStr;
    }

    /**
     * 处理当前注解的值
     *
     * @param property
     * @return
     */
    @Override
    public SerializerParam handleAnnotation(BeanProperty property) {
        SerializerParam params = new SerializerParam();
        // 用户注解值处理
        ExpandUser loadUser = property.getAnnotation(ExpandUser.class);
        if (loadUser != null) {
            // 反射Bean参数， 默认第一个是注解标注的值，不需要指定，只需要指定自定义的注解参数
            params.setRemoteParams(new Object[]{loadUser.mul()});
            if (!"".equals(loadUser.to())) {
                params.setWriteField(loadUser.to());
            }
        }
        return params;
    }
}
```
3. `expand`展开类
```java
/**
 * 展开 数据方法
 */
public class SysUserServiceImpl {

    /**
     * 处理数据
     *
     * @param userIds 注解标记字段的值
     * @param mul 自定义扩展参数值
     * @return
     */
    public Object expand(String userIds, boolean mul) {
        List<SimpleUserInfoVO> userInfos = baseMapper.load(userIds.split(","));
        if (!mul) {
            return userInfos.isEmpty() ? null : userInfos.get(0);
        } else {
            return userInfos;
        }
    }
}
```

4. 展开数据测试
```java
@Data
public class SysCodeRuleInfoVO implements Serializable {

    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("创建人")
    @ExpandUser
    private Long creater;

   /**
    * 假设是多个删除人
    */
    @ApiModelProperty("删除人")
    @ExpandUser(mul = true)
    private String updater;
}
```
响应结果
```json
{
  "id": 1,
  "creater": {
     "id": 1,
     "name": "张三"
  },
  "updater": [
     {
        "id": 1,
        "name": "张二"
     },
     {
        "id": 2,
        "name": "张三"
     }
  ]
}
```

# 缓存更换方法
> 默认为本地内存缓存，扩展为Redis 或其他缓存的方法

1. 自定义实现缓存管理类，注册为Spring的Bean对象。即可

```java
import org.springframework.stereotype.Component;

@Component
public class CustomCovertCache implements ExpandCache {

    @Override
    public <T> void put(String key, T value, Integer timeout) {

    }

    @Override
    public <T> T get(String key) {
        return null;
    }
    
    @Override
    public void delete(String key){
    }

    @Override
    public Set<String> keys(String key){
        return new HashSet<>();
    }
    
    @Override
    public void clear() {

    }
}
```

# 更多spring配置说明

```yaml
spring:
  jackson:
    expand:
      # 是否动态展开，通过接口传参，控制是否要展开字段，默认false
      dynamic-expand: true
      # 动态展开接收参数字段名称，默认expand
      dynamic-expand-parameter-name: expand
      # 动态展开 统一数据的Path前缀，比如前缀是 data.body. 如果配置 expand=userId, 相当于是expnad=data.body.userId, 默认无
      dynamic-expand-common-prefix: data.body
      # 缓存Key 前缀，默认Expand:
      cache-prefix: expand
      # 缓存时间，单位秒, 默认300
      cache-timeout: 300
      # 展开策略, 可选COVER，COPY。覆盖如果有反序列化冲突可选COPY或者指定字段自定义字段策略，默认COVER覆盖
      expand-strategy: copy
      # COPY策略，COPY字段格式，默认$%s
      copy-strategy-format: $_%s
      # 可以扩展到不存在的字段, 设置为false表示被扩展到的字段必须存在, 默认true
      can-expand-to-not-exist-field: true
```

# 动态展开使用方法

1. 设置动态展开`spring.jackson.expand.dynamic-expand=true`打开，（如果@Expand注解的`expand=false`，那即使动态展开打开了，接口传参了依然无法展开）
2. 编写代码时，在需要展开的字段上添加@Expand注解，并实现相关的展开数据方法
3. 调用接口传参，如需要展开多个字段，传参为`/api/users?expand=inUser,editUser,fater.inUser,fater.fater.inUser`, 示例原始json如下

```json
{
  "id": -1,
  "name": "无名",
  "inUser": 1,
  "father": 2
}
```

* 展开1个字段`/api/users?expand=inUser`，json如下

```json
{
  "id": -1,
  "name": "无名",
  "inUser": {
    "id": 1,
    "name": "儿子",
    "inUser": 666
  },
  "father": 2
}

```

* 展开2字段`/api/users?expand=inUser,father`，json如下

```json
{
  "id": -1,
  "name": "无名",
  "inUser": {
    "id": 1,
    "name": "儿子",
    "inUser": 666
  },
  "father": {
    "id": 2,
    "name": "爸爸",
    "inUser": 666
  }
}
```

* 展开3字段`/api/users?expand=inUser,father,father.inUser`，json如下

```json
{
  "id": -1,
  "name": "无名",
  "inUser": {
    "id": 1,
    "name": "儿子",
    "inUser": 666
  },
  "father": {
    "id": 2,
    "name": "爸爸",
    "inUser": {
      "id": 1,
      "name": "儿子",
      "inUser": 666
    }
  }
}
```

* 展开4字段`/api/users?expand=inUser,father,father.inUser,father.inUser.inUser`，json如下

```json
{
  "id": -1,
  "name": "无名",
  "inUser": {
    "id": 1,
    "name": "儿子",
    "inUser": 666
  },
  "father": {
    "id": 2,
    "name": "爸爸",
    "inUser": {
      "id": 3,
      "name": "苍天",
      "inUser": {
        "id": 3,
        "name": "苍天",
        "inUser": 666
      }
    }
  }
}
```
