# Dubbo 注册中心原理总结

Dubbo 注册中心用于实现 **服务注册、服务发现、服务订阅、服务变更通知**。  
它是 Dubbo 服务治理的核心组件，Dubbo 依赖注册中心协调服务地址发现，从而支持动态扩缩容、负载均衡和流量治理。:contentReference[oaicite:0]{index=0}

---

## 1. 注册中心的作用

注册中心主要解决：

```text
服务提供者地址动态变化后，消费者如何发现可用 Provider。
```

核心能力：

- Provider 注册服务地址。
- Consumer 订阅服务地址。
- Provider 上下线自动通知 Consumer。
- Consumer 本地缓存 Provider 列表。
- 配合负载均衡选择具体 Provider。

Dubbo 是 **客户端服务发现**，Consumer 会从注册中心获取 Provider 地址，然后在本地完成路由、负载均衡和调用。Dubbo 支持 Nacos、ZooKeeper、Consul 等常见注册中心。:contentReference[oaicite:1]{index=1}

---

## 2. 整体流程

```text
Provider 启动
  ↓
暴露本地服务
  ↓
向注册中心注册服务地址
  ↓
Consumer 启动
  ↓
向注册中心订阅服务
  ↓
注册中心返回 Provider 地址列表
  ↓
Consumer 本地缓存地址列表
  ↓
调用时通过负载均衡选择 Provider
  ↓
Provider 上下线时注册中心推送变更
  ↓
Consumer 更新本地地址列表
```

---

## 3. Provider 注册流程

Provider 启动时会先暴露本地服务，再将服务地址注册到注册中心。

```text
Spring 容器启动
  ↓
扫描 @DubboService
  ↓
创建服务实现类
  ↓
封装成 Invoker
  ↓
Protocol 暴露服务
  ↓
启动 Netty Server
  ↓
RegistryProtocol 将服务 URL 注册到注册中心
```

注册到注册中心的信息通常包括：

```text
接口名
应用名
IP
端口
协议
版本
分组
权重
方法列表
元数据
```

示例：

```text
dubbo://192.168.1.10:20880/com.demo.UserService?version=1.0.0
```

Dubbo 服务暴露到注册中心时，会通过 `RegistryProtocol` 识别 `registry://` 协议，并把真正的 Provider URL 注册到 Registry。:contentReference[oaicite:2]{index=2}

---

## 4. Consumer 订阅流程

Consumer 启动时，会向注册中心订阅自己需要调用的服务。

```text
Spring 容器启动
  ↓
扫描 @DubboReference
  ↓
根据接口名生成订阅 URL
  ↓
向注册中心订阅 Provider 列表
  ↓
注册中心返回可用 Provider 地址
  ↓
生成远程 Invoker 列表
  ↓
通过 Cluster 封装成一个集群 Invoker
  ↓
生成代理对象注入 Spring 容器
```

Consumer 通过注册中心发现 Provider 地址时，也会走 `registry://` URL，然后通过 `Protocol.refer()` 生成远程服务引用。:contentReference[oaicite:3]{index=3}

---

## 5. 注册中心推送机制

Consumer 订阅服务后，注册中心会监听 Provider 变化。

当 Provider 发生变化：

```text
Provider 新增
Provider 下线
Provider 权重变化
Provider 配置变化
```

注册中心会通知 Consumer。

Consumer 收到通知后：

```text
更新本地 Provider 地址缓存
  ↓
更新 Directory 中的 Invoker 列表
  ↓
后续调用使用新的地址列表
```

Dubbo 支持基于注册中心的自动实例发现，Provider 注册实例地址，Consumer 订阅注册中心变化并自动获取最新实例变更，保证流量转发到正确节点。:contentReference[oaicite:4]{index=4}

---

## 6. 注册中心和调用链路关系

注册中心只参与服务发现，不参与每一次 RPC 调用。

```text
启动时 / 地址变化时：
Consumer <-> Registry

真正调用时：
Consumer -> Provider
```

也就是说：

```text
注册中心负责告诉 Consumer 有哪些 Provider。
Consumer 调用 Provider 时，不经过注册中心。
```

这样可以避免注册中心成为调用链路瓶颈。

---

## 7. 本地缓存机制

Dubbo Consumer 会缓存 Provider 地址列表。

作用：

```text
注册中心短暂不可用时，Consumer 仍然可以使用本地缓存地址继续调用。
```

Dubbo 也支持将注册中心和 Provider 地址列表缓存到本地文件，应用重启时可以恢复注册中心和 Provider 信息。:contentReference[oaicite:5]{index=5}

常见配置：

```yaml
dubbo:
  registry:
    address: nacos://127.0.0.1:8848
    file: /data/dubbo/registry-cache.properties
```

---

## 8. 注册中心宕机影响

### Provider 已启动，Consumer 已拿到地址

```text
注册中心宕机
  ↓
Consumer 仍可基于本地缓存继续调用 Provider
```

因为调用链路不经过注册中心。

---

### 新 Provider 上线

```text
注册中心宕机
  ↓
新 Provider 无法注册
  ↓
Consumer 无法感知新节点
```

