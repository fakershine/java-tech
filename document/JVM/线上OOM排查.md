# 线上 OOM 排查总结

线上 OOM 排查的核心思路是：**先保留现场，再判断 OOM 类型，最后结合日志、Dump、监控和代码定位原因**。

---

## 1. OOM 常见类型

| OOM 类型 | 常见原因 |
|---|---|
| `Java heap space` | 堆内存不足、对象无法回收、内存泄漏 |
| `GC overhead limit exceeded` | GC 频繁但回收效果很差 |
| `Metaspace` | 类加载过多、动态代理类过多、类加载器泄漏 |
| `Direct buffer memory` | 直接内存不足，常见于 Netty、NIO |
| `unable to create new native thread` | 线程数过多、线程池配置不合理 |
| `Requested array size exceeds VM limit` | 创建超大数组或集合 |

---

## 2. 线上 OOM 排查流程

```text
发现 OOM 告警
  ↓
保留现场日志和 Dump 文件
  ↓
确认 OOM 类型
  ↓
查看 JVM 参数和内存配置
  ↓
分析 GC 日志
  ↓
分析 Heap Dump / Thread Dump
  ↓
定位大对象、泄漏对象、线程异常
  ↓
修复代码或调整参数
  ↓
压测验证并上线
```

---

## 3. 第一步：保留现场

线上发生 OOM 后，第一件事不是直接重启，而是尽量保留现场。

### 建议 JVM 参数

```bash
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/dump/
-XX:ErrorFile=/data/logs/hs_err_pid%p.log
```

### GC 日志

JDK 8：

```bash
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:/data/logs/gc.log
```

JDK 9+：

```bash
-Xlog:gc*:file=/data/logs/gc.log:time,uptime,level,tags
```

### 如果服务还没挂，可以手动导出

```bash
jmap -dump:format=b,file=/data/dump/heap.hprof pid
```

查看对象统计：

```bash
jmap -histo:live pid | head -50
```

查看线程栈：

```bash
jstack pid > /data/dump/thread.log
```

---

## 4. 第二步：确认 OOM 类型

从应用日志或 JVM 错误日志中找到异常信息。

例如：

```text
java.lang.OutOfMemoryError: Java heap space
```

然后根据 OOM 类型选择排查方向。

---

## 5. Java heap space 排查

### 常见原因

- 大集合无限增长。
- 本地缓存无上限。
- 一次查询返回大量数据。
- MQ 消费积压在内存中。
- ThreadLocal 未清理。
- 对象被静态变量长期引用。
- 大文件一次性读入内存。

### 排查方式

使用 MAT、VisualVM、JProfiler 分析 Heap Dump。

重点看：

```text
Dominator Tree
Retained Heap
Leak Suspects
对象引用链 GC Roots
```

### 常见现象

```text
某个 Map/List 占用大量内存
某个业务对象数量异常多
某个缓存对象无法释放
某个 ThreadLocalMap 持有大量 value
```

### 解决方式

- 给缓存设置容量和过期时间。
- 大查询改为分页查询。
- 大文件改为流式处理。
- MQ 消费限流，避免一次拉太多。
- ThreadLocal 使用完必须 `remove()`。
- 避免静态集合无限增长。

---

## 6. GC overhead limit exceeded 排查

### 问题含义

JVM 花大量时间做 GC，但只回收了很少内存。

```text
GC 很频繁
  ↓
每次回收效果很差
  ↓
应用几乎无法正常执行
```

### 常见原因

- 堆内存接近打满。
- 内存泄漏。
- 老年代对象过多。
- 分配速率过高。

### 排查方式

查看 GC 日志：

```text
Full GC 是否频繁
Full GC 后老年代是否仍然很高
```

如果 Full GC 后内存下降很少，通常说明：

```text
大量对象仍被引用，可能存在内存泄漏
```

### 解决方式

- 分析 Heap Dump。
- 找到无法释放的对象。
- 优化对象生命周期。
- 必要时调整堆大小。

---

## 7. Metaspace OOM 排查

### 常见异常

```text
java.lang.OutOfMemoryError: Metaspace
```

### 常见原因

- 动态生成类过多。
- CGLIB/JDK 动态代理大量创建。
- Groovy、Janino、反射生成类过多。
- 热部署导致类加载器无法卸载。
- ClassLoader 泄漏。

### 排查方式

查看类加载数量：

```bash
jstat -class pid 1000 10
```

查看 JVM 参数：

```bash
jcmd pid VM.flags
```

使用 MAT 分析是否存在大量 ClassLoader。

### 解决方式

- 设置合理元空间大小：

```bash
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m
```

- 排查动态生成类是否无限增长。
- 避免频繁创建代理类。
- 解决 ClassLoader 泄漏。

---

## 8. Direct buffer memory 排查

### 常见异常

```text
java.lang.OutOfMemoryError: Direct buffer memory
```

### 常见原因

- Netty 直接内存使用过多。
- NIO `ByteBuffer.allocateDirect()` 未释放。
- 直接内存限制太小。
- 堆外内存泄漏。

### 排查方式

查看直接内存参数：

```bash
jcmd pid VM.flags
```

查看 Native Memory：

```bash
jcmd pid VM.native_memory summary
```

