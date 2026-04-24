# ThreadLocal 总结

ThreadLocal 是 Java 提供的一种线程本地变量机制，用于让每个线程都拥有自己独立的变量副本，避免多个线程之间共享变量导致线程安全问题。

---

## 1. ThreadLocal 的作用

ThreadLocal 主要用于保存线程私有数据。

```text
同一个 ThreadLocal 变量
不同线程访问时
拿到的是各自线程内部独立的值
```

常见使用场景：

- 保存用户登录信息。
- 保存请求上下文。
- 保存 TraceId。
- 保存数据库连接。
- 保存事务上下文。
- 保存日期格式化对象，如 `SimpleDateFormat`。

---

## 2. 基本使用

```java
private static final ThreadLocal<String> USER_CONTEXT = new ThreadLocal<>();

public void setUser(String userId) {
    USER_CONTEXT.set(userId);
}

public String getUser() {
    return USER_CONTEXT.get();
}

public void clear() {
    USER_CONTEXT.remove();
}
```

使用完一定要调用：

```java
USER_CONTEXT.remove();
```

---

## 3. 实现原理

ThreadLocal 的数据并不是存储在 ThreadLocal 对象中，而是存储在线程对象 `Thread` 内部的 `ThreadLocalMap` 中。

核心结构：

```text
Thread
  └── ThreadLocalMap
        └── Entry
              ├── key   = ThreadLocal 对象
              └── value = 线程本地变量值
```

也就是说：

```text
每个线程都有自己的 ThreadLocalMap
ThreadLocal 只是作为 key
真正的数据 value 存在线程内部
```

---

## 4. ThreadLocalMap 数据结构

ThreadLocalMap 是 ThreadLocal 的内部 Map 结构。

它的 Entry 大致如下：

```java
static class Entry extends WeakReference<ThreadLocal<?>> {
    Object value;
}
```

特点：

- key 是 ThreadLocal 对象。
- key 是弱引用。
- value 是强引用。
- ThreadLocalMap 属于当前线程。

结构示意：

```text
ThreadLocalMap
  Entry(ThreadLocalA -> valueA)
  Entry(ThreadLocalB -> valueB)
```

---

## 5. 为什么 ThreadLocal 能实现线程隔离

因为每个线程都有自己的 `ThreadLocalMap`。

例如：

```text
线程 A：
ThreadLocalMap {
    threadLocal -> "A 的数据"
}

线程 B：
ThreadLocalMap {
    threadLocal -> "B 的数据"
}
```

虽然使用的是同一个 ThreadLocal 对象，但数据实际存储在不同线程自己的 Map 中，所以线程之间互不影响。

---

## 6. ThreadLocal 内存泄漏问题

### 原因

ThreadLocalMap 中的 key 是弱引用，value 是强引用。

```text
key   = ThreadLocal 弱引用
value = 业务对象强引用
```

如果 ThreadLocal 对象没有外部强引用，GC 后 key 会被回收，变成 `null`。

但是 value 仍然被 ThreadLocalMap 强引用着。

```text
Entry {
    key = null
    value = object
}
```

如果当前线程一直不销毁，例如线程池中的线程，value 就可能一直无法释放，导致内存泄漏。

---

## 7. 为什么线程池中更容易出现内存泄漏

普通线程执行完会销毁，ThreadLocalMap 也会一起销毁。

但线程池中的线程会被复用：

```text
请求 1 使用线程 A
  ↓
ThreadLocal 设置用户信息
  ↓
请求结束但没有 remove
  ↓
线程 A 被复用处理请求 2
```

如果不清理 ThreadLocal：

- 可能导致内存泄漏。
- 可能导致请求之间数据串用。
- 可能拿到上一个请求的用户信息。

---

## 8. 如何避免内存泄漏

核心原则：

```text
使用完 ThreadLocal 后必须 remove
```

推荐写法：

```java
try {
    USER_CONTEXT.set(userId);

    // 执行业务逻辑

} finally {
    USER_CONTEXT.remove();
}
```

在 Web 项目中，可以在 Filter 或 Interceptor 中统一清理：

```java
try {
    chain.doFilter(request, response);
} finally {
    USER_CONTEXT.remove();
}
```

---

## 9. ThreadLocal 常见使用场景

### 1. 保存用户上下文

```java
UserContextHolder.set(userInfo);
```

在业务代码中可以随时获取当前登录用户：

```java
UserContextHolder.get();
```

---

### 2. 保存 TraceId

每个请求生成一个 TraceId，放入 ThreadLocal，日志打印时自动带上。

