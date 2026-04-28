# Kafka

## 1. Kafka 是什么

Kafka 是一个**分布式事件流平台**，也可以作为高吞吐消息队列使用，常用于：

- 日志采集
- 异步解耦
- 流量削峰
- 实时数据处理
- 事件驱动架构
- 数据同步
- 大数据管道

一句话理解：

> Kafka 是一个高吞吐、可持久化、可扩展的分布式消息队列 / 事件流平台。

---

## 2. Kafka 适合解决什么问题

### 2.1 异步解耦

没有 Kafka：

```text
订单服务 -> 库存服务
订单服务 -> 积分服务
订单服务 -> 短信服务
订单服务 -> 推荐服务
```

使用 Kafka 后：

```text
订单服务 -> Kafka -> 库存服务
                -> 积分服务
                -> 短信服务
                -> 推荐服务
```

订单服务只发送订单事件，下游系统自己消费。

---

### 2.2 流量削峰

高并发请求可以先写入 Kafka，再由消费者按自身能力消费：

```text
请求流量 -> Kafka -> Consumer 慢慢消费 -> 数据库
```

适合：

- 秒杀
- 日志采集
- 用户行为埋点
- 大批量数据同步

---

### 2.3 日志采集

Kafka 非常适合做日志管道：

```text
应用日志 -> Kafka -> Flink / Spark / Elasticsearch / ClickHouse
```

---

### 2.4 实时数据处理

Kafka 常用于实时计算链路：

```text
业务系统 -> Kafka -> Flink / Kafka Streams -> 实时指标 / 风控 / 推荐
```

---

## 3. Kafka 核心角色

| 角色 | 说明 |
|---|---|
| Producer | 生产者，负责发送消息 |
| Consumer | 消费者，负责消费消息 |
| Broker | Kafka 服务节点，负责存储和转发消息 |
| Topic | 消息主题，表示一类消息 |
| Partition | Topic 的分区，是 Kafka 并发和顺序性的核心 |
| Consumer Group | 消费者组，用于负载均衡消费 |
| Offset | 消费位移，表示消费到哪个位置 |
| Controller | 集群控制器，负责分区 Leader 选举等管理工作 |

---

## 4. Kafka 架构

```text
Producer
   ↓
Kafka Cluster
   ↓
Consumer
```

更完整：

```text
+----------+        +-------------------------+        +----------+
| Producer | -----> | Broker 1 / 2 / 3 ...    | -----> | Consumer |
+----------+        +-------------------------+        +----------+
                            |
                         Topic
                            |
                       Partition
                            |
                         Offset
```

---

## 5. Topic

Topic 是消息的逻辑分类。

例如：

```text
order-topic
pay-topic
user-topic
log-topic
```

生产者发送消息时指定 Topic：

```text
订单创建消息 -> order-topic
支付成功消息 -> pay-topic
用户行为日志 -> log-topic
```

消费者订阅 Topic：

```text
库存服务订阅 order-topic
积分服务订阅 order-topic
日志系统订阅 log-topic
```

---

## 6. Partition 分区

一个 Topic 可以拆成多个 Partition。

```text
order-topic
    partition-0
    partition-1
    partition-2
```

Partition 的作用：

- 提高并发写入能力
- 提高并发消费能力
- 支持数据分布式存储
- 支持分区内有序

Kafka 只能保证**单个 Partition 内消息有序**，不保证整个 Topic 全局有序。

---

## 7. Offset

Offset 是消息在 Partition 中的位置。

示例：

```text
partition-0:
offset 0 -> message A
offset 1 -> message B
offset 2 -> message C
```

Consumer 通过提交 Offset 记录消费进度。

```text
consumer-group: order-group
topic: order-topic
partition: 0
offset: 100
```

表示 `order-group` 已经消费到 `order-topic` 的 `partition-0` 的 offset 100 附近。

---

## 8. Broker

Broker 是 Kafka 服务节点。

主要职责：

- 接收 Producer 消息
- 存储消息
- 提供 Consumer 拉取消息
- 管理 Topic 和 Partition
- 维护副本
- 参与 Leader 选举

一个 Kafka 集群通常有多个 Broker：

```text
broker-1
broker-2
broker-3
```

---

## 9. 副本机制 Replica

每个 Partition 可以有多个副本。

```text
partition-0:
    leader replica
    follower replica
    follower replica
```

