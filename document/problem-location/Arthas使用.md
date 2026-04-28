# Arthas 使用总结

Arthas 是阿里开源的 **Java 线上诊断工具**，可以在不改代码、不重启服务的情况下排查线上问题，例如 CPU 高、接口慢、线程阻塞、类加载问题、方法参数异常等。:contentReference[oaicite:0]{index=0}

---

## 1. Arthas 能解决什么问题

常见排查场景：

```text
CPU 飙高
接口响应慢
线程池阻塞
死锁
方法入参 / 返回值异常
线上代码版本不一致
类加载冲突
JVM 内存问题
GC 问题
线上日志级别调整
```

---

## 2. 启动 Arthas

常见启动方式：

```bash
java -jar arthas-boot.jar
```

启动后会列出当前机器上的 Java 进程：

```text
[1] 12345 order-service.jar
[2] 23456 user-service.jar
```

选择对应进程后 attach 进去。

---

## 3. 常用命令总览

| 命令 | 作用 |
|---|---|
| `dashboard` | 查看 JVM 实时运行状态 |
| `thread` | 查看线程情况 |
| `jvm` | 查看 JVM 信息 |
| `memory` | 查看内存信息 |
| `sc` | 查看已加载类 |
| `sm` | 查看类的方法 |
| `jad` | 反编译线上类 |
| `watch` | 观察方法入参、返回值、异常 |
| `trace` | 跟踪方法内部调用耗时 |
| `stack` | 查看方法调用栈 |
| `tt` | 记录方法调用现场 |
| `ognl` | 执行 OGNL 表达式 |
| `profiler` | 生成火焰图 |
| `heapdump` | 导出堆内存文件 |

Arthas 官方命令列表也包含 `dashboard`、`thread`、`jvm`、`memory`、`heapdump`、`sc`、`sm`、`jad`、`ognl` 等常用诊断命令。:contentReference[oaicite:1]{index=1}

---

## 4. 查看系统概况：dashboard

```bash
dashboard
```

可以看到：

```text
线程数量
CPU 使用率
内存使用情况
GC 情况
运行时间
系统负载
```

适合第一步快速判断：

```text
CPU 是否高
GC 是否频繁
线程是否异常
内存是否紧张
```

---

## 5. 排查 CPU 高：thread

查看 CPU 最高的线程：

```bash
thread -n 10
```

查看指定线程栈：

```bash
thread 线程ID
```

查看阻塞线程：

```bash
thread -b
```

常见用途：

```text
定位死循环
定位高 CPU 代码
定位锁竞争
定位线程阻塞
```

---

## 6. 排查接口慢：trace

`trace` 可以跟踪某个方法的调用路径，并统计每个节点耗时。:contentReference[oaicite:2]{index=2}

```bash
trace com.demo.OrderService createOrder
```

只看耗时超过 100ms 的调用：

```bash
trace com.demo.OrderService createOrder '#cost > 100'
```

限制输出次数：

```bash
trace com.demo.OrderService createOrder -n 5
```

适合定位：

```text
慢在数据库
慢在 Redis
慢在 RPC
慢在某个内部方法
```

---

## 7. 查看方法参数和返回值：watch

`watch` 可以观察方法调用时的 **入参、返回值和异常信息**，并支持 OGNL 表达式。:contentReference[oaicite:3]{index=3}

查看入参和返回值：

```bash
watch com.demo.OrderService createOrder '{params, returnObj}' -x 3
```

只看异常：

```bash
watch com.demo.OrderService createOrder '{params, throwExp}' -e -x 3
```

只看耗时超过 200ms 的调用：

```bash
watch com.demo.OrderService createOrder '{params, returnObj, #cost}' '#cost > 200' -x 3
```

常见用途：

```text
查看线上真实入参
查看返回值是否符合预期
查看异常堆栈
排查偶发业务问题
```

---

## 8. 查看调用栈：stack

查看某个方法是被谁调用的：

```bash
stack com.demo.OrderService createOrder
```

加条件：

```bash
stack com.demo.OrderService createOrder 'params[0] != null'
```

适合排查：

```text
方法从哪里被调用
某个异常入口是谁
某段逻辑为什么被触发
```

---

## 9. 反编译线上代码：jad

查看线上实际运行的代码：

```bash
jad com.demo.OrderService
```

只看某个方法：

```bash
jad com.demo.OrderService createOrder
```

常见用途：

```text
确认线上代码版本
确认发版是否生效
确认 class 是否被增强
排查代码和 Git 不一致问题
```

---

## 10. 查看类和方法：sc / sm

查看类是否被加载：

```bash
sc com.demo.OrderService
```

查看类详细信息：

