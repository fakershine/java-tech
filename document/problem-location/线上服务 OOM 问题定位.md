# 线上服务 OOM 问题定位

线上 OOM 排查核心思路：

```text
先确认 OOM 类型
再保留现场
再分析堆 / 线程 / GC
最后定位代码和修复
```

---

## 1. 常见 OOM 类型

| OOM 类型 | 常见原因 |
|---|---|
| `Java heap space` | 堆内存不足、对象过多、内存泄漏 |
| `GC overhead limit exceeded` | GC 频繁但回收效果差 |
| `Metaspace` | 类加载过多、动态代理类过多 |
| `Direct buffer memory` | 直接内存不足，常见于 Netty、NIO |
| `unable to create new native thread` | 线程数过多，系统无法创建新线程 |
| `Requested array size exceeds VM limit` | 创建超大数组 |
| `Out of swap space` | 操作系统内存不足 |

---

## 2. 第一时间保留现场

线上 OOM 后不要急着重启，先保留现场。

常用命令：

```bash
jps -l
jstack <pid> > jstack.log
jmap -heap <pid> > heap_info.log
jmap -histo:live <pid> > histo.log
jstat -gcutil <pid> 1000 10
```

如果服务还能导出堆：

```bash
jmap -dump:format=b,file=heap.hprof <pid>
```

建议 JVM 启动参数提前配置：

```bash
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/logs/heapdump
-XX:+PrintGCDetails
-Xloggc:/data/logs/gc.log
```

JDK 9+：

```bash
-Xlog:gc*:file=/data/logs/gc.log:time,uptime,level,tags
```

---

## 3. 查看 OOM 日志

先看应用日志和 JVM 日志中的异常。

例如：

```text
java.lang.OutOfMemoryError: Java heap space
```

或：

```text
java.lang.OutOfMemoryError: Metaspace
```

不同 OOM 类型对应不同排查方向。

---

## 4. Java Heap Space 排查

### 现象

```text
java.lang.OutOfMemoryError: Java heap space
```

说明堆内存不够。

常见原因：

- 一次查询大量数据。
- 大对象创建过多。
- 集合无限增长。
- 缓存没有过期策略。
- 消息消费堆积在内存。
- ThreadLocal 未清理。
- 静态 Map 持有对象。
- 大文件一次性读入内存。

### 排查方式

导出堆：

```bash
jmap -dump:format=b,file=heap.hprof <pid>
```

用 MAT / VisualVM / JProfiler 分析：

```text
Dominator Tree
Leak Suspects
Histogram
GC Roots
```

重点看：

```text
哪个对象占用最大
哪个集合持有最多对象
对象是被谁引用的
是否被 static / ThreadLocal / 缓存持有
```

---

## 5. GC overhead limit exceeded

### 现象

```text
java.lang.OutOfMemoryError: GC overhead limit exceeded
```

说明 JVM 大部分时间都在 GC，但回收效果很差。

常见原因：

```text
堆接近打满
对象持续增长
Full GC 频繁
内存泄漏
```

排查：

```bash
jstat -gcutil <pid> 1000
```

重点看：

```text
Old 区是否长期接近 100%
Full GC 是否频繁
Full GC 后内存是否下降
```

如果 Full GC 后内存降不下来：

```text
大概率存在内存泄漏或对象被长期引用
```

---

## 6. Metaspace OOM

### 现象

```text
java.lang.OutOfMemoryError: Metaspace
```

Metaspace 存放类元数据。

常见原因：

- 动态生成类过多。
- CGLIB / Javassist 代理类过多。
- Groovy / Janino 动态脚本加载类过多。
- 类加载器泄漏。
- 热部署反复加载类。
- Metaspace 配置太小。

排查方向：

```text
查看类加载数量
查看 ClassLoader 是否持续增长
检查动态代理、脚本、表达式引擎
```

常用命令：

```bash
jcmd <pid> VM.classloader_stats
jcmd <pid> GC.class_stats
```

可优化参数：

```bash
-XX:MaxMetaspaceSize=512m
```

但重点还是找类加载泄漏原因。

---

## 7. Direct Buffer Memory OOM

### 现象

```text
java.lang.OutOfMemoryError: Direct buffer memory
```

常见于：

```text
Netty
NIO
ByteBuffer.allocateDirect()
文件传输
高并发网络通信
```

常见原因：

- 直接内存设置太小。
- DirectByteBuffer 未及时释放。
- Netty ByteBuf 泄漏。
- 连接数过多。
- 大量堆外缓冲区未释放。

排查方向：

```text
检查 Netty leak 日志
检查 direct memory 配置
检查 ByteBuf 是否 release
检查连接数和请求体大小
```

相关参数：

```bash
-XX:MaxDirectMemorySize=512m
```

