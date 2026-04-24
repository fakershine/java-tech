# MyBatis 插件总结

MyBatis 插件本质上是基于 **拦截器 + 动态代理** 实现的扩展机制，用于在 SQL 执行过程中的关键节点进行增强。

常见用途：

- 分页
- SQL 日志
- 慢 SQL 监控
- 数据权限
- 动态表名
- 字段加解密
- SQL 改写
- 参数处理
- 结果集处理

---

## 1. MyBatis 插件可以拦截哪些对象

MyBatis 插件只能拦截指定的四大核心对象：

| 拦截对象 | 作用 |
|---|---|
| `Executor` | 拦截 SQL 执行过程 |
| `StatementHandler` | 拦截 Statement 创建和 SQL 预处理 |
| `ParameterHandler` | 拦截 SQL 参数设置 |
| `ResultSetHandler` | 拦截结果集映射 |

---

## 2. 四大对象作用

### 1. Executor

`Executor` 是 SQL 执行器，负责执行查询、更新、提交、回滚等操作。

常见拦截方法：

```java
query()
update()
```

适合做：

- SQL 执行监控
- 缓存处理
- 慢 SQL 统计
- 数据权限控制

---

### 2. StatementHandler

`StatementHandler` 负责创建 JDBC Statement，并对 SQL 进行预处理。

常见拦截方法：

```java
prepare()
parameterize()
batch()
update()
query()
```

适合做：

- 分页 SQL 改写
- 动态表名替换
- SQL 打印
- SQL 优化增强

分页插件通常就是拦截 `StatementHandler#prepare()`，然后改写 SQL。

---

### 3. ParameterHandler

`ParameterHandler` 负责给 SQL 中的占位符设置参数。

常见拦截方法：

```java
setParameters()
```

适合做：

- 参数加密
- 参数脱敏
- 参数校验
- 特殊参数转换

---

### 4. ResultSetHandler

`ResultSetHandler` 负责处理数据库返回的结果集，并映射成 Java 对象。

常见拦截方法：

```java
handleResultSets()
```

适合做：

- 结果脱敏
- 字段解密
- 返回结果增强
- 数据转换

---

## 3. 插件实现方式

自定义 MyBatis 插件需要实现 `Interceptor` 接口。

```java
@Intercepts({
    @Signature(
        type = StatementHandler.class,
        method = "prepare",
        args = {Connection.class, Integer.class}
    )
})
public class SqlLogInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 执行前增强
        long start = System.currentTimeMillis();

        Object result = invocation.proceed();

        // 执行后增强
        long cost = System.currentTimeMillis() - start;
        System.out.println("SQL 执行耗时：" + cost + "ms");

        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 读取插件配置参数
    }
}
```

---

## 4. 核心注解说明

### @Intercepts

表示当前类是一个 MyBatis 插件。

```java
@Intercepts({})
```

---

### @Signature

表示要拦截哪个对象的哪个方法。

```java
@Signature(
    type = StatementHandler.class,
    method = "prepare",
    args = {Connection.class, Integer.class}
)
```

参数说明：

| 参数 | 说明 |
|---|---|
| `type` | 要拦截的对象类型 |
| `method` | 要拦截的方法名 |
| `args` | 方法参数类型 |

---

## 5. Interceptor 接口方法

### intercept()

核心拦截逻辑。

```java
public Object intercept(Invocation invocation) throws Throwable
```

在这里可以：

- 修改 SQL
- 修改参数
- 记录日志
- 统计耗时
- 执行前后增强

调用原方法：

```java
invocation.proceed();
```

---

### plugin()

用于生成代理对象。

```java
public Object plugin(Object target) {
    return Plugin.wrap(target, this);
}
```

MyBatis 会对符合拦截条件的目标对象创建动态代理。

---

### setProperties()

用于读取插件配置。

```java
public void setProperties(Properties properties) {
}
```

例如：

```xml
<plugin interceptor="com.demo.SqlLogInterceptor">
    <property name="slowSqlMillis" value="1000"/>
</plugin>
```

---

## 6. MyBatis 插件底层原理

MyBatis 插件底层基于 **责任链 + 动态代理** 实现。

