# RocketMQ

## 1. RocketMQ 是什么

RocketMQ 是阿里开源、Apache 顶级项目的分布式消息中间件，主要用于：

- 应用解耦
- 异步处理
- 流量削峰
- 分布式事务最终一致性
- 顺序消息
- 延迟消息
- 消息广播
- 日志/事件驱动架构

一句话理解：

> RocketMQ 用来在系统之间可靠地传递消息，让系统之间从同步强依赖变成异步弱依赖。

---

## 2. RocketMQ 适合解决什么问题

### 2.1 应用解耦

没有 MQ：

```text
订单服务 -> 库存服务
订单服务 -> 积分服务
订单服务 -> 优惠券服务
订单服务 -> 短信服务
```

订单服务强依赖多个下游服务。

使用 MQ 后：

```text
订单服务 -> RocketMQ -> 库存服务
                    -> 积分服务
                    -> 优惠券服务
                    -> 短信服务
```

订单服务只负责发送订单事件，下游服务自己消费。

---

### 2.2 异步处理

例如下单流程：

```text
创建订单
扣库存
发积分
发优惠券
发短信
发送站内信
```

其中发短信、发站内信、发积分不一定要同步完成，可以异步处理。

---

### 2.3 流量削峰

秒杀场景中，请求量可能瞬间暴增。

可以先把请求写入 MQ：

```text
用户请求 -> 网关 -> MQ -> 秒杀服务慢慢消费
```

避免流量直接打垮数据库。

---

### 2.4 分布式事务最终一致性

例如支付成功后要更新订单状态：

```text
支付服务 -> 支付成功消息 -> 订单服务更新订单状态
```

RocketMQ 支持事务消息，可以保证本地事务和消息发送的最终一致性。

---

## 3. RocketMQ 核心角色

| 角色 | 说明 |
|---|---|
| Producer | 消息生产者，负责发送消息 |
| Consumer | 消息消费者，负责消费消息 |
| Broker | 消息服务器，负责存储和转发消息 |
| NameServer | 注册中心，负责保存 Broker 路由信息 |
| Topic | 消息主题，用于区分业务类型 |
| MessageQueue | Topic 下的队列，用于负载均衡和并发消费 |

---

## 4. RocketMQ 架构

```text
Producer
   ↓
NameServer 获取 Broker 路由
   ↓
Broker 存储消息
   ↓
Consumer 从 Broker 拉取消息
```

更完整：

```text
                +-------------+
                | NameServer  |
                +-------------+
                 ↑           ↑
        注册 Broker      查询路由
                 ↑           ↑
+----------+   发送消息   +----------+
| Producer | ----------> |  Broker  |
+----------+             +----------+
                              ↑
                              |
                         拉取消息
                              |
                        +----------+
                        | Consumer |
                        +----------+
```

---

## 5. NameServer

NameServer 类似注册中心。

主要作用：

- 保存 Broker 地址
- 保存 Topic 路由信息
- Producer 通过 NameServer 找 Broker
- Consumer 通过 NameServer 找 Broker

特点：

- NameServer 之间互不通信
- 多个 NameServer 可以提高可用性
- Broker 会定时向 NameServer 注册
- Producer 和 Consumer 会定时拉取路由信息

---

## 6. Broker

Broker 是 RocketMQ 的核心。

主要作用：

- 接收 Producer 发送的消息
- 存储消息
- 提供消息拉取能力
- 维护消费进度
- 支持主从复制
- 支持消息重试
- 支持死信队列

Broker 通常分为：

| 类型 | 说明 |
|---|---|
| Master Broker | 主节点，支持读写 |
| Slave Broker | 从节点，通常用于备份和读 |

---

## 7. Topic

Topic 是消息的逻辑分类。

例如：

```text
order-topic
pay-topic
stock-topic
user-topic
```

生产者发送消息时指定 Topic：