| 副本类型 | 说明 |
|---|---|
| Leader | 对外提供读写 |
| Follower | 从 Leader 同步数据 |
| ISR | 与 Leader 保持同步的副本集合 |

Producer 和 Consumer 通常读写 Leader Partition。

Follower 主要负责容灾。

---

## 10. Producer 发送消息流程

```text
1. Producer 获取 Topic 元数据
2. 根据分区策略选择 Partition
3. 将消息发送给 Partition Leader 所在 Broker
4. Broker 写入日志文件
5. 根据 acks 配置返回发送结果
```

流程图：

```text
Producer
   ↓ 选择 Topic / Partition
Broker Leader
   ↓ 写入本地日志
Follower 同步
   ↓
返回 ack
```

---

## 11. Producer 分区策略

Producer 发送消息时，需要决定写入哪个 Partition。

常见策略：

| 策略 | 说明 |
|---|---|
| 指定 Partition | 直接写入指定分区 |
| Key Hash | 根据 key hash 后选择分区 |
| 轮询 | 没有 key 时轮询分区 |
| 自定义分区器 | 根据业务规则选择分区 |

示例：

```text
orderId 相同的消息 -> 同一个 Partition
```

这样可以保证同一个订单的消息在分区内有序。

---

## 12. Consumer 消费流程

```text
1. Consumer 加入 Consumer Group
2. Kafka 为 Consumer 分配 Partition
3. Consumer 从指定 Partition 拉取消息
4. 执行业务逻辑
5. 提交 Offset
```

流程图：

```text
Consumer Group
   ↓
分配 Partition
   ↓
Consumer 拉取消息
   ↓
处理业务
   ↓
提交 Offset
```

---

## 13. Consumer Group

Consumer Group 是消费者组。

### 13.1 同组消费

同一个 Consumer Group 内，一条消息只会被一个 Consumer 消费。

```text
topic: order-topic

consumer-group: stock-group
    consumer-1 消费 partition-0
    consumer-2 消费 partition-1
    consumer-3 消费 partition-2
```

适合：

- 订单处理
- 库存扣减
- 数据同步

---

### 13.2 不同组独立消费

不同 Consumer Group 可以各自消费一份完整消息。

```text
order-topic
   ↓
stock-group  消费一份
point-group  消费一份
sms-group    消费一份
```

适合多个业务系统订阅同一类事件。

---

## 14. Kafka 如何保证顺序性

Kafka 只能保证：

```text
单个 Partition 内消息有序
Topic 全局不保证有序
```

如果要保证同一个业务维度有序，例如同一个订单：

```text
订单创建
订单支付
订单发货
订单完成
```

应该让相同 `orderId` 的消息进入同一个 Partition：

```text
partition = hash(orderId) % partitionCount
```

注意：

```text
分区越多，并发越高
但全局顺序越难保证
```

---

## 15. Kafka 如何保证消息不丢失

消息不丢失要从三个阶段看：

```text
生产阶段
存储阶段
消费阶段
```

---

### 15.1 生产阶段不丢失

关键配置：

```properties
acks=all
retries=3
enable.idempotence=true
```

说明：

| 配置 | 说明 |
|---|---|
| `acks=0` | Producer 不等 Broker 确认，性能高但可能丢 |
| `acks=1` | Leader 写入成功就返回，Leader 宕机可能丢 |
| `acks=all` | ISR 副本确认后返回，可靠性最高 |
| `retries` | 发送失败重试 |
| `enable.idempotence` | 开启幂等生产者，避免重试导致重复写 |

---

### 15.2 Broker 存储阶段不丢失

关键配置：

```properties
replication.factor=3
min.insync.replicas=2
unclean.leader.election.enable=false
```

说明：

| 配置 | 说明 |
|---|---|
| `replication.factor` | 副本数量 |
| `min.insync.replicas` | 最少同步副本数 |
| `unclean.leader.election.enable=false` | 禁止落后副本成为 Leader |

推荐组合：

```properties
acks=all
replication.factor=3
min.insync.replicas=2
unclean.leader.election.enable=false
```

---

### 15.3 消费阶段不丢失

消费者应该：

```text
先处理业务
业务成功后再提交 Offset
```

不推荐：

```text
先提交 Offset
再处理业务
```

否则业务处理失败时，消息已经被认为消费完成，就可能丢消息。

推荐流程：

