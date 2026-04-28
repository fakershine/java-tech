# Thread

## 1. Thread 是什么

`Thread` 是 Java 中表示**线程**的类，位于：

```java
java.lang.Thread
```

线程是程序执行的最小调度单位。

一句话理解：

> Thread 是 Java 中用来创建和控制线程的类，一个线程代表一条独立的执行路径。

---

## 2. 进程和线程的区别

| 对比项 | 进程 | 线程 |
|---|---|---|
| 定义 | 操作系统资源分配的基本单位 | CPU 调度的基本单位 |
| 资源 | 进程之间资源隔离 | 同一进程内线程共享资源 |
| 内存 | 每个进程有独立内存空间 | 同一进程线程共享堆内存 |
| 创建成本 | 较高 | 较低 |
| 通信方式 | IPC，成本较高 | 共享内存，通信方便 |
| 稳定性 | 一个进程崩溃通常不影响其他进程 | 一个线程异常可能影响整个进程 |

简单理解：

```text
进程：一个运行中的程序
线程：程序中的一条执行路径
```

---

## 3. 为什么需要线程

多线程可以提高程序并发能力。

常见用途：

```text
提高 CPU 利用率
异步执行任务
并发处理请求
后台任务处理
定时任务
IO 操作并发
提升系统吞吐量
```

例如：

```text
主线程处理用户请求
子线程异步发送短信
子线程异步写日志
子线程异步处理文件
```

---

# 一、创建线程的方式

## 4. 继承 Thread

```java
public class MyThread extends Thread {

    @Override
    public void run() {
        System.out.println("线程执行：" + Thread.currentThread().getName());
    }
}
```

使用：

```java
public class Test {
    public static void main(String[] args) {
        Thread thread = new MyThread();
        thread.start();
    }
}
```

---

## 5. 实现 Runnable

```java
public class MyRunnable implements Runnable {

    @Override
    public void run() {
        System.out.println("线程执行：" + Thread.currentThread().getName());
    }
}
```

使用：

```java
public class Test {
    public static void main(String[] args) {
        Thread thread = new Thread(new MyRunnable());
        thread.start();
    }
}
```

Lambda 写法：

```java
Thread thread = new Thread(() -> {
    System.out.println("线程执行：" + Thread.currentThread().getName());
});

thread.start();
```

---

## 6. 实现 Callable

`Callable` 可以有返回值，也可以抛异常。

```java
Callable<String> callable = () -> {
    return "hello";
};
```

配合 `FutureTask` 使用：

```java
FutureTask<String> futureTask = new FutureTask<>(callable);

Thread thread = new Thread(futureTask);
thread.start();

String result = futureTask.get();

System.out.println(result);
```

---

## 7. 使用线程池

实际开发中更推荐线程池，而不是手动 `new Thread()`。

```java
ExecutorService executorService = Executors.newFixedThreadPool(10);

executorService.submit(() -> {
    System.out.println("线程池执行任务");
});
```

更推荐自定义线程池：

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
        10,
        20,
        60,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(1000),
        new ThreadPoolExecutor.CallerRunsPolicy()
);
```

---

## 8. 几种创建方式对比

| 方式 | 优点 | 缺点 |
|---|---|---|
| 继承 Thread | 简单 | Java 单继承限制，不够灵活 |
| 实现 Runnable | 更灵活，任务和线程分离 | 无返回值 |
| 实现 Callable | 有返回值，可抛异常 | 需要配合 FutureTask 或线程池 |
| 线程池 | 复用线程，性能好，易管理 | 需要合理配置参数 |

推荐：

```text
简单学习：Thread / Runnable
需要返回值：Callable + FutureTask
实际开发：线程池
```

---

# 二、start 和 run 的区别

## 9. run()

直接调用 `run()`：

```java
Thread thread = new Thread(() -> {
    System.out.println(Thread.currentThread().getName());
});

thread.run();
```

本质是普通方法调用。

输出可能是：

```text
main
```

说明还是主线程执行。

---

## 10. start()

调用 `start()`：

```java
thread.start();
```

会真正启动一个新线程。

执行流程：

```text
start()
   ↓