```text
订单创建消息 -> order-topic
支付成功消息 -> pay-topic
库存变更消息 -> stock-topic
```

消费者订阅指定 Topic：

```text
订单服务消费 order-topic
积分服务消费 order-topic
短信服务消费 pay-topic
```

---

## 8. MessageQueue

一个 Topic 可以有多个 MessageQueue。

例如：

```text
order-topic
    queue-0
    queue-1
    queue-2
    queue-3
```

作用：

- 提高并发能力
- 支持负载均衡消费
- 支持顺序消息

Consumer Group 中的消费者会分配不同队列进行消费。

---

## 9. Producer 发送消息流程

```text
1. Producer 启动
2. 从 NameServer 获取 Topic 路由信息
3. 根据路由选择 Broker
4. 发送消息到 Broker
5. Broker 写入 CommitLog
6. Broker 返回发送结果
```

流程图：

```text
Producer
   ↓ 查询路由
NameServer
   ↓ 返回 Broker 地址
Producer
   ↓ 发送消息
Broker
   ↓ 写入磁盘
返回发送成功
```

---

## 10. Consumer 消费消息流程

```text
1. Consumer 启动
2. 从 NameServer 获取 Topic 路由信息
3. 找到对应 Broker
4. 从 Broker 拉取消息
5. 执行业务消费逻辑
6. 消费成功后提交 offset
```

流程图：

```text
Consumer
   ↓ 查询路由
NameServer
   ↓ 返回 Broker 地址
Consumer
   ↓ 拉取消息
Broker
   ↓ 返回消息
Consumer 执行业务逻辑
   ↓
提交消费进度
```

---

## 11. RocketMQ 消息发送方式

## 11.1 同步发送

发送消息后等待 Broker 返回结果。

适合重要消息。

```java
SendResult result = producer.send(message);
```

特点：

- 可靠性高
- 有返回结果
- 性能比异步低

适合：

- 订单创建
- 支付成功
- 交易通知

---

## 11.2 异步发送

发送消息后不阻塞，通过回调获取结果。

```java
producer.send(message, new SendCallback() {
    @Override
    public void onSuccess(SendResult sendResult) {
        System.out.println("发送成功");
    }

    @Override
    public void onException(Throwable e) {
        System.out.println("发送失败");
    }
});
```

特点：

- 性能高
- 不阻塞主线程
- 需要处理回调结果

适合：

- 日志消息
- 用户行为消息
- 非核心通知

---

## 11.3 单向发送

只负责发送，不关心结果。

```java
producer.sendOneway(message);
```

特点：

- 性能最高
- 可靠性最低
- 不知道是否发送成功

适合：

- 日志采集
- 监控埋点
- 非关键消息

---

## 12. RocketMQ 消费模式

## 12.1 集群消费

同一个 Consumer Group 中，一条消息只会被一个消费者消费。

```text
Consumer Group A
    Consumer 1
    Consumer 2
    Consumer 3
```

一条消息只会被其中一个 Consumer 消费。

适合：

- 订单处理
- 库存扣减
- 积分发放

---

## 12.2 广播消费

同一个 Consumer Group 中，每个消费者都会消费同一条消息。

```text
Consumer 1 消费消息 A
Consumer 2 消费消息 A
Consumer 3 消费消息 A
```

适合：

- 本地缓存刷新
- 配置通知
- 广播事件

---

## 13. Consumer Group

Consumer Group 是消费者分组。

作用：

- 实现负载均衡消费
- 实现消费进度管理
- 实现消息重试
- 区分不同业务消费方

例如：

```text
order-created-topic

库存服务消费组：stock-consumer-group
积分服务消费组：point-consumer-group
短信服务消费组：sms-consumer-group
```

同一个 Topic 可以被多个 Consumer Group 消费。

每个 Consumer Group 都会收到一份完整消息。

---

## 14. 消息消费进度 Offset

RocketMQ 通过 Offset 记录消费进度。