```text
请求进入
  ↓
生成 traceId
  ↓
放入 ThreadLocal
  ↓
后续日志自动获取 traceId
  ↓
请求结束 remove
```

---

### 3. 保存事务上下文

Spring 事务中会通过 ThreadLocal 保存数据库连接、事务状态等信息，保证同一个线程内多个数据库操作使用同一个连接。

---

### 4. 保存日期格式化对象

`SimpleDateFormat` 线程不安全，可以通过 ThreadLocal 给每个线程保存一个独立实例。

```java
private static final ThreadLocal<SimpleDateFormat> FORMATTER =
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
```

---

## 10. InheritableThreadLocal

### 实现原理

`InheritableThreadLocal` 可以让子线程继承父线程中的变量。

```java
private static final InheritableThreadLocal<String> CONTEXT =
        new InheritableThreadLocal<>();
```

父线程设置值后，创建子线程时，子线程可以获取到该值。

### 注意

它只在线程创建时传递一次。

在线程池中，由于线程是提前创建并复用的，所以 `InheritableThreadLocal` 通常无法正确传递上下文。

---

## 11. TransmittableThreadLocal 如何解决 InheritableThreadLocal 的问题

### 1. InheritableThreadLocal 的问题

`InheritableThreadLocal` 可以让子线程继承父线程的变量，但它只在 **线程创建时** 复制一次父线程的数据。

普通新建线程时可以生效：

```java
InheritableThreadLocal<String> context = new InheritableThreadLocal<>();

context.set("user-1");

new Thread(() -> {
    System.out.println(context.get()); // 可以拿到 user-1
}).start();
```

但是在线程池中会有问题。

线程池的线程通常是提前创建并复用的：

```text
线程池初始化
  ↓
工作线程已创建
  ↓
请求 A 设置上下文
  ↓
提交任务到线程池
  ↓
线程池复用旧线程执行任务
```

因为线程不是在提交任务时新建的，所以 `InheritableThreadLocal` 不会重新复制父线程上下文。

这会导致两个问题：

- 子线程拿不到当前请求的上下文。
- 线程复用时可能拿到上一次任务遗留的数据。

---

### 2. TransmittableThreadLocal 的解决思路

`TransmittableThreadLocal`，简称 TTL，主要解决的是 **线程池场景下 ThreadLocal 上下文传递问题**。

它的核心思想是：

```text
不是在线程创建时传递上下文
而是在任务提交时捕获上下文
在线程执行任务前恢复上下文
任务执行完成后清理或还原上下文
```

完整流程：

```text
父线程设置上下文
  ↓
提交 Runnable / Callable 任务
  ↓
TTL 捕获父线程当前上下文
  ↓
线程池工作线程执行任务前，将捕获的上下文设置到当前线程
  ↓
任务执行
  ↓
任务执行完成后，恢复工作线程原来的上下文
```

---

### 3. TTL 的实现原理

TTL 通常通过包装任务来实现上下文传递。

例如原始任务：

```java
Runnable task = () -> {
    System.out.println(userContext.get());
};
```

使用 TTL 包装：

```java
Runnable ttlTask = TtlRunnable.get(task);
executorService.submit(ttlTask);
```

执行过程可以理解为：

```text
TtlRunnable.get(task)
  ↓
捕获提交任务线程中的 TTL 上下文
  ↓
包装原始 Runnable
  ↓
线程池执行包装后的 Runnable
  ↓
执行前设置上下文
  ↓
执行原始任务
  ↓
执行后恢复上下文
```

---

### 4. 为什么 TTL 在线程池中有效

因为 TTL 的上下文传递发生在 **任务提交时**，而不是线程创建时。

对比：

| 机制 | 传递时机 | 线程池中是否可靠 |
|---|---|---|
| InheritableThreadLocal | 子线程创建时 | 不可靠 |
| TransmittableThreadLocal | 任务提交时 | 可靠 |

线程池虽然复用线程，但每次提交任务时，TTL 都会重新捕获当前父线程上下文。

所以它可以保证：

```text
请求 A 提交任务，子任务拿到请求 A 的上下文
请求 B 提交任务，子任务拿到请求 B 的上下文
```

不会因为线程复用而串数据。

---

### 5. TTL 如何避免上下文污染

TTL 在任务执行前会设置当前任务的上下文，任务执行完成后会恢复线程原来的上下文。

伪流程：

```java
public void run() {
    Object oldContext = backupCurrentThreadContext();

    try {
        replayCapturedContext();
        originalRunnable.run();
    } finally {
        restoreOldContext(oldContext);
    }
}
```

也就是说：

