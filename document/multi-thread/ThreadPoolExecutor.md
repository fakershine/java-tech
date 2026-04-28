# ThreadPoolExecutor 总结

`ThreadPoolExecutor` 是 Java 线程池的核心实现类，用于复用线程、控制并发数量、管理任务队列，避免频繁创建和销毁线程带来的性能开销。

---

## 1. 为什么要使用线程池

### 主要作用

- 复用线程，减少线程创建和销毁成本。
- 控制并发线程数量，防止资源耗尽。
- 管理任务队列，实现削峰。
- 统一处理线程创建、任务执行、异常和拒绝策略。
- 提高系统稳定性和响应能力。

---

## 2. ThreadPoolExecutor 核心参数

```java
public ThreadPoolExecutor(
    int corePoolSize,
    int maximumPoolSize,
    long keepAliveTime,
    TimeUnit unit,
    BlockingQueue<Runnable> workQueue,
    ThreadFactory threadFactory,
    RejectedExecutionHandler handler
)
```

| 参数 | 说明 |
|---|---|
| corePoolSize | 核心线程数 |
| maximumPoolSize | 最大线程数 |
| keepAliveTime | 非核心线程空闲存活时间 |
| unit | 时间单位 |
| workQueue | 任务队列 |
| threadFactory | 线程工厂 |
| handler | 拒绝策略 |

---

## 3. 任务执行流程

当提交一个任务时，线程池执行流程如下：

```text
提交任务
  ↓
当前线程数 < corePoolSize
  ↓ 是
创建核心线程执行任务
  ↓ 否
尝试放入任务队列
  ↓
队列未满
  ↓ 是
任务进入队列等待执行
  ↓ 否
当前线程数 < maximumPoolSize
  ↓ 是
创建非核心线程执行任务
  ↓ 否
执行拒绝策略
```

### 简单总结

```text
先创建核心线程
核心线程满了，任务进队列
队列满了，创建非核心线程
线程达到最大值，执行拒绝策略
```

---

## 4. 核心线程数和最大线程数

### corePoolSize

核心线程数，线程池会优先创建核心线程执行任务。

默认情况下，核心线程即使空闲也不会被回收。

### maximumPoolSize

线程池允许创建的最大线程数。

只有当：

```text
核心线程已满
并且任务队列已满
```

才会继续创建非核心线程，直到达到 `maximumPoolSize`。

---

## 5. keepAliveTime

`keepAliveTime` 用于控制非核心线程空闲多久后被回收。

```text
非核心线程空闲时间 > keepAliveTime
  ↓
线程被回收
```

默认情况下，核心线程不会被回收。

如果调用：

```java
threadPoolExecutor.allowCoreThreadTimeOut(true);
```

核心线程空闲超过 `keepAliveTime` 后也可以被回收。

---

## 6. workQueue 任务队列

线程池中常见阻塞队列如下：

| 队列 | 说明 | 特点 |
|---|---|---|
| ArrayBlockingQueue | 有界数组队列 | 固定容量，常用 |
| LinkedBlockingQueue | 链表队列 | 可有界，也可无界 |
| SynchronousQueue | 不存储任务，直接移交线程 | 常用于快速扩容线程 |
| PriorityBlockingQueue | 优先级队列 | 按优先级执行任务 |
| DelayQueue | 延迟队列 | 延迟任务场景 |

---

## 7. 常见队列区别

### ArrayBlockingQueue

```text
有界队列
```

优点：

- 容量固定，风险可控。
- 可以防止任务无限堆积。

缺点：

- 队列满后可能触发扩容线程或拒绝策略。

---

### LinkedBlockingQueue

```text
链表队列
```

如果不指定容量，默认容量非常大，容易导致任务堆积。

风险：

```text
请求太多
  ↓
任务不断进入队列
  ↓
内存不断增长
  ↓
可能 OOM
```

---

### SynchronousQueue

```text
不存储任务
```

每个任务必须直接交给线程执行。

特点：

- 队列不缓存任务。
- 容易创建大量线程。
- 常用于 `newCachedThreadPool`。

---

## 8. 拒绝策略

当线程数达到 `maximumPoolSize`，并且任务队列已满时，会触发拒绝策略。