每个 Consumer Group 对每个 MessageQueue 都有自己的 Offset。

```text
Topic: order-topic
Queue: queue-0
ConsumerGroup: stock-group
Offset: 100
```

表示 stock-group 已经消费到 queue-0 的第 100 条消息。

---

## 15. RocketMQ 如何保证消息不丢失

要从三个阶段看：

```text
生产阶段
存储阶段
消费阶段
```

---

## 15.1 生产阶段不丢失

生产者发送消息时可以使用同步发送：

```java
SendResult result = producer.send(message);
```

并检查发送结果。

推荐措施：

- 使用同步发送
- 开启发送失败重试
- 记录发送失败日志
- 失败消息落库或补偿
- 业务侧保证幂等

---

## 15.2 Broker 存储阶段不丢失

Broker 收到消息后会写入 CommitLog。

可靠性相关配置：

| 配置 | 说明 |
|---|---|
| 同步刷盘 | 消息写入磁盘后才返回成功 |
| 异步刷盘 | 消息写入内存后先返回，后台刷盘 |
| 主从同步复制 | Master 同步到 Slave 后返回 |
| 主从异步复制 | Master 返回后异步复制到 Slave |

可靠性最高组合：

```text
同步刷盘 + 主从同步复制
```

但性能会下降。

---

## 15.3 消费阶段不丢失

消费者消费成功后才返回成功状态。

如果消费失败，RocketMQ 会进行重试。

消费者需要注意：

- 业务处理成功后再返回消费成功
- 消费异常时返回稍后重试
- 消费逻辑要保证幂等
- 失败消息最终进入死信队列后要人工补偿

---

## 16. RocketMQ 会不会重复消费

会。

RocketMQ 通常保证的是：

```text
至少消费一次
```

也就是消息可能重复消费。

可能原因：

- 消费成功但提交 Offset 失败
- 消费超时
- Broker 重试
- Consumer 宕机
- 网络异常
- Rebalance

所以消费者必须做幂等。

---

## 17. 消息幂等怎么做

常见方案：

### 17.1 唯一业务 ID

例如订单消息：

```text
orderId = 1001
```

处理前先判断订单是否已经处理过。

---

### 17.2 去重表

```sql
create table message_consume_record (
    id bigint primary key auto_increment,
    message_id varchar(128),
    consumer_group varchar(128),
    create_time datetime,
    unique key uk_msg_group(message_id, consumer_group)
);
```

消费前插入去重表。

如果唯一键冲突，说明已经消费过。

---

### 17.3 状态机控制

例如订单状态：

```text
待支付 -> 已支付 -> 已发货
```

如果订单已经是已支付，再收到支付成功消息，直接忽略。

---

### 17.4 Redis 去重

```text
SETNX messageId 1
```

设置过期时间，防止重复消费。

适合对强一致性要求不高的场景。

---

## 18. RocketMQ 顺序消息

顺序消息是指消息按照发送顺序被消费。

例如订单状态流转：

```text
订单创建
订单支付
订单发货
订单完成
```

必须按顺序消费。

---

## 18.1 如何保证顺序

核心原则：

```text
同一业务 ID 的消息发送到同一个 MessageQueue
同一个 MessageQueue 由同一个消费者顺序消费
```

例如按 `orderId` 取模选择队列：

```java
producer.send(message, (mqs, msg, arg) -> {
    Long orderId = (Long) arg;
    int index = (int) (orderId % mqs.size());
    return mqs.get(index);
}, orderId);
```

---

## 18.2 顺序消息分类

| 类型 | 说明 |
|---|---|
| 全局顺序 | 整个 Topic 所有消息都顺序消费 |
| 分区顺序 | 同一个队列内消息顺序消费 |

实际开发中大多数使用：

```text
分区顺序
```

因为全局顺序性能较差。

---

## 19. 延迟消息

