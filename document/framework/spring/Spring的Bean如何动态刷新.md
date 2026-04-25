# Spring 的 Bean 如何动态刷新

Spring Bean 动态刷新常见有两类：

```text
1. 配置变更后刷新 Bean
2. 运行时动态注册 / 替换 Bean
```

实际项目中更常见的是第一种：**配置中心变更后，通过 @RefreshScope 刷新 Bean**。

---

## 1. 常见实现方式

| 方式 | 核心原理 | 适用场景 |
|---|---|---|
| `@RefreshScope` | 配置变化后清空 Bean 缓存，下次访问重新创建 | 配置中心动态刷新 |
| `EnvironmentChangeEvent` | 发布环境变更事件，通知配置重新绑定 | 配置属性刷新 |
| `ContextRefresher` | 重新加载配置并触发 refresh | Spring Cloud 动态刷新 |
| 动态注册 BeanDefinition | 运行时向容器注册新的 BeanDefinition | 插件化、动态扩展 |
| `registerSingleton` | 运行时注册单例对象 | 简单动态对象注册 |

---

## 2. @RefreshScope 刷新 Bean

### 实现原理

`@RefreshScope` 是 Spring Cloud 提供的动态刷新能力。

被 `@RefreshScope` 标记的 Bean 不会像普通单例 Bean 一样一直固定不变，而是通过代理对象访问。

核心流程：

```text
Bean 被 @RefreshScope 标记
  ↓
Spring 创建代理对象
  ↓
第一次调用时创建真实 Bean
  ↓
配置发生变化
  ↓
RefreshScope 清空目标 Bean 缓存
  ↓
下次调用代理对象
  ↓
重新创建真实 Bean，并注入最新配置
```

Spring Cloud 官方文档说明，Refresh Scope Bean 是懒代理，第一次方法调用时初始化；Scope 本身会缓存已初始化对象，要让 Bean 下次调用时重新初始化，需要清除它的缓存项。:contentReference[oaicite:0]{index=0}

---

## 3. 使用示例

```java
@Component
@RefreshScope
public class UserConfig {

    @Value("${user.name}")
    private String name;

    public String getName() {
        return name;
    }
}
```

配置变更后，触发刷新：

```bash
curl -X POST http://localhost:8080/actuator/refresh
```

Spring 官方配置中心示例中也说明，修改配置后需要调用 Actuator 的 refresh 端点，客户端才会刷新并读取新值。:contentReference[oaicite:1]{index=1}

---

## 4. Spring Cloud 动态刷新流程

```text
配置中心修改配置
  ↓
应用感知配置变化
  ↓
调用 /actuator/refresh 或接收 Bus 刷新事件
  ↓
重新加载 Environment
  ↓
发布 EnvironmentChangeEvent
  ↓
刷新 @ConfigurationProperties
  ↓
清空 @RefreshScope Bean 缓存
  ↓
下次访问时重新创建 Bean
```

简单理解：

```text
刷新配置源 + 清空 Bean 缓存 + 下次重新创建 Bean
```

---

## 5. 为什么要用代理

如果直接替换 Bean 对象，已经注入到其他 Bean 中的引用不会自动变化。

例如：

```java
@Service
public class OrderService {

    @Autowired
    private UserConfig userConfig;
}
```

如果 `userConfig` 是普通对象，`OrderService` 中持有的是旧引用。

而 `@RefreshScope` 注入的是代理对象：

```text
OrderService 持有代理对象
  ↓
代理对象内部再去找真实目标对象
  ↓
刷新后目标对象变了，但代理对象不变
```

所以可以做到：

```text
依赖方引用不变，目标 Bean 可重新创建
```

---

## 6. @ConfigurationProperties 刷新

配置类一般推荐使用：

```java
@Component
@ConfigurationProperties(prefix = "user")
@RefreshScope
public class UserProperties {

    private String name;

    private Integer age;

    // getter / setter
}
```

相比 `@Value`，`@ConfigurationProperties` 更适合一组配置项绑定。

---

## 7. EnvironmentChangeEvent