```text
执行前：保存线程原来的上下文
执行中：设置提交任务时捕获的上下文
执行后：恢复线程原来的上下文
```

这样可以避免线程池线程复用导致上下文污染。

---

### 6. TTL 的常见使用方式

### 方式一：包装 Runnable

```java
TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();

context.set("user-1");

Runnable task = TtlRunnable.get(() -> {
    System.out.println(context.get());
});

executorService.submit(task);
```

---

### 方式二：包装 Callable

```java
Callable<String> task = TtlCallable.get(() -> {
    return context.get();
});

executorService.submit(task);
```

---

### 方式三：包装线程池

```java
ExecutorService ttlExecutor =
        TtlExecutors.getTtlExecutorService(executorService);
```

之后提交到 `ttlExecutor` 的任务，会自动传递 TTL 上下文。

---

### 方式四：Java Agent

TTL 也支持通过 Java Agent 自动增强线程池，减少手动包装代码。

这种方式适合项目中大量使用线程池、异步任务的场景。

---

### 7. TTL 适用场景

- 异步任务中传递用户上下文。
- 线程池中传递 TraceId。
- 异步日志链路追踪。
- CompletableFuture 异步任务上下文传递。
- Dubbo、Feign、MQ 消费链路中的上下文传递。

---

### 8. 使用注意点

- 使用 TTL 后仍然要注意清理上下文。
- 不建议在线程上下文中存放大对象。
- 线程池任务必须经过 TTL 包装，否则无法传递。
- 如果使用普通 `ExecutorService` 直接提交任务，TTL 不会自动生效。
- 可以通过包装线程池或 Java Agent 降低漏包装风险。

---

### 9. 总结

`InheritableThreadLocal` 的问题在于它只在线程创建时复制父线程上下文，而线程池中的线程是提前创建并复用的，所以在线程池场景下无法正确传递上下文，还可能出现上下文污染。

`TransmittableThreadLocal` 解决这个问题的核心是：在任务提交时捕获父线程上下文，在线程池工作线程执行任务前恢复该上下文，任务执行完成后再清理或还原原来的上下文。

它通常通过包装 `Runnable`、`Callable` 或 `ExecutorService` 实现，也可以通过 Java Agent 自动增强线程池。

一句话总结：

```text
InheritableThreadLocal 是在线程创建时传递上下文，TTL 是在任务提交时捕获上下文、执行前恢复上下文、执行后清理上下文，所以 TTL 能解决线程池复用导致的上下文传递问题。
```

---

## 12. ThreadLocal 注意点

- 使用完必须调用 `remove()`。
- 不要存放大对象。
- 在线程池环境下尤其要注意清理。
- 不适合做跨线程数据传递。
- 父子线程传递可以用 `InheritableThreadLocal`。
- 线程池上下文传递可以用 `TransmittableThreadLocal`。
- ThreadLocal 不是为了解决共享变量问题，而是让变量线程隔离。

---

## 13. ThreadLocal 和 synchronized 的区别

| 对比项 | ThreadLocal | synchronized |
|---|---|---|
| 核心思想 | 每个线程一份数据 | 多线程竞争同一份数据 |
| 是否加锁 | 不加锁 | 加锁 |
| 是否共享数据 | 不共享 | 共享 |
| 性能 | 通常较好 | 可能阻塞 |
| 使用场景 | 线程上下文、线程私有变量 | 共享资源并发修改 |

简单理解：

```text
ThreadLocal：空间换安全，每个线程一份。
synchronized：时间换安全，同一时间一个线程访问。
```

---

## 14. 总结

ThreadLocal 是 Java 提供的线程本地变量机制，它可以让每个线程拥有自己独立的变量副本，从而实现线程隔离。

ThreadLocal 的数据并不是存储在 ThreadLocal 对象中，而是存储在线程对象内部的 ThreadLocalMap 中。ThreadLocal 作为 key，真正的变量值作为 value。因为每个线程都有自己的 ThreadLocalMap，所以不同线程之间的数据互不影响。

ThreadLocalMap 中的 key 是弱引用，value 是强引用。如果 ThreadLocal 被 GC 回收，而线程又长期存活，例如线程池线程，就可能出现 key 为 null 但 value 无法释放的情况，从而导致内存泄漏。

因此使用 ThreadLocal 时一定要在 `finally` 中调用 `remove()` 清理数据，尤其是在 Web 请求和线程池场景下。

一句话总结：

```text
ThreadLocal 的核心是：每个线程维护自己的 ThreadLocalMap，实现线程级别的数据隔离；使用完必须 remove，防止内存泄漏和数据串用。
```