JVM 创建新线程
   ↓
新线程调用 run()
```

---

## 11. start 和 run 对比

| 对比项 | `start()` | `run()` |
|---|---|---|
| 是否创建新线程 | 是 | 否 |
| 执行线程 | 新线程 | 当前线程 |
| 是否可重复调用 | 不可以 | 可以当普通方法调用 |
| 作用 | 启动线程 | 线程任务逻辑 |

一句话：

> `start()` 是启动线程，`run()` 只是普通方法调用。

---

# 三、线程生命周期

## 12. Java 线程状态

Java 中线程状态定义在：

```java
Thread.State
```

共有 6 种：

| 状态 | 说明 |
|---|---|
| `NEW` | 新建状态，线程对象已创建但未启动 |
| `RUNNABLE` | 可运行状态，可能正在运行或等待 CPU |
| `BLOCKED` | 阻塞状态，等待锁 |
| `WAITING` | 无限等待状态 |
| `TIMED_WAITING` | 限时等待状态 |
| `TERMINATED` | 终止状态，线程执行结束 |

---

## 13. 生命周期图

```text
NEW
 ↓ start()
RUNNABLE
 ↓ 等待锁
BLOCKED
 ↓ 获取锁
RUNNABLE
 ↓ wait() / join()
WAITING
 ↓ notify() / 线程结束
RUNNABLE
 ↓ sleep(timeout) / wait(timeout) / join(timeout)
TIMED_WAITING
 ↓ 时间到
RUNNABLE
 ↓ run() 执行结束
TERMINATED
```

---

## 14. NEW

线程对象创建了，但还没有调用 `start()`。

```java
Thread thread = new Thread(() -> {});
```

此时状态：

```text
NEW
```

---

## 15. RUNNABLE

调用 `start()` 后进入 `RUNNABLE`。

```java
thread.start();
```

注意：

```text
RUNNABLE 不代表一定正在运行
也可能是在等待 CPU 时间片
```

---

## 16. BLOCKED

线程等待进入 `synchronized` 锁时，会进入 `BLOCKED`。

```java
synchronized (lock) {
    // 持有锁
}
```

如果其他线程也想进入同一个锁，就会阻塞。

---

## 17. WAITING

线程无限期等待。

常见触发方式：

```text
Object.wait()
Thread.join()
LockSupport.park()
```

需要其他线程唤醒。

---

## 18. TIMED_WAITING

线程限时等待。

常见触发方式：

```text
Thread.sleep(time)
Object.wait(time)
Thread.join(time)
LockSupport.parkNanos()
LockSupport.parkUntil()
```

---

## 19. TERMINATED

线程执行结束。

```java
public void run() {
    System.out.println("done");
}
```

执行完成后进入：

```text
TERMINATED
```

---

# 四、Thread 常用方法

## 20. sleep()

让当前线程睡眠指定时间。

```java
Thread.sleep(1000);
```

特点：

```text
不会释放 synchronized 锁
会进入 TIMED_WAITING
时间到后进入 RUNNABLE
```

示例：

```java
synchronized (lock) {
    Thread.sleep(1000);
}
```

睡眠期间仍然持有 `lock`。

---

## 21. yield()

让出 CPU 执行权。

```java
Thread.yield();
```

特点：

```text
只是提示调度器让出 CPU
不保证一定让出
线程仍然是 RUNNABLE
很少在业务代码中使用
```

---

## 22. join()

等待另一个线程执行结束。

```java
Thread thread = new Thread(() -> {
    System.out.println("子线程执行");
});

thread.start();

thread.join();

