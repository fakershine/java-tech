# Arthas 排查 CPU 100% 和 OOM

Arthas 适合线上快速定位：

```text
CPU 高：定位哪个线程、哪段代码占用 CPU
OOM：定位内存使用情况、导出堆文件、分析大对象
```

---

# 一、Arthas 排查 CPU 100%

## 1. 查看整体状态

```bash
dashboard
```

重点看：

```text
CPU 使用率
线程数量
GC 情况
内存使用情况
```

如果 CPU 很高，继续定位高 CPU 线程。

---

## 2. 查看 CPU 最高的线程

```bash
thread -n 10
```

作用：

```text
查看 CPU 占用最高的前 10 个线程
```

重点看：

```text
线程 ID
CPU 占用
线程状态
线程栈
```

---

## 3. 查看指定线程栈

```bash
thread 线程ID
```

如果某个线程 CPU 很高，查看它正在执行哪段代码。

常见问题：

```text
死循环
复杂计算
正则回溯
JSON 序列化大对象
大量日志打印
频繁重试
```

---

## 4. 查看是否死锁

```bash
thread -b
```

作用：

```text
查看当前阻塞其他线程的线程
```

适合排查：

```text
synchronized 死锁
ReentrantLock 锁竞争
线程长时间 BLOCKED
```

---

## 5. 生成 CPU 火焰图

```bash
profiler start
```

运行一段时间后停止：

```bash
profiler stop --format html
```

作用：

```text
生成 CPU 火焰图，查看 CPU 主要消耗在哪些方法上
```

适合定位：

```text
CPU 热点方法
高频调用方法
复杂计算逻辑
```

---

## 6. 分析方法耗时

如果怀疑某个接口或方法慢，可以用：

```bash
trace com.demo.OrderService createOrder
```

只看耗时超过 100ms 的调用：

```bash
trace com.demo.OrderService createOrder '#cost > 100' -n 5
```

作用：

```text
查看方法内部每一步耗时
```

---

## 7. 查看方法参数和返回值

```bash
watch com.demo.OrderService createOrder '{params, returnObj, #cost}' -x 3 -n 5
```

只看耗时超过 100ms 的请求：

```bash
watch com.demo.OrderService createOrder '{params, returnObj, #cost}' '#cost > 100' -x 3 -n 5
```

作用：

```text
确认是不是某些特殊参数导致 CPU 高
```

---

## 8. CPU 100% 排查流程

```text
1. dashboard 查看整体状态。
2. thread -n 10 找 CPU 最高线程。
3. thread 线程ID 查看线程栈。
4. 判断是否死循环、复杂计算、锁竞争、频繁 GC。
5. profiler 生成火焰图确认 CPU 热点。
6. trace / watch 进一步定位具体方法和参数。
```

---

# 二、Arthas 排查 OOM

## 1. 查看内存整体情况

```bash
memory
```

重点看：

```text
heap
old
eden
survivor
metaspace
direct memory
```

判断是哪块内存压力大。

---

## 2. 查看 JVM 信息

```bash
jvm
```

重点看：

```text
JVM 参数
堆大小
GC 类型
启动参数
系统属性
```

关注是否配置了：

```text
-Xmx
-Xms
-XX:MaxMetaspaceSize
-XX:MaxDirectMemorySize
-XX:+HeapDumpOnOutOfMemoryError
```

---

## 3. 查看 GC 情况

```bash
dashboard
```

重点看：

```text
YGC 次数
FGC 次数
GC 耗时
堆内存是否持续上涨
```

如果 Full GC 频繁，并且 Old 区回收不下来，可能存在内存泄露。

---

## 4. 导出堆文件

```bash
heapdump /tmp/heap.hprof
```

作用：

```text
导出 JVM 堆内存文件
```

然后用：

```text
MAT
VisualVM
JProfiler
```

分析大对象和引用链。

---

## 5. 查看类加载情况

