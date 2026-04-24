# Dubbo 调用流程总结

Dubbo 是一个 RPC 框架，核心目标是让远程服务调用像本地方法调用一样简单。  
一次 Dubbo 调用主要分为：

```text
服务暴露
  ↓
服务引用
  ↓
服务调用
  ↓
结果返回
```

Dubbo 的核心模型是 `Invoker`。官方文档中也强调，`Invoker` 是 Dubbo 的核心实体模型，可以代表本地实现、远程实现或集群实现；`Protocol` 负责服务暴露和服务引用的生命周期管理。:contentReference[oaicite:0]{index=0}

---

## 1. Dubbo 核心组件

| 组件 | 作用 |
|---|---|
| Provider | 服务提供者 |
| Consumer | 服务消费者 |
| Registry | 注册中心，如 Nacos、ZooKeeper |
| Protocol | 协议层，负责服务暴露和引用 |
| Proxy | 代理层，让远程调用像本地调用 |
| Invoker | Dubbo 核心调用模型 |
| Cluster | 集群容错层，封装多个 Invoker |
| Directory | 维护服务提供者 Invoker 列表 |
| Router | 路由过滤服务提供者 |
| LoadBalance | 负载均衡 |
| Filter | 过滤器，做日志、限流、鉴权等 |
| ExchangeClient | 网络通信客户端 |
| Transporter | 底层网络传输，常见是 Netty |

Dubbo 支持多种通信协议，例如 Dubbo2 协议基于 TCP，Triple 协议基于 HTTP/2，也支持 gRPC、REST、Hessian2 等协议扩展。:contentReference[oaicite:1]{index=1}

---

## 2. 服务提供者启动流程

### 实现原理

服务提供者启动时，会把本地服务暴露出去，并注册到注册中心。

流程：

```text
Spring 容器启动
  ↓
扫描 Dubbo 服务注解
  ↓
创建服务实现类
  ↓
通过 ProxyFactory 将服务实现包装成 Invoker
  ↓
Protocol 暴露 Invoker
  ↓
启动 Netty Server 监听端口
  ↓
将服务地址注册到注册中心
```

例如：

```java
@DubboService
public class UserServiceImpl implements UserService {
    @Override
    public User getUser(Long id) {
        return new User(id, "Tom");
    }
}
```

暴露后，注册中心中会保存类似信息：

```text
dubbo://192.168.1.10:20880/com.demo.UserService
```

### 面试理解

Provider 端核心是：

```text
服务实现类 -> Invoker -> Exporter -> 注册中心
```

---

## 3. 服务消费者启动流程

### 实现原理

消费者启动时，会从注册中心订阅服务提供者地址，并为接口生成代理对象。

流程：

```text
Spring 容器启动
  ↓
扫描 @DubboReference
  ↓
从注册中心订阅服务地址
  ↓
生成远程 Invoker
  ↓
多个 Invoker 通过 Cluster 封装成一个集群 Invoker
  ↓
ProxyFactory 生成接口代理对象
  ↓
注入到 Spring Bean 中
```

例如：

```java
@DubboReference
private UserService userService;
```

这里注入的并不是接口真实实现类，而是 Dubbo 生成的代理对象。

### 面试理解

Consumer 端核心是：

```text
注册中心地址列表 -> Invoker 列表 -> Cluster Invoker -> 接口代理对象
```

Dubbo 文档中也提到，消费者引用服务时，`ReferenceConfig` 会调用 `Protocol.refer()` 生成 `Invoker`，然后再把 `Invoker` 转换成客户端需要的接口代理对象。:contentReference[oaicite:2]{index=2}

---

## 4. 一次 Dubbo 调用流程

当消费者调用：

```java
userService.getUser(1L);
```

实际流程如下：

```text
调用接口代理对象
  ↓
进入 InvokerInvocationHandler
  ↓
封装 Invocation
  ↓
经过 Consumer Filter 链
  ↓
Directory 获取可用 Invoker 列表
  ↓
Router 路由过滤
  ↓
LoadBalance 选择一个 Provider
  ↓
Cluster 做容错处理
  ↓
Protocol 发起远程调用
  ↓
ExchangeClient 通过 Netty 发送请求
  ↓
Provider 接收请求
  ↓
Provider Filter 链
  ↓
反射调用真实服务实现
  ↓
返回结果
  ↓
Consumer 收到响应
```

简化理解：

```text
代理对象
  ↓
负载均衡
  ↓
网络调用
  ↓
服务端执行
  ↓
返回结果
```

---

## 5. Consumer 端详细调用流程

### 1. 调用代理对象

```java
userService.getUser(1L);
```

本质调用的是动态代理对象。

```text
接口方法调用
  ↓
InvokerInvocationHandler.invoke()
```

Dubbo 的 Proxy 层会把接口调用转换成对 `Invoker` 的调用。Dubbo 设计文档中也说明，Proxy 层负责把 `Invoker` 转成接口，或者把接口实现转成 `Invoker`，从而让远程调用对用户透明。:contentReference[oaicite:3]{index=3}

---

### 2. 封装 Invocation

Dubbo 会把方法调用信息封装成 `Invocation`。

`Invocation` 中包含：

```text
接口名
方法名
参数类型
参数值
附件信息
调用上下文
```

例如：

```text
interface = UserService
method = getUser
args = [1]
```

---

### 3. 执行 Filter 链

Consumer 端会经过过滤器链。

常见 Filter：

- 日志 Filter
- 监控 Filter
- 超时 Filter
- 限流 Filter
- Trace Filter
- 上下文 Filter

