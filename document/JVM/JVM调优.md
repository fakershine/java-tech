# JVM 调优总结

JVM 调优的核心目标是：**减少 Full GC、降低 GC 停顿时间、提高系统吞吐量、避免 OOM、提升服务稳定性**。

---

## 1. JVM 调优整体思路

```text
先观察现象
  ↓
收集 JVM 指标
  ↓
分析 GC 日志
  ↓
分析内存、线程、对象分布
  ↓
定位问题原因
  ↓
调整代码 / JVM 参数 / 系统架构
```

JVM 调优不是一上来就改参数，而是先定位问题。

---

## 2. JVM 调优关注指标

| 指标 | 说明 |
|---|---|
| 堆内存使用率 | 老年代、新生代使用情况 |
| GC 次数 | Young GC、Full GC 频率 |
| GC 耗时 | 每次 GC 停顿时间 |
| 对象分配速率 | 对象创建是否过快 |
| 老年代增长速度 | 是否存在内存泄漏 |
| 线程数量 | 是否线程过多 |
| CPU 使用率 | 是否 GC 或业务线程导致 CPU 高 |
| 类加载数量 | 是否存在类加载泄漏 |
| 直接内存 | Netty、NIO 是否占用过高 |

---

## 3. 常见 JVM 问题

### 1. 频繁 Young GC

常见原因：

- 新生代太小。
- 短生命周期对象太多。
- 大量临时对象频繁创建。
- 接口 QPS 过高，对象分配速度太快。

优化方向：

- 适当增大新生代。
- 减少临时对象创建。
- 优化代码中的大对象、集合、字符串拼接。
- 使用对象复用要谨慎，避免引入复杂度。

---

### 2. 频繁 Full GC

常见原因：

- 老年代空间不足。
- 大对象直接进入老年代。
- 内存泄漏。
- 元空间不足。
- System.gc() 被频繁调用。
- 新生代对象晋升过快。

优化方向：

- 分析老年代对象来源。
- 检查是否存在内存泄漏。
- 调整堆大小和新老年代比例。
- 禁止显式 GC。
- 优化大对象和缓存使用。

---

### 3. OOM

常见类型：

| OOM 类型 | 原因 |
|---|---|
| Java heap space | 堆内存不足或内存泄漏 |
| Metaspace | 类加载过多或动态生成类过多 |
| Direct buffer memory | 直接内存不足 |
| Unable to create new native thread | 线程数过多 |
| GC overhead limit exceeded | GC 频繁但回收效果差 |

---

## 4. 常用 JVM 参数

### 堆内存参数

```bash
-Xms2g
-Xmx2g
```

说明：

| 参数 | 作用 |
|---|---|
| `-Xms` | 初始堆大小 |
| `-Xmx` | 最大堆大小 |

生产环境通常建议：

```text
-Xms 和 -Xmx 设置为一样
```

这样可以避免堆动态扩容带来的性能抖动。

---

### 新生代参数

```bash
-Xmn1g
```

或者：

```bash
-XX:NewRatio=2
```

说明：

| 参数 | 作用 |
|---|---|
| `-Xmn` | 新生代大小 |
| `-XX:NewRatio` | 老年代和新生代比例 |

如果 Young GC 过于频繁，可以适当增大新生代。

---

### 元空间参数

```bash
-XX:MetaspaceSize=256m
-XX:MaxMetaspaceSize=512m
```

说明：

| 参数 | 作用 |
|---|---|
| `MetaspaceSize` | 触发 Full GC 的初始元空间阈值 |
| `MaxMetaspaceSize` | 最大元空间大小 |

如果使用大量动态代理、反射、CGLIB，需关注元空间。

---

### GC 日志参数

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

建议生产环境必须开启 GC 日志。

---

### OOM 自动 Dump

```bash
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/dump/
```

作用：

```text
发生 OOM 时自动导出堆快照，方便后续分析。
```

---

### 禁止显式 GC

```bash
-XX:+DisableExplicitGC
```

作用：

```text
禁止代码中 System.gc() 触发 Full GC。
```

---

## 5. 常见垃圾收集器

### Serial GC

单线程垃圾收集器。