延迟消息是指消息发送后，不立即被消费，而是延迟一段时间后再投递。

适合场景：

- 订单 30 分钟未支付自动取消
- 会议开始前提醒
- 超时关闭任务
- 定时重试

示例：

```java
Message message = new Message("order-topic", body);
message.setDelayTimeLevel(3);
producer.send(message);
```

旧版本 RocketMQ 使用固定延迟等级。

常见延迟等级类似：

```text
1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
```

---

## 20. 事务消息

RocketMQ 支持事务消息，用于解决：

```text
本地事务执行成功，但消息发送失败
消息发送成功，但本地事务失败
```

典型场景：

```text
支付服务本地更新支付单
发送支付成功消息
订单服务消费消息更新订单状态
```

---

## 20.1 事务消息流程

```text
1. Producer 发送半消息
2. Broker 保存半消息，但 Consumer 不可见
3. Producer 执行本地事务
4. 本地事务成功，提交消息
5. 本地事务失败，回滚消息
6. 如果 Producer 未响应，Broker 回查本地事务状态
```

流程图：

```text
Producer -> Broker：发送半消息
Producer：执行本地事务
    成功 -> Broker：commit
    失败 -> Broker：rollback
    未知 -> Broker：事务回查
Consumer：只能消费 commit 后的消息
```

---

## 20.2 事务消息状态

| 状态 | 说明 |
|---|---|
| COMMIT | 提交消息，消费者可见 |
| ROLLBACK | 回滚消息，消费者不可见 |
| UNKNOWN | 状态未知，Broker 后续回查 |

---

## 21. 消息重试

消费者消费失败后，RocketMQ 会自动重试。

消费失败常见返回：

```java
return ConsumeConcurrentlyStatus.RECONSUME_LATER;
```

或者抛出异常。

RocketMQ 会延迟一段时间后重新投递。

---

## 22. 死信队列

如果消息多次重试后仍然失败，会进入死信队列。

死信队列名称通常类似：

```text
%DLQ%consumerGroup
```

死信队列中的消息需要人工处理或补偿。

常见处理方式：

- 查看失败原因
- 修复业务问题
- 重新投递消息
- 人工补偿数据
- 告警通知

---

## 23. 消息过滤

RocketMQ 支持按 Tag 过滤消息。

发送消息：

```java
Message message = new Message(
    "order-topic",
    "create",
    body
);
```

消费者订阅：

```java
consumer.subscribe("order-topic", "create || pay");
```

说明：

```text
Topic：业务大类
Tag：业务小类
```

例如：

```text
Topic: order-topic
Tag: create / pay / cancel
```

---

## 24. Key 的作用

Message Key 通常用于快速定位消息。

```java
message.setKeys(orderId);
```

常见用途：

- 根据订单号查消息
- 排查消息是否发送成功
- 定位重复消费问题
- 日志追踪

---

## 25. RocketMQ 存储原理

RocketMQ 的消息主要存储在：

```text
CommitLog
ConsumeQueue
IndexFile
```

---

## 25.1 CommitLog

CommitLog 是真正存储消息内容的文件。

特点：

- 顺序写
- 写入性能高
- 所有 Topic 的消息都写在 CommitLog 中

---

## 25.2 ConsumeQueue

ConsumeQueue 是逻辑消费队列。

它不存储完整消息，只存储：

- CommitLog 偏移量
- 消息大小
- Tag HashCode

消费者先读取 ConsumeQueue，再根据偏移量去 CommitLog 读取真实消息。

---

## 25.3 IndexFile

IndexFile 用于按 Key 查询消息。

例如根据订单号查询消息：

```text
orderId -> CommitLog offset
```

---

## 26. RocketMQ 为什么性能高

主要原因：

- CommitLog 顺序写磁盘
- 使用 PageCache
- 零拷贝技术
- 批量发送
- 异步刷盘
- 高效的消费队列 ConsumeQueue
- Topic 队列分片提高并发