Filter 的作用是对请求进行增强处理，例如监控、鉴权、日志、链路追踪等。Dubbo 官方也提供 Filter 扩展机制，允许开发者自定义请求拦截逻辑。:contentReference[oaicite:4]{index=4}

---

### 4. Directory 获取服务列表

`Directory` 负责维护当前可用服务提供者列表。

例如：

```text
Invoker1 -> 192.168.1.10:20880
Invoker2 -> 192.168.1.11:20880
Invoker3 -> 192.168.1.12:20880
```

这些地址来自注册中心。

---

### 5. Router 路由过滤

Router 会根据路由规则过滤服务提供者。

常见场景：

```text
灰度发布
标签路由
地域路由
版本路由
黑白名单
```

例如只调用灰度机器：

```text
tag = gray
```

---

### 6. LoadBalance 负载均衡

路由过滤后，Dubbo 会从可用 Invoker 中选择一个 Provider。

常见负载均衡策略：

| 策略 | 说明 |
|---|---|
| Random | 加权随机，默认常见策略 |
| RoundRobin | 加权轮询 |
| LeastActive | 最少活跃调用数 |
| ConsistentHash | 一致性 Hash |

例如：

```text
可用 Provider = 3 个
  ↓
LoadBalance 选中 192.168.1.11:20880
```

---

### 7. Cluster 集群容错

Cluster 负责在调用失败时进行容错处理。

常见策略：

| 策略 | 说明 |
|---|---|
| Failover | 失败自动重试，默认常见策略 |
| Failfast | 快速失败 |
| Failsafe | 失败安全，异常忽略 |
| Failback | 失败自动恢复，后台重试 |
| Forking | 并行调用多个 Provider，任一成功即返回 |
| Broadcast | 广播调用所有 Provider |

例如默认 Failover：

```text
调用 Provider A 失败
  ↓
重试 Provider B
  ↓
Provider B 成功
  ↓
返回结果
```

注意：

```text
重试可能导致重复调用，所以写操作接口要注意幂等。
```

---

## 6. Provider 端处理流程

Provider 收到请求后，处理流程如下：

```text
Netty Server 接收请求
  ↓
解码请求数据
  ↓
根据接口名、方法名找到 Exporter
  ↓
进入 Provider Filter 链
  ↓
调用 Invoker
  ↓
反射执行真实服务方法
  ↓
封装返回结果
  ↓
编码响应
  ↓
通过 Netty 返回 Consumer
```

例如最终调用：

```java
userServiceImpl.getUser(1L);
```

---

## 7. 注册中心的作用

注册中心主要负责：

```text
服务注册
服务发现
服务订阅
服务变更通知
```

Provider 启动时：

```text
向注册中心注册服务地址
```

Consumer 启动时：

```text
从注册中心订阅服务地址
```

当 Provider 上线或下线时：

```text
注册中心通知 Consumer 更新本地服务列表
```

这样 Consumer 不需要写死 Provider 地址。

---

## 8. Dubbo 调用链路简图

```text
Consumer
  ↓
接口代理对象
  ↓
InvokerInvocationHandler
  ↓
Cluster Invoker
  ↓
Directory
  ↓
Router
  ↓
LoadBalance
  ↓
Consumer Filter
  ↓
Protocol
  ↓
Netty Client
  ↓ 网络传输
Netty Server
  ↓
Provider Filter
  ↓
Provider Invoker
  ↓
真实服务实现
```

---

## 9. Dubbo 为什么像本地调用

因为 Consumer 注入的是接口代理对象。

```java
@DubboReference
private UserService userService;
```

调用：

```java
userService.getUser(1L);
```

表面上像调用本地方法，实际上代理对象在底层完成了：

```text
封装请求
服务发现
负载均衡
远程通信
结果反序列化
```

所以 Dubbo 屏蔽了远程调用细节。

---

## 10. Dubbo 调用中的关键点

### 1. 动态代理

用于生成接口代理对象，让远程调用像本地调用。

---

### 2. Invoker

Dubbo 的核心调用模型。

```text
本地服务可以是 Invoker
远程服务也可以是 Invoker
集群服务也可以是 Invoker
```

---

### 3. 注册中心

用于服务注册和服务发现。

---

### 4. 负载均衡

从多个 Provider 中选择一个进行调用。

---

### 5. 集群容错

调用失败时决定如何处理。

---

### 6. Filter 链

用于扩展调用过程。

---

### 7. 网络通信

底层通过协议和网络框架完成请求发送和响应接收。

---

## 11. 总结

Dubbo 调用流程可以分为服务暴露、服务引用和服务调用三个阶段。

服务提供者启动时，会扫描服务实现类，通过代理工厂将服务实现包装成 `Invoker`，再通过 `Protocol` 暴露服务，启动网络服务，并把服务地址注册到注册中心。

服务消费者启动时，会从注册中心订阅服务地址，为每个服务提供者生成远程 `Invoker`，再通过 `Cluster` 把多个 `Invoker` 封装成一个集群 `Invoker`，最后通过代理工厂生成接口代理对象并注入到 Spring 容器。

真正调用时，消费者调用的是代理对象。代理对象会将接口方法调用封装成 `Invocation`，经过 Filter 链、路由、负载均衡、集群容错后，选择一个 Provider，通过网络协议发送请求。Provider 收到请求后，解码请求，经过服务端 Filter 链，找到对应服务实现并反射调用真实方法，最后将结果返回给 Consumer。

一句话总结：

```text
Dubbo 调用流程 = 代理调用 -> 封装 Invocation -> 路由 -> 负载均衡 -> 集群容错 -> 网络通信 -> 服务端反射执行 -> 返回结果。
```
