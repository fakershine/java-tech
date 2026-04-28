# Redisson 实现延迟队列的原理

## 一、核心思想

Redisson 的延迟队列 `RDelayedQueue` 本质上不是 Redis 原生支持的延迟队列，而是 Redisson 基于 Redis 的基础数据结构封装出来的一套延迟投递机制。

它的核心思想是：

> 任务先进入延迟队列，到期后由 Redisson 内部调度器转移到真正的目标队列，消费者再从目标队列消费。

也就是说，消费者不是直接消费 `RDelayedQueue`，而是消费普通的 `RBlockingQueue`。

整体流程如下：

```text
生产者
  ↓
RDelayedQueue
  ↓
任务到期后转移
  ↓
RBlockingQueue
  ↓
消费者消费
```

---

## 二、基本使用方式

Redisson 延迟队列一般配合 `RBlockingQueue` 使用。

示例代码如下：

```java
RBlockingQueue<ExamSubmitTask> blockingQueue =
        redissonClient.getBlockingQueue("exam:submit:queue");

RDelayedQueue<ExamSubmitTask> delayedQueue =
        redissonClient.getDelayedQueue(blockingQueue);

// 30 分钟后投递到 blockingQueue
delayedQueue.offer(task, 30, TimeUnit.MINUTES);

// 消费端阻塞获取任务
ExamSubmitTask task = blockingQueue.take();
```

这里有两个关键对象：

| 对象 | 作用 |
|---|---|
| `RDelayedQueue` | 负责接收延迟任务 |
| `RBlockingQueue` | 真正给消费者消费的目标队列 |

需要注意：

> 生产者写入的是 `RDelayedQueue`，消费者消费的是 `RBlockingQueue`。

---

## 三、底层使用的数据结构

Redisson 延迟队列底层主要使用了以下 Redis 数据结构：

| Redis 结构 | 作用 |
|---|---|
| `ZSet` | 保存任务到期时间，score 是到期时间戳 |
| `List` | 保存延迟任务内容 |
| `Pub/Sub` | 通知调度器有新的更早任务加入 |
| 目标队列 `List` | 保存已经到期、可消费的任务 |

假设业务队列名是：

```text
exam:submit:queue
```

Redisson 内部大致会维护类似下面的 Redis key：

```text
redisson_delay_queue_timeout:{exam:submit:queue}
redisson_delay_queue:{exam:submit:queue}
redisson_delay_queue_channel:{exam:submit:queue}
exam:submit:queue
```

说明如下：

| Key | 类型 | 作用 |
|---|---|---|
| `redisson_delay_queue_timeout:{queueName}` | `ZSet` | 保存任务到期时间 |
| `redisson_delay_queue:{queueName}` | `List` | 保存延迟任务内容 |
| `redisson_delay_queue_channel:{queueName}` | `Pub/Sub Channel` | 通知调度器重新计算最近到期任务 |
| `queueName` | `List` | 真正的目标阻塞队列 |

---

## 四、添加延迟任务时做了什么？

当执行：

```java
delayedQueue.offer(task, delay, timeUnit);
```

Redisson 内部大致会做以下事情：

```text
1. 计算任务到期时间 timeout = 当前时间 + delay
2. 序列化任务对象
3. 生成一个 randomId
4. 将 randomId 和 value 打包成唯一 value
5. 写入 Timeout ZSet，score 为到期时间
6. 写入延迟队列 List
7. 如果当前任务是最早到期的任务，则 publish 通知调度器
```

可以简化理解为：

```text
ZADD timeoutSet 到期时间 任务内容
RPUSH delayedQueue 任务内容
PUBLISH channel 到期时间
```

其中：

- `ZSet` 用于根据到期时间排序；
- `List` 用于保存完整的延迟任务内容；
- `Pub/Sub` 用于通知 Redisson 内部调度器重新调整下一次任务触发时间。

---

## 五、为什么使用 ZSet？

Redis 的 `ZSet` 天然适合实现延迟队列。

`ZSet` 中每个元素都有一个 `score`，Redisson 将任务的到期时间作为 `score`。

例如：

| 任务 | 延迟时间 | 到期时间 score |
|---|---:|---:|
| taskA | 10 秒 | 当前时间 + 10 秒 |
| taskB | 30 秒 | 当前时间 + 30 秒 |
| taskC | 5 秒 | 当前时间 + 5 秒 |