如果是 Metaspace OOM，可以看类加载信息：

```bash
classloader
```

查看某个类是否被重复加载：

```bash
sc -d com.demo.OrderService
```

常见原因：

```text
动态代理类过多
CGLIB 类过多
脚本动态编译
类加载器泄露
热部署重复加载类
```

---

## 6. 查看大对象来源

Arthas 本身不适合直接完整分析 heap dump，大对象分析更推荐：

```text
heapdump + MAT
```

MAT 重点看：

```text
Dominator Tree
Histogram
Path To GC Roots
Leak Suspects
```

常见泄露对象：

```text
static Map
ThreadLocal
本地缓存
线程池队列
大 List
大 byte[]
大 String
未释放监听器
```

---

## 7. OOM 前线程是否异常

OOM 也可能由线程过多导致。

查看线程：

```bash
thread
```

查看线程数量和状态：

```bash
dashboard
```

如果是：

```text
java.lang.OutOfMemoryError: unable to create new native thread
```

重点排查：

```text
线程池无限创建
异步任务过多
线程阻塞堆积
-Xss 设置过大
系统线程数限制
```

---

## 8. OOM 排查流程

```text
1. memory 查看哪块内存占用高。
2. dashboard 查看 GC 是否频繁。
3. jvm 查看 JVM 参数。
4. heapdump 导出堆文件。
5. 用 MAT 分析大对象和引用链。
6. 如果是 Metaspace，看 classloader 和动态类。
7. 如果是 native thread，看线程数量和线程池。
8. 定位代码后修复泄露点。
```

---

# 三、常见命令总结

| 场景 | 命令 | 作用 |
|---|---|---|
| 查看整体状态 | `dashboard` | CPU、内存、线程、GC |
| 查看高 CPU 线程 | `thread -n 10` | 定位 CPU 高线程 |
| 查看线程栈 | `thread 线程ID` | 定位代码位置 |
| 查看阻塞线程 | `thread -b` | 排查锁竞争 |
| 生成火焰图 | `profiler start/stop` | 分析 CPU 热点 |
| 查看方法耗时 | `trace` | 分析调用链耗时 |
| 查看参数返回值 | `watch` | 排查特殊参数 |
| 查看内存 | `memory` | 查看堆、非堆内存 |
| 查看 JVM | `jvm` | 查看 JVM 参数 |
| 导出堆 | `heapdump` | 分析 OOM |
| 查看类加载 | `classloader` | 排查 Metaspace |
| 查看类信息 | `sc -d` | 查看类加载来源 |
| 反编译代码 | `jad` | 确认线上代码 |

---

# 四、线上使用注意事项

```text
watch、trace 要加 -n 限制次数
watch 的 -x 不要太深
profiler 不要长时间运行
heapdump 可能导致服务短暂停顿
不要随便执行有副作用的 ognl
排查完成后及时退出
```

示例：

```bash
trace com.demo.OrderService createOrder '#cost > 100' -n 5
```

---

# 五、总结

Arthas 排查 CPU 100%，一般先用 `dashboard` 看整体状态，再用 `thread -n 10` 找出 CPU 占用最高的线程，通过 `thread 线程ID` 查看线程栈，定位具体代码。如果还不清楚，可以用 `profiler` 生成火焰图，再配合 `trace` 和 `watch` 分析方法耗时和请求参数。

Arthas 排查 OOM，先用 `memory` 和 `dashboard` 查看内存和 GC 情况，再用 `jvm` 查看 JVM 参数。如果是堆内存问题，使用 `heapdump` 导出堆文件，再用 MAT 分析大对象和 GC Roots 引用链。如果是 Metaspace，则重点看 `classloader`；如果是 native thread OOM，则重点看线程数量和线程池。

一句话总结：

```text
CPU 100%：dashboard + thread + profiler + trace/watch
OOM：memory + dashboard + heapdump + MAT + classloader/thread
```