### 实现原理

当配置发生变化时，可以发布 `EnvironmentChangeEvent`。

作用：

```text
通知 Spring 当前 Environment 中某些配置发生变化
触发相关配置重新绑定
```

适合刷新：

```text
@ConfigurationProperties
日志级别
部分配置绑定对象
```

但它不会自动重建所有普通单例 Bean。

---

## 8. ContextRefresher

### 实现原理

`ContextRefresher` 是 Spring Cloud 中用于触发刷新流程的组件。

核心作用：

```text
重新加载配置源
计算变化的配置项
发布 EnvironmentChangeEvent
刷新 RefreshScope Bean
```

可以在代码中手动触发：

```java
@Autowired
private ContextRefresher contextRefresher;

public void refresh() {
    contextRefresher.refresh();
}
```

---

## 9. 动态注册 BeanDefinition

如果不是配置刷新，而是运行时新增 Bean，可以使用 `BeanDefinitionRegistry`。

Spring 官方 API 中，`BeanDefinitionRegistry` 提供 `registerBeanDefinition(String beanName, BeanDefinition beanDefinition)` 方法，用于注册 Bean 定义。:contentReference[oaicite:2]{index=2}

示例：

```java
@Autowired
private ConfigurableApplicationContext applicationContext;

public void registerBean() {
    DefaultListableBeanFactory beanFactory =
            (DefaultListableBeanFactory) applicationContext.getBeanFactory();

    BeanDefinitionBuilder builder =
            BeanDefinitionBuilder.genericBeanDefinition(DemoService.class);

    beanFactory.registerBeanDefinition(
            "demoService",
            builder.getBeanDefinition()
    );
}
```

适用场景：

```text
插件化
动态数据源
动态策略类
运行时扩展组件
```

---

## 10. 动态注册单例对象

如果对象已经创建好，可以直接注册成单例：

```java
@Autowired
private ConfigurableApplicationContext applicationContext;

public void registerSingleton() {
    DefaultListableBeanFactory beanFactory =
            (DefaultListableBeanFactory) applicationContext.getBeanFactory();

    DemoService demoService = new DemoService();

    beanFactory.registerSingleton("demoService", demoService);
}
```

注意：

```text
registerSingleton 注册的是现成对象
不会完整走 Bean 生命周期
不会自动执行依赖注入、AOP、初始化回调
```

如果需要完整 Spring 生命周期，更推荐注册 `BeanDefinition`。

---

## 11. 哪些 Bean 不适合动态刷新

不建议随意刷新：

```text
DataSource
线程池
连接池
Netty Server
MQ Consumer
定时任务调度器
底层基础设施 Bean
```

原因：

```text
这些 Bean 通常持有连接、线程、文件句柄或外部资源
刷新不当容易造成资源泄漏或服务抖动
```

Spring Cloud 文档也特别提到，`HikariDataSource` 默认属于不可刷新类型。:contentReference[oaicite:3]{index=3}

---

## 12. 总结

Spring Bean 动态刷新常见方式有 `@RefreshScope`、`EnvironmentChangeEvent`、`ContextRefresher`、动态注册 `BeanDefinition` 和动态注册单例对象。

配置中心场景下最常用的是 `@RefreshScope`。它的核心原理是给 Bean 创建代理对象，真实 Bean 会缓存在 RefreshScope 中。配置刷新时，Spring Cloud 会重新加载配置，发布环境变更事件，并清空 `@RefreshScope` Bean 的缓存。下次调用代理对象时，会重新创建真实 Bean，并注入最新配置。

如果是运行时动态新增 Bean，可以通过 `BeanDefinitionRegistry` 注册新的 `BeanDefinition`；如果只是注册一个已经创建好的对象，可以使用 `registerSingleton`，但这种方式不会完整走 Spring Bean 生命周期。

一句话总结：

```text
Spring Bean 动态刷新 = 配置变更后更新 Environment + 清空 RefreshScope 缓存 + 通过代理重新创建 Bean；
动态新增 Bean = 运行时注册 BeanDefinition 或 Singleton。
```
