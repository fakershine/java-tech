# Arthas 实现原理与线上影响总结

Arthas 是 Java 线上诊断工具，可以在 **不修改代码、不重启服务** 的情况下 attach 到目标 JVM，查看线程、内存、类加载、方法调用、入参返回值、火焰图等信息。:contentReference[oaicite:0]{index=0}

---

## 1. Arthas 的整体实现原理

整体流程：

```text
arthas-boot 启动
  ↓
选择目标 Java 进程
  ↓
通过 JVM Attach 机制连接目标进程
  ↓
加载 Arthas Agent 到目标 JVM
  ↓
Agent 在目标 JVM 内启动诊断服务
  ↓
用户通过命令和目标 JVM 交互
```

简单理解：

```text
Arthas = Attach API + Java Agent + Instrumentation + 字节码增强 + 命令交互
```

---

## 2. Attach 机制

Arthas 启动后，会 attach 到目标 Java 进程。

```text
Arthas 进程
  ↓
Attach 到目标 JVM
  ↓
目标 JVM 加载 arthas-agent
```

注意：

```text
Arthas 一般需要和目标 Java 进程使用同一个操作系统用户
```

如果目标 JVM 已经卡死，可能无法响应 attach 信号，这时 Arthas 也可能 attach 失败。官方 FAQ 也提到，如果目标进程不响应或 HotSpot VM 未加载，可能无法 attach。:contentReference[oaicite:1]{index=1}

---

## 3. Agent 原理

Arthas 本质上会把一个 Java Agent 加载到目标 JVM 中。

Agent 加载后，可以拿到：

```text
Instrumentation
```

Instrumentation 可以做：

```text
查看已加载类
重新转换类
增强字节码
导出 class
修改类定义
```

Arthas 命令列表中也包含 `redefine`、`retransform`，它们分别对应 JVM Instrumentation 的类重定义和类重新转换能力。:contentReference[oaicite:2]{index=2}

---

## 4. 字节码增强原理

像下面这些命令：

```text
watch
trace
monitor
stack
tt
```

底层都依赖 **字节码增强**。

官方文档也明确提示，这类命令会使用 byte-code injection，对当前类注入增强逻辑，用于监控和统计。:contentReference[oaicite:3]{index=3}

例如执行：

```bash
watch com.demo.OrderService createOrder '{params, returnObj}'
```

Arthas 会对 `OrderService#createOrder` 做增强：

```text
方法执行前：记录入参
方法执行后：记录返回值
方法抛异常：记录异常
方法结束：输出耗时
```

可以理解为临时插入了类似这样的逻辑：

```java
public Object createOrder(...) {
    long start = System.currentTimeMillis();

    try {
        // 原始业务逻辑
        Object result = 原方法执行();

        // Arthas 输出 returnObj
        return result;
    } catch (Throwable e) {
        // Arthas 输出 throwExp
        throw e;
    } finally {
        // Arthas 输出 cost
    }
}
```

---

## 5. watch 原理

`watch` 用于观察方法的：

```text
入参
返回值
异常
耗时
```

官方文档说明，`watch` 可以观察方法调用时的数据，并支持条件表达式过滤。:contentReference[oaicite:4]{index=4}

常用：

```bash
watch com.demo.OrderService createOrder '{params, returnObj, #cost}' '#cost > 100' -n 5
```

底层原理：

```text
匹配类和方法
  ↓
增强目标方法字节码
  ↓
在方法执行前后采集上下文
  ↓
通过 OGNL 表达式计算输出内容
```

---

## 6. trace 原理

`trace` 用于跟踪方法内部调用链路，并统计每个节点耗时。

官方文档说明，`trace` 可以跟踪指定方法的调用路径，并计算整条路径上每个节点的耗时。:contentReference[oaicite:5]{index=5}

示例：

```bash
trace com.demo.OrderService createOrder '#cost > 100' -n 5
```

底层原理：

```text
增强入口方法
  ↓
记录方法内部调用链
  ↓
统计每个方法调用耗时
  ↓
输出调用树
```

适合排查：

```text
接口慢在哪里
慢在 DB、Redis、RPC 还是本地逻辑
```

---

## 7. profiler 原理

`profiler` 用于生成火焰图。

Arthas 的 `profiler` 命令基于 `async-profiler`，可以生成应用热点火焰图。:contentReference[oaicite:6]{index=6}

常用：

```bash
profiler start
profiler stop --format html
```

它主要通过采样方式分析：

```text
CPU 热点
内存分配热点
锁竞争热点
```

采样式分析通常比对每个方法都插桩的方式开销更低，但仍然不能长时间无控制运行。

---

## 8. Arthas 对线上服务有没有影响？

结论：

```text
有影响，但影响大小取决于使用的命令和范围。
```

可以分成三类看。

---

# 一、低影响命令

这些命令主要是查看 JVM 状态，对业务影响较小：

```text
dashboard
thread
jvm
memory
sc
sm
jad
logger
sysprop
sysenv
```

