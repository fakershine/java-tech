# Dubbo 的代码层级图

## 目录

- [一、Dubbo 源码模块层级图](#一dubbo-源码模块层级图)
- [二、Dubbo 整体调用链代码层级图](#二dubbo-整体调用链代码层级图)
- [三、Dubbo 服务暴露代码层级图](#三dubbo-服务暴露代码层级图)
- [四、Dubbo 服务引用代码层级图](#四dubbo-服务引用代码层级图)
- [五、Dubbo 核心类层级图](#五dubbo-核心类层级图)
- [六、Dubbo 分层架构图](#六dubbo-分层架构图)
- [七、Dubbo 调用过程核心对象流转图](#七dubbo-调用过程核心对象流转图)
- [八、Provider 端代码层级图](#八provider-端代码层级图)
- [九、Consumer 端代码层级图](#九consumer-端代码层级图)
- [十、Dubbo SPI 扩展层级图](#十dubbo-spi-扩展层级图)
- [十一、Dubbo 核心调用模型图](#十一dubbo-核心调用模型图)
- [十二、Dubbo 核心模块职责总结](#十二dubbo-核心模块职责总结)
- [十三、Dubbo 核心对象说明](#十三dubbo-核心对象说明)
- [十四、Provider 服务暴露核心流程详解](#十四provider-服务暴露核心流程详解)
- [十五、Consumer 服务引用核心流程详解](#十五consumer-服务引用核心流程详解)
- [十六、一次 RPC 调用的完整链路](#十六一次-rpc-调用的完整链路)
- [十七、面试回答版本](#十七面试回答版本)
- [十八、一句话总结](#十八一句话总结)

---

## 一、Dubbo 源码模块层级图

Dubbo 的源码模块可以按照职责划分为基础层、配置层、RPC 核心层、集群治理层、注册发现层、网络通信层、序列化层、元数据与监控层、Spring Boot 集成层。

整体源码模块层级大致如下：

```text
apache/dubbo
├── dubbo-common
│   ├── common 工具类
│   ├── URL 模型
│   ├── SPI 扩展机制
│   ├── ExtensionLoader
│   └── 通用常量、线程池、工具类
│
├── dubbo-config
│   ├── ServiceConfig
│   ├── ReferenceConfig
│   ├── ApplicationConfig
│   ├── RegistryConfig
│   ├── ProtocolConfig
│   └── 配置解析、服务暴露、服务引用入口
│
├── dubbo-rpc
│   ├── Protocol
│   ├── Invoker
│   ├── Invocation
│   ├── Result
│   ├── Filter
│   └── RPC 调用核心抽象
│
├── dubbo-cluster
│   ├── Cluster
│   ├── Directory
│   ├── Router
│   ├── LoadBalance
│   ├── FailoverClusterInvoker
│   ├── FailfastClusterInvoker
│   └── 集群容错、负载均衡、路由
│
├── dubbo-registry
│   ├── Registry
│   ├── RegistryFactory
│   ├── RegistryProtocol
│   ├── ZookeeperRegistry
│   ├── NacosRegistry
│   └── 服务注册与发现
│
├── dubbo-remoting
│   ├── Transporter
│   ├── Client
│   ├── Server
│   ├── Channel
│   ├── ExchangeClient
│   ├── ExchangeServer
│   └── 网络通信抽象
│
├── dubbo-serialization
│   ├── Serialization
│   ├── ObjectInput
│   ├── ObjectOutput
│   ├── Hessian
│   ├── Fastjson
│   ├── Kryo
│   └── 序列化与反序列化
│
├── dubbo-metadata
│   ├── MetadataService
│   ├── MetadataReport
│   └── 服务元数据上报与查询
│
├── dubbo-metrics
│   ├── MetricsCollector
│   ├── MetricsReporter
│   └── 指标采集与上报
│
├── dubbo-configcenter
│   ├── DynamicConfiguration
│   └── 动态配置中心
│
├── dubbo-spring-boot-project
│   ├── starter
│   ├── autoconfigure
│   └── Spring Boot 自动装配
│
└── dubbo-demo
    └── 示例代码
```

可以简单记成：

```text
common 是基础
config 是入口
rpc 是核心抽象
cluster 是集群治理
registry 是注册发现
remoting 是网络通信
serialization 是编解码
metadata / metrics 是治理增强
spring-boot-project 是 Spring Boot 集成
```

---

## 二、Dubbo 整体调用链代码层级图

### 2.1 Consumer 调用链

Consumer 端调用链可以理解为：

```text
业务代码调用接口
    ↓
代理对象 Proxy
    ↓
InvokerInvocationHandler
    ↓
MockClusterInvoker
    ↓
AbstractClusterInvoker
    ↓
Directory 获取 Invoker 列表
    ↓
Router 路由过滤
    ↓
LoadBalance 负载均衡选择一个 Invoker
    ↓
ClusterInvoker 执行容错逻辑
    ↓
Filter 责任链
    ↓
DubboInvoker
    ↓
ExchangeClient
    ↓
NettyClient
    ↓
Serialization 序列化
    ↓
网络发送请求
```

Consumer 端核心目标是：

> 把一个本地接口调用，转换成一次远程 RPC 请求。

---

### 2.2 Provider 调用链

Provider 端调用链可以理解为：

```text
NettyServer 接收请求
    ↓
解码请求
    ↓
HeaderExchangeHandler
    ↓
DubboProtocol requestHandler
    ↓
Filter 责任链
    ↓
AbstractProxyInvoker
    ↓
Wrapper / 反射调用
    ↓
真实 ServiceImpl
    ↓
返回结果
    ↓
序列化响应
    ↓
网络返回给 Consumer
```

Provider 端核心目标是：

> 接收远程请求，找到对应本地服务实现并执行方法，然后把结果返回给 Consumer。

---

## 三、Dubbo 服务暴露代码层级图

服务暴露是 Provider 把本地服务发布出去，并注册到注册中心。

```text
@DubboService / @Service
        ↓
ServiceBean
        ↓
ServiceConfig.export()
        ↓
doExport()
        ↓
doExportUrls()
        ↓
doExportUrlsFor1Protocol()
        ↓
ProxyFactory.getInvoker(ref, interfaceClass, url)
        ↓
生成本地 Invoker
        ↓
Protocol.export(invoker)
        ↓
RegistryProtocol.export()
        ↓
DubboProtocol.export()
        ↓
打开 NettyServer
        ↓
Registry.register(providerUrl)
        ↓
注册到 Zookeeper / Nacos
```

简化版：

```text
ServiceConfig
    ↓
ProxyFactory
    ↓
Invoker
    ↓
Protocol
    ↓
RegistryProtocol
    ↓
DubboProtocol
    ↓
NettyServer
    ↓
Registry
```

服务暴露的核心是：

```text
ServiceImpl 被包装成 Invoker
Invoker 被 Protocol 暴露成 Exporter
Exporter 代表一个已经暴露出去的服务
Provider URL 被注册到注册中心
```

---

## 四、Dubbo 服务引用代码层级图

服务引用是 Consumer 获取远程服务代理对象。

```text
@DubboReference / @Reference
        ↓
ReferenceBean
        ↓
ReferenceConfig.get()
        ↓
createProxy()
        ↓
RegistryProtocol.refer()
        ↓
Registry.subscribe()
        ↓
Directory 维护 Provider 列表
        ↓
Cluster.join(directory)
        ↓
生成 ClusterInvoker
        ↓
ProxyFactory.getProxy(invoker)
        ↓
生成接口代理对象
        ↓
注入到业务代码中
```

简化版：

```text
ReferenceConfig
    ↓
RegistryProtocol
    ↓
Registry
    ↓
Directory
    ↓
Cluster
    ↓
Invoker
    ↓
ProxyFactory
    ↓
Proxy
```

服务引用的核心是：

```text
Consumer 从注册中心订阅 Provider 地址
Directory 本地维护 Provider 列表
Cluster 将多个 Invoker 包装成一个具备容错能力的 Invoker
ProxyFactory 生成接口代理对象
业务代码调用代理对象时，最终进入 Invoker.invoke()
```

---

## 五、Dubbo 核心类层级图

```text
业务接口
    ↓
Proxy
    ↓
InvokerInvocationHandler
    ↓
Invoker
    ↓
ClusterInvoker
    ↓
Directory
    ↓
Router
    ↓
LoadBalance
    ↓
ProtocolInvoker
    ↓
Client
    ↓
Codec
    ↓
Serialization
```

核心类说明：

| 类 / 接口 | 所属模块 | 作用 |
|---|---|---|
| `ServiceConfig` | `dubbo-config` | Provider 服务暴露入口 |
| `ReferenceConfig` | `dubbo-config` | Consumer 服务引用入口 |
| `ProxyFactory` | `dubbo-rpc` | 创建代理对象或 Invoker |
| `Invoker` | `dubbo-rpc` | Dubbo 最核心调用抽象 |
| `Invocation` | `dubbo-rpc` | 一次方法调用的信息 |
| `Result` | `dubbo-rpc` | 调用返回结果 |
| `Protocol` | `dubbo-rpc` | 协议暴露和引用抽象 |
| `RegistryProtocol` | `dubbo-registry` | 带注册中心的协议包装 |
| `DubboProtocol` | `dubbo-rpc` | Dubbo 协议实现 |
| `Cluster` | `dubbo-cluster` | 集群容错入口 |
| `Directory` | `dubbo-cluster` | 服务目录，维护 Provider 列表 |
| `Router` | `dubbo-cluster` | 路由过滤 |
| `LoadBalance` | `dubbo-cluster` | 负载均衡 |
| `Filter` | `dubbo-rpc` | 调用链过滤器 |
| `Transporter` | `dubbo-remoting` | 网络传输抽象 |
| `Serialization` | `dubbo-serialization` | 序列化接口 |

---

## 六、Dubbo 分层架构图

Dubbo 可以按照如下层次理解：

```text
┌────────────────────────────────────┐
│          业务层                     │
│   UserService / OrderService        │
└────────────────────────────────────┘
                 ↓
┌────────────────────────────────────┐
│          Config 配置层              │
│   ServiceConfig / ReferenceConfig   │
└────────────────────────────────────┘
                 ↓
┌────────────────────────────────────┐
│          Proxy 代理层               │
│   ProxyFactory / JavassistProxy     │
└────────────────────────────────────┘
                 ↓
┌────────────────────────────────────┐
│          RPC 核心层                 │
│   Invoker / Invocation / Result     │
└────────────────────────────────────┘
                 ↓
┌────────────────────────────────────┐
│          Cluster 集群层             │
│   Cluster / Directory / Router      │
│   LoadBalance / Failover            │
└────────────────────────────────────┘
                 ↓
┌────────────────────────────────────┐
│          Registry 注册层            │
│   Registry / RegistryProtocol       │
└────────────────────────────────────┘
                 ↓
┌────────────────────────────────────┐
│          Protocol 协议层            │
│   DubboProtocol / TripleProtocol    │
└────────────────────────────────────┘
                 ↓
┌────────────────────────────────────┐
│          Remoting 通信层            │
│   Client / Server / Channel         │
└────────────────────────────────────┘
                 ↓
┌────────────────────────────────────┐
│          Serialize 序列化层         │
│   Hessian / JSON / Protobuf / Kryo  │
└────────────────────────────────────┘
                 ↓
┌────────────────────────────────────┐
│          Network 网络层             │
│   Netty / TCP / HTTP2               │
└────────────────────────────────────┘
```

---

## 七、Dubbo 调用过程核心对象流转图

```text
Consumer 调用 userService.getById(1)
        ↓
代理对象拦截方法调用
        ↓
构造 Invocation
        ↓
调用 Invoker.invoke(invocation)
        ↓
ClusterInvoker 做容错
        ↓
Directory 获取 Provider Invoker 列表
        ↓
Router 过滤 Invoker
        ↓
LoadBalance 选择 Invoker
        ↓
Filter 链增强调用
        ↓
DubboInvoker 发起远程调用
        ↓
ExchangeClient 发送 Request
        ↓
NettyClient 写入 Channel
        ↓
Provider NettyServer 收到请求
        ↓
解码 Request
        ↓
找到 Exporter
        ↓
调用 Provider Invoker
        ↓
反射执行 ServiceImpl
        ↓
返回 Result
        ↓
编码 Response
        ↓
Consumer 收到结果
```

---

## 八、Provider 端代码层级图

Provider 端负责服务暴露和请求处理。

```text
@DubboService
    ↓
ServiceBean
    ↓
ServiceConfig
    ↓
ProxyFactory.getInvoker()
    ↓
AbstractProxyInvoker
    ↓
Protocol.export()
    ↓
RegistryProtocol.export()
    ↓
DubboProtocol.export()
    ↓
Exporter
    ↓
NettyServer
```

重点理解：

```text
ServiceImpl 被包装成 Invoker
Invoker 被 Protocol 暴露成 Exporter
Exporter 代表一个已经暴露出去的服务
DubboProtocol 负责启动网络服务
RegistryProtocol 负责注册服务地址
```

---

## 九、Consumer 端代码层级图

Consumer 端负责服务引用和代理调用。

```text
@DubboReference
    ↓
ReferenceBean
    ↓
ReferenceConfig
    ↓
RegistryProtocol.refer()
    ↓
RegistryDirectory
    ↓
Cluster.join()
    ↓
ClusterInvoker
    ↓
ProxyFactory.getProxy()
    ↓
代理对象
```

重点理解：

```text
Consumer 拿到的不是实现类
而是一个代理对象

代理对象内部持有 Invoker
Invoker 负责真正远程调用
```

---

## 十、Dubbo SPI 扩展层级图

Dubbo 的扩展机制通过 `ExtensionLoader` 实现。

```text
ExtensionLoader
    ↓
读取 META-INF/dubbo/
    ↓
加载扩展点配置
    ↓
创建扩展实现
    ↓
Wrapper 包装
    ↓
Adaptive 自适应扩展
    ↓
Inject 依赖注入
```

常见扩展点：

```text
Protocol
Registry
Cluster
LoadBalance
Router
Filter
ProxyFactory
Serialization
Transporter
ThreadPool
```

SPI 机制的作用是：

```text
框架核心逻辑保持稳定
具体能力通过扩展点进行替换和增强
```

例如：

| 扩展点 | 可以扩展的能力 |
|---|---|
| `Protocol` | 扩展通信协议 |
| `Registry` | 扩展注册中心 |
| `LoadBalance` | 扩展负载均衡策略 |
| `Cluster` | 扩展集群容错策略 |
| `Filter` | 扩展调用拦截逻辑 |
| `Serialization` | 扩展序列化方式 |
| `Transporter` | 扩展网络传输实现 |

---

## 十一、Dubbo 核心调用模型图

Dubbo 最核心的抽象是：

```java
Result invoke(Invocation invocation);
```

所以整个框架可以抽象成：

```text
Proxy
  ↓
Invoker
  ↓
Filter Invoker
  ↓
Cluster Invoker
  ↓
Protocol Invoker
  ↓
Remote Invoker
  ↓
Provider Invoker
```

也就是说：

```text
本地代理是 Invoker
远程调用是 Invoker
集群包装后还是 Invoker
过滤器包装后还是 Invoker
服务提供者也可以抽象成 Invoker
```

这就是 Dubbo 代码设计里非常重要的统一抽象。

---

## 十二、Dubbo 核心模块职责总结

| 模块 | 核心职责 |
|---|---|
| `dubbo-common` | 基础工具、URL、SPI、线程池、配置工具 |
| `dubbo-config` | 服务暴露和服务引用入口 |
| `dubbo-rpc` | RPC 核心抽象，包括 `Invoker`、`Protocol`、`Filter` |
| `dubbo-cluster` | 集群容错、路由、负载均衡、服务目录 |
| `dubbo-registry` | 注册中心抽象、服务注册与订阅 |
| `dubbo-remoting` | 网络通信抽象，包括 Client、Server、Channel |
| `dubbo-serialization` | 序列化与反序列化 |
| `dubbo-metadata` | 元数据上报和查询 |
| `dubbo-metrics` | 指标采集和上报 |
| `dubbo-configcenter` | 动态配置中心 |
| `dubbo-spring-boot-project` | Spring Boot 自动装配和集成 |

---

## 十三、Dubbo 核心对象说明

### 13.1 `Invoker`

`Invoker` 是 Dubbo 最核心的调用抽象。

它统一了：

- 本地服务调用；
- 远程服务调用；
- 集群调用；
- 过滤器包装调用；
- 服务提供者调用。

核心方法：

```java
Result invoke(Invocation invocation);
```

可以理解为：

> 只要一个对象可以被调用，在 Dubbo 内部就可以抽象成 Invoker。

---

### 13.2 `Invocation`

`Invocation` 表示一次调用的上下文信息。

通常包含：

- 方法名；
- 参数类型；
- 参数值；
- 附加参数；
- 调用上下文。

可以理解为：

> Invocation 是一次 RPC 方法调用的数据载体。

---

### 13.3 `Result`

`Result` 表示一次调用的返回结果。

它可能包含：

- 正常返回值；
- 异常；
- 附加信息；
- 异步结果。

可以理解为：

> Result 是一次 RPC 调用的返回包装。

---

### 13.4 `Protocol`

`Protocol` 是协议抽象。

它主要负责两个动作：

```java
Exporter<T> export(Invoker<T> invoker);

Invoker<T> refer(Class<T> type, URL url);
```

含义是：

| 方法 | 作用 |
|---|---|
| `export` | 暴露服务 |
| `refer` | 引用服务 |

---

### 13.5 `Exporter`

`Exporter` 表示一个已经暴露出去的服务。

Provider 端会把本地 `Invoker` 通过 `Protocol.export()` 暴露成 `Exporter`。

---

### 13.6 `Directory`

`Directory` 是服务目录。

它维护某个服务接口对应的 Provider 列表。

Consumer 调用时，会通过 Directory 获取当前可用的 Invoker 列表。

---

### 13.7 `Cluster`

`Cluster` 负责把多个 Provider Invoker 包装成一个具备集群容错能力的 Invoker。

例如：

```text
多个 Provider Invoker
        ↓
Cluster.join(directory)
        ↓
FailoverClusterInvoker
```

---

### 13.8 `LoadBalance`

`LoadBalance` 负责从多个 Provider 中选择一个。

常见策略：

- Random；
- RoundRobin；
- LeastActive；
- ConsistentHash。

---

### 13.9 `Filter`

`Filter` 是责任链扩展点。

它可以在 RPC 调用前后增强逻辑，例如：

- 日志；
- 监控；
- 鉴权；
- 限流；
- 参数校验；
- TraceId 透传。

---

## 十四、Provider 服务暴露核心流程详解

Provider 服务暴露可以分为以下几个阶段。

### 14.1 解析服务配置

Dubbo 先解析服务配置，例如：

```java
@DubboService
public class UserServiceImpl implements UserService {
}
```

Spring 集成场景下，`ServiceBean` 会把注解配置转换为 `ServiceConfig`。

---

### 14.2 创建 Invoker

Dubbo 使用 `ProxyFactory` 把本地服务实现包装成 `Invoker`。

```text
UserServiceImpl
        ↓
ProxyFactory.getInvoker()
        ↓
Invoker
```

---

### 14.3 暴露服务

Dubbo 调用 `Protocol.export(invoker)` 暴露服务。

如果使用注册中心，会先进入 `RegistryProtocol`。

```text
Protocol.export(invoker)
        ↓
RegistryProtocol.export()
        ↓
DubboProtocol.export()
```

---

### 14.4 启动网络服务

`DubboProtocol.export()` 会启动底层通信服务。

通常是：

```text
DubboProtocol
        ↓
Exchangers
        ↓
Transporters
        ↓
NettyServer
```

---

### 14.5 注册服务地址

服务启动后，Provider 会把服务地址注册到注册中心。

```text
Provider URL
        ↓
Registry.register()
        ↓
Zookeeper / Nacos
```

---

## 十五、Consumer 服务引用核心流程详解

Consumer 服务引用可以分为以下几个阶段。

### 15.1 解析引用配置

Dubbo 解析引用配置，例如：

```java
@DubboReference
private UserService userService;
```

Spring 集成场景下，`ReferenceBean` 会转换成 `ReferenceConfig`。

---

### 15.2 订阅注册中心

Consumer 会向注册中心订阅服务地址。

```text
ReferenceConfig
        ↓
RegistryProtocol.refer()
        ↓
Registry.subscribe()
```

---

### 15.3 创建 Directory

注册中心返回 Provider 地址列表后，Dubbo 会创建 `RegistryDirectory`。

```text
Provider URL List
        ↓
RegistryDirectory
```

`RegistryDirectory` 负责维护当前服务的 Provider 列表。

---

### 15.4 创建 ClusterInvoker

Dubbo 通过 `Cluster.join(directory)` 创建集群 Invoker。

```text
Directory
        ↓
Cluster.join()
        ↓
FailoverClusterInvoker
```

此时多个 Provider 被包装成一个统一的 Invoker。

---

### 15.5 创建代理对象

Dubbo 使用 `ProxyFactory.getProxy(invoker)` 生成接口代理对象。

```text
ClusterInvoker
        ↓
ProxyFactory.getProxy()
        ↓
UserService 代理对象
```

业务代码注入的就是这个代理对象。

---

## 十六、一次 RPC 调用的完整链路

以 Consumer 调用：

```java
userService.getById(1L);
```

为例，完整链路如下：

```text
业务代码
    ↓
UserService 代理对象
    ↓
InvokerInvocationHandler.invoke()
    ↓
构造 RpcInvocation
    ↓
MockClusterInvoker.invoke()
    ↓
FailoverClusterInvoker.invoke()
    ↓
Directory.list()
    ↓
Router 路由过滤
    ↓
LoadBalance 选择 Invoker
    ↓
Filter 链
    ↓
DubboInvoker.invoke()
    ↓
ExchangeClient.request()
    ↓
NettyClient 发送请求
    ↓
Provider NettyServer 接收请求
    ↓
DubboProtocol requestHandler
    ↓
找到 Exporter
    ↓
Provider Invoker.invoke()
    ↓
AbstractProxyInvoker.doInvoke()
    ↓
UserServiceImpl.getById()
    ↓
返回结果
    ↓
Response 写回 Consumer
```

可以概括成：

```text
代理对象
    ↓
Invocation
    ↓
Cluster
    ↓
Directory
    ↓
Router
    ↓
LoadBalance
    ↓
Filter
    ↓
Protocol
    ↓
Remoting
    ↓
Provider Invoker
    ↓
ServiceImpl
```

---

## 十七、总结

可以这样说：

> Dubbo 的代码层级可以从几个核心模块看。
>
> 最底层是 `dubbo-common`，里面有 URL、SPI、工具类等基础能力。然后是 `dubbo-remoting` 和 `dubbo-serialization`，分别负责网络通信和序列化。再往上是 `dubbo-rpc`，定义了 `Protocol`、`Invoker`、`Invocation`、`Result`、`Filter` 这些核心 RPC 抽象。
>
> 在 RPC 之上是 `dubbo-cluster`，负责集群容错、路由、负载均衡。服务注册发现由 `dubbo-registry` 负责，服务暴露和引用入口在 `dubbo-config`，比如 `ServiceConfig` 和 `ReferenceConfig`。如果和 Spring Boot 集成，则会用到 `dubbo-spring-boot-project`。
>
> 从调用链看，Provider 端是 `ServiceConfig -> ProxyFactory -> Invoker -> Protocol.export -> RegistryProtocol -> DubboProtocol -> NettyServer`。
>
> Consumer 端是 `ReferenceConfig -> RegistryProtocol.refer -> Directory -> ClusterInvoker -> ProxyFactory -> Proxy`。
>
> Dubbo 最核心的代码抽象是 `Invoker`，它把本地调用、远程调用、集群调用、过滤器调用都统一成 `invoke(Invocation)` 模型。

---

## 十八、一句话总结

> Dubbo 的代码层级可以理解为：`Config` 负责入口，`Proxy` 屏蔽远程调用，`RPC` 定义核心抽象，`Cluster` 做治理，`Registry` 做发现，`Protocol` 做协议，`Remoting` 做通信，`Serialization` 做编解码，而 `Invoker` 贯穿整个调用链。
