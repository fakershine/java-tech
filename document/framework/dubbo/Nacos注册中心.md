# Nacos 实现注册中心的原理

Nacos 作为注册中心，核心作用是：**服务注册、服务发现、健康检查、服务变更通知**。

整体流程：

```text
服务提供者注册实例
  ↓
Nacos 保存服务实例信息
  ↓
服务消费者订阅服务
  ↓
Nacos 返回可用实例列表
  ↓
实例上下线或健康状态变化
  ↓
Nacos 通知消费者更新本地缓存
```

Nacos 提供动态服务发现、配置管理、服务元数据和流量管理能力，也支持 DNS 和 RPC 服务发现，并提供实时健康检查，避免请求路由到不健康实例。:contentReference[oaicite:0]{index=0}

---

## 1. Nacos 注册中心核心概念

| 概念 | 说明 |
|---|---|
| Namespace | 命名空间，用于环境隔离，如 dev、test、prod |
| Group | 分组，用于业务隔离 |
| Service | 服务名，例如 `user-service` |
| Instance | 服务实例，具体的 IP + 端口 |
| Cluster | 集群，表示实例所属区域或机房 |
| Metadata | 元数据，例如版本、权重、标签 |
| Ephemeral Instance | 临时实例，依赖心跳保活 |
| Persistent Instance | 持久实例，服务端主动探活 |

Nacos 的服务注册表用于保存服务、实例和元数据；服务实例启动时注册，关闭时注销，消费者通过注册中心查询可用实例。:contentReference[oaicite:1]{index=1}

---

## 2. 服务注册流程

Provider 启动后，会把自己的服务实例信息注册到 Nacos。

```text
服务启动
  ↓
读取服务名、IP、端口、namespace、group
  ↓
向 Nacos Server 发送注册请求
  ↓
Nacos 保存实例信息
  ↓
实例进入服务列表
```

注册信息一般包括：

```text
serviceName
group
namespace
ip
port
clusterName
weight
metadata
ephemeral
healthy
```

示例：

```yaml
spring:
  application:
    name: order-service
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: dev
        group: DEFAULT_GROUP
```

Nacos OpenAPI 中注册和查询实例信息包含 `enabled`、`healthy`、`ephemeral`、`clusterName`、`metadata`、心跳超时时间等字段。:contentReference[oaicite:2]{index=2}

---

## 3. 服务发现流程

Consumer 调用服务前，会从 Nacos 获取服务实例列表。

```text
消费者启动
  ↓
订阅目标服务
  ↓
从 Nacos 拉取 Provider 实例列表
  ↓
缓存到本地
  ↓
调用时从本地实例列表选择一个实例
  ↓
通过负载均衡发起调用
```

例如调用：

```text
order-service 调用 user-service
```

实际流程：

```text
order-service 向 Nacos 查询 user-service
  ↓
Nacos 返回 user-service 实例列表
  ↓
客户端本地负载均衡选择一个实例
  ↓
发起 HTTP / RPC 调用
```

Spring Cloud Alibaba 文档也说明，Nacos Discovery 会自动把服务注册到 Nacos Server，Nacos Server 会跟踪服务并动态刷新服务列表。:contentReference[oaicite:3]{index=3}

---

## 4. 健康检查机制

Nacos 通过健康检查判断实例是否可用。

### 临时实例

临时实例依赖客户端心跳。

```text
Provider 定时发送心跳
  ↓
Nacos 更新实例最后心跳时间
  ↓
超过超时时间未收到心跳
  ↓
实例标记为不健康
  ↓
继续超时则删除实例
```

适合：

```text
普通微服务实例
服务下线后希望自动摘除
```

---

### 持久实例

持久实例不完全依赖客户端心跳，而是由 Nacos Server 主动探测实例健康状态。

```text
Nacos Server 主动探测实例
  ↓
探测成功：健康
  ↓
探测失败：不健康
```

