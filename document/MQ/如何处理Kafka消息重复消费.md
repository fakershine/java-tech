# Kafka 如何处理消息重复消费

Kafka 消息重复消费的核心解决思路是：**Kafka 不能完全避免重复消费，业务端必须保证消费幂等**。

---

## 1. 为什么会重复消费

Kafka 默认语义是：

```text
至少消费一次 At Least Once
```

也就是说，Kafka 更倾向于保证消息不丢，但可能会重复。

常见重复消费场景：

- 消费者处理完业务后，还没提交 offset 就宕机。
- 消费者处理成功，但提交 offset 失败。
- 消费者自动提交 offset 和业务处理时机不一致。
- 消费过程中发生重平衡。
- 消费者处理时间过长，超过 `max.poll.interval.ms`。
- 生产者重试导致消息重复发送。
- MQ 事务或网络异常导致重复投递。

---

## 2. 重复消费示例

```text
消费者拉取消息 A
  ↓
业务处理成功
  ↓
还没提交 offset
  ↓
消费者宕机
  ↓
重启后从旧 offset 继续消费
  ↓
消息 A 被再次消费
```

所以消费者必须支持：

```text
同一条消息消费多次，业务结果仍然一致
```

---

## 3. 关闭自动提交 offset

自动提交 offset 可能导致 offset 提交和业务处理不一致。

不推荐：

```properties
enable.auto.commit=true
```

推荐：

```properties
enable.auto.commit=false
```

手动提交 offset：

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
try {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

    for (ConsumerRecord<String, String> record : records) {
        process(record);
    }

    consumer.commitSync();
} catch (Exception e) {
    // 业务失败不提交 offset，等待下次重试
}
```

---

## 4. 消费端幂等处理

### 核心思想

给每条消息设计一个唯一标识，例如：

```text
message_id
order_id
request_id
biz_no
event_id
```

消费前先判断是否处理过。

---

## 5. 基于数据库唯一索引去重

### 实现原理

创建消息消费记录表，对 `message_id` 建唯一索引。

```sql
CREATE TABLE mq_consume_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(128) NOT NULL,
    consumer_group VARCHAR(128) NOT NULL,
    status VARCHAR(32),
    create_time DATETIME,
    UNIQUE KEY uk_msg_group(message_id, consumer_group)
);
```

消费流程：

```text
1. 消费消息前，先插入 message_id。
2. 插入成功，说明第一次消费。
3. 插入失败，说明已经消费过，直接跳过。
4. 业务处理成功后，更新消费状态。
```

### 优势

- 可靠性高。
- 可追踪消费状态。
- 适合重要业务。

### 劣势

- 多一次数据库操作。
- 高并发下数据库压力较大。
- 消费记录表需要定期清理。

### 适用场景

- 支付回调。
- 订单状态变更。
- 账户流水。
- 重要业务消息。

---

## 6. 基于 Redis SETNX 去重

### 实现原理

使用 Redis 的 `SET NX` 判断消息是否处理过。

```bash
SET mq:consume:messageId 1 NX EX 86400
```

如果返回成功，说明第一次消费；如果失败，说明已经消费过。

消费流程：

```text
1. 根据 message_id 生成去重 key。
2. 使用 SET NX 写入 Redis。
3. 写入成功，执行业务。
4. 写入失败，直接跳过。
```

### 优势

- 性能高。
- 实现简单。
- 适合高并发场景。
- 可以设置过期时间，避免无限增长。

### 劣势

- Redis 异常会影响判断。
- 如果先写 Redis 后业务失败，可能导致消息无法重试。
- 不如数据库唯一索引可靠。

### 适用场景

- 高并发非核心消息。
- 短期去重。
- 日志、通知、状态同步类消息。

---

## 7. 基于业务状态机幂等

### 实现原理

通过业务状态判断是否允许重复处理。

例如订单状态：

```text
待支付 -> 已支付 -> 已发货 -> 已完成
```

支付成功消息只能将订单从 `待支付` 更新为 `已支付`：

```sql
UPDATE t_order
SET status = 'PAID'
WHERE order_no = '1001'
AND status = 'WAIT_PAY';
```

如果重复消费，订单已经是 `PAID`，更新影响行数为 0，不会重复处理。

### 优势

- 符合业务语义。
- 不一定需要额外去重表。
- 可以防止重复处理和非法状态流转。

### 劣势

- 依赖状态机设计。
- 状态流转必须清晰。
- 不适合无状态类消息。

### 适用场景

- 订单支付。
- 订单取消。
- 发货确认。
- 退款状态变更。
- 审批流转。

---

## 8. 基于乐观锁幂等

### 实现原理

通过版本号或条件更新控制重复处理。

```sql
UPDATE account
SET balance = balance - 100,
    version = version + 1
