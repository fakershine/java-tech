# Dubbo 分层架构总结

Dubbo 采用分层架构设计，每一层职责清晰，并且大量依赖 SPI 机制实现扩展。

核心思想：

```text
上层面向业务调用
下层负责远程通信
中间通过 Invoker 统一调用模型
```

Dubbo 官方设计中也强调：除 Proxy 层外，其他层都以 `Invoker` 为核心模型；`Proxy` 层负责在接口和 `Invoker` 之间互相转换。:contentReference[oaicite:0]{index=0}

---

## 1. Dubbo 整体分层

| 层级 | 名称 | 核心作用 |
|---|---|---|
| Service 层 | 服务层 | 定义业务接口和实现 |
| Config 层 | 配置层 | 解析配置，生成服务配置对象 |
| Proxy 层 | 代理层 | 生成代理对象，屏蔽远程调用 |
| Registry 层 | 注册中心层 | 服务注册与发现 |
| Cluster 层 | 集群层 | 路由、负载均衡、容错 |
| Monitor 层 | 监控层 | 统计调用次数、耗时、失败率 |
| Protocol 层 | 协议层 | 服务暴露和服务引用 |
| Exchange 层 | 信息交换层 | 封装请求响应语义 |
| Transport 层 | 网络传输层 | Netty 等网络通信 |
| Serialize 层 | 序列化层 | 对象和字节流转换 |

---

## 2. Service 层

### 作用

定义业务接口和业务实现。

```java
public interface UserService {
    User getUser(Long id);
}
```

Provider 实现接口：

```java
public class UserServiceImpl implements UserService {
}
```

### 总结

```text
Service 层是业务入口，Dubbo 本身不关心具体业务逻辑。
```

---

## 3. Config 层

### 作用

负责读取和解析 Dubbo 配置。

常见配置来源：

```text
注解
XML
YAML
Properties
配置中心
```

典型对象：

```text
ServiceConfig
ReferenceConfig
RegistryConfig
ProtocolConfig
ApplicationConfig
```

### 总结

```text
Config 层负责把用户配置转换成 Dubbo 内部配置模型。
```

---

## 4. Proxy 层

### 作用

生成代理对象，让远程调用像本地方法调用一样。

Consumer 调用：

```java
userService.getUser(1L);
```

实际调用的是代理对象。

### 实现方式

```text
JDK 动态代理
Javassist
```

### 核心转换

```text
Consumer：Invoker -> 接口代理对象
Provider：服务实现类 -> Invoker
```

Dubbo Proxy 层负责将接口实现转换为 `Invoker`，或将 `Invoker` 转换为接口代理对象，从而让 RPC 调用对用户透明。:contentReference[oaicite:1]{index=1}

---

## 5. Registry 层

### 作用

负责服务注册和服务发现。

Provider 启动：

```text
把服务地址注册到注册中心
```

Consumer 启动：

```text
从注册中心订阅 Provider 地址
```

常见注册中心：

```text
Nacos
ZooKeeper
Redis
Consul
```

### 总结

```text
Registry 层解决服务地址动态发现问题。
```

---

## 6. Cluster 层

### 作用

当一个服务有多个 Provider 时，Cluster 层负责治理多个 Provider。

核心能力：

```text
路由
负载均衡
失败重试
集群容错
服务降级
```

调用流程：

```text
Directory 获取 Invoker 列表
  ↓
Router 过滤
  ↓
LoadBalance 选择 Provider
  ↓
Cluster 执行容错策略
```

Dubbo 官方设计中说明，Cluster 的目标是把多个 `Invoker` 伪装成一个 `Invoker`，这样其他层只需要关注一个统一调用入口。:contentReference[oaicite:2]{index=2}

---

## 7. Monitor 层

### 作用

负责调用监控和统计。

常见指标：

```text
调用次数
调用耗时
成功次数
失败次数
QPS
RT
```

### 总结

```text
Monitor 层用于服务治理和性能观测。
```

---

## 8. Protocol 层

### 作用

Protocol 是 Dubbo 的核心层，负责：

```text
服务暴露 export
服务引用 refer
```

Provider 端：

```text
Invoker -> Exporter
```

Consumer 端：

```text
远程服务地址 -> Invoker
```

官方实现文档中也提到，服务暴露的关键过程是把 `Invoker` 转换为 `Exporter`；服务引用的关键过程是通过 `Protocol.refer()` 生成 `Invoker`。:contentReference[oaicite:3]{index=3}

### 常见协议

```text
dubbo
tri / triple
rest
grpc
hessian
```

Dubbo 支持多通信协议，Dubbo2 基于 TCP，Triple 基于 HTTP/2。:contentReference[oaicite:4]{index=4}

---

## 9. Exchange 层

### 作用

封装请求响应模型。

核心能力：

```text
Request
Response
Future
同步调用
异步调用
```

### 总结

```text
Exchange 层在 Transport 之上封装 request-response 语义。
```

---

## 10. Transport 层

### 作用

负责底层网络通信。

常见实现：

```text
Netty
Mina
Grizzly
```

Dubbo 设计文档中说明，Transport 层负责单向消息传输，是对 Netty、Mina、Grizzly 等网络框架的抽象。:contentReference[oaicite:5]{index=5}

### 总结

```text
Transport 层负责连接管理、数据发送、数据接收。
```

---

## 11. Serialize 层

### 作用

负责对象和字节流之间的转换。

常见序列化方式：

```text
Hessian2
Fastjson2
JDK
Protobuf
Kryo
FST
```

### 总结

```text
Serialize 层解决 Java 对象如何在网络中传输的问题。
```

---

## 12. Dubbo 调用链路中的分层关系

```text
Service
  ↓
Proxy
  ↓
Cluster
  ↓
Protocol
  ↓
Exchange
  ↓
Transport
  ↓
Serialize
  ↓
Network
```

Consumer 调用流程：

```text
接口代理对象
  ↓
Invoker
  ↓
Cluster 选择 Provider
  ↓
Protocol 发起调用
  ↓
Exchange 封装请求响应
  ↓
Transport 网络发送
  ↓
Serialize 序列化数据
```

---

## 13. 总结

Dubbo 分层架构主要包括 Service、Config、Proxy、Registry、Cluster、Monitor、Protocol、Exchange、Transport、Serialize 等层。

Service 层定义业务接口；Config 层负责配置解析；Proxy 层生成代理对象；Registry 层负责服务注册发现；Cluster 层负责路由、负载均衡和容错；Monitor 层负责监控统计；Protocol 层负责服务暴露和服务引用；Exchange 层封装请求响应模型；Transport 层负责网络通信；Serialize 层负责序列化和反序列化。

一句话总结：

```text
Dubbo 分层 = 业务接口层 + 配置层 + 代理层 + 注册发现层 + 集群治理层 + 协议层 + 网络通信层 + 序列化层；
核心模型是 Invoker，核心扩展机制是 SPI。
```
