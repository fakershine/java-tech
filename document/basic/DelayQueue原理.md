# DelayQueue 原理总结

`DelayQueue` 是 JDK 提供的 **无界阻塞延迟队列**，用于存放延迟任务。  
队列中的元素必须实现 `Delayed` 接口，只有元素到期后，才能被消费者取出。

常见场景：

```text
订单超时取消
缓存过期清理
延迟重试
定时任务
连接超时关闭
```

---

## 1. DelayQueue 核心特点

| 特点 | 说明 |
|---|---|
| 延迟获取 | 元素到期后才能被取出 |
| 阻塞队列 | 没有到期元素时，消费者阻塞等待 |
| 无界队列 | 理论上容量不限制 |
| 优先级排序 | 按到期时间排序 |
| 线程安全 | 内部使用锁和条件队列控制并发 |

---

## 2. 核心接口 Delayed

放入 `DelayQueue` 的元素必须实现：

```java
public interface Delayed extends Comparable<Delayed> {

    long getDelay(TimeUnit unit);
}
```

需要实现两个方法：

```text
getDelay()：返回还剩多久到期
compareTo()：按到期时间排序
```

---

## 3. 基本使用示例

```java
public class DelayTask implements Delayed {

    private final String taskId;
    private final long executeTime;

    public DelayTask(String taskId, long delayMillis) {
        this.taskId = taskId;
        this.executeTime = System.currentTimeMillis() + delayMillis;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long delay = executeTime - System.currentTimeMillis();
        return unit.convert(delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(
                this.executeTime,
                ((DelayTask) other).executeTime
        );
    }

    public String getTaskId() {
        return taskId;
    }
}
```

使用：

```java
DelayQueue<DelayTask> queue = new DelayQueue<>();

queue.put(new DelayTask("order-1001", 30_000));

DelayTask task = queue.take(); // 30 秒后才能取出
```

---

## 4. 底层数据结构

`DelayQueue` 底层基于：

```text
PriorityQueue + ReentrantLock + Condition
```

可以理解为：

```text
DelayQueue
  ↓
PriorityQueue 按过期时间排序
  ↓
队首元素是最早到期的任务
```

---

## 5. 入队原理

调用：

```java
queue.put(task);
```

内部逻辑：

```text
1. 加锁。
2. 将任务放入 PriorityQueue。
3. 按 compareTo 排序。
4. 如果新任务成为队首，唤醒等待线程。
5. 释放锁。
```

为什么新任务成为队首要唤醒？

```text
因为新任务可能比原来的队首更早到期
消费者需要重新计算等待时间
```

---

## 6. 出队原理

调用：

```java
queue.take();
```

内部逻辑：

```text
1. 加锁。
2. 查看队首任务。
3. 如果队列为空，阻塞等待。
4. 如果队首任务未到期，等待剩余时间。
5. 如果队首任务已到期，取出并返回。
6. 释放锁。
```

核心判断：

```text
队首元素 getDelay() <= 0
```

只有满足这个条件，任务才能被取出。

---

## 7. 为什么只检查队首元素

因为 `DelayQueue` 内部用 `PriorityQueue` 排序。

```text
队首元素 = 最早到期的元素
```

如果队首都没到期，那么后面的元素一定更没到期。

所以只需要判断队首任务即可。

---

## 8. Leader-Follower 优化

`DelayQueue` 内部使用了 Leader-Follower 思想减少无效等待。

简单理解：

```text
只有一个线程作为 leader 等待队首任务到期
其他线程无限期等待
```

好处：

```text
避免多个线程都定时等待同一个任务
减少线程唤醒和竞争
```

当 leader 线程取出任务后，会唤醒其他线程重新竞争。

---

## 9. DelayQueue 为什么是阻塞的

如果没有到期任务：

```text
take() 会阻塞
```

阻塞依赖：

```text
Condition.await()
Condition.awaitNanos()
```

流程：

```text
队列为空 -> await()
队首未到期 -> awaitNanos(delay)
任务到期或有新更早任务加入 -> signal()
```

---

## 10. DelayQueue 的优势

- JDK 原生支持。
- 使用简单。
- 线程安全。
- 适合延迟任务。
- 可以自动按到期时间排序。
- 消费端没有到期任务时会阻塞，不需要手动轮询。

---

## 11. DelayQueue 的劣势

- 数据保存在 JVM 内存中，服务重启会丢失。
- 不适合分布式场景。
- 无界队列，任务过多可能导致 OOM。
- 依赖单进程消费。
- 不适合核心高可靠延迟任务。
- 大量任务下性能不如时间轮。

---

## 12. DelayQueue 和时间轮对比

| 对比项 | DelayQueue | HashedTimeWheel |
|---|---|---|
| 底层结构 | PriorityQueue | 环形数组 |
| 排序方式 | 按到期时间排序 | 按槽位分组 |
| 插入复杂度 | `O(logN)` | 接近 `O(1)` |
| 时间精度 | 较高 | 受 tick 影响 |
| 海量任务性能 | 一般 | 更好 |
| 实现复杂度 | 简单 | 较复杂 |
| 适合场景 | 中小规模延迟任务 | 大量延迟任务 |

---

## 13. DelayQueue 和 Redis 延迟队列对比

| 对比项 | DelayQueue | Redis ZSet |
|---|---|---|
| 存储位置 | JVM 内存 | Redis |
| 是否支持分布式 | 不支持 | 支持 |
| 是否持久化 | 不支持 | 可配合 Redis 持久化 |
| 可靠性 | 较低 | 较高 |
| 实现复杂度 | 简单 | 中等 |
| 适合场景 | 单机延迟任务 | 分布式延迟任务 |

---

## 14. 适用场景

适合：

```text
单机应用
中小规模延迟任务
临时任务调度
非核心业务
内存级超时控制
```

不适合：

```text
核心订单超时取消
分布式延迟任务
任务不能丢失的场景
海量延迟任务
服务重启后任务必须恢复的场景
```

---

## 15. 总结

`DelayQueue` 是 JDK 提供的无界阻塞延迟队列，队列中的元素必须实现 `Delayed` 接口。每个元素通过 `getDelay()` 表示剩余延迟时间，通过 `compareTo()` 按到期时间排序。

`DelayQueue` 底层基于 `PriorityQueue` 实现，队首元素一定是最早到期的任务。消费者调用 `take()` 时，如果队列为空会阻塞；如果队首任务还没到期，会等待剩余时间；只有队首任务到期后才能被取出。

它适合单机场景下的延迟任务，例如缓存清理、超时检测、简单订单延迟处理等。但由于任务存储在 JVM 内存中，服务重启会丢失，也不支持分布式，因此核心业务一般更推荐使用 MQ 延迟消息、Redis ZSet 或时间轮。

一句话总结：

```text
DelayQueue = PriorityQueue 排序 + Delayed 到期判断 + ReentrantLock/Condition 阻塞等待；
适合单机延迟任务，不适合高可靠分布式延迟任务。
```