特点：

```text
一般不会增强业务方法
主要读取 JVM 信息
通常适合线上排查
```

但如果频繁执行、输出内容很大，也会带来一定 CPU、内存和 IO 开销。

---

# 二、中高影响命令

这些命令会做字节码增强：

```text
watch
trace
monitor
stack
tt
```

官方文档也提醒，使用这类命令时要明确指定类、方法和条件，并在排查后通过 `stop` 或 `reset` 移除增强代码。:contentReference[oaicite:7]{index=7}

风险点：

```text
方法调用频率越高，影响越大
匹配类越多，影响越大
输出对象越大，影响越大
OGNL 表达式越复杂，影响越大
trace 调用链越深，影响越大
```

错误示例：

```bash
watch * * '{params, returnObj}' -x 5
```

这类命令范围太大，线上非常危险。

推荐写法：

```bash
watch com.demo.OrderService createOrder '{params, returnObj, #cost}' '#cost > 200' -x 2 -n 5
```

---

# 三、高风险命令

这些命令线上要谨慎使用：

```text
heapdump
redefine
retransform
tt
ognl
profiler 长时间运行
```

## heapdump

```bash
heapdump /tmp/heap.hprof
```

风险：

```text
可能造成 JVM 短暂停顿
会产生大文件
消耗磁盘 IO
可能影响线上 RT
```

---

## tt

`tt` 会记录方法调用现场。

风险：

```text
会把调用现场保存到内存中
记录太多可能导致内存上涨
用完需要清理
```

---

## ognl

`ognl` 可以执行表达式。

风险：

```text
如果调用了有副作用的方法，可能影响业务数据
```

例如不要随意执行：

```bash
ognl '@xxxService@deleteData()'
```

---

## redefine / retransform

这类命令涉及类重定义或重新转换。

风险：

```text
可能影响类行为
可能触发类重新增强
不熟悉不要在线上随意使用
```

---

## profiler

`profiler` 是采样式分析，通常比 trace/watch 侵入性低，但也会消耗资源。

风险：

```text
长时间运行会有额外 CPU 开销
生成结果会消耗磁盘
高峰期要谨慎
```

---

## 9. 如何降低 Arthas 对线上影响

### 1. 精确指定类和方法

推荐：

```bash
trace com.demo.OrderService createOrder -n 5
```

不推荐：

```bash
trace * * 
```

---

### 2. 使用条件过滤

只看慢请求：

```bash
trace com.demo.OrderService createOrder '#cost > 200' -n 5
```

只看异常：

```bash
watch com.demo.OrderService createOrder '{params, throwExp}' -e -n 5
```

---

### 3. 限制执行次数

一定要加：

```bash
-n 5
```

例如：

```bash
watch com.demo.OrderService createOrder '{params, returnObj}' -n 5
```

避免持续增强和持续输出。

---

### 4. 控制输出深度

`-x` 不要太大。

推荐：

```bash
-x 2
```

谨慎：

```bash
-x 5
```

因为深层展开大对象会增加 CPU、内存和日志输出压力。

---

### 5. 排查完及时 reset

移除增强：

```bash
reset
```

停止 Arthas：

```bash
stop
```

官方文档也建议使用相关字节码增强命令后，通过 `stop` 或 `reset` 移除注入代码。:contentReference[oaicite:8]{index=8}

---

## 10. 线上使用建议

```text
1. 优先使用 dashboard、thread、jvm、memory 这类低风险命令。
2. watch、trace 必须指定具体类和方法。
3. watch、trace 必须加 -n 限制次数。
4. 不要匹配过宽，例如 *Controller *。
5. 不要打印超大对象。
6. 不要长时间开启 profiler。
7. heapdump 尽量在低峰期执行。
8. 不要随意使用 ognl 执行业务方法。
9. 排查结束后 reset 或 stop。
```

---

## 11. 总结

Arthas 的实现原理主要是 JVM Attach 机制和 Java Agent。它通过 attach 连接到目标 JVM，把 Arthas Agent 加载进去，然后利用 Instrumentation 获取 JVM 内部信息，并对指定类进行字节码增强。

`dashboard`、`thread`、`jvm`、`memory` 等命令主要读取 JVM 状态，影响较小；`watch`、`trace`、`monitor`、`stack`、`tt` 等命令会进行字节码增强，对目标方法插入统计逻辑，所以对线上服务有一定影响。方法调用越频繁、匹配范围越大、输出对象越大，影响越明显。

线上使用 Arthas 时，要精确指定类和方法，加条件过滤，加 `-n` 限制次数，避免打印大对象，排查完成后执行 `reset` 或 `stop` 清理增强。

一句话总结：

```text
Arthas 原理 = Attach + Agent + Instrumentation + 字节码增强；
线上有影响，但可控；
低风险命令放心用，watch/trace/tt/heapdump 要谨慎、限范围、限次数、及时清理。
```