这样就可以通过 `score` 找到已经到期的任务：

```redis
ZRANGEBYSCORE key 0 当前时间
```

也就是：

```text
score <= 当前时间的任务，就是已经到期的任务
```

所以，`ZSet` 的作用就是：

> 按照任务到期时间对延迟任务进行排序，方便快速找到已经到期的任务。

---

## 六、任务到期后如何转移？

Redisson 内部有一个调度任务，叫做：

```java
QueueTransferTask
```

它负责把已经到期的任务从延迟队列转移到目标队列。

大致逻辑如下：

```text
1. 从 ZSet 中查询已经到期的任务
2. 将到期任务 push 到目标 RBlockingQueue
3. 从延迟队列 List 中删除这些任务
4. 从 Timeout ZSet 中删除这些任务
5. 查询下一个最早到期任务
6. 继续等待下一次触发
```

可以简化成下面的流程：

```text
ZRANGEBYSCORE timeoutSet 0 当前时间
        ↓
RPUSH targetQueue 到期任务
        ↓
LREM delayedQueue 到期任务
        ↓
ZREM timeoutSet 到期任务
```

Redisson 通过 Lua 脚本保证这几个操作的原子性，避免转移过程中出现数据不一致。

---

## 七、为什么需要 Pub/Sub？

Redisson 不是每隔固定时间全量扫描一次 Redis。

它会根据最近一个任务的到期时间来安排下一次执行。

例如：

```text
当前最近任务是 10 分钟后到期
```

那么 Redisson 内部调度器可能会等待 10 分钟后再执行转移逻辑。

但是如果此时又添加了一个 1 分钟后到期的新任务，就需要通知调度器重新计算等待时间。

所以 Redisson 使用 Redis 的 `Pub/Sub` 做通知。

流程如下：

```text
新任务加入
        ↓
判断是否比当前最早任务更早到期
        ↓
如果更早，则 publish 新的到期时间
        ↓
QueueTransferTask 收到消息
        ↓
重新安排下一次转移任务的时间
```

这样做的好处是：

1. 不需要固定频率全量扫描；
2. 可以更及时地处理新加入的更早任务；
3. 减少 Redis 无效查询；
4. 延迟精度比定时任务扫描更高。

---

## 八、为什么要生成 randomId？

Redisson 写入 Redis 的任务内容不是单纯的业务对象，而是：

```text
randomId + value
```

原因是业务对象可能重复。

例如两个任务内容完全一样：

```json
{"examId":1001,"userId":2001}
{"examId":1001,"userId":2001}
```

如果 Redis 里只存业务 value，在删除、转移任务时可能误删。

所以 Redisson 会给每个任务生成一个随机 ID，使每条任务在 Redis 中都是唯一的。

这样即使业务内容相同，也能作为两条独立任务处理。

---

## 九、消费者如何消费？

消费者并不是从 `RDelayedQueue` 中消费，而是从目标队列 `RBlockingQueue` 中消费。

示例代码：

```java
while (true) {
    ExamSubmitTask task = blockingQueue.take();
    submitExam(task);
}
```

任务未到期时：

```text
任务在延迟队列 List 和 Timeout ZSet 中
```

任务到期后：

```text
Redisson 将任务转移到 RBlockingQueue
```

消费者才能通过：

```java
blockingQueue.take();
```

获取到任务。

---

## 十、完整执行流程

完整流程如下：

```text
生产者调用 delayedQueue.offer(task, delay)
        ↓
Redisson 计算任务到期时间
        ↓
任务写入 Timeout ZSet，score = 到期时间
        ↓
任务写入延迟队列 List
        ↓
如果是最早到期任务，则通过 Pub/Sub 通知调度器
        ↓
QueueTransferTask 等待最近任务到期
        ↓
任务到期后执行 Lua 脚本
        ↓
从 ZSet 中取出到期任务
        ↓
将任务转移到目标 RBlockingQueue
        ↓
从延迟队列 List 和 ZSet 中删除任务
        ↓
消费者通过 blockingQueue.take() 获取任务
        ↓
执行业务逻辑
```

---

## 十一、流程图