| 拒绝策略 | 说明 |
|---|---|
| AbortPolicy | 直接抛异常，默认策略 |
| CallerRunsPolicy | 由提交任务的线程自己执行 |
| DiscardPolicy | 直接丢弃任务，不抛异常 |
| DiscardOldestPolicy | 丢弃队列中最老的任务，再提交新任务 |

### 常用推荐

实际项目中一般不建议静默丢弃任务。

推荐：

```text
自定义拒绝策略 + 日志记录 + 告警 + 降级处理
```

---

## 9. ThreadFactory 线程工厂

线程工厂用于自定义线程创建方式。

常见用途：

- 设置线程名称。
- 设置是否为守护线程。
- 设置异常处理器。
- 设置线程优先级。

示例：

```java
ThreadFactory threadFactory = new ThreadFactory() {
    private final AtomicInteger index = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName("order-pool-" + index.getAndIncrement());
        return thread;
    }
};
```

### 为什么要设置线程名

方便排查问题。

例如：

```text
order-pool-1
payment-pool-1
stock-pool-1
```

通过日志、线程栈可以快速定位是哪个业务线程池出问题。

---

## 10. 线程池状态

ThreadPoolExecutor 内部有几种状态：

| 状态 | 说明 |
|---|---|
| RUNNING | 正常接收并处理任务 |
| SHUTDOWN | 不再接收新任务，但会处理队列中的任务 |
| STOP | 不接收新任务，也不处理队列任务，会中断正在执行的任务 |
| TIDYING | 所有任务结束，线程数为 0 |
| TERMINATED | 线程池完全终止 |

状态流转：

```text
RUNNING
  ↓ shutdown()
SHUTDOWN
  ↓ 队列和任务执行完
TIDYING
  ↓ terminated()
TERMINATED
```

或者：

```text
RUNNING
  ↓ shutdownNow()
STOP
  ↓
TIDYING
  ↓
TERMINATED
```

---

## 11. execute 和 submit 的区别

### execute

```java
executor.execute(() -> {
    int i = 1 / 0;
});
```

特点：

- 没有返回值。
- 异常会直接抛到线程的异常处理器。
- 适合不需要返回结果的任务。

---

### submit

```java
Future<?> future = executor.submit(() -> {
    int i = 1 / 0;
});
```

特点：

- 有返回值 `Future`。
- 异常会被封装到 `Future` 中。
- 需要调用 `future.get()` 才能感知异常。

```java
try {
    future.get();
} catch (Exception e) {
    // 这里才能拿到任务异常
}
```

### 总结

| 方法 | 返回值 | 异常表现 |
|---|---|---|
| execute | 无 | 异常直接抛出 |
| submit | Future | 异常封装在 Future 中 |

---

## 12. 常见线程池工具类

Executors 提供了几个快捷创建线程池的方法，但生产环境一般不推荐直接使用。

| 方法 | 说明 | 风险 |
|---|---|---|
| newFixedThreadPool | 固定线程数 | 队列无界，可能 OOM |
| newSingleThreadExecutor | 单线程池 | 队列无界，可能 OOM |
| newCachedThreadPool | 缓存线程池 | 最大线程数过大，可能 OOM |
| newScheduledThreadPool | 定时任务线程池 | 需控制任务异常和堆积 |

### 为什么不推荐 Executors

因为它隐藏了核心参数，容易出现：

- 队列过大导致 OOM。
- 线程数过多导致 OOM。
- 无法自定义拒绝策略。
- 无法清晰控制线程池行为。

推荐手动创建：

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10,
    20,
    60,
    TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(1000),
    threadFactory,
    new ThreadPoolExecutor.AbortPolicy()
);
```

---

## 13. 线程池参数如何设置

线程池参数要根据任务类型设置。

---

### CPU 密集型任务

特点：

```text
大量计算
CPU 占用高
IO 等待少
```

建议：

```text
核心线程数 = CPU 核数 + 1
```

示例：

```text
8 核 CPU
corePoolSize = 8 或 9
```

---

### IO 密集型任务

特点：

```text
大量网络 IO、磁盘 IO、数据库调用
线程经常等待
```

建议线程数可以设置更大：

```text
核心线程数 = CPU 核数 * 2
```

或者根据实际压测调整。

---

### 通用估算公式

```text
线程数 = CPU 核数 * (1 + 线程等待时间 / 线程计算时间)
```

例如：

```text
CPU 核数 = 8
计算时间 = 100ms
等待时间 = 300ms

