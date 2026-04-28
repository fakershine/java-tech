# 如何保证 Kafka 消息顺序性

Kafka 的顺序性核心原则是：

```text
Kafka 只能保证同一个 Partition 内消息有序，不能保证整个 Topic 全局有序。
```

所以要保证消息顺序，关键是：**让需要有序的消息进入同一个 Partition，并且按顺序消费处理。**

---

## 1. Kafka 顺序性的基础

Kafka Topic 由多个 Partition 组成。

```text
Topic
 ├── Partition 0：msg1 -> msg2 -> msg3
 ├── Partition 1：msg4 -> msg5 -> msg6
 └── Partition 2：msg7 -> msg8 -> msg9
```

Kafka 只能保证：

```text
同一个 Partition 内消息有序
不同 Partition 之间不保证顺序
```

---

## 2. 生产端如何保证顺序

### 2.1 相同业务 Key 发送到同一个 Partition

如果订单消息需要按订单维度有序，可以使用 `orderId` 作为消息 Key。

```java
ProducerRecord<String, String> record =
        new ProducerRecord<>("order-topic", orderId, message);
```

Kafka 默认会根据 Key 计算分区：

```text
partition = hash(key) % partition数量
```

这样相同 `orderId` 的消息会进入同一个 Partition。

例如：

```text
orderId = 1001 的消息都进入 Partition 1

创建订单 -> 支付订单 -> 发货订单 -> 完成订单
```

只要进入同一个 Partition，就可以保证这些消息在 Kafka 内部的存储顺序。

---

### 2.2 避免生产端重试导致乱序

生产者发送失败后如果开启重试，可能出现消息乱序。

例如：

```text
消息 A 发送失败，正在重试
消息 B 发送成功
消息 A 重试成功
```

最终顺序可能变成：

```text
B -> A
```

### 解决方式

严格顺序场景可以配置：

```properties
max.in.flight.requests.per.connection=1
```

含义：

```text
同一个连接上，最多只能有一个未确认请求
```

这样可以避免前一条消息失败重试时，后一条消息先发送成功。

如果开启幂等生产者：

```properties
enable.idempotence=true
acks=all
```

Kafka 可以通过 ProducerId 和 SequenceNumber 保证单分区内的幂等和顺序性。

---

## 3. Broker 端如何保证顺序

Kafka Broker 对同一个 Partition 内的消息是追加写入的。

```text
Partition Log:
offset 0 -> offset 1 -> offset 2 -> offset 3
```

每条消息都会有递增的 offset。

```text
offset 越小，消息越早写入
消费者按照 offset 顺序消费
```

所以 Broker 层天然保证：

```text
同一个 Partition 内消息按 offset 顺序存储
```

---

## 4. 消费端如何保证顺序

### 4.1 同一个 Partition 只能被一个消费者消费

在同一个 Consumer Group 中：

```text
一个 Partition 同一时刻只能分配给一个 Consumer
```

所以只要消息在同一个 Partition 中，同一个消费者会按照 offset 顺序拉取。

---

### 4.2 消费端不要多线程乱序处理

即使 Kafka 拉取消息是有序的，如果消费者内部使用多线程并发处理，也可能导致乱序。

错误示例：

```text
Consumer 拉取消息：A -> B -> C

线程池处理：
B 先处理完成
C 第二个完成
A 最后完成
```

最终业务执行顺序变成：

```text
B -> C -> A
```

### 解决方式

#### 方式一：单线程消费单个 Partition

最简单可靠：

```text
一个 Partition 对应一个消费线程
按 offset 顺序处理
处理成功后再提交 offset
```

#### 方式二：按业务 Key 分发到固定线程

如果想提高并发，可以按业务 Key 分发到固定线程。

```text
orderId = 1001 -> thread-1
orderId = 1002 -> thread-2
orderId = 1003 -> thread-3
```

保证同一个业务 Key 的消息始终由同一个线程顺序处理。

---

## 5. 顺序消费的常见方案

### 方案一：全局顺序

如果要求整个 Topic 全局有序：

```text
Topic 只能设置 1 个 Partition
消费者单线程消费
```

### 优势

- 顺序性最强。
- 实现简单。

### 劣势

- 吞吐量低。
- 无法利用多分区并发能力。
- 不适合高并发场景。

---

### 方案二：局部顺序

实际项目中更常见的是局部顺序。

例如：

```text
同一个订单的消息有序
同一个用户的消息有序
同一个账户的消息有序
```

做法：

```text
使用 orderId / userId / accountId 作为消息 Key
保证相同 Key 进入同一个 Partition
```

### 优势

- 既能保证业务维度顺序。
- 又能利用多个 Partition 提高吞吐量。

### 劣势

- 只能保证同一个 Key 内有序。
- 不同 Key 之间不保证顺序。

---

## 6. 顺序性和消费失败

如果某条消息消费失败，不能直接跳过，否则会破坏顺序。

例如：

```text
消息 A -> 消息 B -> 消息 C
```

如果 A 失败，直接处理 B、C，会导致业务顺序错乱。

### 解决方式

- 当前消息失败后暂停该分区消费。
- 重试当前消息。
- 超过重试次数后进入死信队列。
- 人工处理或补偿后再继续消费。
- 消费端做好幂等，避免重复处理。

---

## 7. 常见配置

### Producer 配置

```properties
acks=all
enable.idempotence=true
retries=2147483647
max.in.flight.requests.per.connection=1
```

严格顺序场景建议：

```properties
max.in.flight.requests.per.connection=1
```

---

### Consumer 配置

```properties
enable.auto.commit=false
```

消费成功后手动提交 offset：

```text
拉取消息
  ↓
按顺序处理业务
  ↓
处理成功
  ↓
提交 offset
```

---

## 8. 常见问题总结

| 问题 | 原因 | 解决方案 |
|---|---|---|
| 多分区导致乱序 | 不同分区之间无顺序保证 | 相同业务 Key 发到同一分区 |
| Producer 重试乱序 | 后发消息先成功 | 开启幂等，必要时设置 `max.in.flight=1` |
| Consumer 多线程乱序 | 后面的消息先处理完成 | 单分区单线程，或按 Key 分发到固定线程 |
| 消费失败导致乱序 | 跳过失败消息继续处理 | 当前分区暂停、重试、死信队列 |
| 全局顺序难保证 | 多分区天然并发 | Topic 设置一个 Partition |

---

## 9. 总结

Kafka 只能保证同一个 Partition 内消息有序，不能保证整个 Topic 全局有序。

如果要保证全局顺序，只能让 Topic 只有一个 Partition，并且消费者单线程消费，但这样吞吐量较低。

实际项目中通常保证局部顺序，例如同一个订单、同一个用户、同一个账户的消息有序。做法是在生产消息时使用业务 ID 作为 Key，让相同 Key 的消息进入同一个 Partition；消费端保证同一个 Partition 内消息按 offset 顺序处理，不要用多线程乱序处理。

如果消费端需要并发，可以按业务 Key 分发到固定线程，保证同一个 Key 的消息始终由同一个线程顺序处理。

一句话总结：

```text
Kafka 顺序性 = 相同业务 Key 进入同一个 Partition + Partition 内按 offset 顺序消费 + 消费端避免多线程乱序处理。
```
