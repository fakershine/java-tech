# Kafka 如何保证消息不丢失

Kafka 要保证消息不丢失，需要从三个阶段考虑：

```text
生产者发送消息
  ↓
Broker 存储消息
  ↓
消费者消费消息
```

任何一个环节配置不当，都可能导致消息丢失。

---

## 1. 生产者如何保证不丢消息

### 1.1 设置 acks = all

生产者发送消息时，需要等待 Broker 确认。

```properties
acks=all
```

含义：

```text
Leader 副本写入成功
并且 ISR 中的副本也同步成功
生产者才认为消息发送成功
```

常见配置对比：

| 配置 | 含义 | 是否容易丢消息 |
|---|---|---|
| `acks=0` | 不等待 Broker 确认 | 容易丢 |
| `acks=1` | Leader 写入成功就返回 | Leader 宕机可能丢 |
| `acks=all` | ISR 副本同步成功才返回 | 最安全 |

---

### 1.2 开启重试机制

```properties
retries=3
```

或者设置更大的重试次数：

```properties
retries=Integer.MAX_VALUE
```

当网络异常、Broker 短暂不可用时，Producer 可以自动重试发送。

---

### 1.3 开启幂等生产者

```properties
enable.idempotence=true
```

幂等生产者可以避免 Producer 重试时产生重复消息。

它通过：

```text
ProducerId + SequenceNumber
```

保证同一个 Producer 对同一个分区的消息不会重复写入。

### 作用

```text
不丢消息 + 尽量不重复消息
```

---

### 1.4 设置合理的发送超时时间

```properties
delivery.timeout.ms=120000
request.timeout.ms=30000
```

避免网络抖动时消息还没成功发送就被过早判定失败。

---

### 1.5 使用发送回调确认结果

发送消息时不要只调用 `send()` 就不管了，应该监听发送结果。

```java
producer.send(record, (metadata, exception) -> {
    if (exception != null) {
        // 记录日志、告警、重试或落库补偿
    }
});
```

如果发送失败，需要有补偿机制，例如：

```text
记录失败日志
本地消息表
定时任务重试
告警人工处理
```

---

## 2. Broker 如何保证不丢消息

### 2.1 设置副本数 replication.factor

Kafka 通过副本机制保证消息可靠性。

```properties
replication.factor=3
```

含义：

```text
每个分区有 3 个副本
1 个 Leader
2 个 Follower
```

如果 Leader 宕机，可以从 Follower 中选出新的 Leader。

---

### 2.2 设置 min.insync.replicas

```properties
min.insync.replicas=2
```

含义：

```text
至少有 2 个 ISR 副本写入成功
消息才算写入成功
```

通常搭配：

```properties
acks=all
min.insync.replicas=2
replication.factor=3
```

这样即使一个副本宕机，也能保证消息不会只写入单个副本。

---

### 2.3 禁止不干净 Leader 选举

```properties
unclean.leader.election.enable=false
```

如果允许不干净选举，落后太多的副本也可能成为 Leader，导致已经写入 Leader 但未同步到该副本的数据丢失。

所以生产环境建议关闭。

---

### 2.4 保证磁盘可靠性

Kafka 消息最终存储在磁盘日志文件中。

需要注意：

- Broker 磁盘不能满。
- 磁盘损坏要及时告警。
- 合理配置日志保留时间。
- Broker 集群要多副本部署。

---

## 3. 消费者如何保证不丢消息

### 3.1 关闭自动提交 offset

默认自动提交 offset 可能导致消息丢失。

例如：

```text
消费者拉取消息
  ↓
自动提交 offset
  ↓
业务处理失败或服务宕机
  ↓
消息不会再被消费
  ↓
消息丢失
```

推荐关闭自动提交：

```properties
enable.auto.commit=false
```

---

### 3.2 业务处理成功后再手动提交 offset

正确流程：

```text
拉取消息
  ↓
执行业务逻辑
  ↓
业务处理成功
  ↓
手动提交 offset
```

示例：

```java
consumer.poll();

try {
    // 处理业务
    process(message);

    // 业务成功后提交 offset
    consumer.commitSync();
} catch (Exception e) {
    // 不提交 offset，等待下次重新消费
}
```

这样即使消费过程中宕机，offset 没有提交，消息还会被重新消费。

---

### 3.3 消费端必须保证幂等

手动提交 offset 可以避免消息丢失，但可能导致消息重复消费。

例如：

```text
业务处理成功
  ↓
提交 offset 前宕机
  ↓
重启后消息再次消费
```

所以消费者必须保证幂等。

常见方式：

- 使用业务唯一键去重。
- 使用数据库唯一索引。
- 使用 Redis `SETNX` 去重。
- 使用消费记录表。
- 使用状态机控制重复处理。

---

## 4. 推荐生产配置

### Producer 配置

```properties
acks=all
enable.idempotence=true
retries=2147483647
max.in.flight.requests.per.connection=5
delivery.timeout.ms=120000
request.timeout.ms=30000
```

---

### Broker / Topic 配置

```properties
replication.factor=3
min.insync.replicas=2
unclean.leader.election.enable=false
```

---

### Consumer 配置

```properties
enable.auto.commit=false
```

消费逻辑：

```text
业务处理成功后，再手动提交 offset
```

---

## 5. Kafka 消息丢失常见场景

| 环节 | 丢失原因 | 解决方案 |
|---|---|---|
| Producer | `acks=0`，发送后不确认 | 设置 `acks=all` |
| Producer | 网络异常发送失败 | 开启 `retries` |
| Producer | 重试导致重复 | 开启幂等生产者 |
| Broker | 副本数太少 | 设置 `replication.factor=3` |
| Broker | ISR 副本不足 | 设置 `min.insync.replicas=2` |
| Broker | 不干净 Leader 选举 | 关闭 `unclean.leader.election` |
| Consumer | 先提交 offset，后处理业务 | 关闭自动提交，处理成功后手动提交 |
| Consumer | 消费成功但提交 offset 失败 | 消费端做幂等 |

---

## 6. 总结

Kafka 要保证消息不丢失，需要从生产者、Broker 和消费者三个方面考虑。

生产者端要设置 `acks=all`，确保消息写入 Leader 和 ISR 副本后才算成功；同时开启重试机制和幂等生产者，避免网络异常导致消息丢失或重复。

Broker 端要设置合理的副本数，例如 `replication.factor=3`，并设置 `min.insync.replicas=2`，保证消息至少写入多个同步副本。同时要关闭不干净 Leader 选举，避免落后副本成为 Leader 导致数据丢失。

消费者端要关闭自动提交 offset，改为业务处理成功后手动提交 offset。这样即使消费者处理过程中宕机，消息也可以重新消费。但这种方式可能导致重复消费，所以消费端必须做好幂等处理。

一句话总结：

```text
Kafka 防止消息丢失 = Producer 设置 acks=all + retries + 幂等，
Broker 设置多副本 + ISR + 禁止不干净选举，
Consumer 关闭自动提交 offset，业务成功后手动提交，并保证消费幂等。
```