```text
                 ┌─────────────────────┐
                 │       生产者         │
                 └──────────┬──────────┘
                            │
                            │ delayedQueue.offer(task, delay)
                            ↓
                 ┌─────────────────────┐
                 │    RDelayedQueue     │
                 └──────────┬──────────┘
                            │
            ┌───────────────┼────────────────┐
            │               │                │
            ↓               ↓                ↓
     ┌──────────┐    ┌────────────┐   ┌──────────────┐
     │  ZSet    │    │ Delay List │   │ Pub/Sub      │
     │ 到期时间 │    │ 延迟任务   │   │ 通知调度器   │
     └──────────┘    └────────────┘   └──────────────┘
            │
            │ QueueTransferTask 到期转移
            ↓
     ┌─────────────────────┐
     │   RBlockingQueue    │
     │   目标消费队列       │
     └──────────┬──────────┘
                │
                │ take()
                ↓
          ┌──────────┐
          │  消费者   │
          └──────────┘
```

---

## 十二、Redisson 延迟队列的优点

### 1. 支持分布式

多个服务实例可以共同使用同一个 Redis 延迟队列。

相比 JDK `DelayQueue` 这类单机内存队列，Redisson 延迟队列更加适合分布式系统。

---

### 2. 不依赖本地 JVM 内存

任务存储在 Redis 中，不会因为应用重启直接丢失。

而单机内存队列的数据存在 JVM 内存中，一旦服务重启，未执行的任务就会丢失。

---

### 3. 延迟比定时任务扫描更低

Redisson 不是固定间隔全量扫描，而是根据最近任务到期时间动态调度。

相比定时任务扫描：

```text
每 1 分钟扫描一次
```

Redisson 的延迟通常更低。

---

### 4. 使用简单

开发者只需要使用：

```java
delayedQueue.offer(task, delay, timeUnit);
blockingQueue.take();
```

不需要自己维护：

- ZSet 扫描；
- 任务转移；
- Lua 脚本；
- 并发控制；
- Pub/Sub 通知；
- 阻塞消费。

---

## 十三、Redisson 延迟队列的缺点

### 1. 不是强可靠消息队列

`RDelayedQueue + RBlockingQueue` 没有完整的 ACK、重试、死信队列机制。

如果消费者执行过程中宕机：

```text
take() 成功
        ↓
任务已经从队列中移除
        ↓
业务还没处理完
        ↓
服务宕机
        ↓
任务可能丢失
```

所以对于强一致、强可靠的核心业务链路，不建议只依赖普通的 `RDelayedQueue + RBlockingQueue`。

---

### 2. 依赖 Redis 可用性

如果 Redis 出现以下问题，可能影响延迟任务投递：

- Redis 宕机；
- Redis 主从切换；
- Redis 网络抖动；
- Redis 阻塞；
- Redis 内存不足；
- Redis 淘汰策略误删数据。

---

### 3. 依赖 Redisson 客户端调度

延迟任务到期后的转移由 Redisson 客户端内部的 `QueueTransferTask` 完成。

如果没有服务实例初始化这个 delayed queue，对应的转移任务可能不会执行。

也就是说，使用 Redisson 延迟队列时，需要确保至少有一个应用实例正常运行并初始化了对应队列。

---

### 4. 需要关注 Redisson 版本 Bug

例如 Redisson 3.10.6 中，阻塞队列调用存在内存泄漏问题。

在高频调用 `RBlockingQueue.take()` 的场景下，可能导致 listener 无法释放，最终引发 OOM。

因此使用 Redisson 延迟队列时，需要关注：

- Redisson 版本；
- Release Notes；
- 已知 Issue；
- 生产环境稳定性；
- 压测结果。

---

### 5. 需要关注 Codec 兼容

Redisson 会将任务对象序列化后写入 Redis。

如果升级 Redisson 后 Codec 不兼容，可能导致历史任务无法反序列化。

因此升级 Redisson 时需要确认：

1. 当前使用的 Codec；
2. 新版本默认 Codec 是否变化；
3. 是否需要显式指定 Codec；
4. Redis 中是否有历史延迟任务；
5. 是否需要迁移或清理旧数据。

---

## 十四、和手写 Redis ZSet 延迟队列的区别

### 14.1 手写 ZSet 延迟队列

手写方案一般是：

```text
生产者：
ZADD delay_queue 到期时间 task

消费者：
定时 ZRANGEBYSCORE 查询到期任务
ZREM 删除任务
执行业务逻辑
```

这种方案的问题是：

1. 需要自己写扫描逻辑；
2. 需要自己处理并发竞争；
3. 需要自己保证任务只被消费一次；
4. 需要自己处理任务失败重试；
5. 需要自己处理服务重启后的任务恢复；
6. 需要自己控制扫描频率和延迟。

