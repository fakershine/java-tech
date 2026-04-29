# 微服务注册中心、配置中心与 Spring Cloud 常见面试题总结

## 目录

- [一、注册中心的作用是什么](#一注册中心的作用是什么)
- [二、Nacos 注册中心原理](#二nacos-注册中心原理)
- [三、Nacos 配置中心原理](#三nacos-配置中心原理)
- [四、Nacos 和 Eureka 区别](#四nacos-和-eureka-区别)
- [五、服务注册和服务发现流程](#五服务注册和服务发现流程)
- [六、服务下线后消费者如何感知](#六服务下线后消费者如何感知)
- [七、负载均衡怎么做](#七负载均衡怎么做)
- [八、OpenFeign 原理](#八openfeign-原理)
- [九、Gateway 网关原理](#九gateway-网关原理)
- [十、Sentinel 限流熔断原理](#十sentinel-限流熔断原理)
- [十一、分布式配置如何动态刷新](#十一分布式配置如何动态刷新)
- [十二、面试速记版](#十二面试速记版)
- [十三、总览表](#十三总览表)
- [十四、完整面试回答模板](#十四完整面试回答模板)

---

# 一、注册中心的作用是什么

## 1. 注册中心是什么？

注册中心是微服务架构中的核心基础组件。

它主要用于管理服务实例信息。

简单来说：

> 注册中心用于保存服务提供者的地址信息，并让服务消费者能够动态发现可用服务。

---

## 2. 为什么需要注册中心？

在单体应用中，服务调用通常是本地方法调用。

但在微服务架构中，一个系统会被拆分成多个服务，例如：

```text
用户服务
订单服务
支付服务
库存服务
优惠券服务
```

这些服务通常部署在不同机器或容器中。

服务地址可能会因为以下原因不断变化：

1. 服务扩容；
2. 服务缩容；
3. 服务重启；
4. 容器漂移；
5. 实例故障；
6. 灰度发布；
7. 滚动升级。

如果消费者写死服务地址，就会非常不灵活。

---

## 3. 没有注册中心的问题

如果没有注册中心，服务消费者可能需要这样调用：

```text
http://192.168.1.10:8080/order/create
http://192.168.1.11:8080/order/create
http://192.168.1.12:8080/order/create
```

问题包括：

1. 服务地址写死；
2. 实例扩缩容困难；
3. 服务故障无法自动剔除；
4. 消费者无法感知服务变化；
5. 负载均衡实现复杂；
6. 运维成本高。

---

## 4. 有注册中心后的调用方式

引入注册中心后，服务消费者只需要知道服务名。

例如：

```text
order-service
```

消费者调用时：

```text
根据服务名 order-service 从注册中心获取可用实例列表，
然后选择一个实例发起调用。
```

---

## 5. 注册中心的核心作用

注册中心主要有以下作用：

```text
1. 服务注册
2. 服务发现
3. 服务健康检查
4. 服务上下线感知
5. 服务实例管理
6. 元数据管理
7. 配合负载均衡
8. 支持服务治理
```

---

## 6. 服务注册

服务启动时，会把自己的信息注册到注册中心。

注册信息通常包括：

| 信息 | 说明 |
|---|---|
| 服务名 | 例如 `order-service` |
| IP | 服务实例 IP |
| 端口 | 服务实例端口 |
| 集群名 | 所属集群 |
| 命名空间 | 环境隔离 |
| 权重 | 负载均衡权重 |
| 元数据 | 版本、区域、标签等 |
| 健康状态 | 是否健康 |

---

## 7. 服务发现

服务消费者调用服务时，会向注册中心查询服务实例列表。

例如：

```text
服务名：order-service
实例列表：
192.168.1.10:8080
192.168.1.11:8080
192.168.1.12:8080
```

然后消费者通过负载均衡算法选择一个实例进行调用。

---

## 8. 健康检查

注册中心通常会检测服务实例是否健康。

常见方式：

1. 服务主动发送心跳；
2. 注册中心主动探测服务；
3. 客户端长连接保活；
4. 根据连接状态判断实例存活；
5. 根据超时时间剔除异常实例。

---

## 9. 服务上下线感知

当服务实例上线、下线、异常或恢复时，注册中心会更新服务列表。

消费者可以通过以下方式感知变化：

```text
1. 主动轮询注册中心
2. 注册中心推送变更
3. 客户端订阅服务变更
4. 本地缓存定期刷新
```

---

## 10. 面试回答

注册中心是微服务架构中的服务治理组件，主要用于管理服务实例信息。

服务提供者启动时会把自己的服务名、IP、端口、元数据等信息注册到注册中心。服务消费者调用服务时，根据服务名从注册中心获取可用实例列表，然后通过负载均衡选择一个实例进行调用。

注册中心还负责服务健康检查、服务上下线感知、实例剔除、元数据管理等能力，从而避免服务地址写死，提高系统的可扩展性和可用性。

---

# 二、Nacos 注册中心原理

## 1. Nacos 是什么？

Nacos 是阿里开源的服务发现、配置管理和服务治理平台。

它可以作为：

```text
1. 注册中心
2. 配置中心
3. 服务治理平台
```

在 Spring Cloud Alibaba 体系中，Nacos 常用于替代 Eureka 和 Spring Cloud Config。

---

## 2. Nacos 注册中心核心概念

Nacos 注册中心中常见概念：

| 概念 | 说明 |
|---|---|
| Namespace | 命名空间，用于环境隔离 |
| Group | 分组，用于业务隔离 |
| Service | 服务，例如 `order-service` |
| Cluster | 集群，例如 BJ、SH、HZ |
| Instance | 服务实例，即具体 IP + 端口 |
| Metadata | 元数据，例如版本、区域、标签 |
| Weight | 权重，用于负载均衡 |
| Healthy | 健康状态 |
| Ephemeral | 是否临时实例 |

---

## 3. Namespace 的作用

Namespace 用于环境隔离。

常见用法：

```text
dev 环境
test 环境
prod 环境
```

不同 Namespace 下的服务和配置相互隔离。

---

## 4. Group 的作用

Group 用于对服务或配置进行分组。

默认分组通常是：

```text
DEFAULT_GROUP
```

可以根据业务划分：

```text
ORDER_GROUP
PAY_GROUP
USER_GROUP
```

---

## 5. Service 和 Instance

一个 Service 表示一个服务。

例如：

```text
order-service
```

一个 Service 下可以有多个 Instance。

例如：

```text
order-service
├── 192.168.1.10:8080
├── 192.168.1.11:8080
└── 192.168.1.12:8080
```

---

## 6. Nacos 注册流程

服务提供者启动后，会把自己的实例信息注册到 Nacos。

流程如下：

```text
1. 服务启动
2. 读取服务名、IP、端口、namespace、group、metadata 等信息
3. Nacos Client 向 Nacos Server 发起注册请求
4. Nacos Server 保存服务实例信息
5. 临时实例定期发送心跳或通过连接保活
6. Nacos Server 更新实例健康状态
7. 消费者可以查询或订阅该服务实例列表
```

---

## 7. 注册信息包括什么？

Nacos 注册实例时，常见信息包括：

```text
服务名
IP
端口
集群名
命名空间
分组
权重
是否健康
是否临时实例
元数据
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
        cluster-name: BJ
        metadata:
          version: v1
```

---

## 8. 临时实例和持久实例

Nacos 实例分为两类：

```text
1. 临时实例
2. 持久实例
```

---

### 8.1 临时实例

临时实例适合普通微服务实例。

特点：

```text
服务下线或心跳超时后，Nacos 会自动删除实例。
```

临时实例更适合动态扩缩容场景。

---

### 8.2 持久实例

持久实例适合相对稳定的服务实例。

特点：

```text
即使实例不可用，Nacos 通常不会直接删除实例，而是标记为不健康。
```

---

## 9. 心跳机制

服务实例注册后，需要持续向 Nacos 证明自己还活着。

常见方式：

```text
客户端定期发送心跳；
服务端根据心跳更新时间判断实例是否健康。
```

如果实例长时间没有心跳，Nacos 会将实例标记为不健康，甚至剔除。

---

## 10. 服务发现机制

消费者调用服务时，会从 Nacos 获取服务实例列表。

流程：

```text
1. 消费者根据服务名查询 Nacos
2. Nacos 返回可用服务实例列表
3. 客户端缓存实例列表
4. 客户端通过负载均衡选择实例
5. 发起远程调用
```

---

## 11. 订阅与推送机制

消费者可以订阅某个服务。

当服务实例发生变化时，例如：

```text
新增实例
实例下线
实例不健康
权重变化
元数据变化
```

Nacos 会通知客户端更新本地服务列表。

实际实现上，不同版本 Nacos 在通信机制上会有差异，例如 HTTP、长轮询、gRPC 长连接等。

---

## 12. Nacos 集群一致性

Nacos 为了兼顾不同场景，支持不同一致性模式。

常见理解：

```text
临时实例更偏 AP；
持久实例更偏 CP。
```

也就是说：

1. 临时实例更强调可用性；
2. 持久实例更强调一致性。

---

## 13. Nacos 注册中心整体原理图

```text
服务提供者
    |
    | 注册服务实例
    v
Nacos Server 集群
    |
    | 保存服务列表、健康状态、元数据
    v
服务消费者
    |
    | 查询/订阅服务实例
    v
本地服务列表缓存
    |
    | 负载均衡
    v
调用具体服务实例
```

---

## 14. 面试回答

Nacos 作为注册中心时，服务提供者启动后会通过 Nacos Client 将服务名、IP、端口、命名空间、分组、集群、权重、元数据等信息注册到 Nacos Server。

Nacos Server 会保存服务实例信息，并通过心跳或长连接机制判断实例是否健康。服务消费者根据服务名从 Nacos 获取实例列表，并在本地缓存。实例发生变化时，Nacos 会通过订阅和推送机制通知消费者更新本地缓存。

Nacos 支持临时实例和持久实例，临时实例更适合微服务动态扩缩容场景，实例异常后可以自动剔除；持久实例更适合固定服务，异常时通常标记为不健康。

---

# 三、Nacos 配置中心原理

## 1. 配置中心是什么？

配置中心用于统一管理应用配置。

例如：

```text
数据库连接配置
Redis 配置
线程池配置
限流规则
开关配置
日志级别
业务参数
```

---

## 2. 为什么需要配置中心？

如果没有配置中心，配置通常写在项目本地：

```text
application.yml
application.properties
```

问题包括：

1. 配置分散在各个项目中；
2. 修改配置需要重新打包；
3. 修改配置需要重启服务；
4. 多环境配置管理困难；
5. 配置变更无法统一审计；
6. 动态开关难以实现。

---

## 3. Nacos 配置中心核心概念

Nacos 配置中心核心由以下几个维度定位一份配置：

```text
Namespace + Group + DataId
```

---

## 4. DataId

DataId 是配置文件的唯一标识。

常见 DataId：

```text
order-service.yaml
order-service-dev.yaml
user-service.yaml
sentinel-rules.json
```

---

## 5. Group

Group 是配置分组。

默认是：

```text
DEFAULT_GROUP
```

可以按业务划分：

```text
ORDER_GROUP
PAY_GROUP
COMMON_GROUP
```

---

## 6. Namespace

Namespace 用于环境隔离。

例如：

```text
dev
test
prod
```

同一个 DataId 和 Group，在不同 Namespace 中可以有不同配置。

---

## 7. Nacos 配置加载流程

应用启动时，Nacos 配置加载流程大致如下：

```text
1. 应用启动
2. 读取 bootstrap 或 Spring Boot 配置导入信息
3. 根据 server-addr、namespace、group、dataId 连接 Nacos
4. 从 Nacos Server 拉取远程配置
5. 将远程配置加载到 Spring Environment
6. 创建 Bean 时读取 Environment 中的配置属性
7. 应用启动完成
```

---

## 8. Nacos 配置动态刷新原理

Nacos 配置动态刷新核心思想：

```text
客户端监听配置变化；
配置变化后拉取最新配置；
刷新 Spring Environment；
重新绑定相关 Bean 属性。
```

流程如下：

```text
1. 客户端向 Nacos Server 监听某个 DataId
2. Nacos Server 发现配置发生变化
3. 通知客户端配置已变化
4. 客户端重新拉取最新配置
5. 更新本地缓存和 Spring Environment
6. 发布配置变更事件
7. 刷新被 @RefreshScope 或 @ConfigurationProperties 管理的 Bean
```

---

## 9. 长轮询机制

配置中心常见实现方式是长轮询。

大致过程：

```text
1. 客户端发起监听请求
2. 服务端暂时挂起请求
3. 如果配置发生变化，立即返回
4. 如果超时仍无变化，返回无变化
5. 客户端继续发起下一次监听请求
```

这样可以减少频繁轮询带来的压力，同时让配置变更尽快被客户端感知。

在 Nacos 2.x 中，也大量使用长连接机制来提升通信效率。

---

## 10. 本地缓存机制

Nacos 客户端通常会维护本地配置缓存。

作用：

1. 提高读取性能；
2. 降低对 Nacos Server 的依赖；
3. Nacos Server 短暂不可用时，应用仍可使用本地快照启动；
4. 避免配置中心抖动影响业务启动。

---

## 11. 配置刷新方式

Spring Cloud Alibaba Nacos Config 常见动态刷新方式：

```text
1. @RefreshScope
2. @ConfigurationProperties
3. Nacos Config Listener
4. EnvironmentChangeEvent
```

---

## 12. @Value 动态刷新注意点

普通 `@Value` 注入的字段不一定会自动刷新。

示例：

```java
@Value("${demo.name}")
private String name;
```

如果要让其动态刷新，通常需要配合：

```java
@RefreshScope
```

示例：

```java
@RefreshScope
@RestController
public class DemoController {

    @Value("${demo.name}")
    private String name;
}
```

---

## 13. @ConfigurationProperties 动态刷新

推荐使用：

```java
@ConfigurationProperties(prefix = "demo")
```

示例：

```java
@Component
@ConfigurationProperties(prefix = "demo")
public class DemoProperties {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

这种方式更适合管理一组配置。

---

## 14. Nacos 配置中心整体原理图

```text
Nacos Server
    |
    | 保存配置
    v
配置数据
    |
    | 客户端启动时拉取
    v
Spring Environment
    |
    | Bean 属性绑定
    v
业务代码读取配置

配置变更：
Nacos Server 配置变更
    |
    v
通知客户端
    |
    v
客户端拉取最新配置
    |
    v
刷新 Environment / Bean
```

---

## 15. 面试回答

Nacos 配置中心通过 `Namespace + Group + DataId` 唯一定位一份配置。

应用启动时，Nacos Client 会根据配置的服务地址、命名空间、分组和 DataId 从 Nacos Server 拉取远程配置，并加载到 Spring Environment 中。应用运行过程中，客户端会监听配置变化。当配置发生变化后，Nacos 会通知客户端，客户端重新拉取最新配置，更新本地缓存和 Spring Environment，并触发配置刷新。

如果 Bean 使用了 `@RefreshScope` 或 `@ConfigurationProperties`，就可以实现配置动态刷新。

---

# 四、Nacos 和 Eureka 区别

## 1. Eureka 是什么？

Eureka 是 Netflix 开源的服务注册与发现组件。

它是早期 Spring Cloud Netflix 体系中常用的注册中心。

主要功能：

```text
服务注册
服务发现
心跳检测
服务剔除
自我保护
```

---

## 2. Nacos 是什么？

Nacos 是阿里开源的服务发现、配置管理和服务治理平台。

它不仅可以作为注册中心，还可以作为配置中心。

主要功能：

```text
服务注册与发现
配置管理
服务健康检查
动态 DNS
服务元数据管理
服务治理
```

---

## 3. Nacos 和 Eureka 核心区别

| 对比项 | Nacos | Eureka |
|---|---|---|
| 功能定位 | 注册中心 + 配置中心 + 服务治理 | 主要是注册中心 |
| 配置中心 | 支持 | 不支持 |
| 一致性模型 | 支持 AP 和 CP 场景 | 偏 AP |
| 实例类型 | 支持临时实例和持久实例 | 主要是临时实例 |
| 健康检查 | 支持客户端心跳、服务端检查等 | 主要依赖客户端心跳 |
| 服务推送 | 支持订阅和推送 | 客户端定时拉取为主 |
| 雪崩保护 | 有健康保护等机制 | 有自我保护机制 |
| 控制台 | 功能较丰富 | 相对简单 |
| Spring Cloud Alibaba 集成 | 原生集成 | Spring Cloud Netflix 体系 |
| 配置动态刷新 | 支持 | 不支持 |

---

## 4. 功能范围不同

Eureka 主要解决：

```text
服务注册与发现
```

Nacos 解决：

```text
服务注册与发现
配置管理
服务治理
元数据管理
动态刷新
```

因此 Nacos 的功能范围更广。

---

## 5. 一致性模型不同

Eureka 更偏向 AP。

也就是说：

```text
在分区故障时，优先保证可用性。
```

Nacos 支持不同模式：

```text
临时实例偏 AP；
持久实例偏 CP。
```

因此 Nacos 在不同业务场景下更灵活。

---

## 6. 配置中心能力不同

Eureka 本身不提供配置中心能力。

如果使用 Eureka，通常还需要搭配：

```text
Spring Cloud Config
Apollo
Nacos Config
```

而 Nacos 自带配置中心能力。

---

## 7. 服务变更感知方式不同

Eureka 客户端通常通过定时拉取注册表来感知服务变化。

Nacos 支持客户端订阅服务，服务实例变化后可以通知客户端更新本地缓存。

---

## 8. 面试回答

Nacos 和 Eureka 都可以作为注册中心，但 Nacos 功能更丰富。

Eureka 主要提供服务注册与发现，整体偏 AP，服务消费者通常通过定时拉取注册表感知服务变化。Nacos 不仅支持服务注册与发现，还支持配置中心和服务治理，支持临时实例和持久实例，临时实例偏 AP，持久实例偏 CP，并支持服务订阅和变更推送。

因此在 Spring Cloud Alibaba 体系中，Nacos 通常可以同时承担注册中心和配置中心的角色。

---

# 五、服务注册和服务发现流程

## 1. 服务注册流程

服务注册指服务提供者启动后，将自己的实例信息注册到注册中心。

流程如下：

```text
1. 服务提供者启动
2. 读取应用名称、IP、端口、元数据等信息
3. 创建注册中心客户端
4. 向注册中心发起注册请求
5. 注册中心保存实例信息
6. 服务提供者定期发送心跳或维持长连接
7. 注册中心更新实例健康状态
```

---

## 2. 服务注册示例

配置：

```yaml
spring:
  application:
    name: order-service
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
```

启动后，`order-service` 会注册到 Nacos。

注册信息类似：

```text
服务名：order-service
IP：192.168.1.10
端口：8080
状态：healthy
```

---

## 3. 服务发现流程

服务发现指消费者根据服务名获取服务实例列表。

流程如下：

```text
1. 服务消费者启动
2. 消费者根据服务名查询注册中心
3. 注册中心返回服务实例列表
4. 消费者缓存实例列表
5. 调用时通过负载均衡选择一个实例
6. 发起远程调用
7. 实例变化时更新本地缓存
```

---

## 4. 服务发现示例

消费者调用：

```text
http://order-service/order/create
```

实际过程：

```text
1. 根据 order-service 查询实例列表
2. 获取实例：
   192.168.1.10:8080
   192.168.1.11:8080
3. 负载均衡选择一个实例
4. 实际请求：
   http://192.168.1.10:8080/order/create
```

---

## 5. 服务注册和发现整体图

```text
服务提供者 order-service
        |
        | 注册 IP、端口、元数据
        v
注册中心 Nacos
        |
        | 查询/订阅 order-service
        v
服务消费者 user-service
        |
        | 负载均衡选择实例
        v
调用 order-service 某个实例
```

---

## 6. 面试回答

服务注册是服务提供者启动时，将自己的服务名、IP、端口、元数据等信息注册到注册中心，并通过心跳或长连接维持健康状态。

服务发现是服务消费者调用服务时，根据服务名从注册中心获取可用实例列表，然后通过负载均衡选择一个实例进行调用。

当服务实例发生上下线或健康状态变化时，注册中心会更新服务列表，并通知或等待消费者刷新本地缓存。

---

# 六、服务下线后消费者如何感知

## 1. 服务下线类型

服务下线通常分为两种：

```text
1. 主动下线
2. 异常下线
```

---

## 2. 主动下线

主动下线是指服务正常关闭时，主动通知注册中心注销实例。

例如：

```text
应用优雅停机
服务发布重启
实例缩容
```

流程：

```text
1. 服务收到关闭信号
2. 执行优雅停机
3. 注册中心客户端发送注销请求
4. 注册中心删除或标记该实例不可用
5. 消费者感知实例列表变化
```

---

## 3. 异常下线

异常下线是指服务突然故障，没有机会主动注销。

例如：

```text
进程崩溃
服务器宕机
网络中断
容器被强杀
```

流程：

```text
1. 服务实例停止发送心跳或连接断开
2. 注册中心检测到超时
3. 标记实例不健康
4. 超过剔除时间后删除实例
5. 消费者刷新本地实例列表
```

---

## 4. 消费者感知方式

消费者感知服务下线主要有几种方式：

```text
1. 注册中心推送变更
2. 客户端订阅服务变化
3. 客户端定时拉取服务列表
4. 本地缓存定时刷新
5. 调用失败后主动重试或剔除
```

---

## 5. Nacos 中的感知方式

在 Nacos 中，消费者通常会订阅服务实例变化。

当服务实例下线、不健康或元数据变化时，Nacos 会通知客户端。

客户端收到通知后，会更新本地缓存。

---

## 6. 为什么消费者可能短时间还调用到下线实例？

因为服务下线感知不是绝对实时的。

可能存在以下延迟：

1. 心跳超时检测需要时间；
2. 注册中心推送有延迟；
3. 消费者本地缓存刷新有延迟；
4. 负载均衡缓存有延迟；
5. 网关或客户端连接池中还有旧连接；
6. 服务正在优雅停机但仍有请求进入。

---

## 7. 如何减少调用到下线实例？

常见优化措施：

1. 服务优雅停机；
2. 下线前先从注册中心摘除；
3. 等待一段时间再关闭进程；
4. 配合 Kubernetes readiness 探针；
5. 缩短健康检查间隔；
6. 客户端开启失败重试；
7. 负载均衡过滤不健康实例；
8. 网关及时刷新路由；
9. 熔断降级保护调用方。

---

## 8. 面试回答

服务下线后，消费者主要通过注册中心的服务变更通知、服务订阅、本地缓存刷新或定时拉取来感知。

如果是主动下线，服务会在关闭前向注册中心注销实例，注册中心更新服务列表后通知消费者。如果是异常下线，注册中心会根据心跳超时或连接断开判断实例不健康，随后剔除实例并通知消费者。

由于注册中心检测、推送和客户端缓存刷新都可能存在延迟，所以消费者短时间内仍可能调用到下线实例。通常需要配合优雅停机、健康检查、失败重试、熔断降级和负载均衡过滤来降低影响。

---

# 七、负载均衡怎么做

## 1. 什么是负载均衡？

负载均衡是指：

> 当一个服务有多个实例时，将请求按照一定算法分配到不同实例上。

例如：

```text
order-service
├── 192.168.1.10:8080
├── 192.168.1.11:8080
└── 192.168.1.12:8080
```

消费者调用 `order-service` 时，需要选择其中一个实例。

---

## 2. 负载均衡的作用

负载均衡的作用：

1. 分摊请求压力；
2. 提高系统吞吐量；
3. 提高服务可用性；
4. 避免单实例过载；
5. 支持服务扩缩容；
6. 支持灰度发布；
7. 支持故障实例剔除。

---

## 3. 负载均衡分类

常见分为两类：

```text
1. 服务端负载均衡
2. 客户端负载均衡
```

---

## 4. 服务端负载均衡

服务端负载均衡由独立组件转发请求。

常见组件：

```text
Nginx
LVS
HAProxy
硬件负载均衡器
云负载均衡 SLB
```

流程：

```text
客户端 -> Nginx/SLB -> 后端服务实例
```

特点：

1. 客户端无感知；
2. 统一入口；
3. 运维集中；
4. 转发层可能成为瓶颈；
5. 适合外部流量入口。

---

## 5. 客户端负载均衡

客户端负载均衡由调用方自己选择服务实例。

例如 Spring Cloud 中常见：

```text
Spring Cloud LoadBalancer
Ribbon，老项目中常见
```

流程：

```text
消费者 -> 注册中心获取实例列表 -> 本地负载均衡选择实例 -> 发起调用
```

特点：

1. 不需要独立转发组件；
2. 调用方直接访问服务实例；
3. 性能较好；
4. 每个消费者都需要维护负载均衡逻辑；
5. 适合微服务内部调用。

---

## 6. 常见负载均衡算法

| 算法 | 说明 |
|---|---|
| 轮询 | 按顺序依次选择实例 |
| 随机 | 随机选择一个实例 |
| 加权轮询 | 权重高的实例分配更多请求 |
| 加权随机 | 按权重随机选择 |
| 最少连接 | 选择连接数最少的实例 |
| 最短响应时间 | 选择响应时间最短的实例 |
| 一致性哈希 | 相同 key 尽量路由到同一实例 |
| 区域优先 | 优先选择同区域或同集群实例 |

---

## 7. Nacos 权重负载均衡

Nacos 支持实例权重。

例如：

```text
实例 A 权重：1
实例 B 权重：5
实例 C 权重：10
```

权重越高，理论上获得请求的概率越大。

适合场景：

```text
机器配置不同
灰度发布
流量倾斜
新版本小流量验证
```

---

## 8. Spring Cloud LoadBalancer

Spring Cloud LoadBalancer 是 Spring Cloud 官方提供的客户端负载均衡组件。

常见使用方式：

```java
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

调用：

```java
restTemplate.getForObject("http://order-service/order/1", String.class);
```

其中 `order-service` 会被解析成具体服务实例。

---

## 9. OpenFeign 中的负载均衡

OpenFeign 集成 Spring Cloud LoadBalancer 后，可以根据服务名调用。

示例：

```java
@FeignClient(name = "order-service")
public interface OrderFeignClient {

    @GetMapping("/order/{id}")
    OrderDTO getOrder(@PathVariable Long id);
}
```

Feign 调用时会根据 `order-service` 获取实例列表，并通过负载均衡选择实例。

---

## 10. Gateway 中的负载均衡

Spring Cloud Gateway 可以通过服务名进行路由。

示例：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-route
          uri: lb://order-service
          predicates:
            - Path=/order/**
```

其中：

```text
lb://order-service
```

表示通过负载均衡选择 `order-service` 的一个实例。

---

## 11. 面试回答

负载均衡是指当一个服务有多个实例时，按照一定算法选择一个实例处理请求。

负载均衡分为服务端负载均衡和客户端负载均衡。服务端负载均衡通常通过 Nginx、SLB 等组件实现，客户端无感知；客户端负载均衡由调用方从注册中心获取实例列表，然后本地选择一个实例发起调用，例如 Spring Cloud LoadBalancer。

常见算法有轮询、随机、加权轮询、加权随机、最少连接、最短响应时间和一致性哈希。Nacos 支持实例权重，可以结合负载均衡实现灰度发布和流量倾斜。

---

# 八、OpenFeign 原理

## 1. OpenFeign 是什么？

OpenFeign 是一个声明式 HTTP 客户端。

它可以让我们像调用本地接口一样调用远程 HTTP 服务。

示例：

```java
@FeignClient(name = "order-service")
public interface OrderFeignClient {

    @GetMapping("/order/{id}")
    OrderDTO getOrder(@PathVariable Long id);
}
```

调用时：

```java
orderFeignClient.getOrder(1L);
```

看起来像本地方法调用，但底层会发送 HTTP 请求。

---

## 2. OpenFeign 解决了什么问题？

如果不用 Feign，远程调用可能需要手写：

```java
RestTemplate
WebClient
HttpClient
OkHttp
```

代码会比较繁琐：

```java
String url = "http://order-service/order/" + id;
OrderDTO order = restTemplate.getForObject(url, OrderDTO.class);
```

Feign 可以通过接口和注解简化远程调用。

---

## 3. OpenFeign 核心原理

OpenFeign 核心原理：

```text
1. 扫描 @FeignClient 接口
2. 为接口创建动态代理对象
3. 解析接口上的 Spring MVC 注解
4. 方法调用时构造 HTTP 请求
5. 结合负载均衡选择服务实例
6. 发送 HTTP 请求
7. 使用 Decoder 将响应反序列化为 Java 对象
```

---

## 4. @EnableFeignClients

要使用 Feign，通常需要开启：

```java
@EnableFeignClients
@SpringBootApplication
public class DemoApplication {
}
```

`@EnableFeignClients` 会扫描 `@FeignClient` 接口，并注册代理对象。

---

## 5. @FeignClient 的作用

示例：

```java
@FeignClient(name = "order-service")
public interface OrderFeignClient {
}
```

其中：

```text
name 表示服务名；
Feign 会通过服务名从注册中心获取实例。
```

也可以指定 URL：

```java
@FeignClient(name = "orderClient", url = "http://localhost:8080")
public interface OrderFeignClient {
}
```

---

## 6. 动态代理

Feign 会为接口生成代理对象。

当调用：

```java
orderFeignClient.getOrder(1L);
```

实际调用的是代理对象的逻辑。

代理对象会根据方法上的注解生成 HTTP 请求。

---

## 7. 注解解析

Feign 会解析接口上的注解：

```java
@GetMapping("/order/{id}")
OrderDTO getOrder(@PathVariable Long id);
```

解析结果包括：

```text
HTTP 方法：GET
请求路径：/order/{id}
路径参数：id
返回类型：OrderDTO
```

---

## 8. 编码器和解码器

Feign 中有两个重要组件：

```text
Encoder
Decoder
```

---

### 8.1 Encoder

Encoder 用于把 Java 对象转换成 HTTP 请求体。

例如：

```java
@PostMapping("/order")
Long createOrder(@RequestBody OrderCreateDTO dto);
```

`OrderCreateDTO` 会被编码成 JSON 请求体。

---

### 8.2 Decoder

Decoder 用于把 HTTP 响应转换成 Java 对象。

例如响应 JSON：

```json
{
  "id": 1,
  "name": "订单"
}
```

会被转换成：

```java
OrderDTO
```

---

## 9. Contract

Contract 用于解析接口注解。

Spring Cloud OpenFeign 支持 Spring MVC 注解，例如：

```text
@GetMapping
@PostMapping
@RequestParam
@PathVariable
@RequestBody
```

---

## 10. 负载均衡

如果 Feign 使用服务名调用：

```java
@FeignClient(name = "order-service")
```

那么 Feign 会结合 Spring Cloud LoadBalancer：

```text
1. 根据 order-service 查询实例列表
2. 选择一个实例
3. 拼接真实请求地址
4. 发起 HTTP 调用
```

---

## 11. 超时和重试

Feign 调用需要配置合理的超时时间。

示例：

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connectTimeout: 3000
            readTimeout: 5000
```

注意：

```text
远程调用一定要设置超时，避免线程长时间阻塞。
```

---

## 12. Feign 拦截器

可以通过 `RequestInterceptor` 统一添加请求头。

示例：

```java
@Bean
public RequestInterceptor requestInterceptor() {
    return template -> {
        template.header("Authorization", "token");
        template.header("TraceId", MDC.get("traceId"));
    };
}
```

常见用途：

1. 透传 token；
2. 透传 traceId；
3. 添加租户 ID；
4. 添加灰度标识；
5. 添加公共请求头。

---

## 13. Feign 原理图

```text
业务代码调用 Feign 接口
        |
        v
Feign 动态代理对象
        |
        v
解析方法注解，构造 RequestTemplate
        |
        v
Encoder 编码请求
        |
        v
负载均衡选择服务实例
        |
        v
HTTP Client 发送请求
        |
        v
Decoder 解码响应
        |
        v
返回 Java 对象
```

---

## 14. 面试回答

OpenFeign 是声明式 HTTP 客户端，可以通过接口和注解的方式调用远程服务。

它的核心原理是：Spring 启动时扫描 `@FeignClient` 接口，并为接口创建动态代理对象。调用接口方法时，代理对象会解析方法上的 Spring MVC 注解，构造 HTTP 请求，再结合注册中心和负载均衡选择具体服务实例，最终通过 HTTP Client 发起请求，并通过 Decoder 将响应转换成 Java 对象。

---

# 九、Gateway 网关原理

## 1. Gateway 是什么？

Spring Cloud Gateway 是 Spring Cloud 官方提供的 API 网关组件。

它主要用于微服务系统的统一入口。

常见职责：

```text
路由转发
权限认证
限流熔断
日志审计
跨域处理
灰度发布
请求改写
响应改写
负载均衡
统一鉴权
```

---

## 2. 为什么需要网关？

在微服务架构中，客户端如果直接访问各个微服务，会出现很多问题：

1. 客户端需要知道所有服务地址；
2. 认证鉴权逻辑分散；
3. 跨域处理分散；
4. 日志审计分散；
5. 限流熔断难以统一；
6. 服务暴露过多；
7. 前端调用复杂。

网关可以作为统一入口。

---

## 3. Gateway 核心概念

Spring Cloud Gateway 有三个核心概念：

```text
1. Route
2. Predicate
3. Filter
```

---

## 4. Route 路由

Route 是网关的基本转发规则。

一个 Route 通常包括：

```text
id
uri
predicates
filters
```

示例：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-route
          uri: lb://order-service
          predicates:
            - Path=/order/**
          filters:
            - StripPrefix=1
```

含义：

```text
如果请求路径匹配 /order/**，
就转发到 order-service。
```

---

## 5. Predicate 断言

Predicate 用于判断请求是否匹配某个路由。

常见 Predicate：

| Predicate | 说明 |
|---|---|
| Path | 根据路径匹配 |
| Method | 根据 HTTP 方法匹配 |
| Header | 根据请求头匹配 |
| Query | 根据请求参数匹配 |
| Host | 根据 Host 匹配 |
| Cookie | 根据 Cookie 匹配 |
| After | 某个时间之后匹配 |
| Before | 某个时间之前匹配 |
| Between | 某个时间区间匹配 |

---

## 6. Filter 过滤器

Filter 用于在请求转发前后进行处理。

常见功能：

1. 添加请求头；
2. 删除请求头；
3. 修改请求路径；
4. 权限校验；
5. 日志打印；
6. 限流；
7. 熔断；
8. 请求体修改；
9. 响应体修改。

---

## 7. Filter 分类

Spring Cloud Gateway Filter 主要分为：

```text
1. GlobalFilter
2. GatewayFilter
```

---

### 7.1 GlobalFilter

全局过滤器，对所有路由生效。

示例：

```java
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (token == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
```

---

### 7.2 GatewayFilter

局部过滤器，只对指定路由生效。

配置示例：

```yaml
filters:
  - StripPrefix=1
```

---

## 8. Gateway 请求处理流程

请求进入 Gateway 后，大致流程如下：

```text
1. 客户端请求进入 Gateway
2. RoutePredicateHandlerMapping 匹配路由
3. 找到符合条件的 Route
4. 构建 GatewayFilterChain
5. 执行前置过滤器逻辑
6. 根据 uri 转发到目标服务
7. 接收目标服务响应
8. 执行后置过滤器逻辑
9. 返回响应给客户端
```

---

## 9. Gateway 原理图

```text
Client
  |
  v
Spring Cloud Gateway
  |
  v
Route Predicate 匹配路由
  |
  v
Filter Chain 前置处理
  |
  v
LoadBalancer 选择服务实例
  |
  v
转发到后端服务
  |
  v
Filter Chain 后置处理
  |
  v
返回响应
```

---

## 10. Gateway 和 Nacos 集成

Gateway 可以通过 Nacos 发现服务。

配置：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
    gateway:
      routes:
        - id: order-route
          uri: lb://order-service
          predicates:
            - Path=/order/**
```

`lb://order-service` 表示：

```text
通过注册中心找到 order-service 的实例，
并通过负载均衡转发。
```

---

## 11. Gateway 动态路由

Gateway 路由可以来自：

```text
1. application.yml
2. Java 配置
3. 数据库
4. Nacos 配置中心
5. Redis
6. 自定义 RouteDefinitionRepository
```

如果将路由配置放到 Nacos，可以实现动态路由刷新。

---

## 12. 面试回答

Spring Cloud Gateway 是微服务网关组件，主要作为系统统一入口，负责路由转发、认证鉴权、限流熔断、日志审计、跨域处理等。

Gateway 的核心是 Route、Predicate 和 Filter。Route 表示一条路由规则，Predicate 用于判断请求是否匹配路由，Filter 用于在请求转发前后进行增强处理。

请求进入 Gateway 后，会先通过 Predicate 匹配路由，然后构建过滤器链，执行前置过滤器逻辑，再根据路由目标转发到后端服务，收到响应后执行后置过滤器逻辑，最后返回给客户端。

---

# 十、Sentinel 限流熔断原理

## 1. Sentinel 是什么？

Sentinel 是阿里开源的流量治理组件。

它主要用于保障微服务系统稳定性。

核心能力包括：

```text
流量控制
熔断降级
系统自适应保护
热点参数限流
实时监控
规则动态配置
```

---

## 2. Sentinel 的核心概念

| 概念 | 说明 |
|---|---|
| Resource | 资源，可以是接口、方法、URL |
| Rule | 规则，例如限流规则、熔断规则 |
| Slot | 插槽，用于组成责任链 |
| Context | 调用上下文 |
| Entry | 资源访问入口 |
| BlockException | 被限流或熔断时抛出的异常 |

---

## 3. Resource 资源

Sentinel 一切保护都围绕资源展开。

资源可以是：

```text
Controller 接口
Service 方法
Feign 调用
Gateway 路由
Dubbo 接口
自定义代码块
```

示例：

```java
@SentinelResource(value = "createOrder")
public void createOrder() {
    // 业务逻辑
}
```

---

## 4. Sentinel 工作流程

Sentinel 处理请求的大致流程：

```text
1. 请求进入资源
2. 创建 Entry
3. 经过 Slot 责任链
4. 统计 QPS、线程数、RT、异常等指标
5. 根据限流规则、熔断规则判断是否放行
6. 放行则执行业务逻辑
7. 不放行则抛出 BlockException
8. 退出资源并记录统计数据
```

---

## 5. Slot 责任链

Sentinel 内部使用 Slot 责任链处理请求。

常见 Slot：

| Slot | 作用 |
|---|---|
| NodeSelectorSlot | 构建调用链路 |
| ClusterBuilderSlot | 构建资源统计节点 |
| StatisticSlot | 统计 QPS、RT、异常等 |
| FlowSlot | 流量控制 |
| DegradeSlot | 熔断降级 |
| SystemSlot | 系统保护 |
| AuthoritySlot | 黑白名单控制 |

---

## 6. 限流原理

限流是指控制单位时间内通过的请求数量。

Sentinel 常见限流维度：

```text
QPS
并发线程数
热点参数
调用关系
系统负载
```

---

## 7. QPS 限流

例如配置：

```text
资源：/order/create
阈值类型：QPS
单机阈值：100
```

含义：

```text
每秒最多允许 100 个请求通过。
超过阈值的请求会被限流。
```

---

## 8. 线程数限流

线程数限流根据当前正在处理该资源的线程数判断是否放行。

适合保护慢接口。

例如：

```text
资源：createOrder
最大线程数：20
```

含义：

```text
最多允许 20 个线程同时处理 createOrder。
超过后直接拒绝。
```

---

## 9. 限流控制效果

Sentinel 常见流控效果：

```text
1. 快速失败
2. Warm Up
3. 排队等待
```

---

### 9.1 快速失败

超过阈值后立即拒绝。

适合大多数接口保护场景。

---

### 9.2 Warm Up

预热模式。

系统刚启动时，阈值从较低值逐渐升高到设置值。

适合防止冷启动时流量突然打满系统。

---

### 9.3 排队等待

请求匀速排队通过。

适合处理脉冲流量，把突发流量削平成稳定流量。

---

## 10. 熔断降级原理

熔断是指当下游服务不稳定时，暂时切断对该服务的调用。

目的：

```text
防止故障扩散，避免雪崩。
```

例如：

```text
订单服务调用库存服务，
库存服务响应很慢或大量报错，
订单服务可以对库存服务调用进行熔断。
```

---

## 11. Sentinel 熔断指标

Sentinel 常见熔断判断指标：

```text
1. 慢调用比例
2. 异常比例
3. 异常数
```

---

### 11.1 慢调用比例

如果请求响应时间超过设定 RT，就认为是慢调用。

当慢调用比例超过阈值时，触发熔断。

---

### 11.2 异常比例

如果一段时间内异常请求比例超过阈值，触发熔断。

---

### 11.3 异常数

如果一段时间内异常数量超过阈值，触发熔断。

---

## 12. 熔断状态机

Sentinel 熔断通常有三种状态：

```text
1. Closed：关闭
2. Open：打开
3. Half-Open：半开
```

---

### 12.1 Closed

正常状态，请求正常通过。

---

### 12.2 Open

熔断打开，请求直接拒绝或走降级逻辑。

---

### 12.3 Half-Open

熔断时间窗口结束后，允许少量请求探测。

如果探测成功，熔断关闭。

如果探测失败，继续熔断。

---

## 13. 降级处理

Sentinel 可以通过 `blockHandler` 处理限流异常。

示例：

```java
@SentinelResource(value = "createOrder", blockHandler = "blockHandler")
public String createOrder() {
    return "创建订单成功";
}

public String blockHandler(BlockException e) {
    return "当前请求过多，请稍后再试";
}
```

也可以通过 `fallback` 处理业务异常：

```java
@SentinelResource(value = "createOrder", fallback = "fallback")
public String createOrder() {
    throw new RuntimeException("业务异常");
}

public String fallback(Throwable e) {
    return "服务暂时不可用";
}
```

---

## 14. Sentinel 和 Gateway 集成

Gateway 可以集成 Sentinel 实现网关层限流。

常见限流维度：

```text
路由维度
API 分组维度
请求路径
请求参数
请求来源
```

---

## 15. 面试回答

Sentinel 是流量治理组件，主要提供限流、熔断降级、热点参数限流和系统保护等能力。

Sentinel 的核心是资源和规则。请求进入某个资源时，会经过 Sentinel 的 Slot 责任链，其中 StatisticSlot 负责统计 QPS、RT、异常数、线程数等指标，FlowSlot 根据限流规则判断是否放行，DegradeSlot 根据熔断规则判断是否熔断。

限流可以基于 QPS、并发线程数、热点参数等维度；熔断可以基于慢调用比例、异常比例和异常数。熔断状态通常包括关闭、打开和半开，打开时请求会快速失败或走降级逻辑，半开时会放少量请求探测服务是否恢复。

---

# 十一、分布式配置如何动态刷新

## 1. 什么是分布式配置动态刷新？

分布式配置动态刷新是指：

> 配置发生变化后，应用不需要重启，就可以感知配置变化并使用最新配置。

例如修改：

```yaml
order:
  timeout: 3000
```

改成：

```yaml
order:
  timeout: 5000
```

应用可以在运行中自动使用新值。

---

## 2. 为什么需要动态刷新？

动态刷新常见用途：

1. 修改业务开关；
2. 修改限流规则；
3. 修改线程池参数；
4. 修改日志级别；
5. 修改超时时间；
6. 修改灰度比例；
7. 修改降级策略；
8. 修改黑白名单。

---

## 3. 动态刷新核心流程

分布式配置动态刷新通常分为以下步骤：

```text
1. 配置中心保存配置
2. 应用启动时拉取配置
3. 客户端监听配置变化
4. 配置中心发生变更
5. 通知客户端配置已变化
6. 客户端拉取最新配置
7. 更新本地缓存
8. 更新 Spring Environment
9. 触发配置刷新事件
10. 重新绑定 Bean 属性
```

---

## 4. Nacos 动态刷新流程

```text
应用启动
    |
    v
从 Nacos 拉取配置
    |
    v
加载到 Spring Environment
    |
    v
注册配置监听器
    |
    v
Nacos 配置发生变化
    |
    v
客户端收到变更通知
    |
    v
重新拉取最新配置
    |
    v
更新 Environment
    |
    v
刷新 Bean
```

---

## 5. 使用 @RefreshScope

`@RefreshScope` 可以让 Bean 在配置刷新后重新创建。

示例：

```java
@RefreshScope
@RestController
public class ConfigController {

    @Value("${demo.name}")
    private String name;

    @GetMapping("/name")
    public String name() {
        return name;
    }
}
```

当 `demo.name` 变化后，Bean 会在下一次访问时使用新配置。

---

## 6. 使用 @ConfigurationProperties

推荐使用 `@ConfigurationProperties` 管理一组配置。

示例：

```java
@Component
@ConfigurationProperties(prefix = "demo")
public class DemoProperties {

    private String name;

    private Integer timeout;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
}
```

配置：

```yaml
demo:
  name: nacos
  timeout: 3000
```

优点：

```text
结构清晰；
适合复杂配置；
类型安全；
方便统一管理。
```

---

## 7. 使用 Nacos Listener

也可以直接监听配置变化。

示例：

```java
@Component
public class NacosConfigListener {

    @NacosConfigListener(dataId = "demo.yml", groupId = "DEFAULT_GROUP")
    public void onChange(String config) {
        System.out.println("配置发生变化：" + config);
    }
}
```

适合场景：

```text
刷新本地缓存
更新规则
重新加载策略
动态调整线程池参数
```

---

## 8. 动态刷新注意事项

### 8.1 不是所有配置都适合动态刷新

例如：

```text
数据库连接地址
核心 Bean 创建参数
部分中间件连接参数
服务端口
启动阶段配置
```

这些配置通常不建议运行时随意刷新。

---

### 8.2 @Value 不一定自动刷新

普通 `@Value` 字段可能不会自动刷新。

建议：

```text
使用 @RefreshScope
或者使用 @ConfigurationProperties
```

---

### 8.3 动态刷新要考虑线程安全

如果配置被多个线程读取，刷新时要考虑可见性和一致性。

可以使用：

```text
volatile
AtomicReference
不可变配置对象整体替换
读写锁
```

---

### 8.4 配置变更要有审计

生产环境配置变更需要：

1. 权限控制；
2. 审批流程；
3. 操作日志；
4. 灰度发布；
5. 回滚机制。

---

## 9. 动态刷新常见实现方案

| 方案 | 说明 |
|---|---|
| Nacos Config | 常见于 Spring Cloud Alibaba |
| Apollo | 携程开源配置中心 |
| Spring Cloud Config | Spring Cloud 官方配置中心 |
| Consul KV | 基于 Consul 的 KV 配置 |
| 自研配置中心 | 适合公司内部统一治理 |

---

## 10. 面试回答

分布式配置动态刷新通常依赖配置中心和客户端监听机制。

以 Nacos 为例，应用启动时会从 Nacos 拉取配置并加载到 Spring Environment 中，同时注册配置监听器。当 Nacos 中的配置发生变化后，客户端会收到变更通知，然后重新拉取最新配置，更新本地缓存和 Environment，并触发配置刷新事件。

对于 Spring 应用，可以通过 `@RefreshScope` 或 `@ConfigurationProperties` 实现 Bean 配置刷新。需要注意的是，并不是所有配置都适合动态刷新，例如数据库连接、服务端口等启动阶段配置通常不建议运行时修改。

---

# 十二、面试速记版

## 1. 注册中心的作用是什么？

注册中心用于服务注册、服务发现、健康检查、上下线感知和服务实例管理。

服务提供者注册实例，服务消费者根据服务名发现实例，并通过负载均衡调用。

---

## 2. Nacos 注册中心原理

Nacos 注册中心中，服务提供者启动后注册服务名、IP、端口、元数据等信息。

消费者根据服务名订阅或查询实例列表，并本地缓存。

服务实例通过心跳或长连接维持健康状态，异常实例会被标记不健康或剔除。

---

## 3. Nacos 配置中心原理

Nacos 配置中心通过：

```text
Namespace + Group + DataId
```

定位配置。

应用启动时拉取配置，运行时监听配置变化。配置变化后，客户端拉取最新配置并刷新本地缓存和 Spring Environment。

---

## 4. Nacos 和 Eureka 区别

Eureka 主要是注册中心，偏 AP。

Nacos 同时支持注册中心和配置中心，支持临时实例和持久实例，临时实例偏 AP，持久实例偏 CP，并支持服务变更推送。

---

## 5. 服务注册和服务发现流程

服务注册：

```text
服务启动 -> 注册 IP 端口 -> 发送心跳 -> 注册中心保存实例
```

服务发现：

```text
消费者查询服务名 -> 获取实例列表 -> 本地缓存 -> 负载均衡调用
```

---

## 6. 服务下线后消费者如何感知？

消费者通过注册中心推送、服务订阅、本地缓存刷新、定时拉取等方式感知服务下线。

主动下线时服务会注销实例；异常下线时注册中心通过心跳超时或连接断开判断实例不可用。

---

## 7. 负载均衡怎么做？

负载均衡分为服务端负载均衡和客户端负载均衡。

微服务内部调用常用客户端负载均衡，例如 Spring Cloud LoadBalancer。

常见算法有轮询、随机、加权轮询、加权随机、最少连接、一致性哈希等。

---

## 8. OpenFeign 原理

OpenFeign 是声明式 HTTP 客户端。

核心原理：

```text
扫描 @FeignClient
生成动态代理
解析接口注解
构造 HTTP 请求
负载均衡选择实例
发送请求
解码响应
```

---

## 9. Gateway 网关原理

Gateway 核心概念：

```text
Route
Predicate
Filter
```

请求进入 Gateway 后，先匹配路由，再执行过滤器链，然后通过负载均衡转发到后端服务，响应返回时再执行后置过滤器。

---

## 10. Sentinel 限流熔断原理

Sentinel 以资源为核心，通过 Slot 责任链统计 QPS、RT、异常数、线程数等指标。

FlowSlot 负责限流，DegradeSlot 负责熔断降级。

限流支持 QPS、线程数、热点参数等；熔断支持慢调用比例、异常比例、异常数。

---

## 11. 分布式配置如何动态刷新？

动态刷新流程：

```text
应用启动拉取配置
注册监听器
配置中心配置变更
客户端收到通知
拉取最新配置
更新 Environment
刷新 Bean
```

常用方式：

```text
@RefreshScope
@ConfigurationProperties
Nacos Listener
```

---

# 十三、总览表

| 问题 | 核心结论 |
|---|---|
| 注册中心的作用是什么 | 服务注册、服务发现、健康检查、上下线感知 |
| Nacos 注册中心原理 | 服务注册到 Nacos，消费者订阅服务列表，实例通过心跳或连接保活 |
| Nacos 配置中心原理 | 通过 Namespace、Group、DataId 管理配置，客户端监听变化并动态刷新 |
| Nacos 和 Eureka 区别 | Nacos 支持注册中心和配置中心，Eureka 主要是注册中心 |
| 服务注册和服务发现流程 | 提供者注册实例，消费者根据服务名发现实例并负载均衡调用 |
| 服务下线后消费者如何感知 | 推送、订阅、轮询、本地缓存刷新、调用失败重试 |
| 负载均衡怎么做 | 服务端负载均衡或客户端负载均衡，常用轮询、随机、权重等算法 |
| OpenFeign 原理 | 基于动态代理，解析接口注解，构造 HTTP 请求并解码响应 |
| Gateway 网关原理 | Route + Predicate + Filter，匹配路由后执行过滤器链并转发 |
| Sentinel 限流熔断原理 | 基于资源和 Slot 责任链，按 QPS、线程数、RT、异常等指标控制 |
| 分布式配置如何动态刷新 | 配置中心监听变更，客户端拉取最新配置并刷新 Environment 和 Bean |

---

# 十四、完整面试回答模板

注册中心是微服务架构中的服务治理组件，主要用于服务注册、服务发现、健康检查和服务上下线感知。服务提供者启动后会把服务名、IP、端口、元数据等信息注册到注册中心，服务消费者调用服务时根据服务名获取可用实例列表，并通过负载均衡选择一个实例进行调用。

Nacos 作为注册中心时，服务提供者会通过 Nacos Client 向 Nacos Server 注册实例信息。Nacos Server 保存服务实例、健康状态和元数据。服务实例通过心跳或长连接维持健康状态，如果实例异常，Nacos 会将其标记为不健康或剔除。服务消费者会根据服务名查询或订阅实例列表，并在本地缓存，实例变化时 Nacos 会通知客户端更新缓存。

Nacos 作为配置中心时，通过 `Namespace + Group + DataId` 唯一定位一份配置。应用启动时从 Nacos 拉取远程配置，并加载到 Spring Environment 中。运行过程中客户端会监听配置变化，配置变更后重新拉取最新配置，更新本地缓存和 Environment，并通过 `@RefreshScope`、`@ConfigurationProperties` 或监听器实现动态刷新。

Nacos 和 Eureka 都可以作为注册中心，但 Nacos 功能更丰富。Eureka 主要提供服务注册与发现，整体偏 AP；Nacos 不仅支持注册中心，还支持配置中心和服务治理，并支持临时实例和持久实例，临时实例偏 AP，持久实例偏 CP。Nacos 还支持服务订阅和变更推送，配置动态刷新能力也比 Eureka 更完整。

服务注册流程是：服务提供者启动后，读取服务名、IP、端口和元数据，通过注册中心客户端注册到注册中心，并通过心跳或长连接维持健康状态。服务发现流程是：服务消费者根据服务名从注册中心获取实例列表，本地缓存后通过负载均衡选择实例发起调用。

服务下线后，消费者通常通过注册中心推送、订阅变更、本地缓存刷新或定时拉取来感知。如果是主动下线，服务会在关闭前主动注销实例；如果是异常下线，注册中心会根据心跳超时或连接断开判断实例不可用，然后标记不健康或剔除。由于检测和缓存刷新存在延迟，所以还需要配合优雅停机、失败重试、熔断降级和负载均衡过滤来降低影响。

负载均衡是指当一个服务有多个实例时，按照一定算法选择一个实例处理请求。它分为服务端负载均衡和客户端负载均衡。服务端负载均衡常见组件有 Nginx、LVS、SLB；客户端负载均衡由调用方从注册中心获取实例列表后本地选择实例，例如 Spring Cloud LoadBalancer。常见算法有轮询、随机、加权轮询、加权随机、最少连接和一致性哈希。

OpenFeign 是声明式 HTTP 客户端。Spring 启动时会扫描 `@FeignClient` 接口，并为接口创建动态代理对象。调用接口方法时，代理对象会解析方法上的 Spring MVC 注解，构造 HTTP 请求，结合注册中心和负载均衡选择具体服务实例，然后通过 HTTP Client 发起请求，最后使用 Decoder 将响应转换成 Java 对象。

Spring Cloud Gateway 是微服务网关组件，主要作为系统统一入口，负责路由转发、认证鉴权、限流熔断、日志审计和跨域处理等能力。Gateway 的核心是 Route、Predicate 和 Filter。请求进入 Gateway 后，先通过 Predicate 匹配路由，再执行过滤器链，然后根据路由目标转发到后端服务，响应返回时再执行后置过滤器逻辑。

Sentinel 是流量治理组件，主要提供限流、熔断降级、热点参数限流和系统保护等能力。Sentinel 以资源为核心，请求进入资源时会经过 Slot 责任链。StatisticSlot 负责统计 QPS、RT、异常数、线程数等指标，FlowSlot 根据限流规则判断是否放行，DegradeSlot 根据熔断规则判断是否熔断。限流可以基于 QPS、线程数和热点参数，熔断可以基于慢调用比例、异常比例和异常数。

分布式配置动态刷新通常依赖配置中心和客户端监听机制。以 Nacos 为例，应用启动时会拉取配置并加载到 Spring Environment，同时注册配置监听器。配置发生变化后，客户端收到通知，重新拉取最新配置，更新 Environment，并触发 Bean 属性重新绑定。常见实现方式包括 `@RefreshScope`、`@ConfigurationProperties` 和 Nacos Config Listener。