---

## 27. RocketMQ 高可用

RocketMQ 高可用主要依赖：

- 多 NameServer
- Broker 主从
- Broker 集群
- Producer 重试
- Consumer 重试
- 消费进度持久化
- 死信队列

---

## 28. RocketMQ 和 Kafka 对比

| 对比项 | RocketMQ | Kafka |
|---|---|---|
| 主要场景 | 业务消息、事务消息、延迟消息 | 日志、大数据、流式处理 |
| 事务消息 | 支持较好 | 支持事务，但业务事务场景使用较复杂 |
| 延迟消息 | 支持 | 原生支持较弱，通常需额外实现 |
| 顺序消息 | 支持 | 分区内有序 |
| 消息重试 | 支持较完善 | 通常业务侧处理 |
| 死信队列 | 支持 | 通常业务侧实现 |
| 吞吐量 | 高 | 很高 |
| 使用复杂度 | 适中 | 偏底层一些 |

简单理解：

```text
RocketMQ 更偏业务消息
Kafka 更偏日志和流处理
```

---

## 29. RocketMQ 和 RabbitMQ 对比

| 对比项 | RocketMQ | RabbitMQ |
|---|---|---|
| 协议 | 自定义协议 | AMQP |
| 吞吐量 | 高 | 中等 |
| 事务消息 | 支持较好 | 支持，但性能影响较大 |
| 延迟消息 | 支持 | 通常依赖插件 |
| 顺序消息 | 支持 | 支持较弱 |
| 路由能力 | 一般 | 很强 |
| 适合场景 | 高并发业务消息 | 复杂路由、传统企业消息 |

---

## 30. Spring Boot 使用 RocketMQ

### 30.1 引入依赖

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>${rocketmq.version}</version>
</dependency>
```

---

### 30.2 配置

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: order-producer-group
```

---

### 30.3 发送消息

```java
@RestController
public class OrderController {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @PostMapping("/order/create")
    public String createOrder() {
        OrderMessage message = new OrderMessage();
        message.setOrderId("1001");
        message.setUserId("2001");

        rocketMQTemplate.convertAndSend("order-topic:create", message);

        return "success";
    }
}
```

---

### 30.4 消费消息

```java
@Component
@RocketMQMessageListener(
    topic = "order-topic",
    consumerGroup = "stock-consumer-group"
)
public class OrderMessageConsumer implements RocketMQListener<OrderMessage> {

    @Override
    public void onMessage(OrderMessage message) {
        System.out.println("收到订单消息：" + message.getOrderId());

        // 执行业务逻辑
        // 扣库存、发积分、发通知等
    }
}
```

---

## 31. RocketMQ 常见问题

## 31.1 消息积压

现象：

```text
Consumer 消费速度小于 Producer 发送速度
```

原因：

- 消费逻辑太慢
- Consumer 实例太少
- 消费线程数太少
- 下游数据库慢
- 下游接口慢
- 某条消息反复失败重试

解决：

- 增加 Consumer 实例
- 增加消费线程数
- 优化消费逻辑
- 批量消费
- 排查慢 SQL
- 临时扩容
- 跳过或转移异常消息
- 提高 Topic 队列数

---

## 31.2 消息重复消费

原因：

- 消费成功但提交 Offset 失败
- Consumer 宕机
- 网络异常
- Rebalance
- 消费超时

解决：

- 消费端做幂等
- 使用业务唯一 ID 去重
- 去重表
- Redis SETNX
- 状态机控制

---

## 31.3 消息丢失

排查方向：

```text
Producer 是否发送成功
Broker 是否刷盘成功
Broker 主从是否同步
Consumer 是否正确返回消费状态
失败消息是否进入重试队列或死信队列
```

解决：

- 同步发送
- 失败重试
- 同步刷盘
- 主从同步复制
- 消费失败返回重试
- 死信队列告警和补偿

---