Netty 可开启泄漏检测：

```bash
-Dio.netty.leakDetection.level=advanced
```

---

## 8. Unable to Create New Native Thread

### 现象

```text
java.lang.OutOfMemoryError: unable to create new native thread
```

说明 JVM 想创建新线程，但操作系统资源不足。

常见原因：

- 线程创建过多。
- 线程池配置不合理。
- 没有限制异步任务。
- 请求线程阻塞堆积。
- 系统线程数限制太小。
- 每个线程栈内存过大。

排查命令：

```bash
ps -eLf | grep <pid> | wc -l
jstack <pid> > jstack.log
ulimit -u
```

重点看：

```text
线程数量
线程状态
线程池名称
是否大量 WAITING / BLOCKED
```

优化方向：

```text
控制线程池大小
减少无限创建线程
降低 -Xss
排查阻塞调用
调整系统线程限制
```

---

## 9. ThreadLocal 内存泄漏

### 典型场景

在线程池中使用 `ThreadLocal` 后没有 `remove()`。

```java
private static final ThreadLocal<UserContext> LOCAL = new ThreadLocal<>();

try {
    LOCAL.set(userContext);
    // 业务逻辑
} finally {
    LOCAL.remove();
}
```

为什么容易泄漏：

```text
线程池线程长期存活
ThreadLocalMap 中 value 被线程长期持有
如果不 remove，用户上下文无法释放
```

常见泄漏对象：

```text
用户信息
大对象
数据库连接
请求上下文
TraceContext
```

---

## 10. 缓存导致 OOM

常见问题：

```text
本地 Map 无限增长
Caffeine 没有限制大小
Guava Cache 没有过期策略
静态集合保存业务数据
热点数据不断加入但不淘汰
```

错误示例：

```java
private static final Map<String, Object> CACHE = new HashMap<>();
```

优化：

```text
设置最大容量
设置过期时间
使用 Caffeine / Redis
定期清理
避免把大对象放本地缓存
```

---

## 11. 一次性加载大量数据

常见问题：

```text
select * 查全表
导出 Excel 一次加载全部数据
大文件一次读入内存
分页参数失效
接口返回超大列表
```

优化：

```text
分页查询
游标查询
流式处理
分批导出
限制最大查询范围
大文件分片处理
```

---

## 12. OOM 排查流程

```text
1. 查看日志，确认 OOM 类型。
2. 保留现场：jstack、jmap、jstat、GC 日志。
3. 如果是堆 OOM，导出 heap dump。
4. 用 MAT 分析大对象和引用链。
5. 查看 GC 日志，判断是否频繁 Full GC。
6. 如果是 Direct Memory，排查 Netty / NIO / ByteBuffer。
7. 如果是 Metaspace，排查动态类和 ClassLoader。
8. 如果是 native thread，排查线程数量和线程池。
9. 定位代码后修复。
10. 压测验证，增加监控告警。
```

---

## 13. 临时止血方案

线上已经 OOM 时，可以先止血：

```text
重启异常实例
临时扩容实例
限流
关闭大流量接口
降级非核心功能
回滚最近版本
清理异常缓存
调大 JVM 内存参数
```

注意：

```text
重启前尽量保留 heap dump、jstack、GC 日志。
```

---

## 14. 常见优化手段

| 问题 | 优化方式 |
|---|---|
| 堆内存不足 | 分析 heap dump，减少对象持有 |
| Full GC 频繁 | 优化对象生命周期，调整堆大小 |
| 本地缓存过大 | 设置最大容量和过期时间 |
| ThreadLocal 泄漏 | finally 中 remove |
| 查询数据过多 | 分页、流式、限制范围 |
| 大文件处理 | 分片、流式读写 |
| 线程过多 | 规范线程池，限制队列和最大线程数 |
| 直接内存泄漏 | 检查 ByteBuf release，配置 MaxDirectMemorySize |
| Metaspace OOM | 排查动态类加载和 ClassLoader 泄漏 |

---

## 15. 总结

线上 OOM 排查时，我会先看异常类型，因为不同 OOM 对应不同方向。如果是 `Java heap space`，重点分析堆内对象，导出 heap dump，用 MAT 查看大对象、引用链和 GC Roots；如果是 `GC overhead limit exceeded`，重点看 Full GC 是否频繁以及 GC 后内存是否下降；如果是 `Metaspace`，排查动态类加载和 ClassLoader 泄漏；如果是 `Direct buffer memory`，排查 Netty、NIO 和堆外内存；如果是 `unable to create new native thread`，排查线程数、线程池和系统线程限制。

一句话总结：

```text
OOM 排查 = 先看 OOM 类型 + 保留现场 + 分析 heap dump / GC / jstack + 定位引用链和异常增长对象。
```
