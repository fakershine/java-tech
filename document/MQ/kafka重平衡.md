# Kafka 重平衡总结

Kafka 重平衡（Rebalance）是指：**消费者组内消费者数量、订阅关系或 Topic 分区发生变化时，Kafka 重新分配分区给消费者的过程**。

---

## 1. 什么是重平衡

Kafka 中，一个 Topic 可以有多个分区，一个消费者组可以有多个消费者。

规则：

```text
同一个消费者组内，一个分区同一时刻只能被一个消费者消费。
```

例如：

```text
Topic：P0、P1、P2、P3

消费者组：
C1 -> P0、P1
C2 -> P2、P3
```

如果新增消费者 C3，Kafka 会重新分配分区，这个过程就是重平衡。

---

## 2. 触发重平衡的场景

常见触发场景：

- 新消费者加入消费者组。
- 消费者退出或宕机。
- 消费者心跳超时。
- 消费者长时间没有调用 `poll()`。
- Topic 分区数量发生变化。
- 消费者订阅的 Topic 发生变化。
- 分区分配策略发生变化。

---

## 3. 重平衡流程

```text
消费者加入消费者组
  ↓
Group Coordinator 选出 Leader Consumer
  ↓
Leader Consumer 根据分配策略生成分区分配方案
  ↓
通过 SyncGroup 同步分配结果
  ↓
消费者按照新分配结果开始消费
```

简单理解：

```text
成员变化 -> 暂停消费 -> 重新分配分区 -> 恢复消费
```

---

## 4. 常见分区分配策略

| 策略 | 说明 |
|---|---|
| RangeAssignor | 按 Topic 维度连续分配分区 |
| RoundRobinAssignor | 所有分区轮询分配给消费者 |
| StickyAssignor | 尽量保持原有分配，减少分区迁移 |
| CooperativeStickyAssignor | 增量协作式重平衡，减少暂停时间 |

---

## 5. Eager Rebalance 和 Cooperative Rebalance

### Eager Rebalance

早期默认方式。

特点：

```text
所有消费者暂停消费
所有分区全部撤销
重新统一分配
```

缺点：

- 停顿时间长。
- 分区迁移多。
- 容易导致消费延迟升高。

---

### Cooperative Rebalance

增量协作式重平衡。

特点：

```text
只撤销需要迁移的分区
未变化的分区继续消费
逐步完成分区调整
```

优势：

- 减少消费暂停。
- 减少分区迁移。
- 对大消费者组更友好。

---

## 6. 重平衡带来的问题

- 消费暂停，导致消费延迟升高。
- 可能产生重复消费。
- 本地缓存或状态需要重建。
- 频繁重平衡会导致消息积压。
- 影响消费者组稳定性。

---

## 7. 如何减少重平衡

### 1. 合理配置心跳参数

```properties
heartbeat.interval.ms=3000
session.timeout.ms=10000
```

说明：

- `heartbeat.interval.ms`：消费者发送心跳的间隔。
- `session.timeout.ms`：Coordinator 判断消费者失效的时间。

---

### 2. 避免处理时间超过 poll 间隔

```properties
max.poll.interval.ms=300000
max.poll.records=500
```

如果单批消息处理太久，超过 `max.poll.interval.ms`，会触发重平衡。

优化方式：

- 减少 `max.poll.records`。
- 优化业务处理耗时。
- 适当调大 `max.poll.interval.ms`。
- 耗时任务异步处理。

---

### 3. 使用静态成员

```properties
group.instance.id=consumer-1
```

作用：

- 给消费者固定身份。
- 短暂重启时减少不必要的重平衡。

---

### 4. 使用 CooperativeStickyAssignor

```properties
partition.assignment.strategy=org.apache.kafka.clients.consumer.CooperativeStickyAssignor
```

作用：

- 减少分区迁移。
- 降低重平衡期间的消费暂停。

---

### 5. 优雅关闭消费者

关闭前主动提交 offset，并调用：

```java
consumer.close();
```

避免消费者异常退出导致等待超时。

---

## 8. ConsumerRebalanceListener

Kafka 提供 `ConsumerRebalanceListener` 监听分区变化。

常用方法：

```java
onPartitionsRevoked(Collection<TopicPartition> partitions)

onPartitionsAssigned(Collection<TopicPartition> partitions)
```

常见用途：

- 分区撤销前提交 offset。
- 分区撤销前保存本地状态。
- 分区重新分配后初始化资源。
- 恢复消费进度。

示例：

```java
consumer.subscribe(topics, new ConsumerRebalanceListener() {

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        consumer.commitSync();
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        // 初始化分区状态
    }
});
```

---

## 9. 总结

Kafka 重平衡是消费者组内消费者和分区重新分配的过程，通常在消费者加入、退出、宕机、心跳超时、长时间不调用 `poll()`、Topic 分区变化时触发。

重平衡会导致消费暂停、重复消费、消费延迟升高和本地状态重建。优化方向主要是减少重平衡触发次数和降低重平衡影响。

常见优化方式包括：合理配置 `heartbeat.interval.ms`、`session.timeout.ms`、`max.poll.interval.ms`，避免单批消息处理过久；使用静态成员 `group.instance.id`；使用 `CooperativeStickyAssignor`；在 `ConsumerRebalanceListener` 中提交 offset 和保存状态。

一句话总结：

```text
Kafka 重平衡 = 消费者组成员或分区变化后重新分配分区；
优化核心 = 减少触发次数、减少暂停时间、减少分区迁移、保证消费幂等。
```