## 31.4 消费很慢

原因：

- 单条消息处理耗时长
- 消费线程数太少
- 数据库慢
- Redis 慢
- 下游 HTTP 慢
- 消费者机器资源不足
- 消息顺序消费导致并发低

解决：

- 优化业务逻辑
- 增加消费者数量
- 增加队列数量
- 调整消费线程池
- 批量处理
- 异步处理
- 拆分 Topic
- 优化下游依赖

---

## 32. RocketMQ 面试常问问题

## 32.1 RocketMQ 如何保证消息不丢失？

从三个阶段保证：

```text
生产阶段：同步发送 + 失败重试 + 失败记录
存储阶段：同步刷盘 + 主从同步复制
消费阶段：消费成功后再提交 offset，失败自动重试，最终进入死信队列
```

同时业务侧需要做好补偿和幂等。

---

## 32.2 RocketMQ 如何保证顺序消费？

核心是：

```text
同一业务 ID 的消息发送到同一个 MessageQueue
同一个 MessageQueue 由同一个消费者顺序消费
```

例如订单消息按照 `orderId` 选择队列。

---

## 32.3 RocketMQ 为什么会重复消费？

因为 RocketMQ 通常保证至少消费一次。

如果消费者处理成功但提交 Offset 失败，或者消费过程中发生网络异常、宕机、Rebalance，都可能导致消息重新投递。

所以消费端必须保证幂等。

---

## 32.4 RocketMQ 消息积压怎么处理？

处理思路：

```text
1. 查看积压量和消费 TPS
2. 判断是否某些队列积压严重
3. 查看消费者是否异常
4. 查看消费逻辑是否慢
5. 检查数据库、Redis、HTTP 下游是否慢
6. 临时扩容 Consumer
7. 增加消费线程数
8. 必要时新建 Topic 转发积压消息并扩容消费
```

---

## 32.5 RocketMQ 事务消息原理是什么？

事务消息流程：

```text
1. 发送半消息
2. 执行本地事务
3. 本地事务成功则提交消息
4. 本地事务失败则回滚消息
5. 如果事务状态未知，Broker 会回查 Producer
```

Consumer 只能消费提交后的消息。

---

## 33. 总结

RocketMQ 是一个分布式消息中间件，主要用于应用解耦、异步处理、流量削峰、顺序消息、延迟消息和分布式事务最终一致性。

它的核心组件包括 Producer、Consumer、Broker、NameServer、Topic 和 MessageQueue。Producer 发送消息前会先从 NameServer 获取 Topic 路由信息，然后把消息发送到对应 Broker。Broker 负责存储消息，Consumer 从 Broker 拉取消息并消费。

RocketMQ 的消息存储主要基于 CommitLog、ConsumeQueue 和 IndexFile。CommitLog 存储完整消息，采用顺序写磁盘提升性能；ConsumeQueue 是逻辑消费队列，保存消息在 CommitLog 中的偏移量；IndexFile 用于按 Key 查询消息。

消息可靠性方面，生产阶段可以使用同步发送和失败重试，Broker 存储阶段可以使用同步刷盘和主从同步复制，消费阶段消费失败会自动重试，超过次数进入死信队列。由于 RocketMQ 通常保证至少消费一次，所以消费者必须做好幂等。

RocketMQ 还支持顺序消息、延迟消息和事务消息。顺序消息通过把同一业务 ID 的消息发送到同一个队列来保证顺序；延迟消息用于超时取消订单等场景；事务消息通过半消息、本地事务提交和事务回查机制保证本地事务和消息发送的最终一致性。

---

## 34. 一句话总结

> RocketMQ 是一个高性能、高可靠的分布式消息中间件，核心能力包括异步解耦、削峰填谷、顺序消息、延迟消息、事务消息、重试和死信队列；使用时重点要关注消息不丢失、重复消费幂等、顺序消费、消息积压和事务一致性。