适用场景：

- 单核机器。
- 小内存应用。
- 客户端程序。

生产服务一般很少使用。

---

### Parallel GC

吞吐量优先的垃圾收集器。

特点：

- 多线程 GC。
- 关注整体吞吐量。
- 停顿时间可能较长。

适用场景：

- 后台任务。
- 批处理系统。
- 对吞吐量要求高、对延迟不敏感的系统。

---

### CMS GC

低延迟垃圾收集器，主要用于老年代回收。

特点：

- 并发标记。
- 并发清理。
- 减少 STW 时间。

劣势：

- 会产生内存碎片。
- 对 CPU 资源敏感。
- JDK 9 后被废弃。

---

### G1 GC

JDK 8 以后常用的服务端垃圾收集器。

特点：

- 面向低延迟。
- 将堆划分为多个 Region。
- 可以设置期望停顿时间。
- 适合大堆内存。

常用参数：

```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
```

适用场景：

- 大多数 Java 服务端应用。
- 中大堆内存服务。
- 对延迟有一定要求的系统。

---

### ZGC / Shenandoah

超低延迟垃圾收集器。

特点：

- 停顿时间极短。
- 适合大内存、低延迟场景。
- JDK 11+、JDK 17+ 使用较多。

适用场景：

- 大堆内存。
- 低延迟服务。
- 对 GC 停顿非常敏感的系统。

---

## 6. GC 调优思路

### 1. Young GC 调优

目标：

```text
Young GC 不要太频繁，单次耗时可接受。
```

优化方向：

- 增大新生代。
- 减少对象创建。
- 减少短时间大批量对象分配。
- 优化集合初始化容量。
- 避免频繁创建大数组、大字符串。

---

### 2. Full GC 调优

目标：

```text
尽量避免频繁 Full GC。
```

优化方向：

- 增大老年代。
- 分析是否存在内存泄漏。
- 避免大对象直接进入老年代。
- 避免缓存无限增长。
- 检查元空间是否不足。
- 禁止显式调用 `System.gc()`。

---

### 3. 停顿时间调优

如果使用 G1，可以设置：

```bash
-XX:MaxGCPauseMillis=200
```

但注意：

```text
停顿时间目标不是越小越好。
```

目标太小可能导致 GC 过于频繁，影响吞吐量。

---

## 7. 常用排查工具

| 工具 | 作用 |
|---|---|
| `jps` | 查看 Java 进程 |
| `jstat` | 查看 GC 和内存变化 |
| `jmap` | 导出堆快照、查看对象分布 |
| `jstack` | 查看线程栈 |
| `jcmd` | 综合诊断工具 |
| MAT | 分析 heap dump |
| VisualVM | 可视化分析 JVM |
| Arthas | 在线诊断 Java 应用 |
| GCViewer / GCEasy | 分析 GC 日志 |

---

## 8. 常用排查命令

### 查看 Java 进程

```bash
jps -l
```

---

### 查看 GC 情况

```bash
jstat -gcutil pid 1000 10
```

表示每 1 秒打印一次，共打印 10 次。

重点看：

```text
YGC：Young GC 次数
YGCT：Young GC 总耗时
FGC：Full GC 次数
FGCT：Full GC 总耗时
O：老年代使用率
M：元空间使用率
```

---

### 查看堆内对象统计

```bash
jmap -histo:live pid | head -20
```

---

### 导出堆 Dump

```bash
jmap -dump:format=b,file=/data/dump/heap.hprof pid
```

导出后可以用 MAT 分析。

---

### 查看线程栈

```bash
jstack pid > thread.log
```

用于排查：

- 死锁。
- 线程阻塞。
- CPU 高。
- 线程池打满。
- 大量 WAITING / BLOCKED 线程。

---

## 9. OOM 排查思路

### 1. 堆内存 OOM

现象：

```text
java.lang.OutOfMemoryError: Java heap space
```

排查：

```text
查看 heap dump
  ↓
分析大对象
  ↓
分析对象引用链
  ↓
判断是否内存泄漏
```

常见原因：