System.out.println("主线程继续执行");
```

执行顺序：

```text
子线程执行
主线程继续执行
```

作用：

```text
当前线程等待目标线程执行完成
```

---

## 23. interrupt()

中断线程。

```java
thread.interrupt();
```

注意：

> `interrupt()` 不会强制杀死线程，只是设置中断标记。

如果线程处于 `sleep()`、`wait()`、`join()`，会抛出：

```java
InterruptedException
```

---

## 24. isInterrupted()

判断线程是否被中断。

```java
boolean interrupted = thread.isInterrupted();
```

特点：

```text
不会清除中断标记
```

---

## 25. interrupted()

静态方法，判断当前线程是否被中断。

```java
boolean interrupted = Thread.interrupted();
```

特点：

```text
会清除当前线程的中断标记
```

---

## 26. currentThread()

获取当前正在执行的线程。

```java
Thread current = Thread.currentThread();

System.out.println(current.getName());
```

---

## 27. setName / getName

设置和获取线程名称。

```java
Thread thread = new Thread(() -> {});
thread.setName("order-thread");

System.out.println(thread.getName());
```

推荐：

```text
给线程设置有意义的名称
方便排查问题
```

---

## 28. setPriority()

设置线程优先级。

```java
thread.setPriority(Thread.MAX_PRIORITY);
```

优先级范围：

```text
1 到 10
```

常量：

```java
Thread.MIN_PRIORITY  // 1
Thread.NORM_PRIORITY // 5
Thread.MAX_PRIORITY  // 10
```

注意：

```text
线程优先级只是调度建议
不保证优先级高的一定先执行
```

---

## 29. setDaemon()

设置守护线程。

```java
Thread thread = new Thread(() -> {
    while (true) {
        System.out.println("守护线程执行");
    }
});

thread.setDaemon(true);
thread.start();
```

注意：

```text
必须在 start() 之前调用 setDaemon()
```

---

# 五、守护线程

## 30. 什么是守护线程

守护线程是为其他线程服务的后台线程。

典型例子：

```text
GC 线程
后台监控线程
定时清理线程
```

特点：

```text
当 JVM 中只剩守护线程时，JVM 会退出
守护线程不适合执行必须完成的业务任务
```

---

## 31. 用户线程和守护线程区别

| 对比项 | 用户线程 | 守护线程 |
|---|---|---|
| 作用 | 执行业务任务 | 后台辅助任务 |
| JVM 是否等待它结束 | 会 | 不会 |
| 示例 | main 线程、业务线程 | GC 线程、监控线程 |
| 适合场景 | 核心业务 | 后台清理、监控 |

---

# 六、线程中断机制

## 32. Java 为什么不用 stop 停止线程

`Thread.stop()` 已经过时。

原因：

```text
会强制终止线程
可能导致锁突然释放
可能破坏对象一致性
可能造成数据损坏
```

正确方式：

```text
使用 interrupt 协作式停止线程
```

---

## 33. interrupt 正确使用方式

```java
Thread thread = new Thread(() -> {
    while (!Thread.currentThread().isInterrupted()) {
        System.out.println("执行任务");
    }

    System.out.println("线程退出");
});

thread.start();

thread.interrupt();
```

---

## 34. sleep 中处理中断

```java
Thread thread = new Thread(() -> {
    try {
        Thread.sleep(10000);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        System.out.println("收到中断，退出");
    }
});
```

为什么要重新设置中断标记：

```text
sleep 抛出 InterruptedException 后，会清除中断标记
如果后续逻辑还需要感知中断，需要重新 interrupt
```

---

# 七、线程安全

## 35. 什么是线程安全

多个线程同时访问共享数据时，如果结果始终正确，就叫线程安全。

线程不安全示例：

```java
private int count = 0;

public void increment() {
    count++;
}
```

`count++` 不是原子操作。

它大致分为：

```text
读取 count
加 1
写回 count
```

多线程同时执行可能导致数据丢失。

---

## 36. 线程安全问题产生原因

主要原因：

```text
多个线程共享同一变量
操作不是原子的
线程之间可见性问题
指令重排序问题
```

---

## 37. 解决线程安全问题的方式

常见方式：

```text
synchronized
Lock
volatile
Atomic 原子类
ThreadLocal
并发集合
不可变对象
减少共享变量
```

---

## 38. synchronized

```java
public synchronized void increment() {
    count++;
}
```

或者：

```java
synchronized (this) {
    count++;
}
```

作用：

```text
保证原子性
保证可见性
保证有序性
```

---

## 39. volatile

```java
private volatile boolean running = true;
```

作用：

```text
保证可见性
禁止指令重排序
```

不保证复合操作原子性。

例如：

```java
volatile int count = 0;