```text
1. 拉取消息
2. 执行业务逻辑
3. 业务成功后手动提交 Offset
```

---

## 16. Kafka 会不会重复消费

会。

Kafka 在实际业务中通常按“至少一次”来设计。

重复消费常见原因：

- 消费成功但提交 Offset 失败
- Consumer 处理超时
- Consumer 宕机
- Rebalance
- 网络异常
- Producer 重试

所以消费者必须做幂等。

---

## 17. 消费幂等怎么做

### 17.1 业务唯一键

例如：

```text
orderId
payId
requestId
eventId
```

处理前先判断是否已经处理过。

---

### 17.2 去重表

```sql
create table mq_consume_record (
    id bigint primary key auto_increment,
    message_id varchar(128) not null,
    consumer_group varchar(128) not null,
    create_time datetime not null,
    unique key uk_msg_group(message_id, consumer_group)
);
```

消费时先插入去重表。

如果唯一键冲突，说明已经消费过。

---

### 17.3 状态机控制

例如订单状态：

```text
待支付 -> 已支付 -> 已发货 -> 已完成
```

如果订单已经是“已支付”，再次收到支付成功消息，直接忽略。

---

### 17.4 Redis SETNX

```text
SETNX messageId 1
EXPIRE messageId 86400
```

适合对一致性要求没那么高的场景。

---

## 18. Kafka Rebalance

Rebalance 是消费者组内 Partition 重新分配的过程。

触发场景：

- Consumer 新加入
- Consumer 下线
- Topic 分区数变化
- Consumer 心跳超时
- 订阅关系变化

Rebalance 期间可能导致：

- 消费暂停
- 重复消费
- 消费延迟上升

优化思路：

```text
减少频繁上下线
合理配置 session.timeout.ms
合理配置 heartbeat.interval.ms
消费逻辑不要阻塞 poll
使用静态成员机制减少不必要 rebalance
```

---

## 19. Kafka 消息积压怎么处理

消息积压本质：

```text
生产速度 > 消费速度
```

常见原因：

- Consumer 数量不足
- Partition 数量不足
- 消费逻辑太慢
- 下游数据库慢
- 下游接口慢
- 单条消息反复失败
- Rebalance 频繁
- 消费线程阻塞

排查思路：

```text
1. 查看 Consumer Lag
2. 查看 Topic 分区数
3. 查看 Consumer 数量
4. 查看消费耗时
5. 查看下游 DB / Redis / HTTP 是否慢
6. 查看是否有异常消息反复重试
```

解决方案：

```text
增加 Consumer 实例
增加 Partition 数量
优化消费逻辑
批量消费
异步处理
优化下游依赖
临时扩容
拆分 Topic
```

注意：

```text
同一个 Consumer Group 内，Consumer 数量超过 Partition 数量时，多出来的 Consumer 无法分配到 Partition。
```

---

## 20. Kafka 高吞吐原因

Kafka 性能高主要因为：

- 顺序写磁盘
- Page Cache
- 零拷贝
- 批量发送
- 批量拉取
- 分区并行
- 压缩机制
- Append-only 日志结构

---

## 21. Kafka 存储原理

Kafka 的消息以日志文件形式追加写入。

每个 Partition 对应一个有序日志：

```text
topic-order
  partition-0
    00000000000000000000.log
    00000000000000000000.index
    00000000000000000000.timeindex
```

主要文件：

| 文件 | 说明 |
|---|---|
| `.log` | 存储真实消息 |
| `.index` | offset 到物理位置的索引 |
| `.timeindex` | 时间戳索引 |

---

## 22. Kafka 消息清理机制

Kafka 消息不是消费完立即删除，而是按策略保留。

### 22.1 按时间保留

```properties
log.retention.hours=168
```

表示保留 7 天。

---

### 22.2 按大小保留

```properties
log.retention.bytes=1073741824
```

超过大小后清理旧数据。

---

### 22.3 日志压缩 Log Compaction

保留相同 key 的最新值。

适合：

```text
用户状态
配置变更
维表数据
```

---

## 23. Kafka 事务消息

Kafka 支持事务，用于保证一组消息写入的原子性。

典型场景：

```text
消费 topic-A
处理业务
生产 topic-B
提交 offset
```

希望这些操作要么一起成功，要么一起失败。

事务相关配置：

```properties
transactional.id=my-transactional-id
enable.idempotence=true
```