适合：

```text
数据库
缓存
DNS 服务
固定基础设施服务
```

---

## 5. 临时实例和持久实例区别

| 对比项 | 临时实例 | 持久实例 |
|---|---|---|
| 是否依赖心跳 | 是 | 否，主要由服务端探活 |
| 下线后是否自动删除 | 会 | 通常不会直接删除 |
| 适用对象 | 普通微服务 | 固定服务、基础设施 |
| 一致性倾向 | AP | CP |
| 常见使用 | Spring Cloud 微服务默认常见 | 少量固定实例 |

简单理解：

```text
临时实例：客户端心跳保活，宕机后自动摘除。
持久实例：服务端主动探活，不轻易删除。
```

---

## 6. 服务变更通知机制

Consumer 不只是启动时拉取一次实例列表，还会订阅服务变化。

当出现：

```text
Provider 新增
Provider 下线
Provider 不健康
实例权重变化
实例元数据变化
```

Nacos 会通知 Consumer。

Consumer 收到通知后：

```text
更新本地服务实例缓存
  ↓
后续调用使用新的实例列表
```

这样可以实现服务动态扩缩容。

---

## 7. 本地缓存机制

Consumer 会在本地缓存从 Nacos 获取到的服务实例列表。

作用：

```text
减少频繁访问 Nacos
提高调用性能
Nacos 短暂不可用时仍可使用旧实例列表
```

所以真正服务调用时：

```text
Consumer 不会每次都请求 Nacos
而是使用本地缓存的实例列表
```

---

## 8. Nacos 是否参与每次调用

不参与。

```text
服务发现阶段：
Consumer ↔ Nacos

真正调用阶段：
Consumer → Provider
```

也就是说：

```text
Nacos 只负责服务注册和发现
不转发业务请求
```

这样可以避免注册中心成为业务调用链路瓶颈。

---

## 9. Nacos 集群数据同步

Nacos 通常以集群方式部署，避免单点故障。

大致思路：

```text
多个 Nacos Server 组成集群
  ↓
服务注册数据在集群内同步
  ↓
客户端连接任意 Nacos 节点
  ↓
节点之间保持数据一致
```

临时实例更偏 AP，优先保证可用性；持久实例更偏 CP，优先保证一致性。

---

## 10. Nacos 注册中心完整流程

```text
1. Provider 启动，向 Nacos 注册服务实例。
2. Provider 定时向 Nacos 发送心跳。
3. Consumer 启动，向 Nacos 订阅目标服务。
4. Nacos 返回可用 Provider 实例列表。
5. Consumer 将实例列表缓存到本地。
6. Consumer 调用时通过负载均衡选择一个实例。
7. Provider 下线或心跳超时后，Nacos 更新实例状态。
8. Nacos 将服务变更通知 Consumer。
9. Consumer 更新本地缓存。
10. 后续请求不再调用异常实例。
```

---

## 11. 总结

Nacos 作为注册中心，核心原理是服务实例注册、服务发现、健康检查和变更通知。

服务提供者启动后，会把服务名、IP、端口、集群、权重、元数据等信息注册到 Nacos；服务消费者启动后，会向 Nacos 订阅目标服务，并把返回的实例列表缓存在本地。真正调用时，消费者不会经过 Nacos，而是从本地实例列表中通过负载均衡选择一个实例直接调用。

Nacos 支持临时实例和持久实例。临时实例通过客户端心跳保活，心跳超时后会被标记不健康甚至删除；持久实例更多依赖服务端主动探活，不会因为客户端断开心跳就直接删除。实例上线、下线、权重或健康状态变化时，Nacos 会通知消费者，消费者更新本地缓存，从而实现动态服务发现。

一句话总结：

```text
Nacos 注册中心 = Provider 注册实例 + 心跳健康检查 + Consumer 订阅实例列表 + 变更推送 + 本地缓存调用。
```