前提是启动参数开启：

```bash
-XX:NativeMemoryTracking=summary
```

### 解决方式

- 设置直接内存大小：

```bash
-XX:MaxDirectMemorySize=1g
```

- 检查 Netty ByteBuf 是否释放。
- 避免无限制分配 DirectByteBuffer。
- 使用池化并正确释放资源。

---

## 9. unable to create new native thread 排查

### 常见异常

```text
java.lang.OutOfMemoryError: unable to create new native thread
```

### 常见原因

- 线程创建过多。
- 线程池最大线程数设置过大。
- 使用 `new Thread()` 无限制创建线程。
- 系统线程数限制过低。
- 容器内存不足。

### 排查方式

查看线程数量：

```bash
jstack pid > thread.log
```

或者：

```bash
ps -eLf | grep java | wc -l
```

查看系统限制：

```bash
ulimit -u
```

查看线程栈，确认线程都在做什么：

```text
大量 WAITING
大量 BLOCKED
大量业务线程池线程
大量 HTTP/RPC 客户端线程
```

### 解决方式

- 使用线程池，不要无限创建线程。
- 线程池设置合理最大线程数和有界队列。
- 排查线程泄漏。
- 调整系统线程数限制。
- 控制容器内存和线程栈大小。

---

## 10. ThreadLocal 导致 OOM

### 问题原因

线程池线程长期存活，如果 ThreadLocal 使用后没有清理，value 可能一直挂在线程上。

```text
Thread
  ↓
ThreadLocalMap
  ↓
Entry(key=null, value=大对象)
```

### 常见场景

- 用户上下文未清理。
- TraceId 未清理。
- 大对象放入 ThreadLocal。
- 在线程池中复用线程。

### 解决方式

必须在 `finally` 中清理：

```java
try {
    USER_CONTEXT.set(user);
    // 业务逻辑
} finally {
    USER_CONTEXT.remove();
}
```

---

## 11. 本地缓存导致 OOM

### 常见原因

- 使用 `Map` 做缓存但不限制大小。
- 缓存没有过期时间。
- Key 维度过细，持续增长。
- 缓存大对象。

### 错误示例

```java
private static final Map<String, Object> CACHE = new HashMap<>();
```

### 推荐方式

使用 Caffeine：

```java
Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build();
```

---

## 12. 大查询导致 OOM

### 常见问题

一次性查询大量数据：

```sql
SELECT * FROM order_info;
```

或者导出大量数据时一次性放入 List：

```java
List<Order> list = orderMapper.selectAll();
```

### 解决方式

- 分页查询。
- 游标查询。
- 流式处理。
- 分批导出。
- 限制最大导出数量。

---

## 13. 消息积压导致 OOM

### 常见问题

消费者一次拉取太多消息，或者消息先堆到内存队列中再处理。

```text
Kafka 拉取大量消息
  ↓
放入内存队列
  ↓
消费速度跟不上
  ↓
内存不断增长
  ↓
OOM
```

### 解决方式

- 控制 `max.poll.records`。
- 使用有界队列。
- 线程池设置拒绝策略。
- 消费端限流。
- 下游慢时暂停消费或降级。
- 不要无限堆积内存队列。

---

## 14. 线上应急处理

### 如果服务已经不可用

可以先：

```text
保存日志和 Dump
  ↓
临时重启恢复服务
  ↓
保留现场文件离线分析
```

### 如果 OOM 频繁发生

临时措施：

- 扩大堆内存。
- 降低流量。
- 关闭非核心功能。
- 限制接口并发。
- 暂停大查询、大导出。
- 降低 MQ 拉取数量。
- 清理异常缓存。

注意：

```text
扩容只能缓解，不一定解决根因。
```

---

## 15. 常用工具

| 工具 | 作用 |
|---|---|
| `jstat` | 查看 GC 和内存变化 |
| `jmap` | 导出堆 Dump、查看对象统计 |
| `jstack` | 查看线程栈 |
| `jcmd` | JVM 综合诊断 |
| MAT | 分析 Heap Dump |
| VisualVM | 可视化分析内存和线程 |
| Arthas | 在线诊断 Java 应用 |
| GCEasy | 分析 GC 日志 |

---

## 16. 总结

线上 OOM 排查首先要保留现场，包括应用日志、GC 日志、Heap Dump 和 Thread Dump。然后根据异常类型判断方向，比如 `Java heap space` 重点分析堆内对象和引用链，`Metaspace` 重点分析类加载和动态代理，`Direct buffer memory` 重点分析堆外内存，`unable to create new native thread` 重点分析线程数量和线程池。

排查堆 OOM 时，一般使用 MAT 分析 Dump 文件，重点查看大对象、对象数量、Dominator Tree、Retained Heap 和 GC Roots 引用链。常见原因包括本地缓存无限增长、ThreadLocal 未清理、大查询、消息积压、静态集合持有对象等。

解决 OOM 不能只靠加内存，应该从代码、缓存、查询、线程池、消息消费和 JVM 参数多个方面处理。

一句话总结：

```text
线上 OOM 排查 = 保留现场 + 确认类型 + 分析 Dump/GC/线程 + 定位引用链 + 修复根因。
```
