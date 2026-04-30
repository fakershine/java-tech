# ZooKeeper 面试题完整总结

## 目录

- [一、ZooKeeper 是什么](#一zookeeper-是什么)
- [二、ZooKeeper 核心作用](#二zookeeper-核心作用)
- [三、ZooKeeper 数据模型](#三zookeeper-数据模型)
- [四、ZooKeeper 节点类型](#四zookeeper-节点类型)
- [五、ZooKeeper Watcher 机制](#五zookeeper-watcher-机制)
- [六、ZooKeeper 集群角色](#六zookeeper-集群角色)
- [七、ZooKeeper 选举机制](#七zookeeper-选举机制)
- [八、ZooKeeper 的 ZAB 协议](#八zookeeper-的-zab-协议)
- [九、ZooKeeper 如何保证一致性](#九zookeeper-如何保证一致性)
- [十、ZooKeeper 分布式锁实现](#十zookeeper-分布式锁实现)
- [十一、ZooKeeper 注册中心原理](#十一zookeeper-注册中心原理)
- [十二、ZooKeeper 配置中心原理](#十二zookeeper-配置中心原理)
- [十三、ZooKeeper 和 Redis 分布式锁区别](#十三zookeeper-和-redis-分布式锁区别)
- [十四、ZooKeeper 和 Nacos 区别](#十四zookeeper-和-nacos-区别)
- [十五、ZooKeeper 常见面试题](#十五zookeeper-常见面试题)
- [十六、面试速记版](#十六面试速记版)
- [十七、完整面试回答模板](#十七完整面试回答模板)

---

# 一、ZooKeeper 是什么

ZooKeeper 是一个分布式协调服务。

它主要用于解决分布式系统中的一致性协调问题，例如：

```text
服务注册与发现
配置管理
分布式锁
Leader 选举
集群管理
分布式队列
命名服务
```

简单理解：

> ZooKeeper 本身不是业务系统，而是给分布式系统提供协调能力的基础组件。

---

## 1. ZooKeeper 的核心特点

```text
1. 高可用
2. 强一致性
3. 顺序一致性
4. 支持 Watcher 监听机制
5. 支持临时节点
6. 支持顺序节点
7. 适合做分布式协调
```

---

## 2. ZooKeeper 常见使用场景

```text
1. Dubbo 注册中心
2. Kafka 早期版本元数据管理
3. HBase 集群协调
4. Hadoop 生态组件协调
5. 分布式锁
6. 分布式 Leader 选举
7. 配置变更监听
```

---

# 二、ZooKeeper 核心作用

ZooKeeper 主要解决分布式系统中的协调问题。

---

## 1. 服务注册与发现

服务提供者启动后，在 ZooKeeper 上注册临时节点。

服务消费者监听节点变化，获取服务地址列表。

```text
服务提供者 -> ZooKeeper 注册服务
服务消费者 -> ZooKeeper 订阅服务
ZooKeeper -> 通知消费者服务变更
```

---

## 2. 配置管理

配置保存在 ZooKeeper 节点中。

客户端监听配置节点变化。

配置发生变化后，ZooKeeper 通知客户端。

```text
配置节点变化
     |
     v
Watcher 触发
     |
     v
客户端重新拉取配置
```

---

## 3. 分布式锁

利用 ZooKeeper 的：

```text
临时顺序节点
```

实现分布式锁。

多个客户端竞争锁时，谁创建的顺序节点最小，谁获得锁。

---

## 4. Leader 选举

多个节点在同一个路径下创建临时顺序节点。

编号最小的节点成为 Leader。

Leader 宕机后，其临时节点自动删除，其他节点重新选举。

---

# 三、ZooKeeper 数据模型

ZooKeeper 的数据模型类似 Linux 文件系统，是一棵树。

每个节点称为：

```text
ZNode
```

结构类似：

```text
/
|-- app
|   |-- config
|   |-- service
|       |-- provider-00000001
|       |-- provider-00000002
|
|-- lock
    |-- order-lock-00000001
    |-- order-lock-00000002
```

---

## 1. ZNode 是什么？

ZNode 是 ZooKeeper 中的数据节点。

每个 ZNode 可以：

```text
1. 存储少量数据
2. 拥有子节点
3. 被客户端监听
4. 设置为临时节点或持久节点
```

---

## 2. ZNode 适合存什么？

ZooKeeper 适合存储：

```text
少量元数据
服务地址
配置信息
锁节点
状态信息
```

不适合存储：

```text
大文件
大量业务数据
日志数据
图片视频
复杂业务表数据
```

---

## 3. 为什么 ZooKeeper 不适合存大数据？

因为 ZooKeeper 的设计目标是分布式协调，不是数据库。

如果存大量数据，会导致：

```text
内存压力大
同步成本高
集群性能下降
Watcher 通知压力大
```

---

# 四、ZooKeeper 节点类型

ZooKeeper 中常见节点类型有四种：

```text
1. 持久节点
2. 持久顺序节点
3. 临时节点
4. 临时顺序节点
```

---

## 1. 持久节点

创建后一直存在，除非手动删除。

```text
PERSISTENT
```

适合：

```text
配置节点
服务目录
系统元数据
```

示例：

```text
/app/config
/service/order
```

---

## 2. 持久顺序节点

持久存在，并且 ZooKeeper 会在节点名后追加递增序号。

```text
PERSISTENT_SEQUENTIAL
```

示例：

```text
/task/task-000000001
/task/task-000000002
```

适合：

```text
任务队列
有序编号
```

---

## 3. 临时节点

临时节点和客户端 Session 绑定。

客户端断开连接或 Session 过期后，临时节点会自动删除。

```text
EPHEMERAL
```

适合：

```text
服务注册
在线状态
临时标识
```

例如服务实例注册：

```text
/service/order/192.168.1.10:8080
```

服务宕机后，该临时节点自动消失。

---

## 4. 临时顺序节点

临时节点 + 顺序编号。

```text
EPHEMERAL_SEQUENTIAL
```

适合：

```text
分布式锁
Leader 选举
公平队列
```

示例：

```text
/lock/order-lock-000000001
/lock/order-lock-000000002
```

---

# 五、ZooKeeper Watcher 机制

## 1. Watcher 是什么？

Watcher 是 ZooKeeper 的事件监听机制。

客户端可以监听某个节点，当节点发生变化时，ZooKeeper 会通知客户端。

---

## 2. Watcher 可以监听哪些事件？

常见事件包括：

```text
节点创建
节点删除
节点数据变化
子节点变化
Session 状态变化
```

---

## 3. Watcher 机制流程

```text
1. 客户端读取节点并注册 Watcher
2. ZooKeeper 保存监听关系
3. 节点发生变化
4. ZooKeeper 通知客户端
5. 客户端收到通知后重新拉取数据
6. 如需继续监听，需要重新注册 Watcher
```

---

## 4. Watcher 特点

```text
1. Watcher 是一次性的
2. Watcher 通知是异步的
3. Watcher 只通知发生了变化，不直接返回最新数据
4. 客户端收到通知后需要重新读取数据
5. 如果还要继续监听，需要重新注册
```

---

## 5. 为什么 Watcher 是一次性的？

因为如果 Watcher 永久存在，服务端维护成本会很高。

一次性 Watcher 可以降低 ZooKeeper 服务端压力。

客户端需要在收到通知后重新注册监听。

---

## 6. Watcher 使用场景

```text
1. 配置变更通知
2. 服务上下线通知
3. 分布式锁等待通知
4. Leader 节点变化通知
```

---

# 六、ZooKeeper 集群角色

ZooKeeper 集群中主要有三种角色：

```text
Leader
Follower
Observer
```

---

## 1. Leader

Leader 是集群中的主节点。

主要职责：

```text
1. 处理写请求
2. 发起事务提案
3. 协调数据同步
4. 维护集群一致性
```

---

## 2. Follower

Follower 是参与投票的从节点。

主要职责：

```text
1. 处理读请求
2. 转发写请求给 Leader
3. 参与 Leader 选举
4. 参与事务投票
```

---

## 3. Observer

Observer 是观察者节点。

主要职责：

```text
1. 处理读请求
2. 接收 Leader 同步数据
3. 不参与投票
4. 不参与 Leader 选举
```

Observer 适合：

```text
读多写少场景下扩展读能力。
```

---

## 4. 为什么 ZooKeeper 集群一般是奇数台？

ZooKeeper 依赖过半机制。

例如：

```text
3 台机器，需要 2 台可用
5 台机器，需要 3 台可用
```

如果是 4 台机器，也需要 3 台可用。

对比：

```text
3 台最多容忍 1 台宕机
4 台也最多容忍 1 台宕机
```

所以 4 台相比 3 台没有提升容错能力，反而增加成本。

因此 ZooKeeper 集群通常部署奇数台：

```text
3 台
5 台
7 台
```

---

# 七、ZooKeeper 选举机制

## 1. 什么时候会发生 Leader 选举？

常见场景：

```text
1. 集群启动时
2. Leader 宕机时
3. Leader 与多数节点失联时
```

---

## 2. 选举核心依据

ZooKeeper 选举 Leader 时，主要比较：

```text
1. zxid
2. myid
```

优先级：

```text
zxid 越大，数据越新，优先成为 Leader
zxid 相同，myid 越大，优先成为 Leader
```

---

## 3. zxid 是什么？

zxid 是 ZooKeeper 的事务 ID。

每次写操作都会产生一个新的 zxid。

zxid 越大，说明该节点数据越新。

---

## 4. myid 是什么？

myid 是 ZooKeeper 节点的服务器编号。

每个节点都有唯一 myid。

例如：

```text
server.1
server.2
server.3
```

---

## 5. 选举流程简化版

```text
1. 每个节点先投自己
2. 节点之间交换投票
3. 比较 zxid，zxid 大者胜
4. zxid 相同，比较 myid，myid 大者胜
5. 某个节点获得超过半数投票
6. 该节点成为 Leader
7. 其他节点成为 Follower
```

---

# 八、ZooKeeper 的 ZAB 协议

## 1. ZAB 是什么？

ZAB 全称：

```text
ZooKeeper Atomic Broadcast
```

中文可以理解为：

```text
ZooKeeper 原子广播协议
```

ZAB 是 ZooKeeper 保证分布式数据一致性的核心协议。

---

## 2. ZAB 主要解决什么问题？

ZAB 主要解决：

```text
1. 写请求如何在集群中保持顺序一致
2. Leader 崩溃后如何恢复
3. Follower 如何和 Leader 保持数据一致
```

---

## 3. ZAB 有两种模式

```text
1. 崩溃恢复模式
2. 消息广播模式
```

---

## 4. 崩溃恢复模式

当集群启动或 Leader 宕机时，进入恢复模式。

流程：

```text
1. 选举新的 Leader
2. 新 Leader 确认最新事务
3. Follower 和 Leader 同步数据
4. 集群恢复可用
```

---

## 5. 消息广播模式

当 Leader 正常工作时，进入消息广播模式。

写请求流程：

```text
1. 客户端发送写请求
2. 如果请求到 Follower，Follower 转发给 Leader
3. Leader 生成事务 Proposal
4. Leader 将 Proposal 广播给 Follower
5. Follower 写入本地事务日志并返回 ACK
6. Leader 收到超过半数 ACK 后提交事务
7. Leader 通知 Follower 提交
8. 客户端收到写成功响应
```

---

## 6. ZAB 保证了什么？

ZAB 保证：

```text
1. 所有写请求按顺序执行
2. 已提交事务不会丢失
3. 未提交事务不会被错误提交
4. Leader 切换后数据仍然一致
```

---

# 九、ZooKeeper 如何保证一致性

ZooKeeper 保证一致性的核心依赖：

```text
1. 单 Leader 写入
2. ZAB 协议
3. 过半写入机制
4. zxid 事务编号
5. 顺序执行写请求
6. Leader 选举时选择数据最新节点
```

---

## 1. 单 Leader 写入

所有写请求最终都由 Leader 处理。

这样可以避免多个节点同时写导致冲突。

---

## 2. 过半机制

事务只要被超过半数节点确认，就可以提交。

例如 5 台机器：

```text
至少 3 台确认，事务才算成功。
```

这样可以保证即使少数节点宕机，数据仍然不会丢失。

---

## 3. 顺序一致性

ZooKeeper 会给每个事务分配递增 zxid。

所有节点按照 zxid 顺序应用事务。

---

## 4. 最终所有节点一致

Follower 会从 Leader 同步数据，保证最终和 Leader 一致。

---

# 十、ZooKeeper 分布式锁实现

## 1. 为什么 ZooKeeper 适合做分布式锁？

ZooKeeper 有几个特性非常适合分布式锁：

```text
1. 临时节点：客户端宕机后节点自动删除
2. 顺序节点：可以实现公平锁
3. Watcher：可以监听前一个节点释放锁
4. 强一致性：避免多个客户端同时获得锁
```

---

## 2. 分布式锁实现流程

假设锁路径：

```text
/locks/order
```

加锁流程：

```text
1. 客户端在 /locks/order 下创建临时顺序节点
2. 获取 /locks/order 下所有子节点并排序
3. 判断自己创建的节点是否编号最小
4. 如果是最小节点，获得锁
5. 如果不是最小节点，监听自己前一个节点
6. 前一个节点删除后，再次判断自己是否最小
7. 获得锁后执行业务
8. 业务完成后删除自己的节点释放锁
```

---

## 3. 为什么监听前一个节点？

如果所有客户端都监听最小节点，会导致锁释放时大量客户端同时被唤醒。

这叫：

```text
羊群效应
```

正确做法：

```text
每个客户端只监听自己前一个节点。
```

这样锁释放时，只唤醒下一个等待者。

---

## 4. ZooKeeper 锁的优点

```text
1. 天然支持锁自动释放
2. 支持公平锁
3. 一致性强
4. 不容易死锁
```

---

## 5. ZooKeeper 锁的缺点

```text
1. 性能不如 Redis
2. 实现和运维复杂度较高
3. 依赖 ZooKeeper 集群稳定性
4. 不适合极高频加锁场景
```

---

# 十一、ZooKeeper 注册中心原理

## 1. 注册中心核心功能

注册中心主要负责：

```text
1. 服务注册
2. 服务发现
3. 服务上下线通知
4. 服务健康感知
```

---

## 2. 服务注册流程

服务提供者启动后，在 ZooKeeper 上创建临时节点。

例如：

```text
/dubbo/com.demo.UserService/providers/192.168.1.10:20880
```

这个节点是临时节点。

如果服务宕机或连接断开，节点会自动删除。

---

## 3. 服务发现流程

服务消费者启动后，订阅服务路径。

例如：

```text
/dubbo/com.demo.UserService/providers
```

消费者获取所有 provider 节点，得到服务地址列表。

---

## 4. 服务变更通知

当服务提供者上下线时，子节点发生变化。

ZooKeeper 触发 Watcher 通知消费者。

消费者重新拉取最新服务列表。

---

## 5. 注册中心流程图

```text
Provider 启动
    |
    v
创建临时节点
    |
    v
Consumer 监听服务目录
    |
    v
Provider 宕机
    |
    v
临时节点删除
    |
    v
Watcher 通知 Consumer
    |
    v
Consumer 更新本地服务列表
```

---

# 十二、ZooKeeper 配置中心原理

## 1. 配置中心实现思路

配置数据存储在 ZooKeeper 节点上。

客户端启动时读取配置，并注册 Watcher。

当配置节点变化时，客户端收到通知，重新拉取配置。

---

## 2. 配置中心流程

```text
1. 配置保存到 /config/app
2. 应用启动读取 /config/app
3. 应用注册 Watcher
4. 配置被修改
5. ZooKeeper 通知应用
6. 应用重新读取配置
7. 应用刷新本地配置
```

---

## 3. 示例路径

```text
/config/order-service/database
/config/order-service/redis
/config/order-service/limit
/config/order-service/desensitize
```

---

## 4. 配置中心注意事项

ZooKeeper 可以做配置中心，但不适合存放大量配置或频繁变更配置。

原因：

```text
1. ZooKeeper 更适合协调数据
2. 配置数据不宜太大
3. 高频配置变更会造成 Watcher 压力
```

如果是现代微服务配置中心，通常会选择：

```text
Nacos
Apollo
Spring Cloud Config
```

---

# 十三、ZooKeeper 和 Redis 分布式锁区别

| 对比项 | ZooKeeper 分布式锁 | Redis 分布式锁 |
|---|---|---|
| 实现方式 | 临时顺序节点 + Watcher | SET NX PX + Lua |
| 一致性 | 强一致性更好 | 性能高，一致性依赖实现 |
| 性能 | 相对较低 | 较高 |
| 锁释放 | Session 过期自动删除临时节点 | 依赖过期时间和主动删除 |
| 公平性 | 容易实现公平锁 | 默认非公平 |
| 实现复杂度 | 较高 | 较低 |
| 适用场景 | 对一致性要求高 | 高性能、高并发加锁 |
| 运维成本 | 较高 | 较低 |

---

## 面试回答

如果对锁一致性、公平性要求比较高，可以选择 ZooKeeper 分布式锁。

如果是高并发、短时间锁，并且业务能接受一定复杂度和风险，可以使用 Redis 分布式锁。

实际项目中 Redis 分布式锁使用更多，因为性能好、接入简单；ZooKeeper 更适合强协调场景。

---

# 十四、ZooKeeper 和 Nacos 区别

| 对比项 | ZooKeeper | Nacos |
|---|---|---|
| 定位 | 分布式协调服务 | 服务注册发现 + 配置中心 |
| 数据模型 | 树形 ZNode | 服务模型 + 配置模型 |
| 一致性 | 偏 CP | 支持 AP 和 CP 场景 |
| 注册中心 | 支持，但不是专门为微服务设计 | 专门面向微服务注册发现 |
| 配置中心 | 可以实现，但能力较基础 | 原生配置中心能力更完整 |
| 动态刷新 | 依赖 Watcher | 原生支持配置监听 |
| 健康检查 | 依赖临时节点和 Session | 支持临时实例、永久实例、健康检查 |
| 运维体验 | 偏底层 | 更适合微服务体系 |
| 常见场景 | 分布式锁、协调、Dubbo 注册 | Spring Cloud Alibaba 微服务 |

---

## 面试回答

ZooKeeper 是一个通用的分布式协调组件，核心能力是强一致的节点存储、临时节点、顺序节点和 Watcher 机制，适合做分布式锁、Leader 选举和集群协调。

Nacos 更偏微服务场景，原生支持服务注册发现和配置中心，控制台、动态配置、服务健康检查等能力更完整。

如果是 Dubbo 老项目或需要强协调能力，可以使用 ZooKeeper；如果是 Spring Cloud Alibaba 微服务体系，通常更推荐 Nacos。

---

# 十五、ZooKeeper 常见面试题

## 1. ZooKeeper 是 CP 还是 AP？

ZooKeeper 更偏 CP。

也就是：

```text
一致性 Consistency
分区容错性 Partition tolerance
```

当发生网络分区时，ZooKeeper 会优先保证数据一致性。

如果集群无法形成多数派，就不能继续提供写服务。

---

## 2. ZooKeeper 为什么适合做注册中心？

因为它支持：

```text
1. 临时节点
2. Watcher 机制
3. 强一致性
```

服务提供者注册临时节点。

服务宕机后临时节点自动删除。

消费者监听服务目录变化，从而感知服务上下线。

---

## 3. ZooKeeper 为什么适合做分布式锁？

因为它支持：

```text
1. 临时节点自动释放锁
2. 顺序节点实现公平锁
3. Watcher 监听锁释放
4. 强一致性避免多个客户端同时获得锁
```

---

## 4. ZooKeeper 的 Watcher 是永久的吗？

不是。

Watcher 是一次性的。

触发一次后就失效，如果还需要继续监听，需要重新注册。

---

## 5. ZooKeeper 如何避免分布式锁羊群效应？

不要让所有客户端监听同一个锁节点。

每个客户端只监听自己前一个顺序节点。

当前一个节点释放时，只唤醒下一个客户端。

---

## 6. ZooKeeper 集群为什么一般部署奇数台？

因为 ZooKeeper 使用过半机制。

3 台和 4 台都最多只能容忍 1 台故障，但 4 台成本更高。

所以一般部署奇数台，例如 3、5、7 台。

---

## 7. ZooKeeper 写请求怎么处理？

写请求最终由 Leader 处理。

如果客户端请求到 Follower，Follower 会转发给 Leader。

Leader 生成事务 Proposal，广播给 Follower，超过半数节点 ACK 后提交。

---

## 8. ZooKeeper 读请求怎么处理？

读请求可以由 Follower 直接处理。

但如果客户端需要读到最新数据，可以调用 sync 操作或读 Leader。

---

# 十六、面试速记版

## 1. ZooKeeper 是什么？

```text
ZooKeeper 是分布式协调服务，主要用于服务注册发现、配置管理、分布式锁、Leader 选举和集群管理。
```

---

## 2. ZooKeeper 数据模型

```text
树形结构，每个节点叫 ZNode。
ZNode 可以存少量数据，也可以有子节点。
```

---

## 3. ZNode 类型

```text
持久节点
持久顺序节点
临时节点
临时顺序节点
```

---

## 4. Watcher 机制

```text
客户端监听节点变化。
节点变化后 ZooKeeper 通知客户端。
Watcher 是一次性的，触发后需要重新注册。
```

---

## 5. 集群角色

```text
Leader：处理写请求，发起事务
Follower：处理读请求，参与投票
Observer：处理读请求，不参与投票
```

---

## 6. 选举机制

```text
优先比较 zxid，zxid 越大数据越新。
zxid 相同，比较 myid，myid 越大优先级越高。
获得超过半数投票的节点成为 Leader。
```

---

## 7. ZAB 协议

```text
ZooKeeper Atomic Broadcast。
包含崩溃恢复和消息广播。
用于保证写请求顺序一致和集群数据一致。
```

---

## 8. 分布式锁

```text
使用临时顺序节点。
编号最小的节点获得锁。
未获得锁的节点监听自己前一个节点。
前一个节点删除后重新竞争锁。
```

---

## 9. 注册中心

```text
服务提供者创建临时节点。
消费者监听服务目录。
服务宕机后临时节点删除，消费者收到通知并更新服务列表。
```

---

## 10. ZooKeeper 和 Redis 锁区别

```text
ZooKeeper 锁一致性强，支持公平锁，性能相对低。
Redis 锁性能高，实现简单，但需要注意锁超时、误删锁和可靠性问题。
```

---

## 11. ZooKeeper 和 Nacos 区别

```text
ZooKeeper 是分布式协调服务。
Nacos 是服务注册发现和配置中心。
Spring Cloud Alibaba 体系下通常更推荐 Nacos。
```

---

# 十七、完整面试回答模板

ZooKeeper 是一个分布式协调服务，主要用于解决分布式系统中的协调一致性问题。它常见的使用场景包括服务注册与发现、配置管理、分布式锁、Leader 选举和集群管理。ZooKeeper 的数据模型是一棵类似文件系统的树，每个节点叫 ZNode，ZNode 可以存储少量数据，也可以有子节点。

ZooKeeper 的节点类型主要有四种：持久节点、持久顺序节点、临时节点和临时顺序节点。持久节点创建后会一直存在，适合存储配置和服务目录；临时节点和客户端 Session 绑定，客户端断开或 Session 过期后会自动删除，适合服务注册；顺序节点会在节点名后追加递增编号，适合实现分布式锁和 Leader 选举。

ZooKeeper 有 Watcher 监听机制。客户端可以监听某个节点的数据变化、节点删除、节点创建或子节点变化。当节点发生变化时，ZooKeeper 会通知客户端，客户端收到通知后再重新拉取最新数据。需要注意的是，Watcher 是一次性的，触发一次后就失效，如果还需要继续监听，需要重新注册。

ZooKeeper 集群中主要有 Leader、Follower 和 Observer 三种角色。Leader 负责处理写请求和事务提案，Follower 可以处理读请求，也会参与 Leader 选举和事务投票，Observer 只处理读请求，不参与投票，主要用于扩展读能力。ZooKeeper 集群一般部署奇数台，比如 3 台、5 台，因为它依赖过半机制，3 台和 4 台都只能容忍 1 台宕机，所以部署 4 台没有明显收益。

ZooKeeper 的一致性主要依赖 ZAB 协议。ZAB 全称是 ZooKeeper Atomic Broadcast，主要包括崩溃恢复和消息广播两个阶段。正常情况下，所有写请求最终都由 Leader 处理，Leader 生成事务 Proposal 并广播给 Follower，超过半数节点 ACK 后提交事务。Leader 宕机时，集群会重新选举新的 Leader，并保证已提交事务不丢失，未提交事务不会被错误提交。

ZooKeeper 做注册中心时，服务提供者启动后会在服务目录下创建临时节点，节点内容一般是服务地址。消费者监听服务目录，获取服务提供者列表。当服务提供者宕机或网络断开导致 Session 过期时，临时节点会自动删除，ZooKeeper 通过 Watcher 通知消费者，消费者重新拉取服务列表，从而感知服务上下线。

ZooKeeper 做分布式锁时，通常使用临时顺序节点。多个客户端在同一个锁目录下创建临时顺序节点，编号最小的节点获得锁。没有获得锁的客户端监听自己前一个节点，当前一个节点删除后，再判断自己是否是最小节点。如果是，则获得锁。使用临时节点的好处是客户端宕机后锁会自动释放；使用顺序节点可以实现公平锁；监听前一个节点可以避免羊群效应。

ZooKeeper 更偏 CP，也就是优先保证一致性和分区容错性。当集群无法形成多数派时，ZooKeeper 不会继续提供写服务，因为它要保证数据一致性。

如果拿 ZooKeeper 和 Redis 分布式锁对比，ZooKeeper 锁一致性更强，天然支持公平锁和锁自动释放，但性能相对 Redis 较低，运维和实现复杂度也更高。Redis 分布式锁性能好、实现简单，适合高并发短时间加锁场景，但要注意锁超时、误删锁和主从切换带来的问题。

如果拿 ZooKeeper 和 Nacos 对比，ZooKeeper 是通用分布式协调服务，适合分布式锁、Leader 选举、集群协调和 Dubbo 注册中心。Nacos 更偏现代微服务场景，原生支持服务注册发现、配置中心、健康检查和动态配置管理。如果是 Spring Cloud Alibaba 体系，通常会优先选择 Nacos；如果是老 Dubbo 体系或强协调场景，可以选择 ZooKeeper。