---

### 14.2 Redisson 延迟队列

Redisson 方案是：

```text
生产者：
delayedQueue.offer(task, delay)

Redisson：
写 ZSet + List + Pub/Sub

内部调度：
QueueTransferTask 转移到 RBlockingQueue

消费者：
blockingQueue.take()
```

Redisson 帮我们封装了：

- 延迟任务写入；
- 到期时间排序；
- 到期任务转移；
- Pub/Sub 通知；
- 阻塞队列消费；
- Lua 原子操作。

---

### 14.3 对比表

| 对比项 | 手写 ZSet | Redisson RDelayedQueue |
|---|---|---|
| 实现成本 | 较高 | 较低 |
| 调度逻辑 | 自己实现 | Redisson 封装 |
| 到期转移 | 自己实现 | Redisson 自动转移 |
| 阻塞消费 | 需要自己实现 | 支持 `RBlockingQueue.take()` |
| 并发控制 | 自己处理 | Redisson 内部处理 |
| Pub/Sub 通知 | 自己实现 | Redisson 内部实现 |
| Lua 脚本 | 自己编写 | Redisson 内部封装 |
| 版本风险 | 主要是业务代码风险 | 依赖 Redisson 版本 |
| 可靠性 | 取决于实现 | 普通队列也不是强可靠 |

---

## 十五、适用场景

Redisson 延迟队列适合以下场景：

- 考试结束自动交卷；
- 订单超时取消；
- 支付超时关闭；
- 优惠券到期提醒；
- 任务延迟执行；
- 短信延迟发送；
- 邮件延迟发送；
- 轻量级分布式延迟任务；
- 对可靠性要求不是极端严格的业务场景。

例如考试自动交卷场景：

```text
用户开始考试
        ↓
生成自动交卷延迟任务
        ↓
任务延迟时间 = 考试结束时间 - 当前时间
        ↓
任务到期后进入阻塞队列
        ↓
消费者执行自动交卷逻辑
```

---

## 十六、不适合的场景

如果业务对可靠性要求非常高，不建议只依赖 `RDelayedQueue + RBlockingQueue`。

例如：

- 金融支付核心链路；
- 必须保证任务一定执行；
- 需要 ACK 机制；
- 需要失败重试；
- 需要死信队列；
- 需要消息轨迹；
- 需要严格投递保证；
- 需要大量任务堆积；
- 需要高吞吐消息处理。

这类场景更适合使用：

- RocketMQ 延迟消息；
- RabbitMQ TTL + 死信队列；
- Pulsar 延迟消息；
- 支持可靠投递的任务调度系统；
- Redisson 新版 Reliable Queue；
- 自研可靠延迟任务系统。

---

## 十七、和消息队列延迟消息的区别

| 对比项 | Redisson 延迟队列 | 消息队列延迟消息 |
|---|---|---|
| 底层依赖 | Redis | MQ Broker |
| 使用成本 | 较低 | 中等或较高 |
| 延迟精度 | 较好 | 取决于 MQ 实现 |
| 可靠性 | 普通队列可靠性一般 | 通常更强 |
| ACK 机制 | 普通队列不完善 | 一般支持 |
| 重试机制 | 需要业务补充 | 一般支持 |
| 死信队列 | 普通队列不完善 | 一般支持 |
| 消息堆积能力 | 依赖 Redis | MQ 更适合大规模堆积 |
| 适合场景 | 轻量级延迟任务 | 核心异步消息链路 |

如果只是考试自动交卷、订单超时关闭这类轻量级延迟任务，Redisson 延迟队列是可以考虑的。

如果是支付、交易、账务这类强可靠链路，更建议使用可靠消息队列或任务调度系统。

---

## 十八、使用时的注意事项

### 18.1 消费端要做好幂等

延迟任务可能因为重试、补偿、服务异常等原因被重复执行。

因此消费逻辑必须支持幂等。

例如自动交卷场景：

```text
任务执行前先判断试卷状态
```

如果试卷已经提交，则直接跳过：

```java
if (examRecord.isSubmitted()) {
    return;
}
```

---

### 18.2 任务中不要放太大的对象

Redisson 会将任务对象序列化后写入 Redis。

如果任务对象过大，会带来以下问题：

