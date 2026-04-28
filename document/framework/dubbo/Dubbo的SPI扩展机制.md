# Dubbo SPI 扩展机制总结

Dubbo SPI 是 Dubbo 提供的一套扩展机制，用于实现框架组件的 **可插拔、可替换、可增强**。

Dubbo 中很多核心能力都基于 SPI 扩展，例如：

- 协议扩展
- 注册中心扩展
- 负载均衡扩展
- 集群容错扩展
- 序列化扩展
- 过滤器扩展

---

## 1. 什么是 SPI

SPI 全称是：

```text
Service Provider Interface
```

它的作用是：

```text
定义接口
  ↓
不同厂商或开发者提供实现类
  ↓
框架运行时按需加载具体实现
```

简单理解：

```text
SPI = 面向接口编程 + 配置文件加载实现类
```

---

## 2. Java SPI 和 Dubbo SPI 的区别

### Java SPI

JDK 自带 SPI 通过 `ServiceLoader` 加载实现类。

配置文件路径：

```text
META-INF/services/接口全限定名
```

文件内容：

```text
实现类全限定名
```

### Java SPI 缺点

- 一次性加载所有实现类。
- 不支持按名称获取指定实现。
- 不支持 IOC。
- 不支持 AOP。
- 扩展能力较弱。

---

### Dubbo SPI

Dubbo 对 Java SPI 做了增强。

配置文件路径：

```text
META-INF/dubbo/
META-INF/dubbo/internal/
META-INF/services/
```

配置内容是：

```text
扩展名 = 实现类全限定名
```

示例：

```text
random=org.apache.dubbo.rpc.cluster.loadbalance.RandomLoadBalance
roundrobin=org.apache.dubbo.rpc.cluster.loadbalance.RoundRobinLoadBalance
leastactive=org.apache.dubbo.rpc.cluster.loadbalance.LeastActiveLoadBalance
```

---

## 3. Dubbo SPI 的核心特点

| 特性 | 说明 |
|---|---|
| 按名称加载 | 可以根据扩展名加载指定实现 |
| 按需加载 | 不会一次性加载所有实现 |
| IOC | 支持依赖注入 |
| AOP | 支持 Wrapper 包装增强 |
| 自适应扩展 | 支持运行时根据参数选择实现 |
| 自动激活 | 支持根据条件自动加载扩展 |
| 缓存机制 | 扩展实例会被缓存 |

---

## 4. @SPI 注解

Dubbo 扩展接口通常会标注 `@SPI`。

示例：

```java
@SPI("random")
public interface LoadBalance {

    Invoker<?> select(List<Invoker<?>> invokers, URL url, Invocation invocation);
}
```

含义：

```text
当前接口是 Dubbo SPI 扩展点
默认扩展名是 random
```

如果没有指定具体扩展名，则默认使用 `random`。

---

## 5. ExtensionLoader

`ExtensionLoader` 是 Dubbo SPI 的核心加载器。

它的作用类似于：

```text
Dubbo SPI 的工厂类
```

常见方法：

```java
ExtensionLoader.getExtensionLoader(LoadBalance.class);

getExtension("random");

getDefaultExtension();

getAdaptiveExtension();

getActivateExtension();
```

### 示例

```java
ExtensionLoader<LoadBalance> loader =
        ExtensionLoader.getExtensionLoader(LoadBalance.class);

LoadBalance loadBalance = loader.getExtension("random");
```

执行结果：

```text
加载 random 对应的 RandomLoadBalance 实现类
```

---

## 6. Dubbo SPI 加载流程

以加载 `LoadBalance` 为例：

```text
调用 ExtensionLoader.getExtensionLoader(LoadBalance.class)
  ↓
检查接口是否带 @SPI 注解
  ↓
读取 META-INF/dubbo/ 等目录下的配置文件
  ↓
解析 扩展名 = 实现类
  ↓
根据扩展名查找实现类
  ↓
实例化实现类
  ↓
执行依赖注入
  ↓
执行 Wrapper 包装
  ↓
缓存实例
  ↓
返回扩展对象
```

简化理解：

```text
找配置 -> 找实现类 -> 创建对象 -> 注入依赖 -> 包装增强 -> 返回实例
```

---

## 7. 按名称加载扩展

Dubbo SPI 支持通过扩展名加载指定实现。

配置文件：

```text
random=org.apache.dubbo.rpc.cluster.loadbalance.RandomLoadBalance
roundrobin=org.apache.dubbo.rpc.cluster.loadbalance.RoundRobinLoadBalance
```

代码：

```java
LoadBalance loadBalance =
        ExtensionLoader.getExtensionLoader(LoadBalance.class)
                .getExtension("roundrobin");
```

表示加载：

```text
RoundRobinLoadBalance
```

---

## 8. 自适应扩展 @Adaptive

### 作用

自适应扩展用于在运行时根据 URL 参数动态选择具体扩展实现。

例如：

```text
loadbalance=random
```

Dubbo 会根据这个参数选择对应的负载均衡实现。

---

### 示例

```java
@Adaptive("loadbalance")
Invoker<?> select(List<Invoker<?>> invokers, URL url, Invocation invocation);
```

含义：