---

### Provider 下线

```text
注册中心宕机
  ↓
Consumer 可能无法及时感知节点下线
  ↓
调用失败后通过容错机制处理
```

---

## 9. ZooKeeper 注册中心原理

ZooKeeper 注册中心通常基于节点结构保存服务信息。

大致结构：

```text
/dubbo
  /com.demo.UserService
    /providers
      /dubbo://192.168.1.10:20880/...
      /dubbo://192.168.1.11:20880/...
    /consumers
    /routers
    /configurators
```

Provider 启动：

```text
在 providers 目录下创建临时节点
```

Consumer 启动：

```text
订阅 providers 目录
监听子节点变化
```

Provider 下线：

```text
临时节点自动删除
ZooKeeper 通知 Consumer
Consumer 更新本地 Provider 列表
```

### 特点

- 临时节点适合感知服务上下线。
- Watcher 机制可以通知 Consumer 地址变化。
- 偏 CP，一致性较强。
- 大规模服务场景下 Watcher 压力需要关注。

---

## 10. Nacos 注册中心原理

Nacos 注册中心主要维护服务实例列表，并支持实例健康检查和变更通知。

Provider 启动：

```text
向 Nacos 注册服务实例
```

Consumer 启动：

```text
从 Nacos 订阅服务实例列表
```

实例变化：

```text
Nacos 推送变更给 Consumer
Consumer 更新本地服务列表
```

Dubbo 官方示例中也展示了使用 Nacos 作为注册中心实现自动服务发现。:contentReference[oaicite:6]{index=6}

### 特点

- 同时支持注册中心和配置中心。
- 支持健康检查。
- 支持服务分组、命名空间。
- 国内 Spring Cloud Alibaba 体系常用。

---

## 11. Registry SPI 扩展

Dubbo 注册中心本身也是 SPI 扩展点。

核心接口：

```text
RegistryFactory
Registry
```

Dubbo 官方 Registry 扩展文档说明，注册中心扩展用于服务注册与发现，扩展接口包括 `RegistryFactory` 和 `Registry`。:contentReference[oaicite:7]{index=7}

自定义注册中心时，一般需要：

```text
实现 RegistryFactory
实现 Registry
通过 SPI 文件注册扩展
```

SPI 文件：

```text
META-INF/dubbo/org.apache.dubbo.registry.RegistryFactory
```

内容：

```text
xxx=com.demo.XxxRegistryFactory
```

---

## 12. 注册中心相关核心对象

| 对象 | 作用 |
|---|---|
| Registry | 注册中心操作接口 |
| RegistryFactory | 创建 Registry |
| RegistryProtocol | 负责注册中心协议下的服务暴露和引用 |
| Directory | 维护可用 Invoker 列表 |
| NotifyListener | 接收注册中心地址变更通知 |
| Invoker | Dubbo 统一调用模型 |
| URL | Dubbo 中服务地址和参数的统一描述 |

---

## 13. 注册中心和负载均衡关系

注册中心负责：

```text
提供 Provider 地址列表
```

负载均衡负责：

```text
从 Provider 地址列表中选择一个节点
```

调用链路：

```text
Registry 获取地址
  ↓
Directory 保存 Invoker 列表
  ↓
Router 过滤 Invoker
  ↓
LoadBalance 选择 Invoker
  ↓
发起 RPC 调用
```

---

## 14. Dubbo 2 和 Dubbo 3 服务发现差异

Dubbo 服务发现机制从 Dubbo 2 的 **接口级服务发现** 演进到 Dubbo 3 的 **应用级服务发现**。:contentReference[oaicite:8]{index=8}

### 接口级服务发现

```text
以接口为维度注册服务地址
```

示例：

```text
com.demo.UserService -> Provider 地址列表
```

### 应用级服务发现

```text
以应用为维度注册实例
接口信息放入元数据中心
```

优势：

- 注册数据量更小。
- 更适合大规模微服务。
- 更适合云原生和应用级治理。

---

## 15. 总结

Dubbo 注册中心用于服务注册和服务发现。Provider 启动后会先暴露本地服务，然后将服务地址注册到注册中心；Consumer 启动后向注册中心订阅服务，获取 Provider 地址列表，并在本地生成 Invoker 列表。

注册中心只参与服务发现，不参与每次 RPC 调用。Consumer 真正调用 Provider 时，会从本地缓存的 Invoker 列表中经过路由、负载均衡选择一个 Provider 直接调用。

当 Provider 上线、下线或配置变化时，注册中心会通知 Consumer，Consumer 更新本地地址列表。即使注册中心短暂不可用，只要 Consumer 已经缓存了 Provider 地址，已有服务调用通常仍然可以继续。

Dubbo 常见注册中心有 ZooKeeper、Nacos、Consul 等。ZooKeeper 通常通过临时节点和 Watcher 感知上下线；Nacos 通过服务实例注册、健康检查和变更推送实现服务发现。

一句话总结：

```text
Dubbo 注册中心 = Provider 注册地址 + Consumer 订阅地址 + 注册中心推送变更 + Consumer 本地缓存调用。
```