启动时，MyBatis 会把插件保存到 `InterceptorChain` 中。

```text
InterceptorChain
  ↓
Interceptor 1
  ↓
Interceptor 2
  ↓
Interceptor 3
```

当创建 `Executor`、`StatementHandler`、`ParameterHandler`、`ResultSetHandler` 时，会调用：

```java
interceptorChain.pluginAll(target)
```

对目标对象进行层层代理。

---

## 7. 插件执行流程

整体流程：

```text
MyBatis 启动
  ↓
加载插件配置
  ↓
创建 Interceptor
  ↓
加入 InterceptorChain
  ↓
创建四大核心对象
  ↓
判断是否需要代理
  ↓
生成代理对象
  ↓
执行 SQL 时进入插件逻辑
  ↓
调用 invocation.proceed() 执行原方法
```

---

## 8. 插件链执行顺序

如果配置了多个插件，会形成代理链。

```text
PluginA -> PluginB -> PluginC -> 原始对象
```

执行时类似：

```text
PluginA 前置逻辑
  ↓
PluginB 前置逻辑
  ↓
PluginC 前置逻辑
  ↓
原始方法执行
  ↓
PluginC 后置逻辑
  ↓
PluginB 后置逻辑
  ↓
PluginA 后置逻辑
```

注意：

```text
插件顺序可能影响最终结果。
```

例如分页插件和 SQL 改写插件顺序不同，可能导致 SQL 结果不同。

---

## 9. 分页插件实现原理

分页插件通常拦截：

```text
StatementHandler.prepare()
```

原始 SQL：

```sql
SELECT * FROM user WHERE status = 1
```

插件改写后：

```sql
SELECT * FROM user WHERE status = 1 LIMIT 0, 10
```

大致流程：

```text
拦截 StatementHandler
  ↓
获取原始 SQL
  ↓
判断是否需要分页
  ↓
根据数据库类型拼接分页语句
  ↓
替换 BoundSql 中的 SQL
  ↓
继续执行
```

---

## 10. 插件常见应用场景

| 场景 | 常用拦截对象 |
|---|---|
| 分页 | `StatementHandler` |
| SQL 日志 | `StatementHandler` / `Executor` |
| 慢 SQL 监控 | `Executor` |
| 数据权限 | `StatementHandler` |
| 动态表名 | `StatementHandler` |
| 参数加密 | `ParameterHandler` |
| 结果脱敏 | `ResultSetHandler` |
| 多租户 | `StatementHandler` |
| 字段解密 | `ResultSetHandler` |

---

## 11. 插件使用注意点

- 插件只能拦截 MyBatis 指定的四大对象。
- 拦截方法签名必须完全正确。
- 插件会影响 SQL 执行链路，使用不当会影响性能。
- 修改 SQL 时要注意参数映射关系。
- 多个插件同时使用时要注意执行顺序。
- 分页、数据权限、动态表名等插件要重点测试复杂 SQL。
- 插件中不要执行耗时逻辑，避免拖慢所有 SQL。
- 插件异常可能影响整个数据库访问流程。

---

## 12. 总结

MyBatis 插件是 MyBatis 提供的扩展机制，底层基于动态代理和责任链实现。

MyBatis 插件只能拦截四类核心对象：`Executor`、`StatementHandler`、`ParameterHandler` 和 `ResultSetHandler`。

自定义插件需要实现 `Interceptor` 接口，并通过 `@Intercepts` 和 `@Signature` 指定要拦截的对象和方法。插件的核心逻辑写在 `intercept()` 方法中，通过 `invocation.proceed()` 调用原始方法。

MyBatis 启动时会把所有插件加入 `InterceptorChain`，在创建核心对象时通过 `pluginAll()` 对目标对象进行代理。执行 SQL 时，如果命中插件拦截条件，就会进入插件逻辑。

实际项目中，分页插件、数据权限插件、SQL 日志插件、字段加解密插件等都可以通过 MyBatis 插件实现。

一句话总结：

```text
MyBatis 插件 = 拦截四大核心对象 + 动态代理增强 + InterceptorChain 责任链执行。
```