WHERE account_id = 1
AND version = 10;
```

或者：

```sql
UPDATE stock
SET count = count - 1
WHERE product_id = 1001
AND count > 0;
```

只有符合条件时才更新成功。

### 优势

- 不阻塞。
- 适合并发更新。
- 可以防止重复扣减。

### 劣势

- 需要设计版本号或业务条件。
- 高并发下可能频繁失败重试。
- 不适合所有业务。

### 适用场景

- 库存扣减。
- 账户余额更新。
- 业务状态更新。

---

## 9. 基于业务唯一键幂等

### 实现原理

业务数据本身有唯一键，例如：

```text
order_no
payment_no
refund_no
transaction_id
```

插入业务数据时，依赖唯一索引防重复。

```sql
CREATE UNIQUE INDEX uk_payment_no ON payment(payment_no);
```

重复消费时，重复插入会失败，避免重复创建数据。

### 优势

- 简单可靠。
- 和业务强绑定。
- 不需要额外消息表。

### 劣势

- 只适合创建类业务。
- 需要提前设计业务唯一键。

### 适用场景

- 创建支付单。
- 创建退款单。
- 创建流水记录。
- 创建订单记录。

---

## 10. offset 提交策略

### 推荐做法

```text
业务处理成功后，再提交 offset
```

这样可以避免消息丢失。

### 代价

如果业务成功但 offset 提交失败，会重复消费。

所以：

```text
手动提交 offset + 消费端幂等
```

是 Kafka 消费端最常见方案。

---

## 11. 重平衡时如何减少重复消费

Kafka 重平衡时，分区会被撤销并重新分配。

可以使用 `ConsumerRebalanceListener` 在分区撤销前提交 offset。

```java
consumer.subscribe(topics, new ConsumerRebalanceListener() {

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        consumer.commitSync();
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        // 重新分配后恢复状态
    }
});
```

作用：

```text
分区被撤销前提交已处理 offset
减少重复消费范围
```

---

## 12. 生产端也要避免重复发送

如果 Producer 发送失败后重试，也可能产生重复消息。

推荐配置：

```properties
acks=all
enable.idempotence=true
retries=2147483647
```

说明：

- `acks=all`：保证消息可靠写入。
- `retries`：发送失败自动重试。
- `enable.idempotence=true`：避免生产者重试导致同一分区内重复写入。

---

## 13. 常见方案对比

| 方案 | 核心思想 | 优势 | 劣势 | 适用场景 |
|---|---|---|---|---|
| 手动提交 offset | 业务成功后提交 | 避免消息丢失 | 可能重复消费 | 所有消费场景 |
| 数据库唯一索引 | message_id 去重 | 可靠、可追踪 | DB 压力较大 | 重要业务 |
| Redis SETNX | 缓存去重 key | 性能高 | 可靠性弱于 DB | 高并发短期去重 |
| 状态机 | 判断状态是否可流转 | 符合业务语义 | 依赖状态设计 | 订单、支付、退款 |
| 乐观锁 | version / 条件更新 | 无阻塞 | 需重试处理 | 库存、余额 |
| 业务唯一键 | 唯一业务单号 | 简单可靠 | 适合创建类业务 | 订单、流水 |

---

## 14. 总结

Kafka 出现重复消费是正常现象，因为 Kafka 通常采用至少一次消费语义。常见原因是消费者处理完业务后还没提交 offset 就宕机，或者提交 offset 失败，或者发生消费者重平衡。

解决重复消费的核心不是完全避免重复，而是在消费端做好幂等。一般做法是关闭自动提交 offset，改为业务处理成功后手动提交 offset，同时使用业务唯一键、数据库唯一索引、Redis `SETNX`、消费记录表、状态机或乐观锁来保证重复消费不会产生重复结果。

如果是重要业务，推荐使用数据库唯一索引或消费记录表；如果是高并发短期去重，可以使用 Redis `SETNX`；如果是订单、支付、退款等有状态流转的业务，推荐结合状态机实现幂等。

一句话总结：

```text
Kafka 重复消费不可完全避免，最佳实践是：手动提交 offset + 消费端幂等 + 业务唯一键/去重表/状态机。
```