```text
从 URL 中读取 loadbalance 参数
根据参数值选择对应扩展实现
```

如果 URL 中是：

```text
loadbalance=roundrobin
```

则使用：

```text
RoundRobinLoadBalance
```

---

### 自适应扩展流程

```text
调用自适应扩展方法
  ↓
从 URL 中获取扩展名
  ↓
通过 ExtensionLoader 获取对应扩展实现
  ↓
调用真实扩展方法
```

简单理解：

```text
@Adaptive = 运行时根据参数动态选择扩展实现
```

---

## 9. 自动激活 @Activate

### 作用

`@Activate` 用于根据条件自动激活某些扩展。

常用于 Filter。

例如：

```java
@Activate(group = "consumer")
public class MonitorFilter implements Filter {
}
```

表示：

```text
当前 Filter 在 consumer 侧自动生效
```

---

### 常见属性

| 属性 | 说明 |
|---|---|
| group | 按 consumer / provider 分组激活 |
| value | 根据 URL 参数是否存在激活 |
| order | 控制扩展执行顺序 |
| before | 指定在哪些扩展之前执行 |
| after | 指定在哪些扩展之后执行 |

---

### 使用场景

- Consumer Filter 自动生效。
- Provider Filter 自动生效。
- 监控过滤器。
- 日志过滤器。
- 鉴权过滤器。
- 限流过滤器。

---

## 10. Wrapper 机制

### 作用

Wrapper 是 Dubbo SPI 中的 AOP 增强机制。

如果某个扩展实现类有一个构造方法参数是扩展接口类型，Dubbo 会认为它是 Wrapper。

示例：

```java
public class ProtocolFilterWrapper implements Protocol {

    private final Protocol protocol;

    public ProtocolFilterWrapper(Protocol protocol) {
        this.protocol = protocol;
    }
}
```

Dubbo 会自动将真实扩展对象包装起来。

流程：

```text
真实扩展对象
  ↓
Wrapper1 包装
  ↓
Wrapper2 包装
  ↓
最终返回增强后的对象
```

### 作用

- 实现 Filter 链。
- 增强 Protocol。
- 添加监控、日志、拦截等能力。

---

## 11. IOC 依赖注入

Dubbo SPI 支持简单 IOC。

如果扩展类中有 setter 方法，Dubbo 会尝试自动注入依赖。

示例：

```java
public class XxxLoadBalance implements LoadBalance {

    private Router router;

    public void setRouter(Router router) {
        this.router = router;
    }
}
```

Dubbo 会通过 `ExtensionFactory` 查找并注入对应依赖。

常见 `ExtensionFactory`：

```text
SpiExtensionFactory
SpringExtensionFactory
AdaptiveExtensionFactory
```

---

## 12. Dubbo SPI 和 Filter

Dubbo 的 Filter 扩展就是 SPI 机制的典型应用。

配置文件中定义：

```text
monitor=org.apache.dubbo.monitor.support.MonitorFilter
trace=org.apache.dubbo.rpc.protocol.dubbo.filter.TraceFilter
```

然后通过 `@Activate` 自动激活。

调用链类似：

```text
Filter1
  ↓
Filter2
  ↓
Filter3
  ↓
真实 Invoker
```

这就是 Dubbo 的过滤器链。

---

## 13. Dubbo SPI 常见扩展点

| 扩展点 | 作用 |
|---|---|
| Protocol | 协议扩展，如 dubbo、tri、rest |
| RegistryFactory | 注册中心扩展 |
| Cluster | 集群容错扩展 |
| LoadBalance | 负载均衡扩展 |
| RouterFactory | 路由扩展 |
| Filter | 调用过滤器扩展 |
| Serialization | 序列化扩展 |
| ProxyFactory | 动态代理扩展 |
| Transporter | 网络传输扩展 |
| ThreadPool | 线程池扩展 |

---

## 14. Dubbo SPI 的优势

- 扩展能力强。
- 按需加载，性能更好。
- 支持按名称获取实现。
- 支持运行时自适应选择。
- 支持 IOC 依赖注入。
- 支持 Wrapper AOP 增强。
- 支持自动激活扩展。
- 框架内部大量模块可插拔。

---

## 15. 总结

Dubbo SPI 是 Dubbo 的扩展机制，它基于 Java SPI 做了增强。

Java SPI 只能通过 `ServiceLoader` 一次性加载所有实现类，而 Dubbo SPI 支持按名称加载、按需加载、默认扩展、自适应扩展、自动激活、IOC 和 Wrapper AOP 增强。

Dubbo SPI 的核心类是 `ExtensionLoader`。它会读取 `META-INF/dubbo/`、`META-INF/dubbo/internal/`、`META-INF/services/` 等目录下的扩展配置文件，根据扩展名找到实现类，创建实例并缓存。

`@SPI` 用于声明扩展点和默认扩展；`@Adaptive` 用于运行时根据 URL 参数动态选择具体扩展；`@Activate` 用于按条件自动激活扩展，常见于 Filter；Wrapper 机制用于对扩展对象进行包装增强。

一句话总结：

```text
Dubbo SPI = 按名称加载扩展 + ExtensionLoader + @SPI + @Adaptive + @Activate + IOC + Wrapper AOP。
```