count++;
```

仍然不是线程安全的。

---

## 40. AtomicInteger

```java
AtomicInteger count = new AtomicInteger(0);

count.incrementAndGet();
```

底层基于 CAS 实现原子操作。

适合：

```text
计数器
并发累加
状态标记
```

---

# 八、ThreadLocal

## 41. ThreadLocal 是什么

`ThreadLocal` 用来为每个线程保存一份独立变量副本。

```java
private static final ThreadLocal<String> USER_CONTEXT = new ThreadLocal<>();
```

使用：

```java
USER_CONTEXT.set("user-1");

String user = USER_CONTEXT.get();

USER_CONTEXT.remove();
```

简单理解：

```text
同一个 ThreadLocal
不同线程拿到的是自己的变量副本
```

---

## 42. ThreadLocal 常见场景

```text
用户上下文
登录信息
TraceId
MDC 日志链路 ID
数据库连接
事务上下文
租户信息
```

---

## 43. ThreadLocal 注意事项

在线程池中使用 `ThreadLocal` 一定要记得清理：

```java
try {
    USER_CONTEXT.set("user-1");
    // 业务逻辑
} finally {
    USER_CONTEXT.remove();
}
```

原因：

```text
线程池中的线程会复用
如果不 remove，可能导致数据串用或内存泄漏
```

---

# 九、线程通信

## 44. wait 和 notify

`wait()` 和 `notify()` 必须在 `synchronized` 中使用。

```java
synchronized (lock) {
    lock.wait();
}
```

唤醒：

```java
synchronized (lock) {
    lock.notify();
}
```

---

## 45. wait 会释放锁

```java
synchronized (lock) {
    lock.wait();
}
```

调用 `wait()` 后：

```text
当前线程进入 WAITING
释放 lock 锁
等待其他线程 notify
```

---

## 46. notify 不会立即释放锁

```java
synchronized (lock) {
    lock.notify();
    // 继续执行同步代码
}
```

`notify()` 只是唤醒等待线程，但当前线程要执行完同步代码块并释放锁后，被唤醒的线程才能继续竞争锁。

---

## 47. wait 和 sleep 区别

| 对比项 | wait | sleep |
|---|---|---|
| 所属类 | `Object` | `Thread` |
| 是否释放锁 | 会释放锁 | 不释放锁 |
| 是否必须在 synchronized 中 | 是 | 否 |
| 唤醒方式 | notify / notifyAll / 超时 | 时间到 / interrupt |
| 用途 | 线程通信 | 暂停执行 |

---

# 十、线程上下文切换

## 48. 什么是上下文切换

CPU 从一个线程切换到另一个线程执行时，需要保存和恢复现场。

保存的信息包括：

```text
程序计数器
寄存器
栈信息
线程状态
```

这就是上下文切换。

---

## 49. 上下文切换成本

上下文切换会带来开销：

```text
CPU 时间消耗
缓存失效
线程调度开销
吞吐下降
延迟增加
```

所以：

```text
线程不是越多越好
线程数要合理配置
```

---

# 十一、Thread 和 Runnable 的区别

## 50. Thread 和 Runnable 区别

| 对比项 | Thread | Runnable |
|---|---|---|
| 类型 | 类 | 接口 |
| 是否能继承其他类 | 不能，因为 Java 单继承 | 可以 |
| 职责 | 线程 + 任务 | 只表示任务 |
| 解耦性 | 较差 | 更好 |
| 推荐程度 | 一般 | 更推荐 |

推荐：

```text
优先使用 Runnable / Callable，把任务和线程分离
```

---

# 十二、Thread 和线程池

## 51. 为什么不推荐频繁 new Thread

频繁创建线程的问题：

```text
创建销毁成本高
线程数量不可控
容易 OOM
缺少统一管理
不方便监控
不方便设置队列和拒绝策略
```

推荐：

```text
使用线程池统一管理线程
```

---

## 52. 线程池的优势

```text
复用线程
降低创建销毁成本
控制最大并发数
统一管理任务队列
支持拒绝策略
方便监控
提高系统稳定性
```

---

# 十三、常见面试题

## 53. Thread 的生命周期有哪些？

```text
NEW
RUNNABLE
BLOCKED
WAITING
TIMED_WAITING
TERMINATED
```

---

## 54. start 和 run 的区别？

```text
start 会真正启动一个新线程，并由新线程执行 run 方法。
run 只是普通方法调用，不会创建新线程。
```

---

## 55. sleep 和 wait 的区别？

```text
sleep 是 Thread 的静态方法，不会释放锁，常用于线程暂停。
wait 是 Object 的方法，会释放锁，必须在 synchronized 中使用，常用于线程通信。
```

---

## 56. notify 和 notifyAll 的区别？

```text
notify 随机唤醒一个等待线程。
notifyAll 唤醒所有等待线程。
```

一般更推荐：

```text
notifyAll
```

因为 `notify` 容易因为唤醒错误线程导致程序无法继续。

---

## 57. interrupt 能停止线程吗？

```text
interrupt 不能强制停止线程。
它只是设置中断标记。
如果线程处于 sleep、wait、join，会抛出 InterruptedException。
线程是否退出，需要代码主动响应中断。
```

---

## 58. 线程安全问题怎么解决？

常见方式：

```text
synchronized
Lock
volatile
Atomic 原子类
ThreadLocal
并发集合
减少共享变量
不可变对象
```

---

## 59. volatile 能保证原子性吗？

不能。

`volatile` 只能保证：

```text
可见性
禁止指令重排序
```

不能保证：

```text
复合操作原子性
```

例如：

```java
volatile int count = 0;
count++;
```

仍然线程不安全。

---

## 60. 为什么线程池中使用 ThreadLocal 要 remove？

因为线程池会复用线程。

如果不调用 `remove()`：

```text
上一个任务设置的 ThreadLocal 数据
可能被下一个任务读到
```

还可能导致：

```text
内存泄漏
上下文污染
```

---

# 十四、总结

`Thread` 是 Java 中表示线程的类，一个线程代表程序中的一条独立执行路径。创建线程常见方式有继承 `Thread`、实现 `Runnable`、实现 `Callable` 配合 `FutureTask`，以及使用线程池。实际开发中更推荐线程池，因为线程池可以复用线程、控制并发数量、统一管理任务队列和拒绝策略。

线程启动要调用 `start()`，而不是直接调用 `run()`。`start()` 会真正创建新线程，然后由新线程执行 `run()`；直接调用 `run()` 只是普通方法调用，不会创建新线程。

Java 线程生命周期包括 `NEW`、`RUNNABLE`、`BLOCKED`、`WAITING`、`TIMED_WAITING` 和 `TERMINATED`。线程等待锁时会进入 `BLOCKED`，调用 `wait()`、`join()` 等会进入 `WAITING`，调用 `sleep()`、`wait(timeout)` 等会进入 `TIMED_WAITING`。

线程安全问题主要来自多个线程共享变量、操作非原子、可见性问题和指令重排序。常见解决方式包括 `synchronized`、`Lock`、`volatile`、原子类、`ThreadLocal` 和并发集合。需要注意的是，`volatile` 只能保证可见性和有序性，不能保证 `count++` 这种复合操作的原子性。

线程中断使用 `interrupt()`，但它不会强制停止线程，只是设置中断标记。线程是否退出，需要任务代码主动检查中断状态或者响应 `InterruptedException`。

---

## 61. 一句话总结

> `Thread` 是 Java 中表示线程的类，核心要掌握创建方式、`start()` 和 `run()` 区别、线程生命周期、`sleep/wait/join/interrupt`、线程安全、ThreadLocal 和线程池；实际开发中不建议频繁手动创建线程，而应优先使用线程池。