线程数 = 8 * (1 + 300 / 100) = 32
```

---

## 14. 线程池隔离

不同业务最好使用不同线程池，避免互相影响。

不推荐：

```text
订单、支付、库存、消息发送共用一个线程池
```

推荐：

```text
订单线程池
支付线程池
库存线程池
消息线程池
```

### 好处

- 避免某个慢任务拖垮所有业务。
- 方便针对不同业务单独调参。
- 方便监控和告警。
- 提高系统稳定性。

---

## 15. 线程池常见问题

### 1. 任务堆积

原因：

- 核心线程数太小。
- 任务执行太慢。
- 队列太大。
- 下游服务响应慢。

解决：

- 调整线程数。
- 优化任务耗时。
- 缩小队列并配合拒绝策略。
- 增加限流、熔断、降级。

---

### 2. OOM

原因：

- 使用无界队列。
- 最大线程数过大。
- 任务对象占用内存大。
- 任务生产速度远大于消费速度。

解决：

- 使用有界队列。
- 合理设置最大线程数。
- 增加拒绝策略。
- 做限流和削峰。

---

### 3. 线程池打满

表现：

- 队列持续增长。
- 活跃线程数接近最大线程数。
- 任务执行延迟升高。
- 触发拒绝策略。

排查：

- 查看线程池监控。
- dump 线程栈。
- 分析是否下游接口慢。
- 分析是否有锁等待或死循环。

---

### 4. 异常被吞

使用 `submit` 时，如果不调用 `Future.get()`，异常可能不会明显暴露。

建议：

- 使用 `execute`。
- 或重写 `afterExecute`。
- 或统一包装任务异常处理。

---

## 16. 线程池监控指标

生产环境建议监控以下指标：

| 指标 | 说明 |
|---|---|
| corePoolSize | 核心线程数 |
| maximumPoolSize | 最大线程数 |
| poolSize | 当前线程数 |
| activeCount | 活跃线程数 |
| queueSize | 队列长度 |
| completedTaskCount | 已完成任务数 |
| taskCount | 总任务数 |
| rejectedCount | 拒绝任务数 |

重点关注：

```text
activeCount 长期接近 maximumPoolSize
queueSize 持续增长
rejectedCount 增加
任务耗时变长
```

这些通常说明线程池已经出现瓶颈。

---

## 17. shutdown 和 shutdownNow

### shutdown

```java
executor.shutdown();
```

特点：

- 不再接收新任务。
- 已提交任务继续执行。
- 队列中的任务会继续执行。
- 比较优雅。

---

### shutdownNow

```java
executor.shutdownNow();
```

特点：

- 不再接收新任务。
- 尝试中断正在执行的任务。
- 返回队列中未执行的任务。
- 不保证任务一定停止。

---

## 18. 使用注意点

- 不建议使用 Executors 创建线程池。
- 推荐手动创建 ThreadPoolExecutor。
- 必须使用有界队列。
- 必须设置合理拒绝策略。
- 必须设置有意义的线程名称。
- 不同业务尽量使用不同线程池。
- 线程池参数要结合压测调整。
- 线程池要接入监控和告警。
- 异步任务中使用 ThreadLocal 要注意清理和上下文传递。

---

## 19. 总结

`ThreadPoolExecutor` 是 Java 线程池的核心实现类，主要用于线程复用、任务队列管理、并发控制和拒绝处理。

它的核心参数包括核心线程数、最大线程数、线程空闲时间、任务队列、线程工厂和拒绝策略。任务提交后，线程池会先判断核心线程是否已满，未满则创建核心线程执行；核心线程满了则进入任务队列；队列满了再创建非核心线程；如果线程数达到最大值，则执行拒绝策略。

线程池常见队列有 `ArrayBlockingQueue`、`LinkedBlockingQueue`、`SynchronousQueue` 等。生产环境不建议使用 `Executors` 快捷方法，因为可能使用无界队列或过大的最大线程数，导致 OOM。

实际使用中，要根据任务类型设置线程数。CPU 密集型任务线程数一般接近 CPU 核数，IO 密集型任务可以适当增大。不同业务建议使用不同线程池，并配置有界队列、自定义线程名、拒绝策略、监控和告警。

一句话总结：

```text
ThreadPoolExecutor 的核心是：用有限线程处理大量任务，通过线程数、队列和拒绝策略控制系统并发和稳定性。
```
