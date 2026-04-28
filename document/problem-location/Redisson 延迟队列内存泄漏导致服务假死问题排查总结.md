# Redisson 延迟队列内存泄漏导致服务假死问题排查总结

## 目录

- [一、背景](#一背景)
- [二、事故现象](#二事故现象)
- [三、初步判断](#三初步判断)
- [四、现场证据](#四现场证据)
- [五、MAT 分析过程](#五mat-分析过程)
- [六、对象引用链分析](#六对象引用链分析)
- [七、源码排查](#七源码排查)
- [八、根因确认](#八根因确认)
- [九、本地复现](#九本地复现)
- [十、修复方案](#十修复方案)
- [十一、临时规避方案](#十一临时规避方案)
- [十二、二次 OOM 补充说明](#十二二次-oom-补充说明)
- [十三、最终结论](#十三最终结论)
- [十四、复盘改进项](#十四复盘改进项)
- [十五、经验总结](#十五经验总结)
- [十六、面试表达版本](#十六面试表达版本)
- [十七、简短总结](#十七简短总结)

---

## 一、背景

项目中存在考试功能，业务要求在考试结束时自动提交试卷，因此系统需要支持一个**延迟任务执行能力**，用于在指定时间点触发自动交卷逻辑。

前期针对延迟任务的实现方案做过调研，常见实现方式大体可以分为以下四类：

| 方案 | 代表实现 | 优点 | 缺点 |
|---|---|---|---|
| 单机内存队列 | JDK `DelayQueue`、Netty `HashedWheelTimer` | 实现简单，性能较好 | 服务重启任务丢失；单机内存堆积可能 OOM；多实例下可能重复执行 |
| Redis ZSet | 手动实现、Redisson 延迟队列 | 支持分布式；任务可持久化；实现成本适中 | 依赖 Redis；需要关注框架版本稳定性 |
| 消息队列 | RabbitMQ、RocketMQ、Pulsar 等 | 可靠性较好，适合大规模异步任务 | 当前项目使用 Kafka，Kafka 原生不直接支持精确延迟队列 |
| 定时任务扫描 | XXL-JOB、Spring Scheduler 等 | 实现简单，易于理解 | 需要周期性扫描，数据量大时代价高；延迟取决于扫描间隔 |

综合考虑后，最终选择了基于 **Redis + Redisson 延迟队列** 的方案。

当时使用的 Redisson 版本为：

```xml
<redisson.version>3.10.6</redisson.version>
```

选择 Redisson 的主要原因：

1. 支持分布式延迟队列；
2. 避免单机内存队列带来的任务丢失和重复执行问题；
3. 相比定时任务全量扫描，延迟更低，性能更可控；
4. 项目已有 Redis 基础设施，引入成本较低。

---

## 二、事故现象

某天早上 10 点左右，测试人员在群里反馈：**测试环境不可用**，怀疑是否有人正在发版。

经过确认：

- 当时没有人在发版；
- 服务进程仍然存在；
- 接口基本不可用；
- 重启服务后，测试环境恢复正常。

从现象上看，这不是普通的应用异常退出，而更像是服务进入了**假死状态**。

---

## 三、初步判断

服务假死常见原因包括：

1. JVM 发生严重 GC，应用长时间 Stop The World；
2. 堆内存持续上涨，最终触发 OOM；
3. 线程池被打满，业务线程阻塞；
4. 数据库或 Redis 连接池耗尽；
5. 死锁或大量线程等待锁；
6. 下游接口长时间无响应，导致请求线程堆积。

由于问题是在一晚上无人操作的情况下出现，并且白天频繁发版、重启时没有暴露出来，因此重点怀疑是**内存持续增长导致的 OOM 或频繁 Full GC**。

---

## 四、现场证据

查看服务启动参数中配置的 dump 目录后，发现 JVM 在当天 **9:40 左右生成了堆转储文件**。

这说明服务确实发生过严重内存问题，JVM 触发了堆转储。

随后将 dump 文件下载到本地，并使用 **MAT（Memory Analyzer Tool）** 进行分析。

---

## 五、MAT 分析过程

### 5.1 查看内存概览

打开 dump 文件后，在 MAT 概览页发现有一块对象占用内存非常高。

![MAT Overview](img.png)

这一步初步说明：堆内存中存在明显的大对象或异常引用链，需要继续分析对象支配关系。

---

### 5.2 查看 Leak Suspects

进入 **Leak Suspects** 页面后，发现 MAT 给出了一个主要内存泄漏疑点：

- 内存占比约 **64.9%**；
- 涉及关键类：
  - `org.redisson.connection.SingleConnectionManager`
  - `io.netty.util.concurrent.GenericFutureListener[]`

![Leak Suspects](img_1.png)

这说明大量对象被 `SingleConnectionManager` 间接持有，导致无法被 GC 回收。

---

### 5.3 查看泄漏线程堆栈

继续点击泄漏详情，查看线程堆栈信息。

![线程堆栈](img_2.png)

通过线程堆栈，定位到项目中的具体调用代码。

![业务代码定位 1](img_3.png)

![业务代码定位 2](img_4.png)

从调用链可以判断，问题与项目中使用的 **Redisson 延迟队列消费逻辑** 有关。

---

## 六、对象引用链分析

### 6.1 查看 Dominator Tree

为了确认对象为什么没有被回收，继续进入 MAT 的 **Dominator Tree** 页面，并按照 `Retained Heap` 从大到小排序。

结果发现一个占用内存非常大的对象：

```java
org.redisson.connection.SingleConnectionManager
```

![Dominator Tree](img_5.png)

`Retained Heap` 很大，说明这个对象持有了大量无法释放的下游对象。

---

### 6.2 查看外部引用

继续查看该对象的 **List Objects -> with outgoing references**，发现其引用链中存在一个非常大的数组：

```java
io.netty.util.concurrent.GenericFutureListener[]
```

该数组长度为：

```text
16384
```

其中已有元素数量为：

```text
13345
```

![GenericFutureListener 数组](img_6.png)

这说明大量 `GenericFutureListener` 被不断注册到了某个 Promise 对象上，但没有被及时移除，最终导致监听器数组持续膨胀。

---

## 七、源码排查

### 7.1 定位 Redisson 阻塞队列调用

通过本地 Debug 和源码跟踪，发现项目中使用 Redisson 延迟队列时，底层会使用阻塞队列进行任务消费。

业务代码大致如下：

```java
RBlockingQueue<ExamSubmitTask> blockingQueue =
        redissonClient.getBlockingQueue(queueName);

ExamSubmitTask task = blockingQueue.take();
```

在 Redisson 3.10.6 中，`RBlockingQueue.take()` 每次阻塞调用时，都会向 `shutdownPromise` 注册一个监听器。

相关调用链大致如下：

```text
RBlockingQueue.take()
    -> CommandAsyncService.handleBlockingOperations()
    -> shutdownPromise.addListener(...)
    -> GenericFutureListener[] 扩容并保存 listener
```

本地 Debug 跟踪过程如下：

![Debug 1](img_33.png)

![Debug 2](img_34.png)

![Debug 3](img_35.png)

![Debug 4](img_36.png)

![Debug 5](img_37.png)

---

### 7.2 发现监听器只添加不移除

继续查看 Redisson 3.10.6 源码后发现：

- 每次调用阻塞队列的 `take()` 方法，都会注册一个 listener；
- 但在正常业务调用完成后，并没有找到对应的 `removeListener` 逻辑；
- 这些 listener 会一直挂在 `shutdownPromise` 上；
- 只有在 Redisson 执行 shutdown 时，才会统一执行并释放这些 listener。

也就是说，只要应用持续运行，阻塞队列消费次数越多，`GenericFutureListener[]` 中积累的 listener 就越多。

最终结果如下：

```text
RBlockingQueue.take() 调用次数增加
        ↓
shutdownPromise 不断 addListener
        ↓
GenericFutureListener[] 持续扩容
        ↓
listener 引用链无法释放
        ↓
Promise / Listener / AsyncDetails 等对象无法被 GC
        ↓
老年代持续增长
        ↓
Full GC 频繁或 OOM
        ↓
服务假死
```

---

## 八、根因确认

去 GitHub 查看 Redisson 后续版本更新记录后，发现 **Redisson 3.10.7** 的 CHANGELOG 中明确提到：

```text
Fixed - memory leak during blocking methods invocation of Queue objects
```

也就是修复了 Queue 对象阻塞方法调用过程中的内存泄漏问题。

继续对比 Redisson 3.10.6 和 3.10.7 的源码，可以看到 `CommandAsyncService` 类中的 `handleBlockingOperations` 方法发生了变化。

### 修改前

![Redisson 3.10.6 修改前代码](img_10.png)

### 修改后

![Redisson 3.10.7 修改后代码](img_9.png)

核心变化是新增了类似下面的逻辑：

```java
shutdownPromise.removeListener(listener);
```

也就是在阻塞调用完成后，将之前注册到 `shutdownPromise` 上的 listener 移除。

因此，最终确认根因是：

> 项目使用的 Redisson 3.10.6 存在阻塞队列方法调用导致的内存泄漏问题。延迟队列消费逻辑长期运行后，`RBlockingQueue.take()` 不断向 `shutdownPromise` 注册 listener，但旧版本没有在调用完成后移除 listener，导致 `GenericFutureListener[]`、`RedissonPromise`、`AsyncDetails` 等对象长期无法被 GC 回收，最终引发堆内存持续增长并导致服务假死 / OOM。

---

## 九、本地复现

为了验证根因，随后在本地环境进行问题复现。

### 9.1 复现步骤

1. 启动项目；
2. 使用 VisualVM 实时观察堆内存和实例数量；
3. 创建约 **30000 个延迟任务**；
4. 等待延迟任务陆续执行完成；
5. 观察 JVM 对象实例数量和老年代内存变化；
6. 手动触发 Full GC，观察相关对象是否能够被回收。

---

### 9.2 测试前状态

测试前，VisualVM 中堆内存和实例直方图处于正常状态。

![测试前内存占比](img_11.png)

![测试前实例直方图](img_12.png)

![测试前堆内存状态](img_13.png)

---

### 9.3 测试后状态

创建并执行完 30000 个延迟任务后，观察到以下对象实例数量明显增加：

```java
io.netty.util.concurrent.ImmediateEventExecutor$ImmediatePromise
org.redisson.misc.RedissonPromise
org.redisson.command.CommandAsyncService$AsyncDetails
```

![测试后实例直方图](img_14.png)

![测试后内存占比](img_15.png)

同时观察到：

- 老年代内存明显上涨；
- 多次 Minor GC 后对象数量没有下降；
- 手动执行 Full GC 后，对象数量仍然没有明显变化；
- 相关 Promise、Listener、AsyncDetails 对象一直无法回收。

![任务执行后对象数量持续增加](img_16.png)

这说明这些对象不是短生命周期临时对象，而是被某条引用链长期持有，符合内存泄漏特征。

---

## 十、修复方案

### 10.1 升级 Redisson 版本

将 Redisson 从 `3.10.6` 升级到已修复该问题的版本。

最低修复版本：

```xml
<redisson.version>3.10.7</redisson.version>
```

不过实际升级时，不建议只机械地升级到 `3.10.7`，而是需要结合项目情况综合评估：

- Spring Boot 版本；
- Redis 版本；
- JDK 版本；
- Redisson 客户端配置；
- Codec 序列化方式；
- 线上已有 Redis 数据兼容性；
- 是否存在其他已知 Bug。

---

### 10.2 注意 Codec 序列化兼容问题

Redisson 升级时需要特别注意 **Codec 序列化兼容问题**。

如果 Redis 中已经存在使用旧 Codec 写入的数据，直接切换到不兼容的 Codec，可能导致旧数据无法反序列化。

因此升级前需要确认：

1. 当前项目显式配置了什么 Codec；
2. Redis 中是否存在需要长期保留的 Redisson 数据；
3. 新版本默认 Codec 是否发生变化；
4. 是否需要在升级后继续显式指定旧 Codec；
5. 是否需要清理旧延迟队列数据；
6. 是否需要编写兼容迁移脚本。

需要注意的是，原记录中的 `Kyro` 应修正为 `Kryo`。

更稳妥的表述是：

> 本次升级不只关注内存泄漏修复，还需要重点验证 Codec 兼容性。由于 Redisson 的数据需要经过 Codec 序列化后写入 Redis，如果升级前后 Codec 不一致，可能导致旧数据无法反序列化。因此升级时不能只修改版本号，还需要明确指定 Codec，并评估 Redis 中历史延迟队列数据是否需要清理或迁移。

---

### 10.3 修复验证

升级后建议重点验证以下内容：

1. 批量创建延迟任务后，任务是否能正常消费；
2. 延迟任务消费完成后，相关 Promise、Listener 对象是否能被 GC；
3. 长时间运行后，老年代是否持续上涨；
4. 多次 Full GC 后，异常对象是否能被回收；
5. Redis 中旧任务数据是否能够正常解析；
6. 服务重启后，延迟任务是否能够继续正常处理；
7. 是否存在任务重复执行或任务丢失问题。

---

## 十一、临时规避方案

在正式升级前，可以采取以下临时措施降低风险。

### 11.1 定期重启服务

定期重启测试环境服务可以短期释放泄漏对象，避免内存持续增长。

但这只能作为短期止血方案，不能根治问题。

---

### 11.2 减少延迟任务堆积量

控制测试环境批量造数规模，避免短时间内产生大量延迟任务。

---

### 11.3 增加 JVM 内存监控告警

重点监控：

- 堆内存使用率；
- 老年代使用率；
- Young GC 次数；
- Full GC 次数；
- Full GC 耗时；
- OOM dump 文件生成情况。

---

### 11.4 增加 Redisson 延迟队列消费监控

重点监控：

- 延迟任务生产数量；
- 延迟任务消费数量；
- 队列积压量；
- 消费线程是否存活；
- Redis 连接状态；
- 延迟任务执行失败数量。

---

## 十二、二次 OOM 补充说明

后续又发生了一次 OOM，相关 MAT 分析截图如下：

![第二次 OOM 分析 1](img_30.png)

![第二次 OOM 分析 2](img_31.png)

![第二次 OOM 分析 3](img_32.png)

这里不建议直接把第二次 OOM 和第一次问题混为一谈，除非满足以下条件：

1. Dominator Tree 中的最大对象仍然是 Redisson / Netty 相关对象；
2. 引用链仍然指向 `GenericFutureListener[]`、`RedissonPromise`、`AsyncDetails` 等对象；
3. 堆栈仍然能定位到 Redisson 阻塞队列调用；
4. 升级 Redisson 后该问题不再复现。

如果第二次 OOM 的最大对象、引用链或业务调用栈不同，则应该作为**新的 OOM 问题单独分析**。

推荐补充结论：

> 后续再次出现 OOM，因此继续对第二次 dump 文件进行分析。需要重点对比两次 dump 的 Dominator Tree、Leak Suspects 和 GC Roots 引用链，确认是否属于同一类 Redisson 阻塞队列 listener 泄漏问题。如果引用链一致，则说明旧版本 Redisson 问题在高频延迟任务场景下具有稳定复现性；如果引用链不同，则需要单独建立新的 OOM 排查结论，避免误归因。

---

## 十三、最终结论

### 13.1 直接表现

测试环境服务进程仍在，但接口不可用，服务表现为假死。

---

### 13.2 直接原因

JVM 堆内存持续增长，最终触发 OOM 或严重 Full GC，导致服务不可用。

---

### 13.3 根本原因

项目使用的 Redisson 3.10.6 版本存在阻塞队列方法调用导致的内存泄漏问题。

延迟队列消费逻辑中频繁调用 `RBlockingQueue.take()`，每次调用都会向 `shutdownPromise` 注册 listener，但旧版本没有在调用结束后移除 listener，导致 listener 数组和相关 Promise 对象持续堆积，最终引发内存泄漏。

---

### 13.4 修复方式

升级 Redisson 至包含该修复的版本，并在升级过程中重点验证：

- Codec 兼容性；
- 延迟队列历史数据兼容性；
- 长时间运行稳定性；
- 批量延迟任务压测结果；
- JVM 内存和 GC 指标。

---

## 十四、复盘改进项

### 14.1 技术改进

| 改进项 | 说明 |
|---|---|
| 升级 Redisson | 升级到修复内存泄漏问题的版本 |
| 固定 Codec 配置 | 避免升级后 Codec 变化导致历史数据不兼容 |
| 增加延迟队列监控 | 监控任务生产、消费、积压和失败情况 |
| 增加 JVM 监控 | 监控堆内存、老年代、GC、OOM dump |
| 增加压测验证 | 对延迟任务进行批量压测和长时间稳定性测试 |
| 增加版本风险评估 | 引入或升级中间件框架时检查 Release Notes 和 Issue |
| 增加故障现场保留流程 | 服务重启前优先保留线程栈、堆对象直方图和 heap dump |
| 增加回滚方案 | 升级中间件版本前准备可回滚版本和数据兼容方案 |

---

### 14.2 排查流程改进

本次问题中，重启服务虽然快速恢复了测试环境，但也可能破坏现场。

后续遇到服务假死问题时，建议先尽量保留现场信息。

常用命令如下：

```bash
# 查看 Java 进程
jps -l

# 导出线程栈
jstack <pid> > jstack.log

# 查看堆对象直方图
jmap -histo:live <pid> > histo.log

# 查看 GC 情况
jstat -gcutil <pid> 1000 10

# 必要时手动 dump
jmap -dump:format=b,file=heap.hprof <pid>
```

如果线上故障影响严重，可以先保留最小现场后再重启：

1. 先导出 `jstack`；
2. 再导出 `jmap histo`；
3. 如果条件允许，导出 heap dump；
4. 最后重启恢复服务。

---

## 十五、经验总结

### 15.1 服务假死不一定是 CPU 问题

服务进程还在，但接口不可用，可能是：

- OOM；
- Full GC；
- 线程池耗尽；
- 连接池耗尽；
- 死锁；
- 下游依赖阻塞。

不能只看进程是否存活，还要结合 JVM、线程、GC、连接池、日志综合判断。

---

### 15.2 使用开源框架不能只看功能是否满足

Redisson 延迟队列功能本身满足业务需求，但旧版本在阻塞队列场景下存在内存泄漏问题。

因此引入中间件能力时，需要关注：

1. 当前使用版本是否过旧；
2. 是否存在已知 Bug；
3. Release Notes 中是否有相关修复；
4. 是否经过压力测试；
5. 是否具备监控和降级手段；
6. 是否存在版本升级带来的兼容性问题。

---

### 15.3 内存泄漏排查要形成完整证据链

本次排查的证据链如下：

```text
服务假死
    ↓
发现 OOM dump
    ↓
MAT 查看 Leak Suspects
    ↓
发现 SingleConnectionManager 占用内存异常
    ↓
Dominator Tree 定位 GenericFutureListener[] 大量堆积
    ↓
查看 GC Roots 引用链
    ↓
定位到 Redisson 阻塞队列 take() 调用
    ↓
源码分析发现 listener 只添加不移除
    ↓
官方 3.10.7 CHANGELOG 证实该问题已修复
    ↓
本地批量延迟任务复现
    ↓
确认 Redisson 3.10.6 内存泄漏
```

这类证据链比单纯说 “Redisson 有 Bug” 更有说服力。

---

## 十六、总结

可以这样表达：

> 我之前在项目中负责考试自动交卷功能，需要实现延迟任务。调研后选择了 Redis + Redisson 延迟队列方案，因为它相比单机 DelayQueue 更适合分布式场景，也比定时任务扫描延迟更低。
>
> 后来测试环境出现过一次服务假死，进程还在但接口不可用。重启后恢复，但我怀疑是 JVM 内存问题，于是查看 dump 目录，发现确实生成了 heap dump。
>
> 我用 MAT 分析 dump，先看 Leak Suspects，发现 `SingleConnectionManager` 和 `GenericFutureListener[]` 占用了大量内存。然后通过 Dominator Tree 和 GC Roots 继续追踪，发现大量 listener 被挂在 Redisson 的 `shutdownPromise` 上无法释放。
>
> 接着我本地 Debug Redisson 源码，发现我们使用的 Redisson 3.10.6 中，`RBlockingQueue.take()` 每次调用都会向 `shutdownPromise` 注册 listener，但正常调用结束后没有移除。只有 Redisson shutdown 时才会统一释放，所以服务长期运行后 listener 会越积越多，最终导致 OOM。
>
> 后来我查 Redisson 的更新记录，发现 3.10.7 正好修复了 Queue 阻塞方法调用导致的内存泄漏问题。再对比源码，发现新版本在回调中增加了 `removeListener` 逻辑。最后我通过本地创建 30000 个延迟任务复现了对象无法回收的问题，验证了根因。
>
> 最终处理方案是升级 Redisson，同时评估 Codec 兼容性，避免升级后 Redis 中已有数据无法反序列化。这个问题也让我意识到，引入开源框架时不能只验证功能，还要关注版本 Bug、Release Notes、压测结果和线上监控。

---

## 十七、简短总结

本次事故的核心原因是：

> Redisson 3.10.6 在阻塞队列方法调用过程中存在内存泄漏问题，`RBlockingQueue.take()` 每次调用都会向 `shutdownPromise` 注册 listener，但调用结束后没有移除，导致 listener 和相关 Promise 对象持续堆积，最终造成 JVM 堆内存上涨，引发 OOM 或服务假死。

最终解决方案是：

> 升级 Redisson 到修复该问题的版本，并重点验证 Codec 兼容性、历史 Redis 数据兼容性、延迟任务压测结果和 JVM 内存稳定性。