- Redis 内存占用增加；
- 网络传输变慢；
- 序列化和反序列化变慢；
- GC 压力增大；
- Redis 大 key 风险增加。

建议任务中只存必要字段，例如：

```java
public class ExamSubmitTask {
    private Long examId;
    private Long userId;
    private Long recordId;
}
```

不要把完整试卷、题目列表、用户答案等大对象都塞进延迟任务。

---

### 18.3 需要监控队列积压

建议监控：

- 延迟任务生产数量；
- 延迟任务消费数量；
- 延迟队列积压数量；
- 阻塞队列积压数量；
- 消费失败数量；
- 消费耗时；
- Redis 内存使用；
- Redis 慢命令；
- JVM 堆内存；
- Full GC 次数。

---

### 18.4 需要考虑任务失败补偿

如果任务消费失败，应该有补偿方案。

常见方式：

1. 失败后重新投递延迟任务；
2. 记录失败任务表；
3. 定时任务扫描失败记录；
4. 人工补偿；
5. 接入可靠消息队列；
6. 增加死信队列机制。

---

### 18.5 需要注意服务重启

虽然任务在 Redis 中，不会因为应用重启直接丢失，但是需要注意：

1. 服务重启期间，到期任务可能无法及时转移；
2. 服务恢复后，需要重新初始化 `RDelayedQueue`；
3. 消费线程需要重新启动；
4. 如果消费者执行中宕机，已经取出的任务可能丢失。

---

## 十九、面试回答版本

可以这样回答：

> Redisson 延迟队列底层主要是基于 Redis 的 ZSet、List 和 Pub/Sub 实现的。
>
> 当调用 `RDelayedQueue.offer()` 添加任务时，Redisson 会计算任务的到期时间，然后把任务写入一个 ZSet，score 就是到期时间戳，同时也会写入一个延迟队列 List。
>
> Redisson 内部有一个 `QueueTransferTask` 调度任务，它会根据 ZSet 中最早到期任务的时间设置定时器。到期后，它通过 Lua 脚本从 ZSet 中取出已经到期的任务，然后把这些任务转移到真正的目标队列，也就是 `RBlockingQueue`。
>
> 消费者并不是直接消费 `RDelayedQueue`，而是阻塞消费目标 `RBlockingQueue`。所以整体流程就是：先进入延迟队列，到期后转移到阻塞队列，再由消费者消费。
>
> 它的优点是实现简单、支持分布式、延迟比定时扫描更低；缺点是普通 `RDelayedQueue + RBlockingQueue` 没有完整 ACK 和死信机制，不是强可靠消息队列，另外也需要关注 Redisson 版本 Bug 和 Codec 兼容问题。

---

## 二十、结合考试自动交卷场景说明

在考试自动交卷场景中，可以这样设计：

### 20.1 生产延迟任务

用户开始考试时，计算考试结束时间。

```java
long delay = examEndTime - System.currentTimeMillis();

ExamSubmitTask task = new ExamSubmitTask();
task.setExamId(examId);
task.setUserId(userId);
task.setRecordId(recordId);

delayedQueue.offer(task, delay, TimeUnit.MILLISECONDS);
```

---

### 20.2 消费延迟任务

消费者从 `RBlockingQueue` 中阻塞获取任务：

```java
while (true) {
    ExamSubmitTask task = blockingQueue.take();
    autoSubmitExam(task);
}
```

---

### 20.3 自动交卷逻辑

自动交卷时需要先判断状态：

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

这样即使任务重复执行，也不会重复交卷。

---

## 二十一、核心总结

Redisson 延迟队列可以总结为：

```text
Redisson 延迟队列 =
    ZSet 记录任务到期时间
  + List 保存延迟任务内容
  + Pub/Sub 通知调度器重新计算最近任务
  + QueueTransferTask 到期转移任务
  + RBlockingQueue 给消费者阻塞消费
```

最关键的一句话：

> Redisson 延迟队列不是消费者直接从延迟队列里取任务，而是任务到期后由 Redisson 内部调度器转移到目标阻塞队列，消费者再从目标队列消费。

---

## 二十二、一句话总结

> Redisson 延迟队列的实现原理是：使用 Redis ZSet 按任务到期时间排序，使用 List 保存延迟任务，使用 Pub/Sub 通知调度器调整最近执行时间，任务到期后由 `QueueTransferTask` 将任务转移到 `RBlockingQueue`，最终由消费者阻塞消费。
