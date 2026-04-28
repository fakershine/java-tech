# Redis Pub/Sub 是什么？以及它的实现原理

## 目录

- [一、Redis Pub/Sub 是什么](#一redis-pubsub-是什么)
- [二、核心命令](#二核心命令)
- [三、基本使用示例](#三基本使用示例)
- [四、Redis Pub/Sub 的特点](#四redis-pubsub-的特点)
- [五、Redis Pub/Sub 的实现原理](#五redis-pubsub-的实现原理)
- [六、频道订阅的实现原理](#六频道订阅的实现原理)
- [七、发布消息的实现原理](#七发布消息的实现原理)
- [八、模式订阅的实现原理](#八模式订阅的实现原理)
- [九、普通订阅和模式订阅的区别](#九普通订阅和模式订阅的区别)
- [十、为什么 Pub/Sub 不可靠](#十为什么-pubsub-不可靠)
- [十一、Redis Pub/Sub 的数据结构理解](#十一redis-pubsub-的数据结构理解)
- [十二、Pub/Sub 的整体流程图](#十二pubsub-的整体流程图)
- [十三、Redis Pub/Sub 和 Redis Stream 的区别](#十三redis-pubsub-和-redis-stream-的区别)
- [十四、Redis Pub/Sub 和消息队列的区别](#十四redis-pubsub-和消息队列的区别)
- [十五、Redis Pub/Sub 的适用场景](#十五redis-pubsub-的适用场景)
- [十六、不适合的场景](#十六不适合的场景)
- [十七、在 Redisson 延迟队列中的作用](#十七在-redisson-延迟队列中的作用)
- [十八、为什么 Redisson 可以用 Pub/Sub](#十八为什么-redisson-可以用-pubsub)
- [十九、Pub/Sub 的核心问题](#十九pubsub-的核心问题)
- [二十、面试回答版本](#二十面试回答版本)
- [二十一、一句话总结](#二十一一句话总结)

---

## 一、Redis Pub/Sub 是什么

Redis 的 **Pub/Sub** 是 **Publish / Subscribe** 的缩写，也就是：

> 发布 / 订阅模式。

它是一种消息通信机制，用来实现：

```text
发布者向某个频道发送消息
        ↓
所有订阅该频道的客户端都能收到消息
```

可以简单理解为：

```text
频道 = 微信群
发布者 = 发消息的人
订阅者 = 群成员
消息 = 群消息
```

例如：

```text
客户端 A 订阅 exam_channel
客户端 B 向 exam_channel 发布消息
客户端 A 就能收到这条消息
```

---

## 二、核心命令

Redis Pub/Sub 主要有以下几个命令：

| 命令 | 作用 |
|---|---|
| `PUBLISH` | 向指定频道发布消息 |
| `SUBSCRIBE` | 订阅一个或多个频道 |
| `UNSUBSCRIBE` | 取消订阅频道 |
| `PSUBSCRIBE` | 按模式订阅频道 |
| `PUNSUBSCRIBE` | 取消模式订阅 |
| `PUBSUB` | 查看发布订阅相关信息 |

---

## 三、基本使用示例

### 3.1 订阅频道

客户端 A 执行：

```redis
SUBSCRIBE exam_channel
```

执行后，客户端 A 会进入订阅状态，等待该频道的消息。

---

### 3.2 发布消息

客户端 B 执行：

```redis
PUBLISH exam_channel "exam submit task arrived"
```

---

### 3.3 订阅者收到消息

客户端 A 会收到类似结果：

```text
message
exam_channel
exam submit task arrived
```

也就是说：

```text
只要客户端订阅了 exam_channel，
其他客户端往这个频道 publish 消息，
订阅者就能实时收到。
```

---

## 四、Redis Pub/Sub 的特点

### 4.1 实时广播

Redis Pub/Sub 是实时推送机制。

发布者发布消息后，Redis 会立即把消息推送给当前订阅该频道的客户端。

```text
发布者
  ↓
Redis Channel
  ↓ ↓ ↓
订阅者1 订阅者2 订阅者3
```

---

### 4.2 支持一对多

一个频道可以有多个订阅者。

发布者向频道发送一条消息，所有订阅者都能收到。

---

### 4.3 消息不持久化

Pub/Sub 的消息不会保存到 Redis 中。

如果订阅者不在线，消息就会丢失。

例如：

```text
10:00 发布了一条消息
10:01 客户端才订阅
```

那么 10:00 的消息不会补发。

---

### 4.4 没有 ACK 机制

Redis 只负责把消息推送给订阅者，不关心订阅者是否处理成功。

```text
Redis 推送消息
        ↓
订阅者收到
        ↓
是否处理成功，Redis 不管
```

---

### 4.5 没有失败重试

如果订阅者处理消息失败，Redis 不会重新投递。

---

### 4.6 没有消息堆积能力

Kafka、RocketMQ 这类消息队列可以保存消息，消费者慢一点也可以后续继续消费。

但 Redis Pub/Sub 不保存消息，因此不存在真正意义上的消息堆积。

```text
消费者在线：可以收到
消费者离线：消息丢失
```

---

## 五、Redis Pub/Sub 的实现原理

Redis Pub/Sub 的实现核心是：

> Redis Server 内部维护了频道和订阅客户端之间的映射关系。

可以简单理解为：

```text
channel -> client list
```

也就是：

```text
某个频道下面挂着一批订阅这个频道的客户端
```

当有客户端往这个频道发布消息时，Redis 就根据这个映射关系找到所有订阅者，然后把消息逐个推送给这些客户端。

---

## 六、频道订阅的实现原理

当客户端执行：

```redis
SUBSCRIBE exam_channel
```

Redis 会在服务端维护一份订阅关系。

内部可以抽象成类似这样的结构：

```text
pubsub_channels = {
    "exam_channel": [clientA, clientB, clientC],
    "order_channel": [clientD, clientE]
}
```

也就是说：

```text
exam_channel 频道下面保存了所有订阅该频道的客户端连接
```

当客户端订阅频道时，Redis 会做两件事：

```text
1. 把 channel 加入客户端自己的订阅列表
2. 把 client 加入 Redis Server 的 channel -> clients 映射中
```

可以理解为：

```text
客户端维度：
clientA 订阅了 exam_channel、order_channel

服务端维度：
exam_channel 被 clientA、clientB 订阅
order_channel 被 clientA、clientC 订阅
```

---

## 七、发布消息的实现原理

当客户端执行：

```redis
PUBLISH exam_channel "hello"
```

Redis 会做以下事情：

```text
1. 根据 channel 名称找到所有订阅该频道的客户端
2. 遍历这些客户端
3. 将消息写入每个客户端的输出缓冲区
4. Redis 网络层将消息发送给客户端
```

流程如下：

```text
PUBLISH exam_channel "hello"
        ↓
Redis 查找 pubsub_channels["exam_channel"]
        ↓
找到 clientA、clientB、clientC
        ↓
分别向这些客户端写入消息
        ↓
订阅者收到消息
```

所以，Redis Pub/Sub 的发布本质上就是：

> 根据频道找到订阅者列表，然后逐个推送消息。

---

## 八、模式订阅的实现原理

除了普通频道订阅，Redis 还支持模式订阅。

例如：

```redis
PSUBSCRIBE exam_*
```

这表示订阅所有匹配 `exam_*` 的频道。

例如这些频道都能匹配：

```text
exam_start
exam_submit
exam_timeout
```

Redis 内部会维护一份模式订阅关系，可以抽象为：

```text
pubsub_patterns = {
    "exam_*": [clientA, clientB],
    "order_*": [clientC]
}
```

当执行：

```redis
PUBLISH exam_submit "timeout"
```

Redis 不仅会查找普通频道订阅者：

```text
pubsub_channels["exam_submit"]
```

还会遍历所有模式订阅规则：

```text
pubsub_patterns
```

然后判断当前频道 `exam_submit` 是否匹配某个 pattern。

如果匹配，就把消息也推送给对应的模式订阅客户端。

---

## 九、普通订阅和模式订阅的区别

| 对比项 | 普通订阅 | 模式订阅 |
|---|---|---|
| 命令 | `SUBSCRIBE` | `PSUBSCRIBE` |
| 匹配方式 | 精确匹配频道名 | 按通配符匹配 |
| 示例 | `SUBSCRIBE exam_submit` | `PSUBSCRIBE exam_*` |
| 接收范围 | 只接收指定频道 | 接收所有匹配频道 |
| 性能 | 更好 | 需要模式匹配，开销更大 |

示例：

```redis
SUBSCRIBE exam_submit
```

只能收到：

```text
exam_submit
```

而：

```redis
PSUBSCRIBE exam_*
```

可以收到：

```text
exam_start
exam_submit
exam_timeout
```

---

## 十、为什么 Pub/Sub 不可靠

Pub/Sub 不可靠的根本原因是：

> Redis Pub/Sub 只维护订阅关系，不保存消息。

它不像消息队列那样有消息存储。

普通 MQ 的流程一般是：

```text
生产者发送消息
        ↓
Broker 持久化消息
        ↓
消费者消费消息
        ↓
消费者 ACK
        ↓
Broker 删除消息
```

而 Redis Pub/Sub 的流程是：

```text
发布者发送消息
        ↓
Redis 找到当前在线订阅者
        ↓
Redis 直接推送消息
        ↓
结束
```

中间没有：

```text
消息保存
ACK 确认
失败重试
消费进度
死信队列
```

所以只要订阅者当时不在线，或者网络断开，这条消息就无法再次获取。

---

## 十一、Redis Pub/Sub 的数据结构理解

可以把 Redis Server 中的 Pub/Sub 相关结构理解成两类。

### 11.1 普通频道订阅表

```text
channel -> clients
```

示例：

```text
exam_channel -> [clientA, clientB]
order_channel -> [clientC]
```

作用是：

> 根据频道名快速找到订阅该频道的所有客户端。

---

### 11.2 模式订阅表

```text
pattern -> clients
```

示例：

```text
exam_* -> [clientA]
order_* -> [clientB, clientC]
```

作用是：

> 发布消息时，判断频道名是否匹配某些 pattern，如果匹配，则推送给对应客户端。

---

## 十二、Pub/Sub 的整体流程图

```text
客户端 A
SUBSCRIBE exam_channel
        ↓
Redis 保存订阅关系
exam_channel -> clientA


客户端 B
PUBLISH exam_channel "hello"
        ↓
Redis 查找 exam_channel 对应的客户端列表
        ↓
找到 clientA
        ↓
把消息写入 clientA 的输出缓冲区
        ↓
clientA 收到消息
```

多个订阅者时：

```text
PUBLISH exam_channel "hello"
        ↓
Redis
        ↓
┌──────────┬──────────┬──────────┐
↓          ↓          ↓
clientA   clientB   clientC
```

---

## 十三、Redis Pub/Sub 和 Redis Stream 的区别

Redis 里除了 Pub/Sub，还有 `Stream`，两者很容易混淆。

| 对比项 | Pub/Sub | Stream |
|---|---|---|
| 消息是否持久化 | 不持久化 | 持久化 |
| 离线后能否消费历史消息 | 不能 | 可以 |
| ACK 机制 | 不支持 | 支持 |
| 消费者组 | 不支持 | 支持 |
| 消息堆积 | 不支持 | 支持 |
| 适合场景 | 实时通知 | 可靠消息流 |
| 可靠性 | 较弱 | 较强 |

简单说：

```text
Pub/Sub 适合实时通知
Stream 更像轻量级消息队列
```

如果业务要求消息不能丢，应该优先考虑：

```text
Redis Stream
RocketMQ
RabbitMQ
Kafka
Pulsar
```

而不是普通 Pub/Sub。

---

## 十四、Redis Pub/Sub 和消息队列的区别

| 对比项 | Redis Pub/Sub | MQ 消息队列 |
|---|---|---|
| 消息持久化 | 不支持 | 通常支持 |
| 离线消息 | 不支持 | 通常支持 |
| ACK 确认 | 不支持 | 通常支持 |
| 失败重试 | 不支持 | 通常支持 |
| 消息堆积 | 不支持 | 支持 |
| 消费进度 | 不保存 | 通常保存 |
| 死信队列 | 不支持 | 通常支持 |
| 消费模式 | 广播 | 点对点 / 广播 |
| 可靠性 | 较弱 | 较强 |
| 适合场景 | 实时通知 | 可靠异步任务 |

所以：

> Redis Pub/Sub 更适合做实时通知，不适合做可靠消息队列。

---

## 十五、Redis Pub/Sub 的适用场景

Redis Pub/Sub 适合：

- 配置变更通知；
- 缓存刷新通知；
- 多节点事件广播；
- 简单实时消息推送；
- WebSocket 服务间消息转发；
- 延迟队列中通知调度器重新计算时间；
- 本地缓存失效通知。

例如配置刷新：

```text
后台修改系统配置
        ↓
PUBLISH config_refresh
        ↓
所有应用节点收到通知
        ↓
刷新本地缓存
```

---

## 十六、不适合的场景

Redis Pub/Sub 不适合：

- 支付成功通知；
- 扣库存消息；
- 订单状态流转；
- 交易流水同步；
- 必须保证不丢的任务；
- 需要失败重试的任务；
- 需要死信队列的任务；
- 消费者可能长时间离线的场景。

这些场景更适合：

- Kafka；
- RocketMQ；
- RabbitMQ；
- Pulsar；
- Redis Stream。

---

## 十七、在 Redisson 延迟队列中的作用

Redisson 延迟队列底层用了：

```text
ZSet + List + Pub/Sub
```

其中：

| 结构 | 作用 |
|---|---|
| `ZSet` | 保存任务到期时间 |
| `List` | 保存任务内容 |
| `Pub/Sub` | 通知调度器重新计算最近任务时间 |

这里要特别注意：

> 在 Redisson 延迟队列中，Pub/Sub 不是用来保存任务的，而是用来通知调度器的。

例如当前最近任务是：

```text
10 分钟后执行
```

Redisson 内部调度器会准备 10 分钟后执行任务转移。

但是此时又新增了一个任务：

```text
1 分钟后执行
```

如果不通知调度器，调度器可能还会等 10 分钟，这样新任务就会延迟执行。

所以 Redisson 会通过 Pub/Sub 发布通知：

```text
来了一个更早到期的任务
```

调度器收到通知后，重新计算下一次执行时间。

流程如下：

```text
新增延迟任务
        ↓
判断是否比当前最早任务更早
        ↓
如果更早，PUBLISH 通知
        ↓
Redisson 调度器收到消息
        ↓
重新安排定时任务
```

---

## 十八、为什么 Redisson 可以用 Pub/Sub

因为 Redisson 延迟队列中，Pub/Sub 只承担通知作用。

即使这条 Pub/Sub 消息丢了，任务本身还在：

```text
ZSet + List
```

里面。

所以 Pub/Sub 丢失不会直接导致任务数据丢失，只可能导致调度器没有及时重新计算最近执行时间。

真正保存任务的是：

```text
ZSet 保存到期时间
List 保存任务内容
```

Pub/Sub 只是优化调度效率。

---

## 十九、Pub/Sub 的核心问题

Redis Pub/Sub 最大的问题是：

> 消息不可靠。

具体表现为：

```text
订阅者不在线，消息丢失
网络异常，消息可能丢失
没有 ACK，处理失败 Redis 不知道
没有重试，失败不会自动补发
没有持久化，Redis 不保存历史消息
```

所以它适合做：

```text
通知
广播
实时事件
```

不适合做：

```text
可靠任务
核心业务消息
强一致链路
```

---

## 二十、总结

可以这样回答：

> Redis Pub/Sub 是 Redis 提供的发布订阅机制。发布者通过 `PUBLISH` 命令向某个 channel 发送消息，订阅者通过 `SUBSCRIBE` 订阅 channel，之后就可以实时收到该 channel 上发布的消息。
>
> 它的底层实现可以理解为 Redis Server 维护了一个 channel 到 client 列表的映射关系。当客户端订阅某个频道时，Redis 会把这个客户端加入该频道对应的订阅者列表。当有客户端向这个频道发布消息时，Redis 就找到这个频道下所有订阅者，把消息逐个写入这些客户端的输出缓冲区。
>
> Redis 还支持模式订阅，也就是 `PSUBSCRIBE`。模式订阅底层维护的是 pattern 到 client 的关系。发布消息时，Redis 除了查找普通频道订阅者，还会遍历模式订阅规则，判断当前 channel 是否匹配 pattern，如果匹配就推送给对应客户端。
>
> Pub/Sub 的特点是实时广播、实现简单、延迟低，但它不持久化消息，没有 ACK、没有重试、没有消费进度。如果订阅者不在线，消息就会丢失。因此它适合做实时通知和广播，不适合做可靠消息队列。
>
> 在 Redisson 延迟队列中，Pub/Sub 主要用于通知内部调度器重新计算最近任务到期时间，真正保存任务的是 ZSet 和 List。

---

## 二十一、一句话总结

> Redis Pub/Sub 的实现原理是 Redis Server 维护 `channel -> clients` 的订阅关系，发布消息时根据 channel 找到所有订阅客户端并逐个推送；它适合实时通知和广播，但由于不持久化、无 ACK、无重试，所以不能作为可靠消息队列使用。