---

## 24. Kafka 的投递语义

| 语义 | 说明 | 实现方式 |
|---|---|---|
| At most once | 最多一次，可能丢，不重复 | 先提交 offset 后处理 |
| At least once | 至少一次，不丢但可能重复 | 处理成功后提交 offset |
| Exactly once | 精确一次 | 幂等 Producer + 事务 + 合理消费处理 |

实际业务中最常用：

```text
At least once + 消费端幂等
```

---

## 25. Kafka 和 RocketMQ 对比

| 对比项 | Kafka | RocketMQ |
|---|---|---|
| 主要定位 | 日志、事件流、实时数据管道 | 业务消息、事务消息 |
| 吞吐量 | 很高 | 高 |
| 顺序消息 | 分区内有序 | 支持顺序消息 |
| 延迟消息 | 原生能力相对弱，常需额外实现 | 支持较好 |
| 事务消息 | 支持，但使用复杂度较高 | 业务事务消息支持友好 |
| 消费重试 | 通常业务侧处理或框架封装 | 内置重试和死信机制更明显 |
| 消息保留 | 按时间/大小保留 | 更偏传统消息消费模型 |
| 典型场景 | 日志采集、流处理、大数据 | 订单、支付、业务解耦 |

简单理解：

```text
Kafka 更偏事件流和大数据管道
RocketMQ 更偏业务消息和事务消息
```

---

## 26. Kafka 常用命令

### 26.1 查看 Topic

```bash
kafka-topics.sh --bootstrap-server localhost:9092 --list
```

---

### 26.2 创建 Topic

```bash
kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic order-topic \
  --partitions 3 \
  --replication-factor 3
```

---

### 26.3 查看 Topic 详情

```bash
kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic order-topic
```

---

### 26.4 生产消息

```bash
kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-topic
```

---

### 26.5 消费消息

```bash
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-topic \
  --from-beginning
```

---

### 26.6 查看 Consumer Group

```bash
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --list
```

---

### 26.7 查看消费进度

```bash
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group order-consumer-group
```

重点看：

```text
CURRENT-OFFSET
LOG-END-OFFSET
LAG
```

---

## 27. Spring Boot 使用 Kafka

### 27.1 Maven 依赖

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

---

### 27.2 配置

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
    consumer:
      group-id: order-consumer-group
      enable-auto-commit: false
      auto-offset-reset: earliest
```

---

### 27.3 发送消息

```java
@RestController
public class OrderController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/order/create")
    public String createOrder() {
        kafkaTemplate.send("order-topic", "orderId-1001", "order created");
        return "success";
    }
}
```

---

### 27.4 消费消息

```java
@Component
public class OrderConsumer {

    @KafkaListener(
        topics = "order-topic",
        groupId = "order-consumer-group"
    )
    public void consume(String message) {
        System.out.println("收到消息：" + message);

        // 处理业务逻辑
    }
}
```

---

### 27.5 手动提交 Offset

```java
@KafkaListener(topics = "order-topic", groupId = "order-consumer-group")
public void consume(String message, Acknowledgment ack) {
    try {
        // 1. 处理业务
        System.out.println("处理消息：" + message);

        // 2. 业务成功后提交 offset
        ack.acknowledge();
    } catch (Exception e) {
        // 不提交 offset，后续可重试
        throw e;
    }
}
```

配置：

```yaml
spring:
  kafka:
    listener:
      ack-mode: manual
```

---

## 28. Kafka 常见问题

### 28.1 消息丢失

排查：

```text
Producer 是否 acks=all
是否开启 retries
是否开启幂等 Producer
Broker 副本数是否足够
min.insync.replicas 是否合理
Consumer 是否先提交 offset 再处理业务
```

---

### 28.2 消息重复

原因：

```text
Producer 重试
Consumer 处理成功但 offset 提交失败
Consumer Rebalance
网络异常
```

解决：

```text
Producer 开启幂等
Consumer 做幂等
业务唯一键去重
状态机控制
去重表
```

---

### 28.3 消息积压

排查：

```bash
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group order-consumer-group
```

看 `LAG`。

解决：

```text
扩容 Consumer
增加 Partition
优化消费逻辑
批量消费
异步处理
优化下游数据库/接口
```

---

### 28.4 消费不到消息

排查：

```text
Topic 是否写对
Consumer Group 是否写对
auto-offset-reset 配置
是否已经提交过 offset
是否有分区分配
是否 Rebalance 异常
```

---

### 28.5 分区数不够

现象：

```text
Consumer 数量增加了，但消费能力没有提升
```

原因：

```text
同一个 Consumer Group 中，一个 Partition 同一时间只能分配给一个 Consumer。
```

解决：

```text
增加 Topic Partition 数量
重新规划 key 分区策略
拆分 Topic
```

---

## 29. Kafka 面试常问问题

### 29.1 Kafka 为什么性能高？

主要原因：

```text
顺序写磁盘
Page Cache
零拷贝
批量发送和批量拉取
分区并行
压缩传输
日志追加写模型
```

---

### 29.2 Kafka 如何保证消息不丢失？

从三端回答：

```text
Producer：
  acks=all
  retries
  enable.idempotence=true

