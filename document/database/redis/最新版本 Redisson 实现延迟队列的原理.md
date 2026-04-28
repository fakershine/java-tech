# 最新版本 Redisson 实现延迟队列的原理

> 版本口径：截至 2026-04-28，Redisson GitHub Releases 中标记的最新版本为 `redisson-4.3.1`。在最新版本中，传统的 `RDelayedQueue` 已经被标记为 `@Deprecated`，官方源码注释提示：应改用带 delay 能力的 `RReliableQueue`。

---

## 目录

- [一、最新版本中的关键变化](#一最新版本中的关键变化)
- [二、Redisson 延迟队列有两条实现路线](#二redisson-延迟队列有两条实现路线)
- [三、旧版 RDelayedQueue 的实现原理](#三旧版-rdelayedqueue-的实现原理)
- [四、RDelayedQueue 底层 Redis Key 结构](#四rdelayedqueue-底层-redis-key-结构)
- [五、RDelayedQueue 添加延迟任务的流程](#五rdelayedqueue-添加延迟任务的流程)
- [六、RDelayedQueue 到期任务转移流程](#六rdelayedqueue-到期任务转移流程)
- [七、QueueTransferTask 的调度机制](#七queuetransfertask-的调度机制)
- [八、RDelayedQueue 为什么需要 Pub/Sub](#八rdelayedqueue-为什么需要-pubsub)
- [九、RDelayedQueue 的优缺点](#九rdelayedqueue-的优缺点)
- [十、最新版推荐方案：RReliableQueue Delay](#十最新版推荐方案rreliablequeue-delay)
- [十一、RReliableQueue 延迟消息的使用方式](#十一rreliablequeue-延迟消息的使用方式)
- [十二、RReliableQueue 延迟消息的实现原理](#十二rreliablequeue-延迟消息的实现原理)
- [十三、RReliableQueue 的可靠性机制](#十三rreliablequeue-的可靠性机制)
- [十四、RDelayedQueue 与 RReliableQueue 对比](#十四rdelayedqueue-与-rreliablequeue-对比)
- [十五、考试自动交卷场景如何设计](#十五考试自动交卷场景如何设计)
- [十六、从 RDelayedQueue 迁移到 RReliableQueue 的注意事项](#十六从-rdelayedqueue-迁移到-rreliablequeue-的注意事项)
- [十七、面试回答版本](#十七面试回答版本)
- [十八、一句话总结](#十八一句话总结)
- [参考资料](#参考资料)

---

## 一、最新版本中的关键变化

在旧版本中，Redisson 常用的延迟队列实现是：

```java
RBlockingQueue<T> blockingQueue = redissonClient.getBlockingQueue("queue:name");
RDelayedQueue<T> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
```

但是在最新版本中，`RDelayedQueue` 已经被标记为废弃：

```java
@Deprecated
public interface RDelayedQueue<V> extends RQueue<V>, RDestroyable
```

官方源码注释中也明确提示：

```text
Instead, use the RReliableQueue object with delay feature.
```

也就是说，最新版 Redisson 的推荐方向是：

> 不再优先使用传统 `RDelayedQueue`，而是使用带 delay 能力的 `RReliableQueue`。

不过需要注意：

> Redisson 官方文档中说明 `Reliable Queue` 属于 Redisson PRO 能力。如果项目使用的是社区版，需要确认当前依赖版本和授权能力是否支持 `RReliableQueue`。

---

## 二、Redisson 延迟队列有两条实现路线

最新版 Redisson 中，延迟任务可以从两个角度理解：

| 实现路线 | 状态 | 核心特点 | 是否推荐 |
|---|---|---|---|
| `RDelayedQueue + RBlockingQueue` | 已废弃，但仍可看到源码 | 基于 `ZSet + List + Pub/Sub + QueueTransferTask`，到期后转移到目标队列 | 不建议新项目继续使用 |
| `RReliableQueue` 的 delay 功能 | 最新推荐方案 | 支持 delay、ACK、visibility timeout、delivery limit、DLQ、去重等可靠队列能力 | 推荐新方案，但需确认版本/授权 |

简单理解：

```text
旧方案：
RDelayedQueue 负责延迟
RBlockingQueue 负责消费

新方案：
RReliableQueue 自身支持延迟、消费确认、失败重投、死信等能力
```

---

## 三、旧版 RDelayedQueue 的实现原理

虽然 `RDelayedQueue` 已经被废弃，但很多项目仍在使用它，因此理解它的实现原理仍然很重要。

`RDelayedQueue` 的核心思想是：

> 任务先进入延迟队列，到期后由 Redisson 内部调度器转移到真正的目标队列，消费者再从目标队列消费。

整体流程：

```text
生产者
  ↓
RDelayedQueue.offer(task, delay)
  ↓
任务进入延迟队列
  ↓
到期后 Redisson 内部转移任务
  ↓
RBlockingQueue
  ↓
消费者 take / poll 消费
```

消费者不是直接消费 `RDelayedQueue`，而是消费它关联的目标队列：

```java
RBlockingQueue<ExamSubmitTask> blockingQueue =
        redissonClient.getBlockingQueue("exam:submit:queue");

RDelayedQueue<ExamSubmitTask> delayedQueue =
        redissonClient.getDelayedQueue(blockingQueue);

delayedQueue.offer(task, 30, TimeUnit.MINUTES);

ExamSubmitTask task = blockingQueue.take();
```

---

## 四、RDelayedQueue 底层 Redis Key 结构

以业务队列名为：

```text
exam:submit:queue
```

Redisson 内部大致维护以下结构：

| Redis Key | 数据结构 | 作用 |
|---|---|---|
| `exam:submit:queue` | `List` | 目标队列，消费者真正消费的队列 |
| `redisson_delay_queue:{exam:submit:queue}` | `List` | 延迟队列，保存未到期任务内容 |
| `redisson_delay_queue_timeout:{exam:submit:queue}` | `ZSet` | 记录任务到期时间，score 是到期时间戳 |
| `redisson_delay_queue_channel:{exam:submit:queue}` | `Pub/Sub Channel` | 通知调度器重新计算最近任务到期时间 |

所以传统 Redisson 延迟队列可以总结为：

```text
RDelayedQueue =
    Timeout ZSet
  + Delay List
  + Pub/Sub Channel
  + QueueTransferTask
  + Destination Queue
```

---

## 五、RDelayedQueue 添加延迟任务的流程

当执行：

```java
delayedQueue.offer(task, delay, timeUnit);
```

Redisson 内部大致执行以下步骤：

```text
1. 校验 delay 不能小于 0
2. 计算任务到期时间：timeout = 当前时间 + delay
3. 生成 randomId
4. 序列化业务对象 value
5. 将 randomId + value 打包成唯一任务内容
6. 写入 timeout ZSet，score = timeout
7. 写入 delay List
8. 如果当前任务是最早到期任务，则 publish 通知调度器
```

简化后的 Redis 操作可以理解为：

```text
ZADD redisson_delay_queue_timeout:{queueName} timeout packedValue
RPUSH redisson_delay_queue:{queueName} packedValue

如果 packedValue 是 ZSet 中最早到期的元素：
    PUBLISH redisson_delay_queue_channel:{queueName} timeout
```

### 为什么要生成 randomId？

因为业务任务内容可能重复。

例如两个自动交卷任务内容完全一样：

```json
{"examId":1001,"userId":2001,"recordId":3001}
{"examId":1001,"userId":2001,"recordId":3001}
```

如果只存业务 value，后续 `LREM` 或 `ZREM` 时可能误删重复元素。

所以 Redisson 会把：

```text
randomId + encodedValue
```

打包成唯一值。

这样即使业务内容相同，也能作为两条独立任务存在。

---

## 六、RDelayedQueue 到期任务转移流程

Redisson 内部有一个 `QueueTransferTask`，负责把到期任务从延迟队列转移到目标队列。

到期转移的大致逻辑：

```text
1. 从 timeout ZSet 中查询 score <= 当前时间的任务
2. 每次最多取一批，比如 100 条
3. 解包 randomId + value
4. 将 value push 到目标队列
5. 从 delay List 中移除 packedValue
6. 从 timeout ZSet 中删除 packedValue
7. 返回下一个最早到期任务的时间戳
```

简化后的 Redis 操作：

```text
ZRANGEBYSCORE timeoutSet 0 now LIMIT 0 100
        ↓
RPUSH destinationQueue value
        ↓
LREM delayQueue 1 packedValue
        ↓
ZREM timeoutSet packedValue
        ↓
ZRANGE timeoutSet 0 0 WITHSCORES
```

这些操作通过 Lua 脚本执行，可以保证同一段转移逻辑在 Redis 侧具备原子性。

---

## 七、QueueTransferTask 的调度机制

`QueueTransferTask` 不是固定每秒全量扫描 Redis。

它的调度方式更接近：

> 根据最近一条任务的到期时间来设置下一次执行时间。

流程如下：

```text
启动 QueueTransferTask
        ↓
执行 pushTaskAsync()
        ↓
转移已经到期的任务
        ↓
查询下一个最早到期任务时间
        ↓
如果还有任务，则根据该时间注册本地定时器
        ↓
到时间后再次执行 pushTaskAsync()
```

如果转移失败，Redisson 会做兜底重试，例如延迟几秒后再次调度。

可以理解为：

```text
不是固定扫描
而是按最近到期时间动态调度
```

这比普通定时任务扫描更高效。

---

## 八、RDelayedQueue 为什么需要 Pub/Sub

假设当前最近的任务是：

```text
10 分钟后执行
```

那么 Redisson 调度器会计划 10 分钟后执行转移。

但是此时又新增了一个任务：

```text
1 分钟后执行
```

如果没有通知机制，调度器可能还会等 10 分钟，导致 1 分钟后的任务延迟执行。

因此，添加任务时，如果新任务成为当前最早到期任务，Redisson 会通过 Pub/Sub 发布通知：

```text
PUBLISH redisson_delay_queue_channel:{queueName} newTimeout
```

调度器收到通知后：

```text
取消原来的定时任务
重新按 newTimeout 安排下一次转移
```

因此 Pub/Sub 的作用是：

> 通知调度器重新计算最近任务到期时间，而不是保存任务数据。

真正保存任务的是：

```text
ZSet + List
```

---

## 九、RDelayedQueue 的优缺点

### 9.1 优点

| 优点 | 说明 |
|---|---|
| 使用简单 | 直接 `offer(task, delay)` 即可 |
| 支持分布式 | 任务存储在 Redis，不依赖单机 JVM 内存 |
| 延迟比定时扫描低 | 基于最近到期时间动态调度 |
| 封装完整 | 不需要自己写 ZSet 扫描、Lua、Pub/Sub |

---

### 9.2 缺点

| 缺点 | 说明 |
|---|---|
| 已废弃 | 最新版本推荐改用 `RReliableQueue` delay 能力 |
| 不是强可靠队列 | 普通 `RBlockingQueue` 没有完整 ACK、visibility timeout、DLQ |
| 依赖客户端调度 | 到期转移依赖 Redisson 客户端内部 `QueueTransferTask` |
| 依赖 Redis 可用性 | Redis 故障可能影响投递和消费 |
| 版本 Bug 风险 | 老版本曾出现阻塞队列内存泄漏等问题 |
| Codec 兼容风险 | 升级版本时要关注序列化兼容问题 |

---

## 十、最新版推荐方案：RReliableQueue Delay

最新版 Redisson 推荐使用：

```java
RReliableQueue<T>
```

并通过消息级别的 `delay` 参数实现延迟投递。

官方文档中，`RReliableQueue` 支持很多可靠消息能力：

| 能力 | 说明 |
|---|---|
| Message Delay | 消息延迟投递 |
| Message Visibility Timeout | 消费后在一段时间内对其他消费者不可见 |
| Delivery Limit | 最大投递次数 |
| ACK | 消费成功后确认 |
| Negative ACK | 消费失败或拒绝 |
| Dead Letter Queue | 失败次数达到上限后进入死信队列 |
| Deduplication | 基于 ID 或消息 hash 去重 |
| Priority | 消息优先级 |
| TTL | 消息过期时间 |
| Bulk Operations | 批量添加、消费、确认 |
| Atomic Operations | 队列操作原子执行 |

官方文档还说明，`RReliableQueue` 不是普通 Redis List 队列，而是基于更复杂的数据结构实现，其中包括 Stream。

因此，最新推荐方案和旧的 `RDelayedQueue` 最大差别是：

```text
RDelayedQueue：
只负责延迟转移，消费可靠性弱

RReliableQueue：
延迟 + 可靠消费 + ACK + 重投 + 死信 + 去重
```

---

## 十一、RReliableQueue 延迟消息的使用方式

添加延迟消息：

```java
RReliableQueue<ExamSubmitTask> queue =
        redissonClient.getReliableQueue("exam:submit:queue");

ExamSubmitTask task = new ExamSubmitTask();
task.setExamId(examId);
task.setUserId(userId);
task.setRecordId(recordId);

Message<ExamSubmitTask> message = queue.add(
        QueueAddArgs.messages(
                MessageArgs.payload(task)
                        .delay(Duration.ofMinutes(30))
        )
);
```

添加带更多可靠性参数的消息：

```java
Message<ExamSubmitTask> message = queue.add(
        QueueAddArgs.messages(
                MessageArgs.payload(task)
                        .delay(Duration.ofMinutes(30))
                        .deliveryLimit(5)
                        .timeToLive(Duration.ofHours(2))
                        .deduplicationById(
                                "exam-submit:" + recordId,
                                Duration.ofHours(3)
                        )
        )
);
```

消费消息的核心思路：

```java
Message<ExamSubmitTask> message = queue.poll();

if (message != null) {
    try {
        ExamSubmitTask task = message.getPayload();

        autoSubmitExam(task);

        queue.ack(message);
    } catch (Exception e) {
        queue.nack(message);
    }
}
```

> 具体方法名以项目实际 Redisson 版本 API 为准。核心思想是：消费成功后 ACK，失败后 NACK 或等待 visibility timeout 后重投。

---

## 十二、RReliableQueue 延迟消息的实现原理

`RReliableQueue` 的完整内部实现细节属于 Redisson 的可靠队列实现，官方文档描述的是架构级能力，而不是像 `RDelayedQueue` 那样可以完整从社区版源码中看到全部 Lua 逻辑。

从官方文档可以确定：

1. `RReliableQueue` 支持消息级别 delay；
2. delay 可以精确到毫秒；
3. 消息添加后会生成唯一消息 ID；
4. 消息包含 payload、headers 等信息；
5. 队列不是简单 Redis List，而是复杂数据结构，其中包括 Stream；
6. 支持 ACK、visibility timeout、delivery limit、DLQ、去重、优先级等；
7. 队列操作是原子执行的；
8. 官方说明它不依赖周期性后台任务。

可以从逻辑上这样理解 `RReliableQueue` 的延迟消息：

```text
生产者 add message with delay
        ↓
消息写入 Redis 可靠队列内部结构
        ↓
消息在 delay 时间内不可被消费者获取
        ↓
delay 到期后，消息变为可见状态
        ↓
消费者 poll 获取消息
        ↓
消息进入 visibility timeout，不再被其他消费者看到
        ↓
业务处理成功：ACK 删除或标记完成
        ↓
业务处理失败：NACK 或 visibility timeout 到期后重投
        ↓
投递次数超过 deliveryLimit：进入 DLQ 或删除
```

和旧版 `RDelayedQueue` 的关键区别：

```text
旧版 RDelayedQueue：
到期后把任务转移到另一个 RBlockingQueue

新版 RReliableQueue：
消息本身属于可靠队列，delay 到期后变为可消费状态
```

也就是说，新版思路更像一个可靠消息队列，而不是一个单独的“延迟转移器”。

---

## 十三、RReliableQueue 的可靠性机制

### 13.1 Visibility Timeout

消费者取出消息后，消息不会立刻永久删除，而是在一段时间内对其他消费者不可见。

```text
consumer A poll message
        ↓
message invisible to other consumers
        ↓
consumer A 处理业务
```

如果 consumer A 在规定时间内 ACK：

```text
处理成功
        ↓
ACK
        ↓
消息完成
```

如果 consumer A 宕机或没有 ACK：

```text
visibility timeout 到期
        ↓
消息重新变为可见
        ↓
其他消费者可以再次消费
```

这解决了旧 `RBlockingQueue.take()` 中“取出来后服务宕机导致消息丢失”的问题。

---

### 13.2 ACK / NACK

ACK 表示消息处理成功。

NACK 表示消息处理失败或拒绝。

```text
处理成功：ACK
处理失败：NACK
```

有了 ACK 机制，队列就能知道消息是否真正完成，而不是只要被消费者取走就算成功。

---

### 13.3 Delivery Limit

`deliveryLimit` 用于限制消息最多投递多少次。

例如：

```java
MessageArgs.payload(task).deliveryLimit(5)
```

表示消息最多投递 5 次。

如果超过投递上限，可以移动到死信队列，或者被删除。

---

### 13.4 Dead Letter Queue

如果消息多次消费失败，就可以进入 DLQ。

```text
消息处理失败
        ↓
重投
        ↓
多次失败
        ↓
进入 Dead Letter Queue
```

DLQ 适合后续排查或人工补偿。

---

### 13.5 Deduplication

`RReliableQueue` 支持基于业务 ID 或消息内容 hash 的去重。

例如考试自动交卷场景可以使用：

```text
exam-submit:{recordId}
```

作为去重 ID，避免同一场考试重复生成多个自动交卷任务。

---

## 十四、RDelayedQueue 与 RReliableQueue 对比

| 对比项 | RDelayedQueue | RReliableQueue Delay |
|---|---|---|
| 最新状态 | 已废弃 | 最新推荐方向 |
| 主要用途 | 延迟转移任务 | 可靠消息队列 |
| 底层结构 | ZSet + List + Pub/Sub + 目标队列 | 官方说明为复杂结构，包括 Stream |
| 消费队列 | 目标 `RBlockingQueue` | `RReliableQueue` 自身 |
| 是否需要任务转移 | 需要，到期后转移到目标队列 | 不强调外部转移，消息到期后变为可见 |
| ACK | 不支持完整 ACK | 支持 |
| NACK | 不支持 | 支持 |
| Visibility Timeout | 不支持 | 支持 |
| Delivery Limit | 不支持 | 支持 |
| DLQ | 不支持 | 支持 |
| Deduplication | 需要业务自己做 | 支持 |
| Priority | 不支持 | 支持 |
| 适合场景 | 轻量级延迟任务 | 可靠延迟消息 |
| 版本风险 | 老版本 Bug 较多，需要关注废弃状态 | 需要确认版本和授权 |

---

## 十五、考试自动交卷场景如何设计

### 15.1 使用旧版 RDelayedQueue 的设计

```java
RBlockingQueue<ExamSubmitTask> blockingQueue =
        redissonClient.getBlockingQueue("exam:submit:queue");

RDelayedQueue<ExamSubmitTask> delayedQueue =
        redissonClient.getDelayedQueue(blockingQueue);

long delay = examEndTime - System.currentTimeMillis();

delayedQueue.offer(task, delay, TimeUnit.MILLISECONDS);
```

消费端：

```java
while (true) {
    ExamSubmitTask task = blockingQueue.take();
    autoSubmitExam(task);
}
```

业务幂等：

```java
public void autoSubmitExam(ExamSubmitTask task) {
    ExamRecord record = examRecordService.getById(task.getRecordId());

    if (record == null) {
        return;
    }

    if (record.isSubmitted()) {
        return;
    }

    examSubmitService.submit(task.getRecordId());
}
```

这种方案简单，但需要自己补充：

- 幂等；
- 失败重试；
- 任务补偿；
- 消费失败记录；
- 队列积压监控；
- OOM 和 GC 监控。

---

### 15.2 使用新版 RReliableQueue 的设计

```java
RReliableQueue<ExamSubmitTask> queue =
        redissonClient.getReliableQueue("exam:submit:queue");

long delay = examEndTime - System.currentTimeMillis();

queue.add(
        QueueAddArgs.messages(
                MessageArgs.payload(task)
                        .delay(Duration.ofMillis(delay))
                        .deliveryLimit(5)
                        .timeToLive(Duration.ofHours(3))
                        .deduplicationById(
                                "exam-submit:" + task.getRecordId(),
                                Duration.ofHours(3)
                        )
        )
);
```

消费端：

```java
Message<ExamSubmitTask> message = queue.poll();

if (message != null) {
    try {
        autoSubmitExam(message.getPayload());
        queue.ack(message);
    } catch (Exception e) {
        queue.nack(message);
    }
}
```

业务层仍然必须做幂等：

```java
if (record.isSubmitted()) {
    queue.ack(message);
    return;
}
```

因为任何延迟任务系统都不能替代业务幂等。

---

## 十六、从 RDelayedQueue 迁移到 RReliableQueue 的注意事项

### 16.1 确认版本和授权

`RReliableQueue` 属于新版可靠队列能力，使用前需要确认：

- 当前 Redisson 版本；
- 当前使用的是社区版还是 PRO 版；
- 项目依赖中是否存在对应 API；
- 运行环境是否具备授权能力。

---

### 16.2 清理或迁移旧延迟任务

旧版 `RDelayedQueue` 使用的 Redis Key 与新版可靠队列结构不同。

迁移前需要确认：

1. Redis 中是否存在未到期的旧延迟任务；
2. 是否允许清空旧队列；
3. 是否需要把旧任务读出后重新投递到新队列；
4. 是否存在任务重复执行风险。

---

### 16.3 注意 Codec 兼容

延迟任务对象会序列化写入 Redis。

升级 Redisson 或迁移队列实现时，需要确认：

- 当前 Codec；
- 新版本默认 Codec；
- 历史任务是否还能反序列化；
- 是否需要显式指定 Codec；
- 是否需要数据迁移脚本。

---

### 16.4 保留业务幂等

无论使用哪种方案，自动交卷都必须先判断状态。

例如：

```text
如果试卷已经提交，则不再重复提交。
```

延迟任务系统只能保证“尽量按时触发”，不能替代业务幂等和状态机控制。

---

### 16.5 增加监控

建议监控：

- 延迟任务新增数量；
- 延迟任务消费数量；
- 延迟任务积压数量；
- 消费失败数量；
- 消费重试次数；
- DLQ 数量；
- Redis 内存；
- Redis 慢命令；
- JVM 堆内存；
- Full GC；
- 消费线程存活状态。

---

## 十七、总结

可以这样回答：

> 最新版本 Redisson 中，传统的 `RDelayedQueue` 已经被标记为废弃，官方推荐使用带 delay 能力的 `RReliableQueue`。
>
> 旧版 `RDelayedQueue` 的底层主要是基于 Redis 的 `ZSet + List + Pub/Sub` 实现。添加任务时，Redisson 会计算任务到期时间，把到期时间作为 ZSet 的 score，同时把任务内容写入延迟队列 List。如果当前任务是最早到期任务，就通过 Pub/Sub 通知内部调度器重新计算执行时间。
>
> Redisson 内部有一个 `QueueTransferTask`，它会根据 ZSet 中最近任务的到期时间动态调度。任务到期后，通过 Lua 脚本从 ZSet 中取出到期任务，转移到目标 `RBlockingQueue`，消费者再从 `RBlockingQueue` 中阻塞消费。
>
> 但是这个方案本质上只是延迟转移，不是强可靠消息队列。普通 `RBlockingQueue` 没有完整 ACK、重试、死信和 visibility timeout，所以如果消费者取出任务后宕机，任务可能丢失。
>
> 最新推荐的 `RReliableQueue` 则更像可靠消息队列。它支持 delay、ACK、NACK、visibility timeout、delivery limit、DLQ、去重和优先级。延迟消息在 delay 到期前不可消费，到期后变为可消费；消费者处理成功后 ACK，失败后可以 NACK 或等待 visibility timeout 后重新投递。相比旧版 `RDelayedQueue`，它更适合对可靠性要求更高的延迟任务场景。

---

## 十八、一句话总结

> 最新版本 Redisson 中，传统 `RDelayedQueue` 已废弃；它的旧实现是基于 `ZSet + List + Pub/Sub + QueueTransferTask` 将到期任务转移到目标队列，而新版推荐的 `RReliableQueue` 是带 delay、ACK、visibility timeout、重投和死信能力的可靠队列方案。

---

## 参考资料

- Redisson GitHub Releases：`redisson-4.3.1` 为当前 GitHub 标记的 latest release  
  <https://github.com/redisson/redisson/releases>
- Redisson `RDelayedQueue` 最新源码：已标记 `@Deprecated`，并提示使用 `RReliableQueue` delay feature  
  <https://github.com/redisson/redisson/blob/master/redisson/src/main/java/org/redisson/api/RDelayedQueue.java>
- Redisson `RedissonDelayedQueue` 最新源码：包含 `ZSet + List + Pub/Sub + QueueTransferTask` 相关实现  
  <https://raw.githubusercontent.com/redisson/redisson/master/redisson/src/main/java/org/redisson/RedissonDelayedQueue.java>
- Redisson `QueueTransferTask` 最新源码：包含 topic 监听、动态定时调度和 `pushTaskAsync()` 调用  
  <https://raw.githubusercontent.com/redisson/redisson/master/redisson/src/main/java/org/redisson/QueueTransferTask.java>
- Redisson 官方 Queue 文档：Reliable Queue 支持 delay、ACK、visibility timeout、delivery limit、DLQ、deduplication 等能力  
  <https://redisson.pro/docs/data-and-services/queues/>