- 大集合无限增长。
- 本地缓存无上限。
- ThreadLocal 未清理。
- 查询一次返回大量数据。
- 消息积压在内存中。

---

### 2. 元空间 OOM

现象：

```text
java.lang.OutOfMemoryError: Metaspace
```

常见原因：

- 动态生成类过多。
- CGLIB 代理类过多。
- 类加载器无法卸载。
- 热部署导致类加载泄漏。

优化：

```bash
-XX:MaxMetaspaceSize=512m
```

同时要排查类加载是否异常增长。

---

### 3. 直接内存 OOM

现象：

```text
java.lang.OutOfMemoryError: Direct buffer memory
```

常见原因：

- Netty 使用直接内存过多。
- NIO ByteBuffer 未释放。
- 直接内存参数设置过小。

参数：

```bash
-XX:MaxDirectMemorySize=1g
```

---

### 4. 线程数过多 OOM

现象：

```text
java.lang.OutOfMemoryError: unable to create new native thread
```

常见原因：

- 线程池配置不合理。
- 创建线程没有上限。
- 线程泄漏。
- 系统最大线程数限制过小。

排查：

```bash
jstack pid
```

查看线程数量和线程状态。

---

## 10. CPU 过高排查思路

流程：

```text
top 找到高 CPU Java 进程
  ↓
top -Hp pid 找到高 CPU 线程
  ↓
线程 ID 转 16 进制
  ↓
jstack 查找对应 nid
  ↓
定位具体代码
```

命令：

```bash
top
top -Hp pid
printf "%x\n" 线程ID
jstack pid | grep nid -A 30
```

常见原因：

- 死循环。
- 频繁 GC。
- 正则回溯。
- 大量 JSON 序列化。
- 加密解密计算。
- 线程池过多导致上下文切换。

---

## 11. 内存泄漏常见场景

| 场景 | 说明 |
|---|---|
| 静态集合 | static Map/List 持续增长 |
| 本地缓存 | 缓存无过期、无容量限制 |
| ThreadLocal | 使用后未 remove |
| 监听器未注销 | 对象一直被引用 |
| 连接未关闭 | IO、数据库连接泄漏 |
| 大对象引用 | 大对象被长生命周期对象持有 |
| 消息堆积 | 消息暂存在内存中无法释放 |

---

## 12. 线上 JVM 参数示例

### 普通 Spring Boot 服务示例

```bash
-Xms2g
-Xmx2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/dump/
-Xlog:gc*:file=/data/logs/gc.log:time,uptime,level,tags
-XX:+DisableExplicitGC
```

JDK 8 GC 日志写法：

```bash
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:/data/logs/gc.log
```

---

## 13. JVM 调优注意点

- 不要没有问题就盲目调 JVM 参数。
- 先优化代码，再调 JVM。
- 生产环境必须开启 GC 日志。
- `-Xms` 和 `-Xmx` 建议设置一致。
- 避免大对象频繁创建。
- 避免本地缓存无限增长。
- 避免长时间持有对象引用。
- 避免频繁 Full GC。
- 调优前后要对比监控数据。
- 参数调整必须经过压测验证。

---

## 14. 总结

JVM 调优一般先从现象入手，比如接口变慢、CPU 飙高、频繁 Full GC、OOM 等。然后通过 GC 日志、`jstat`、`jmap`、`jstack`、Arthas、MAT 等工具分析问题。

如果是频繁 Young GC，通常是对象创建过快或新生代太小；如果是频繁 Full GC，重点排查老年代是否增长过快、是否存在内存泄漏、大对象、元空间不足或显式 GC。

JVM 参数中常见的是 `-Xms`、`-Xmx`、`-Xmn`、`MetaspaceSize`、`MaxMetaspaceSize`、GC 收集器参数和 GC 日志参数。生产环境一般建议 `-Xms` 和 `-Xmx` 设置一致，并开启 GC 日志和 OOM 自动 Dump。

实际调优时，优先优化代码和对象生命周期，再根据 GC 日志和监控数据调整堆大小、新生代比例和垃圾收集器。

一句话总结：

```text
JVM 调优 = 监控指标 + GC 日志 + Dump 分析 + 代码优化 + 参数调整。
```