```bash
sc -d com.demo.OrderService
```

查看类的方法：

```bash
sm com.demo.OrderService
```

查看方法详情：

```bash
sm -d com.demo.OrderService createOrder
```

适合排查：

```text
类是否加载
加载自哪个 classloader
方法签名是否正确
是否存在多个同名类
```

---

## 11. 执行 OGNL：ognl

`ognl` 可以执行表达式，例如查看静态变量、调用 Spring Bean 方法等；官方文档也说明可以执行多行 OGNL 表达式。:contentReference[oaicite:4]{index=4}

查看静态变量：

```bash
ognl '@com.demo.Config@VERSION'
```

调用静态方法：

```bash
ognl '@java.lang.System@getProperty("java.home")'
```

常见用途：

```text
查看静态变量
调用静态方法
查看 Spring 上下文
临时验证线上状态
```

---

## 12. 生成火焰图：profiler

`profiler` 命令基于 async-profiler，可用于生成热点火焰图；默认事件是 CPU，也支持 alloc、lock 等事件。:contentReference[oaicite:5]{index=5}

开始采样：

```bash
profiler start
```

查看状态：

```bash
profiler status
```

停止并生成 HTML：

```bash
profiler stop --format html
```

指定采样 30 秒：

```bash
profiler start --duration 30
```

适合排查：

```text
CPU 热点
方法耗时热点
锁竞争
对象分配热点
```

---

## 13. 记录方法现场：tt

`tt` 可以记录方法调用时的参数、返回值、异常等现场，方便之后查看；但官方文档特别提醒，`tt` 会把调用现场保存到内存中的 Map，长期使用需要手动释放，否则可能导致 OOM。:contentReference[oaicite:6]{index=6}

记录方法调用：

```bash
tt -t com.demo.OrderService createOrder
```

查看记录列表：

```bash
tt -l
```

查看某次调用：

```bash
tt -i 1000
```

清理记录：

```bash
tt --delete-all
```

使用建议：

```text
线上谨慎使用
一定要限制次数
用完及时清理
```

---

## 14. 导出堆内存：heapdump

```bash
heapdump /tmp/heap.hprof
```

适合排查：

```text
内存泄露
大对象
缓存膨胀
ThreadLocal 泄露
OOM 问题
```

导出后可以用：

```text
MAT
VisualVM
JProfiler
```

进行分析。

---

## 15. 修改日志级别：logger

查看日志配置：

```bash
logger
```

临时修改日志级别：

```bash
logger --name ROOT --level INFO
```

修改某个包：

```bash
logger --name com.demo --level DEBUG
```

适合：

```text
线上临时打开 DEBUG 日志
排查完成后改回 INFO / WARN
```

---

## 16. 常见排查套路

### CPU 高

```bash
dashboard
thread -n 10
thread 线程ID
profiler start
profiler stop --format html
```

---

### 接口慢

```bash
trace com.demo.OrderService createOrder '#cost > 100'
watch com.demo.OrderService createOrder '{params, returnObj, #cost}' '#cost > 100' -x 3
```

---

### 参数异常

```bash
watch com.demo.OrderService createOrder '{params, returnObj}' -x 4
```

---

### 线上代码不一致

```bash
sc -d com.demo.OrderService
jad com.demo.OrderService
```

---

### 类加载问题

```bash
sc -d com.demo.OrderService
classloader
```

---

### 内存问题

```bash
memory
jvm
heapdump /tmp/heap.hprof
```

---

## 17. 线上使用注意事项

- 不要长时间执行 `watch`、`trace`、`tt`。
- 一定要用 `-n` 限制执行次数。
- 表达式不要打印超大对象。
- `watch -x` 深度不要太大。
- `tt` 用完要清理。
- 高峰期谨慎 `heapdump`。
- 不要随意执行有副作用的 `ognl`。
- 排查完成后退出 Arthas。

退出：

```bash
quit
```

完全退出 Arthas Server：

```bash
stop
```

---

## 18. 总结

Arthas 是 Java 线上诊断工具，可以在不重启服务、不修改代码的情况下排查线上问题。

常用命令包括：`dashboard` 查看 JVM 实时状态，`thread` 定位高 CPU 和线程阻塞，`trace` 分析方法调用耗时，`watch` 查看方法入参和返回值，`stack` 查看调用来源，`jad` 反编译线上代码，`sc` 和 `sm` 查看类和方法信息，`profiler` 生成火焰图，`heapdump` 导出堆内存。

一句话总结：

```text
Arthas 使用 = dashboard 看整体 + thread 查线程 + trace 查耗时 + watch 看参数 + jad 看代码 + profiler 看热点。
```