Broker：
  replication.factor >= 3
  min.insync.replicas >= 2
  unclean.leader.election.enable=false

Consumer：
  业务处理成功后再提交 offset
  关闭自动提交或谨慎使用自动提交
  消费端做好幂等和补偿
```

---

### 29.3 Kafka 如何保证顺序消费？

Kafka 只能保证单 Partition 内有序。

如果要保证同一个订单有序：

```text
使用 orderId 作为 key
让同一个 orderId 的消息进入同一个 Partition
同一个 Partition 由一个 Consumer 顺序消费
```

---

### 29.4 Kafka 为什么会重复消费？

因为 Kafka 常见使用模式是至少一次语义。

重复消费原因：

```text
消费成功但 offset 提交失败
Consumer 宕机
Rebalance
网络异常
Producer 重试
```

解决方式：

```text
消费端做幂等
```

---

### 29.5 Kafka 消息积压怎么排查？

思路：

```text
1. 查看 Consumer Group Lag
2. 查看 Consumer 是否存活
3. 查看分区是否均衡
4. 查看消费逻辑是否慢
5. 查看下游 DB / Redis / HTTP 是否慢
6. 查看是否频繁 Rebalance
7. 根据情况扩容 Consumer 或增加 Partition
```

---

## 30. KRaft 和 ZooKeeper

早期 Kafka 依赖 ZooKeeper 管理元数据、Broker 协调和 Controller 选举。

新版本 Kafka 使用 KRaft 模式，将元数据管理内置到 Kafka 自身的控制平面中。

简单理解：

```text
老架构：
Kafka Broker + ZooKeeper

新架构：
Kafka Broker + KRaft Controller
```

KRaft 的目标：

```text
减少外部依赖
简化运维
提升元数据管理能力
提升集群扩展性
```

---

## 31. 总结

Kafka 是一个分布式事件流平台，也可以作为高吞吐消息队列使用，常用于日志采集、异步解耦、流量削峰、实时计算和事件驱动架构。

Kafka 的核心组件包括 Producer、Consumer、Broker、Topic、Partition、Consumer Group 和 Offset。Producer 将消息写入 Topic，Topic 会拆分为多个 Partition，每个 Partition 是一个有序追加日志。Consumer 以 Consumer Group 的方式消费消息，同一个 Consumer Group 内，一个 Partition 同一时间只会分配给一个 Consumer，因此 Kafka 能通过 Partition 实现并发消费，同时保证单分区内有序。

Kafka 的高性能主要来自顺序写磁盘、Page Cache、零拷贝、批量发送、批量拉取和分区并行。可靠性方面，Producer 端可以配置 `acks=all`、`retries` 和 `enable.idempotence=true`，Broker 端通过副本机制、ISR、`min.insync.replicas` 保证消息持久化，Consumer 端通常在业务处理成功后再提交 Offset，避免消息丢失。

Kafka 通常会出现重复消费，因此消费端必须保证幂等。常见幂等方案有业务唯一 ID、去重表、Redis SETNX 和状态机控制。Kafka 只能保证 Partition 内有序，如果要保证同一个订单的消息顺序，需要使用相同的 key，让这些消息进入同一个 Partition。

---

## 32. 一句话总结

> Kafka 是一个高吞吐、可持久化、可扩展的分布式事件流平台；核心是 Topic、Partition、Offset 和 Consumer Group，优势是高吞吐和流式处理能力，使用时重点关注消息不丢失、重复消费幂等、分区顺序、消费积压和 Rebalance。
