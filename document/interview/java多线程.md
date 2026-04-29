# Java 多线程与并发编程常见面试题总结

## 目录

- [一、线程和进程的区别](#一线程和进程的区别)
- [二、创建线程有哪些方式](#二创建线程有哪些方式)
- [三、ThreadLocal 原理](#三threadlocal-原理)
- [四、synchronized 原理](#四synchronized-原理)
- [五、synchronized 和 ReentrantLock 区别](#五synchronized-和-reentrantlock-区别)
- [六、volatile 关键字作用](#六volatile-关键字作用)
- [七、CAS 是什么](#七cas-是什么)
- [八、AQS 原理](#八aqs-原理)
- [九、线程池核心参数有哪些](#九线程池核心参数有哪些)
- [十、线程池执行流程](#十线程池执行流程)
- [十一、线程池拒绝策略有哪些](#十一线程池拒绝策略有哪些)
- [十二、如何防止线程池 OOM](#十二如何防止线程池-oom)
- [十三、CountDownLatch、CyclicBarrier、Semaphore 区别](#十三countdownlatchcyclicbarriersemaphore-区别)
- [十四、面试速记版](#十四面试速记版)
- [十五、总览表](#十五总览表)

---

# 一、线程和进程的区别

## 1. 什么是进程？

进程是操作系统进行资源分配的基本单位。

一个正在运行的程序就是一个进程。

例如：

```text
启动一个 IDEA，是一个进程；
启动一个 Chrome，是一个进程；
启动一个 Java 程序，也是一个进程。
```

每个进程都有自己独立的内存空间。

---

## 2. 什么是线程？

线程是 CPU 调度和执行的基本单位。

一个进程中可以包含多个线程。

例如一个 Java 程序启动后，至少会有：

```text
main 线程
GC 线程
JIT 编译线程
其他后台线程
```

---

## 3. 进程和线程的区别

| 对比项 | 进程 | 线程 |
|---|---|---|
| 定义 | 程序运行的实例 | 进程中的执行单元 |
| 资源分配 | 操作系统资源分配的基本单位 | CPU 调度的基本单位 |
| 内存空间 | 每个进程有独立内存空间 | 同一进程内线程共享内存 |
| 通信方式 | 进程间通信成本高 | 线程间通信相对简单 |
| 创建销毁 | 成本较高 | 成本较低 |
| 稳定性 | 一个进程崩溃通常不影响其他进程 | 一个线程异常可能影响整个进程 |
| 切换开销 | 较大 | 较小 |
| 数据共享 | 不方便 | 方便，但需要考虑线程安全 |

---

## 4. 举例说明

假设有一个 Java Web 应用：

```text
整个 Web 应用运行在一个 JVM 进程中；
每个请求可能由不同线程处理；
多个线程共享堆内存中的对象。
```

所以：

```text
进程负责资源隔离；
线程负责具体执行任务。
```

---

## 5. 面试回答

进程是操作系统分配资源的基本单位，每个进程都有独立的内存空间。

线程是 CPU 调度执行的基本单位，一个进程中可以包含多个线程，同一进程内的线程共享进程资源。

进程之间资源隔离，通信成本较高；线程之间共享内存，通信方便，但也更容易出现线程安全问题。

---

# 二、创建线程有哪些方式

## 1. 继承 Thread 类

### 1.1 示例

```java
public class MyThread extends Thread {

    @Override
    public void run() {
        System.out.println("线程执行：" + Thread.currentThread().getName());
    }

    public static void main(String[] args) {
        MyThread thread = new MyThread();
        thread.start();
    }
}
```

---

### 1.2 注意点

启动线程应该调用：

```java
start();
```

而不是直接调用：

```java
run();
```

区别：

| 方法 | 说明 |
|---|---|
| `start()` | 创建新线程，并由新线程执行 `run()` |
| `run()` | 普通方法调用，不会创建新线程 |

---

## 2. 实现 Runnable 接口

### 2.1 示例

```java
public class MyRunnable implements Runnable {

    @Override
    public void run() {
        System.out.println("线程执行：" + Thread.currentThread().getName());
    }

    public static void main(String[] args) {
        Thread thread = new Thread(new MyRunnable());
        thread.start();
    }
}
```

---

### 2.2 Lambda 写法

```java
public class Test {
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            System.out.println("线程执行：" + Thread.currentThread().getName());
        });

        thread.start();
    }
}
```

---

## 3. 实现 Callable 接口

`Callable` 和 `Runnable` 类似，但它可以有返回值，也可以抛出异常。

### 3.1 示例

```java
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class MyCallable implements Callable<String> {

    @Override
    public String call() throws Exception {
        return "执行结果";
    }

    public static void main(String[] args) throws Exception {
        FutureTask<String> futureTask = new FutureTask<>(new MyCallable());

        Thread thread = new Thread(futureTask);
        thread.start();

        String result = futureTask.get();
        System.out.println(result);
    }
}
```

---

## 4. 使用线程池

实际开发中更推荐使用线程池创建和管理线程。

### 4.1 示例

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolTest {

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        executorService.execute(() -> {
            System.out.println("线程池执行任务：" + Thread.currentThread().getName());
        });

        executorService.shutdown();
    }
}
```

---

## 5. 四种方式对比

| 创建方式 | 是否有返回值 | 是否可以抛异常 | 是否推荐 |
|---|---|---|---|
| 继承 Thread | 否 | 否 | 不太推荐 |
| 实现 Runnable | 否 | 否 | 可以 |
| 实现 Callable | 是 | 是 | 可以 |
| 线程池 | 可以 | 可以 | 推荐 |

---

## 6. 面试回答

创建线程常见方式有四种：

第一种是继承 `Thread` 类，重写 `run()` 方法。

第二种是实现 `Runnable` 接口，作为参数传给 `Thread`。

第三种是实现 `Callable` 接口，配合 `FutureTask` 使用，可以获取返回值。

第四种是使用线程池，实际开发中最推荐，因为线程池可以复用线程，避免频繁创建和销毁线程。

---

# 三、ThreadLocal 原理

## 1. ThreadLocal 是什么？

`ThreadLocal` 是线程本地变量。

它可以让每个线程都拥有一份独立的变量副本，线程之间互不影响。

简单理解：

> ThreadLocal 不是用来解决共享变量线程安全问题的，而是让每个线程使用自己的变量副本。

---

## 2. 使用示例

```java
public class ThreadLocalTest {

    private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

    public static void main(String[] args) {
        THREAD_LOCAL.set("main-thread-value");

        System.out.println(THREAD_LOCAL.get());

        THREAD_LOCAL.remove();
    }
}
```

---

## 3. 多线程示例

```java
public class ThreadLocalDemo {

    private static final ThreadLocal<Integer> THREAD_LOCAL = new ThreadLocal<>();

    public static void main(String[] args) {
        Runnable task = () -> {
            THREAD_LOCAL.set((int) (Math.random() * 100));

            System.out.println(Thread.currentThread().getName() + "：" + THREAD_LOCAL.get());

            THREAD_LOCAL.remove();
        };

        new Thread(task, "线程A").start();
        new Thread(task, "线程B").start();
    }
}
```

每个线程访问到的都是自己线程内部保存的值。

---

## 4. ThreadLocal 底层结构

每个 `Thread` 对象内部都有一个成员变量：

```java
ThreadLocal.ThreadLocalMap threadLocals;
```

也就是说：

```text
ThreadLocalMap 是 Thread 的成员变量
```

结构关系：

```text
Thread
 └── ThreadLocalMap
      ├── Entry(ThreadLocal, value)
      ├── Entry(ThreadLocal, value)
      └── Entry(ThreadLocal, value)
```

---

## 5. ThreadLocalMap 中的 Entry

`ThreadLocalMap` 的 Entry 结构大致如下：

```java
static class Entry extends WeakReference<ThreadLocal<?>> {
    Object value;

    Entry(ThreadLocal<?> k, Object v) {
        super(k);
        value = v;
    }
}
```

特点：

```text
key 是 ThreadLocal 弱引用；
value 是实际存储的值。
```

---

## 6. ThreadLocal set 流程

当执行：

```java
threadLocal.set(value);
```

大致流程是：

1. 获取当前线程；
2. 获取当前线程的 `ThreadLocalMap`；
3. 如果 `ThreadLocalMap` 不存在，则创建；
4. 以当前 `ThreadLocal` 对象作为 key；
5. 将 value 存入当前线程的 `ThreadLocalMap` 中。

---

## 7. ThreadLocal get 流程

当执行：

```java
threadLocal.get();
```

大致流程是：

1. 获取当前线程；
2. 获取当前线程的 `ThreadLocalMap`；
3. 使用当前 `ThreadLocal` 对象作为 key 查找 value；
4. 如果找到，返回对应 value；
5. 如果没有找到，则返回初始值。

---

## 8. ThreadLocal remove 流程

当执行：

```java
threadLocal.remove();
```

会从当前线程的 `ThreadLocalMap` 中删除当前 `ThreadLocal` 对应的 Entry。

---

## 9. ThreadLocal 为什么可能内存泄漏？

`ThreadLocalMap` 中：

```text
key 是弱引用；
value 是强引用。
```

如果 `ThreadLocal` 对象没有外部强引用，key 可能被 GC 回收，变成 `null`。

但是 value 仍然被 `ThreadLocalMap` 强引用。

如果线程长期不结束，例如线程池中的线程，那么 value 就可能一直无法被回收，造成内存泄漏。

---

## 10. 如何避免 ThreadLocal 内存泄漏？

使用完后一定要调用：

```java
threadLocal.remove();
```

推荐写法：

```java
try {
    threadLocal.set(value);

    // 业务逻辑
} finally {
    threadLocal.remove();
}
```

---

## 11. 常见使用场景

ThreadLocal 常见使用场景包括：

1. 保存用户登录信息；
2. 保存请求上下文；
3. 保存数据库连接；
4. 保存 TraceId；
5. 保存事务上下文；
6. 保存日期格式化工具对象。

---

## 12. 面试回答

`ThreadLocal` 用于保存线程本地变量，每个线程都有自己独立的一份数据副本，线程之间互不影响。

它的底层原理是每个 `Thread` 内部维护一个 `ThreadLocalMap`，`ThreadLocal` 对象作为 key，具体数据作为 value。

由于 `ThreadLocalMap` 的 key 是弱引用，value 是强引用，如果使用完不调用 `remove()`，在线程池场景下可能导致内存泄漏。因此使用完后应该在 `finally` 中调用 `remove()`。

---

# 四、synchronized 原理

## 1. synchronized 是什么？

`synchronized` 是 Java 中的同步关键字，用来保证多线程环境下的线程安全。

它可以保证：

1. 原子性；
2. 可见性；
3. 有序性。

---

## 2. synchronized 可以修饰什么？

`synchronized` 可以修饰：

1. 实例方法；
2. 静态方法；
3. 代码块。

---

## 3. 修饰实例方法

```java
public synchronized void test() {
    // 同步代码
}
```

锁对象是：

```text
当前对象 this
```

---

## 4. 修饰静态方法

```java
public static synchronized void test() {
    // 同步代码
}
```

锁对象是：

```text
当前类的 Class 对象
```

例如：

```java
User.class
```

---

## 5. 修饰代码块

```java
public void test() {
    synchronized (this) {
        // 同步代码
    }
}
```

锁对象是括号中的对象。

---

## 6. synchronized 底层原理

`synchronized` 底层是基于 JVM 对象监视器 `Monitor` 实现的。

每个 Java 对象都可以关联一个 Monitor。

当线程进入同步代码块时，需要先获取对象的 Monitor。

执行完同步代码后，再释放 Monitor。

---

## 7. synchronized 代码块底层指令

同步代码块在字节码层面主要依赖两个指令：

```text
monitorenter
monitorexit
```

示例：

```java
synchronized (lock) {
    // 业务逻辑
}
```

字节码中类似：

```text
monitorenter
业务逻辑
monitorexit
```

---

## 8. synchronized 方法底层原理

同步方法不是通过 `monitorenter` 和 `monitorexit` 实现的，而是通过方法访问标志：

```text
ACC_SYNCHRONIZED
```

JVM 调用方法时，如果发现方法有 `ACC_SYNCHRONIZED` 标志，就会自动获取锁。

---

## 9. Java 对象头和 Mark Word

Java 对象在内存中主要包括：

```text
对象头
实例数据
对齐填充
```

对象头中有一部分叫：

```text
Mark Word
```

`Mark Word` 中会存储和锁相关的信息，例如：

1. 锁状态；
2. 线程 ID；
3. 偏向锁标识；
4. 锁记录指针；
5. Monitor 指针。

---

## 10. synchronized 锁升级过程

JDK 1.6 之后对 `synchronized` 做了大量优化，引入了锁升级机制。

锁状态大致包括：

```text
无锁 -> 偏向锁 -> 轻量级锁 -> 重量级锁
```

---

## 11. 偏向锁

偏向锁适用于只有一个线程访问同步代码的场景。

如果一个线程获取了锁，锁会偏向这个线程。

之后该线程再次进入同步代码时，不需要进行复杂的加锁操作。

---

## 12. 轻量级锁

当有其他线程尝试竞争锁时，偏向锁会升级为轻量级锁。

轻量级锁主要通过 CAS 操作尝试获取锁。

如果竞争不激烈，可以避免线程阻塞和操作系统层面的线程切换。

---

## 13. 重量级锁

如果竞争激烈，轻量级锁会升级为重量级锁。

重量级锁依赖操作系统的互斥量，线程竞争失败会进入阻塞状态。

线程阻塞和唤醒需要操作系统参与，开销较大。

---

## 14. synchronized 的特性

`synchronized` 具有以下特性：

| 特性 | 说明 |
|---|---|
| 原子性 | 同一时刻只有一个线程执行同步代码 |
| 可见性 | 释放锁前会刷新共享变量，获取锁后会读取最新值 |
| 有序性 | 对同步代码块内操作有一定顺序保证 |
| 可重入性 | 同一个线程可以重复获取同一把锁 |
| 非公平性 | 默认是非公平锁 |

---

## 15. 可重入锁示例

```java
public class SyncDemo {

    public synchronized void methodA() {
        methodB();
    }

    public synchronized void methodB() {
        System.out.println("methodB");
    }
}
```

同一个线程进入 `methodA()` 后，还可以继续进入 `methodB()`，不会发生死锁。

这说明 `synchronized` 是可重入锁。

---

## 16. 面试回答

`synchronized` 是 Java 中的同步关键字，可以修饰方法和代码块，用于保证线程安全。

它底层基于对象监视器 Monitor 实现。同步代码块通过 `monitorenter` 和 `monitorexit` 指令实现，同步方法通过 `ACC_SYNCHRONIZED` 标志实现。

JDK 1.6 之后，`synchronized` 引入了锁升级机制，包括偏向锁、轻量级锁和重量级锁，以减少加锁开销。

---

# 五、synchronized 和 ReentrantLock 区别

## 1. ReentrantLock 是什么？

`ReentrantLock` 是 `java.util.concurrent.locks` 包下的可重入锁。

示例：

```java
import java.util.concurrent.locks.ReentrantLock;

public class LockDemo {

    private final ReentrantLock lock = new ReentrantLock();

    public void test() {
        lock.lock();

        try {
            // 业务逻辑
        } finally {
            lock.unlock();
        }
    }
}
```

---

## 2. 基本区别

| 对比项 | synchronized | ReentrantLock |
|---|---|---|
| 类型 | JVM 关键字 | JDK API 类 |
| 加锁方式 | 自动加锁、释放锁 | 手动 lock、unlock |
| 是否可重入 | 可重入 | 可重入 |
| 是否公平 | 非公平 | 支持公平和非公平 |
| 是否可中断 | 不支持主动中断等待锁 | 支持 `lockInterruptibly()` |
| 是否支持超时 | 不支持 | 支持 `tryLock(timeout)` |
| 条件队列 | 单一等待队列 | 可创建多个 Condition |
| 释放锁 | JVM 自动释放 | 必须手动释放 |
| 实现机制 | Monitor | AQS |

---

## 3. synchronized 的特点

优点：

1. 使用简单；
2. 代码简洁；
3. JVM 自动释放锁；
4. 不容易忘记释放锁；
5. JVM 层面优化较多。

缺点：

1. 不支持公平锁；
2. 不支持尝试获取锁；
3. 不支持超时获取锁；
4. 不支持中断等待锁；
5. 条件队列能力较弱。

---

## 4. ReentrantLock 的特点

优点：

1. 支持公平锁和非公平锁；
2. 支持可中断获取锁；
3. 支持超时获取锁；
4. 支持尝试获取锁；
5. 支持多个条件队列 `Condition`。

缺点：

1. 使用比 `synchronized` 复杂；
2. 必须手动释放锁；
3. 如果忘记 `unlock()`，可能导致死锁。

---

## 5. 公平锁和非公平锁

创建公平锁：

```java
ReentrantLock lock = new ReentrantLock(true);
```

创建非公平锁：

```java
ReentrantLock lock = new ReentrantLock(false);
```

或者：

```java
ReentrantLock lock = new ReentrantLock();
```

默认是非公平锁。

---

## 6. tryLock 示例

```java
if (lock.tryLock()) {
    try {
        // 获取锁成功
    } finally {
        lock.unlock();
    }
} else {
    // 获取锁失败
}
```

---

## 7. Condition 示例

```java
private final ReentrantLock lock = new ReentrantLock();
private final Condition condition = lock.newCondition();

public void awaitMethod() throws InterruptedException {
    lock.lock();
    try {
        condition.await();
    } finally {
        lock.unlock();
    }
}

public void signalMethod() {
    lock.lock();
    try {
        condition.signal();
    } finally {
        lock.unlock();
    }
}
```

---

## 8. 面试回答

`synchronized` 是 JVM 层面的关键字，使用简单，可以自动释放锁，底层基于 Monitor 实现。

`ReentrantLock` 是 JDK 提供的显式锁，底层基于 AQS 实现，需要手动加锁和释放锁。

`ReentrantLock` 功能更强，支持公平锁、可中断锁、超时获取锁、尝试获取锁和多个条件队列；而 `synchronized` 使用更简单，适合大多数普通同步场景。

---

# 六、volatile 关键字作用

## 1. volatile 是什么？

`volatile` 是 Java 中的轻量级同步机制。

它主要用于修饰变量。

```java
private volatile boolean flag = true;
```

---

## 2. volatile 的两个核心作用

`volatile` 主要有两个作用：

```text
1. 保证可见性
2. 禁止指令重排序
```

注意：

```text
volatile 不能保证复合操作的原子性
```

---

## 3. 保证可见性

### 3.1 什么是可见性？

在多线程环境下，一个线程修改了共享变量，其他线程不一定能立刻看到最新值。

原因是每个线程可能会使用自己的工作内存或 CPU 缓存。

---

### 3.2 volatile 如何保证可见性？

被 `volatile` 修饰的变量：

1. 写操作会立即刷新到主内存；
2. 读操作会直接从主内存读取最新值。

示例：

```java
private volatile boolean running = true;

public void stop() {
    running = false;
}

public void run() {
    while (running) {
        // 执行任务
    }
}
```

如果 `running` 没有使用 `volatile`，一个线程修改为 `false` 后，另一个线程可能一直看不到变化。

---

## 4. 禁止指令重排序

为了提高性能，编译器和 CPU 可能会对指令进行重排序。

`volatile` 可以通过内存屏障禁止特定类型的指令重排序。

---

## 5. 双重检查锁中的 volatile

经典场景：

```java
public class Singleton {

    private static volatile Singleton instance;

    private Singleton() {
    }

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }

        return instance;
    }
}
```

这里 `volatile` 的作用是禁止对象创建过程中的指令重排序。

对象创建大致可以分为：

```text
1. 分配内存
2. 初始化对象
3. 将引用指向内存地址
```

如果发生重排序，可能变成：

```text
1. 分配内存
2. 将引用指向内存地址
3. 初始化对象
```

其他线程可能拿到一个还没有初始化完成的对象。

---

## 6. volatile 不能保证原子性

示例：

```java
private volatile int count = 0;

public void increment() {
    count++;
}
```

`count++` 不是原子操作，它包含：

```text
1. 读取 count
2. count 加 1
3. 写回 count
```

所以即使 `count` 使用了 `volatile`，多线程下仍然可能出现线程安全问题。

---

## 7. 如何保证原子性？

可以使用：

1. `synchronized`
2. `ReentrantLock`
3. `AtomicInteger`
4. `LongAdder`

例如：

```java
AtomicInteger count = new AtomicInteger(0);

count.incrementAndGet();
```

---

## 8. 面试回答

`volatile` 是轻量级同步机制，主要有两个作用：保证变量的可见性和禁止指令重排序。

它适合一个线程写、多个线程读的状态标记场景，比如停止标志。

但是 `volatile` 不能保证复合操作的原子性，例如 `i++` 即使加了 `volatile`，也不是线程安全的。

---

# 七、CAS 是什么

## 1. CAS 的定义

CAS 全称是：

```text
Compare And Swap
```

中文叫：

```text
比较并交换
```

它是一种乐观锁思想。

---

## 2. CAS 的三个操作数

CAS 有三个核心操作数：

| 操作数 | 含义 |
|---|---|
| V | 内存中的当前值 |
| A | 预期旧值 |
| B | 要更新的新值 |

CAS 的逻辑是：

```text
如果 V == A，说明没有被其他线程修改，则把 V 更新为 B；
如果 V != A，说明已经被其他线程修改，则更新失败。
```

---

## 3. CAS 伪代码

```java
boolean compareAndSwap(V, A, B) {
    if (V == A) {
        V = B;
        return true;
    }

    return false;
}
```

---

## 4. AtomicInteger 示例

```java
import java.util.concurrent.atomic.AtomicInteger;

public class CasDemo {

    private static final AtomicInteger count = new AtomicInteger(0);

    public static void main(String[] args) {
        count.incrementAndGet();

        System.out.println(count.get());
    }
}
```

`AtomicInteger` 底层就是基于 CAS 实现的。

---

## 5. CAS 的优点

CAS 的优点：

1. 不需要加锁；
2. 不会阻塞线程；
3. 性能较好；
4. 适合竞争不激烈的场景。

---

## 6. CAS 的缺点

CAS 也有一些问题：

1. ABA 问题；
2. 自旋时间过长；
3. 只能保证单个变量的原子操作。

---

## 7. ABA 问题

### 7.1 什么是 ABA 问题？

一个变量原来是 A，被其他线程改成 B，又改回 A。

当前线程使用 CAS 检查时，发现还是 A，就认为没有被修改过。

但实际上这个变量已经被修改过。

过程：

```text
线程1读取值 A
线程2把 A 改成 B
线程2又把 B 改成 A
线程1 CAS 发现还是 A，更新成功
```

这就是 ABA 问题。

---

### 7.2 如何解决 ABA 问题？

可以使用版本号机制。

Java 中可以使用：

```java
AtomicStampedReference
```

示例：

```java
AtomicStampedReference<String> reference =
        new AtomicStampedReference<>("A", 1);
```

除了比较值，还比较版本号。

---

## 8. 自旋时间过长

CAS 失败后通常会不断重试。

如果竞争非常激烈，线程一直 CAS 失败，就会不断自旋，浪费 CPU 资源。

---

## 9. 只能保证单个变量原子操作

CAS 通常只能保证一个变量的原子更新。

如果要保证多个变量的一致性，需要使用锁，或者将多个变量封装成一个对象进行 CAS。

---

## 10. 面试回答

CAS 是比较并交换，是一种乐观锁机制。

它会比较内存中的值和预期值是否相同，如果相同就更新为新值，否则更新失败。

CAS 不需要阻塞线程，性能较好，常用于原子类和并发工具中。但它存在 ABA 问题、自旋开销大、只能保证单变量原子操作等问题。

---

# 八、AQS 原理

## 1. AQS 是什么？

AQS 全称是：

```text
AbstractQueuedSynchronizer
```

中文叫：

```text
抽象队列同步器
```

它是 Java 并发包中很多锁和同步工具的底层基础。

---

## 2. AQS 用在哪些地方？

很多并发工具底层都基于 AQS 实现，例如：

1. `ReentrantLock`
2. `Semaphore`
3. `CountDownLatch`
4. `ReentrantReadWriteLock`
5. `CyclicBarrier` 部分配合锁和条件队列
6. `FutureTask`

---

## 3. AQS 核心思想

AQS 的核心思想是：

> 使用一个 volatile int 类型的 state 表示同步状态，同时使用 FIFO 双向队列管理获取锁失败的线程。

核心组成：

```text
state + CLH 队列
```

---

## 4. state 是什么？

AQS 内部有一个变量：

```java
private volatile int state;
```

`state` 表示同步状态。

不同组件对 `state` 的含义不同：

| 组件 | state 含义 |
|---|---|
| ReentrantLock | 锁重入次数 |
| Semaphore | 可用许可证数量 |
| CountDownLatch | 倒计数数量 |
| ReentrantReadWriteLock | 读锁和写锁状态 |

---

## 5. CLH 队列是什么？

当线程获取锁失败时，会被封装成一个 Node 节点，加入 AQS 的同步队列中。

队列是一个 FIFO 双向队列。

结构类似：

```text
head <-> node1 <-> node2 <-> node3
```

每个 Node 中保存：

1. 当前线程；
2. 前驱节点；
3. 后继节点；
4. 等待状态；
5. 独占或共享模式。

---

## 6. AQS 获取锁的大致流程

以独占锁为例：

1. 线程尝试通过 CAS 修改 `state`；
2. 如果修改成功，说明获取锁成功；
3. 如果修改失败，说明锁被其他线程占用；
4. 当前线程被封装成 Node；
5. Node 加入 AQS 队列尾部；
6. 当前线程进入阻塞等待；
7. 当前驱节点是 head 且锁可获取时，再次尝试获取锁；
8. 获取成功后，将当前节点设置为 head。

---

## 7. AQS 释放锁的大致流程

1. 当前线程释放同步状态；
2. 修改 `state`；
3. 如果锁完全释放；
4. 唤醒队列中的后继节点；
5. 后继节点对应线程继续尝试获取锁。

---

## 8. 独占模式和共享模式

AQS 支持两种模式：

| 模式 | 说明 | 典型实现 |
|---|---|---|
| 独占模式 | 同一时刻只有一个线程能获取资源 | ReentrantLock |
| 共享模式 | 同一时刻多个线程可以获取资源 | Semaphore、CountDownLatch |

---

## 9. ReentrantLock 中的 AQS

在 `ReentrantLock` 中：

```text
state = 0 表示锁未被占用；
state > 0 表示锁被占用，数值表示重入次数。
```

加锁：

```text
CAS 将 state 从 0 改为 1
```

重入：

```text
当前线程再次获取锁，state + 1
```

释放锁：

```text
state - 1
```

当 `state` 减为 0 时，锁真正释放。

---

## 10. CountDownLatch 中的 AQS

在 `CountDownLatch` 中：

```text
state 表示计数器数量
```

调用：

```java
countDown();
```

会让：

```text
state - 1
```

当 `state` 变成 0 时，等待的线程会被唤醒。

---

## 11. 面试回答

AQS 是 Java 并发包的核心基础组件，像 `ReentrantLock`、`Semaphore`、`CountDownLatch` 等都基于 AQS 实现。

AQS 内部通过一个 `volatile int state` 表示同步状态，并通过 CAS 修改 state。同时，它维护了一个 FIFO 双向队列，用来保存获取锁失败后阻塞等待的线程。

线程获取锁失败后会进入 AQS 队列等待，释放锁时会唤醒队列中的后继节点。

---

# 九、线程池核心参数有哪些

## 1. ThreadPoolExecutor 构造方法

线程池核心类是：

```java
ThreadPoolExecutor
```

常用构造方法：

```java
public ThreadPoolExecutor(
        int corePoolSize,
        int maximumPoolSize,
        long keepAliveTime,
        TimeUnit unit,
        BlockingQueue<Runnable> workQueue,
        ThreadFactory threadFactory,
        RejectedExecutionHandler handler) {
}
```

---

## 2. 七大核心参数

| 参数 | 含义 |
|---|---|
| `corePoolSize` | 核心线程数 |
| `maximumPoolSize` | 最大线程数 |
| `keepAliveTime` | 非核心线程空闲存活时间 |
| `unit` | 时间单位 |
| `workQueue` | 任务队列 |
| `threadFactory` | 线程工厂 |
| `handler` | 拒绝策略 |

---

## 3. corePoolSize

核心线程数。

线程池中长期保留的线程数量。

即使核心线程空闲，也不会被回收。

如果设置了：

```java
allowCoreThreadTimeOut(true);
```

核心线程空闲超时后也可以被回收。

---

## 4. maximumPoolSize

最大线程数。

线程池中允许创建的最大线程数量。

```text
最大线程数 = 核心线程数 + 非核心线程数
```

---

## 5. keepAliveTime

非核心线程空闲后的存活时间。

如果非核心线程超过这个时间没有任务执行，就会被回收。

---

## 6. unit

`keepAliveTime` 的时间单位。

例如：

```java
TimeUnit.SECONDS
TimeUnit.MILLISECONDS
TimeUnit.MINUTES
```

---

## 7. workQueue

任务队列。

当核心线程都在忙时，新任务会先进入任务队列等待。

常见队列：

| 队列 | 说明 |
|---|---|
| `ArrayBlockingQueue` | 有界阻塞队列 |
| `LinkedBlockingQueue` | 链表阻塞队列，可有界可无界 |
| `SynchronousQueue` | 不存储任务，直接移交 |
| `PriorityBlockingQueue` | 优先级队列 |
| `DelayQueue` | 延迟队列 |

---

## 8. threadFactory

线程工厂，用于创建线程。

可以自定义线程名称，方便排查问题。

示例：

```java
ThreadFactory threadFactory = runnable -> {
    Thread thread = new Thread(runnable);
    thread.setName("biz-pool-" + thread.getId());
    return thread;
};
```

---

## 9. handler

拒绝策略。

当线程数达到最大线程数，并且任务队列已满时，新提交的任务会触发拒绝策略。

---

## 10. 面试回答

线程池核心参数有七个：核心线程数、最大线程数、非核心线程空闲存活时间、时间单位、任务队列、线程工厂和拒绝策略。

其中核心线程数决定常驻线程数量，最大线程数决定线程池最多能创建多少线程，任务队列用于缓存等待执行的任务，拒绝策略用于处理线程池无法继续接收任务的情况。

---

# 十、线程池执行流程

## 1. 线程池执行任务入口

线程池提交任务常见方式：

```java
execute();
submit();
```

区别：

| 方法 | 说明 |
|---|---|
| `execute()` | 提交无返回值任务 |
| `submit()` | 提交有返回值任务，返回 Future |

---

## 2. execute 执行流程

当提交一个任务时，线程池大致执行流程如下：

```text
1. 如果当前线程数 < corePoolSize，创建核心线程执行任务
2. 否则，将任务放入 workQueue
3. 如果队列已满，并且当前线程数 < maximumPoolSize，创建非核心线程执行任务
4. 如果当前线程数已经达到 maximumPoolSize，并且队列也满了，执行拒绝策略
```

---

## 3. 流程图

```text
提交任务
   |
   v
当前线程数 < corePoolSize ?
   |
   ├── 是：创建核心线程执行任务
   |
   └── 否：
        |
        v
   任务队列是否已满？
        |
        ├── 否：加入任务队列等待执行
        |
        └── 是：
             |
             v
        当前线程数 < maximumPoolSize ?
             |
             ├── 是：创建非核心线程执行任务
             |
             └── 否：执行拒绝策略
```

---

## 4. 需要注意的点

很多人误以为线程池会先创建到最大线程数，再放入队列。

这是错误的。

正确顺序是：

```text
核心线程 -> 任务队列 -> 非核心线程 -> 拒绝策略
```

---

## 5. 举例说明

假设线程池参数如下：

```java
corePoolSize = 2
maximumPoolSize = 4
workQueue 容量 = 3
```

提交任务流程：

| 提交任务 | 处理方式 |
|---|---|
| 第 1 个任务 | 创建核心线程执行 |
| 第 2 个任务 | 创建核心线程执行 |
| 第 3 个任务 | 进入队列 |
| 第 4 个任务 | 进入队列 |
| 第 5 个任务 | 进入队列 |
| 第 6 个任务 | 队列满，创建非核心线程执行 |
| 第 7 个任务 | 创建非核心线程执行 |
| 第 8 个任务 | 触发拒绝策略 |

---

## 6. 面试回答

线程池执行流程是：提交任务后，先判断当前线程数是否小于核心线程数，如果小于，就创建核心线程执行任务。

如果核心线程已满，就尝试把任务放入阻塞队列。

如果队列也满了，并且当前线程数小于最大线程数，就创建非核心线程执行任务。

如果线程数已经达到最大线程数，并且队列也满了，就执行拒绝策略。

---

# 十一、线程池拒绝策略有哪些

## 1. 什么情况下会触发拒绝策略？

当同时满足以下条件时，会触发拒绝策略：

```text
1. 当前线程数已经达到 maximumPoolSize
2. 任务队列已经满了
3. 又有新任务提交
```

---

## 2. Java 内置四种拒绝策略

Java 提供了四种常见拒绝策略：

| 拒绝策略 | 说明 |
|---|---|
| `AbortPolicy` | 直接抛出异常，默认策略 |
| `CallerRunsPolicy` | 由提交任务的线程自己执行任务 |
| `DiscardPolicy` | 直接丢弃任务，不抛异常 |
| `DiscardOldestPolicy` | 丢弃队列中最老的任务，然后尝试提交新任务 |

---

## 3. AbortPolicy

默认拒绝策略。

当任务无法提交时，直接抛出异常：

```java
RejectedExecutionException
```

示例：

```java
new ThreadPoolExecutor.AbortPolicy();
```

特点：

```text
可以及时发现问题，但可能影响业务流程。
```

---

## 4. CallerRunsPolicy

由提交任务的线程自己执行该任务。

示例：

```java
new ThreadPoolExecutor.CallerRunsPolicy();
```

特点：

```text
不会丢任务；
可以降低任务提交速度；
适合需要削峰的场景。
```

---

## 5. DiscardPolicy

直接丢弃新任务，不抛异常。

示例：

```java
new ThreadPoolExecutor.DiscardPolicy();
```

特点：

```text
风险较高，任务会悄悄丢失。
```

---

## 6. DiscardOldestPolicy

丢弃队列中最老的任务，然后重新尝试提交当前任务。

示例：

```java
new ThreadPoolExecutor.DiscardOldestPolicy();
```

特点：

```text
可能丢失重要老任务。
```

---

## 7. 自定义拒绝策略

实际开发中，推荐自定义拒绝策略，记录日志、告警或持久化任务。

示例：

```java
RejectedExecutionHandler handler = (runnable, executor) -> {
    // 记录日志
    System.err.println("任务被拒绝：" + runnable);

    // 可以发送告警、落库、写 MQ 等
};
```

---

## 8. 面试回答

线程池内置四种拒绝策略。

`AbortPolicy` 是默认策略，会直接抛出异常。

`CallerRunsPolicy` 会让提交任务的线程自己执行任务。

`DiscardPolicy` 会直接丢弃任务，不抛异常。

`DiscardOldestPolicy` 会丢弃队列中最老的任务，再尝试提交新任务。

实际项目中通常会自定义拒绝策略，记录日志、告警或者把任务持久化，避免任务无感知丢失。

---

# 十二、如何防止线程池 OOM

## 1. 线程池为什么会导致 OOM？

线程池导致 OOM 常见原因有：

1. 使用无界队列；
2. 提交任务速度远大于消费速度；
3. 线程数设置过大；
4. 单个任务占用内存过大；
5. 任务执行时间过长；
6. 拒绝策略不合理；
7. 使用 `Executors` 创建线程池导致参数不可控。

---

## 2. 不推荐直接使用 Executors

例如：

```java
ExecutorService executor = Executors.newFixedThreadPool(10);
```

`newFixedThreadPool` 底层使用的是：

```java
LinkedBlockingQueue
```

默认容量是：

```text
Integer.MAX_VALUE
```

如果任务大量堆积，可能导致 OOM。

---

## 3. 使用有界队列

推荐使用有界队列，例如：

```java
BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(1000);
```

示例：

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
        10,
        20,
        60L,
        TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(1000),
        new ThreadPoolExecutor.CallerRunsPolicy()
);
```

---

## 4. 合理设置线程池参数

线程池参数需要根据任务类型设置。

### CPU 密集型任务

CPU 密集型任务主要消耗 CPU。

推荐线程数：

```text
CPU 核心数 + 1
```

---

### IO 密集型任务

IO 密集型任务经常等待网络、磁盘、数据库等 IO。

推荐线程数可以适当大一些：

```text
CPU 核心数 * 2
```

或者根据实际压测结果调整。

---

## 5. 设置合理拒绝策略

避免任务无限堆积。

推荐策略：

1. `CallerRunsPolicy`；
2. 自定义拒绝策略；
3. 记录日志；
4. 告警；
5. 任务落库；
6. 投递 MQ。

---

## 6. 控制任务提交速度

可以使用限流手段：

1. Sentinel；
2. Guava RateLimiter；
3. 滑动窗口；
4. 令牌桶；
5. 漏桶；
6. MQ 削峰。

---

## 7. 避免任务中保存大对象

任务对象如果携带大对象，例如大集合、大文件内容、大量上下文数据，会增加内存压力。

应该避免：

```java
executor.execute(() -> {
    // 持有大对象
});
```

尽量只传必要参数。

---

## 8. 做好监控和告警

线程池应该监控以下指标：

| 指标 | 含义 |
|---|---|
| activeCount | 活跃线程数 |
| poolSize | 当前线程数 |
| corePoolSize | 核心线程数 |
| maximumPoolSize | 最大线程数 |
| queueSize | 队列长度 |
| completedTaskCount | 已完成任务数 |
| rejectedCount | 拒绝任务数 |
| taskCostTime | 任务耗时 |

---

## 9. 合理拆分线程池

不同类型任务不要共用一个线程池。

例如：

```text
订单线程池
支付线程池
消息推送线程池
日志线程池
导出线程池
```

这样可以避免某一类慢任务拖垮整个系统。

---

## 10. 防止 OOM 的核心措施

核心措施包括：

1. 不使用无界队列；
2. 不使用不可控的 Executors；
3. 使用 `ThreadPoolExecutor` 显式创建线程池；
4. 设置合理核心线程数和最大线程数；
5. 使用有界队列；
6. 设置合理拒绝策略；
7. 对任务提交进行限流；
8. 监控线程池状态；
9. 对慢任务进行隔离；
10. 避免任务对象持有大对象。

---

## 11. 面试回答

线程池 OOM 主要是因为任务提交速度大于消费速度，导致任务在无界队列中大量堆积，或者线程数设置过大导致内存耗尽。

防止线程池 OOM，首先不要直接使用 `Executors` 创建线程池，因为它可能使用无界队列。应该使用 `ThreadPoolExecutor`，明确设置核心线程数、最大线程数、有界队列和拒绝策略。

同时要根据任务类型合理设置线程数，对任务提交进行限流，做好线程池监控和告警，并对不同业务线程池进行隔离。

---

# 十三、CountDownLatch、CyclicBarrier、Semaphore 区别

## 1. CountDownLatch

### 1.1 是什么？

`CountDownLatch` 是倒计时器。

它允许一个或多个线程等待其他线程完成操作。

---

### 1.2 使用场景

适合场景：

```text
一个线程等待多个线程执行完成后再继续执行。
```

例如：

```text
主线程等待多个子任务执行完成，再进行汇总。
```

---

### 1.3 示例

```java
import java.util.concurrent.CountDownLatch;

public class CountDownLatchDemo {

    public static void main(String[] args) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                System.out.println(Thread.currentThread().getName() + " 执行完成");
                latch.countDown();
            }).start();
        }

        latch.await();

        System.out.println("所有任务执行完成，主线程继续执行");
    }
}
```

---

### 1.4 特点

1. 基于计数器；
2. 调用 `countDown()` 计数减 1；
3. 调用 `await()` 阻塞等待；
4. 计数减到 0 后，等待线程被唤醒；
5. 不能重复使用。

---

## 2. CyclicBarrier

### 2.1 是什么？

`CyclicBarrier` 是循环屏障。

它让一组线程互相等待，直到所有线程都到达屏障点后，再一起继续执行。

---

### 2.2 使用场景

适合场景：

```text
多个线程互相等待，全部到齐后再一起继续执行。
```

例如：

```text
多人游戏加载，所有玩家加载完成后一起开始。
```

---

### 2.3 示例

```java
import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierDemo {

    public static void main(String[] args) {
        CyclicBarrier barrier = new CyclicBarrier(3, () -> {
            System.out.println("所有线程都到达屏障，开始下一步");
        });

        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " 到达屏障");
                    barrier.await();
                    System.out.println(Thread.currentThread().getName() + " 继续执行");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
```

---

### 2.4 特点

1. 多个线程互相等待；
2. 调用 `await()` 表示到达屏障；
3. 所有线程到达后一起继续执行；
4. 可以重复使用；
5. 可以设置屏障任务。

---

## 3. Semaphore

### 3.1 是什么？

`Semaphore` 是信号量。

它用于控制同时访问某个资源的线程数量。

---

### 3.2 使用场景

适合场景：

```text
限制并发访问数量。
```

例如：

```text
数据库连接池；
限流；
停车场车位控制；
接口并发访问控制。
```

---

### 3.3 示例

```java
import java.util.concurrent.Semaphore;

public class SemaphoreDemo {

    public static void main(String[] args) {
        Semaphore semaphore = new Semaphore(3);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    semaphore.acquire();

                    System.out.println(Thread.currentThread().getName() + " 获取许可，开始执行");

                    Thread.sleep(1000);

                    System.out.println(Thread.currentThread().getName() + " 释放许可");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    semaphore.release();
                }
            }).start();
        }
    }
}
```

---

### 3.4 特点

1. 控制并发数量；
2. `acquire()` 获取许可；
3. `release()` 释放许可；
4. 可以实现限流；
5. 可以设置公平或非公平模式。

---

## 4. 三者对比

| 对比项 | CountDownLatch | CyclicBarrier | Semaphore |
|---|---|---|---|
| 中文含义 | 倒计时器 | 循环屏障 | 信号量 |
| 核心作用 | 一个或多个线程等待其他线程完成 | 多个线程互相等待，到齐后继续 | 控制同时访问资源的线程数量 |
| 是否可复用 | 不可复用 | 可复用 | 可复用 |
| 主要方法 | `countDown()`、`await()` | `await()` | `acquire()`、`release()` |
| 计数变化 | 递减 | 递减后重置 | 获取减少，释放增加 |
| 使用场景 | 主线程等待多个子任务完成 | 多个线程分阶段协作 | 限制并发访问数量 |
| 底层实现 | AQS | ReentrantLock + Condition | AQS |

---

## 5. 面试回答

`CountDownLatch` 是倒计时器，用于一个或多个线程等待其他线程执行完成，计数减到 0 后等待线程继续执行，不能重复使用。

`CyclicBarrier` 是循环屏障，用于多个线程互相等待，所有线程都到达屏障后再一起继续执行，可以重复使用。

`Semaphore` 是信号量，用于控制同时访问某个资源的线程数量，通过 `acquire()` 获取许可，通过 `release()` 释放许可，常用于限流和资源池控制。

---

# 十四、面试速记版

## 1. 线程和进程区别

进程是操作系统资源分配的基本单位，线程是 CPU 调度执行的基本单位。

一个进程可以包含多个线程，线程共享进程资源，但也容易产生线程安全问题。

---

## 2. 创建线程方式

创建线程常见方式有：

```text
继承 Thread
实现 Runnable
实现 Callable
使用线程池
```

实际开发中推荐使用线程池。

---

## 3. ThreadLocal 原理

每个线程内部都有一个 `ThreadLocalMap`。

`ThreadLocal` 作为 key，数据作为 value。

每个线程访问的是自己线程中的数据副本。

使用完要调用 `remove()`，避免线程池场景下内存泄漏。

---

## 4. synchronized 原理

`synchronized` 底层基于 Monitor 实现。

同步代码块通过 `monitorenter` 和 `monitorexit` 实现。

同步方法通过 `ACC_SYNCHRONIZED` 标志实现。

JDK 1.6 后有锁升级机制：偏向锁、轻量级锁、重量级锁。

---

## 5. synchronized 和 ReentrantLock 区别

`synchronized` 是 JVM 关键字，自动释放锁，使用简单。

`ReentrantLock` 是 JDK API，基于 AQS，需要手动释放锁，支持公平锁、可中断、超时获取锁和多个 Condition。

---

## 6. volatile 作用

`volatile` 保证可见性，禁止指令重排序。

但它不能保证复合操作的原子性，比如 `i++` 不是线程安全的。

---

## 7. CAS 是什么

CAS 是比较并交换，是一种乐观锁机制。

如果内存值等于预期值，就更新为新值，否则更新失败。

CAS 存在 ABA 问题、自旋开销大、只能保证单变量原子操作等问题。

---

## 8. AQS 原理

AQS 是抽象队列同步器。

核心是：

```text
volatile int state + FIFO 双向队列
```

线程获取锁失败后进入队列等待，释放锁时唤醒后继节点。

---

## 9. 线程池核心参数

线程池七大核心参数：

```text
corePoolSize
maximumPoolSize
keepAliveTime
unit
workQueue
threadFactory
handler
```

---

## 10. 线程池执行流程

线程池执行顺序：

```text
核心线程 -> 任务队列 -> 非核心线程 -> 拒绝策略
```

---

## 11. 线程池拒绝策略

内置四种拒绝策略：

```text
AbortPolicy
CallerRunsPolicy
DiscardPolicy
DiscardOldestPolicy
```

实际项目中常自定义拒绝策略，记录日志、告警或持久化任务。

---

## 12. 防止线程池 OOM

防止线程池 OOM 的关键：

```text
不用无界队列
不用 Executors 默认线程池
使用 ThreadPoolExecutor
设置有界队列
设置合理拒绝策略
限制任务提交速度
做好监控告警
```

---

## 13. CountDownLatch、CyclicBarrier、Semaphore 区别

`CountDownLatch` 用于等待多个任务完成，不能复用。

`CyclicBarrier` 用于多个线程互相等待，到齐后继续执行，可以复用。

`Semaphore` 用于控制并发访问数量。

---

# 十五、总览表

| 问题 | 核心结论 |
|---|---|
| 线程和进程区别 | 进程是资源分配单位，线程是 CPU 调度单位 |
| 创建线程方式 | Thread、Runnable、Callable、线程池 |
| ThreadLocal 原理 | 每个 Thread 维护 ThreadLocalMap，ThreadLocal 为 key，value 为线程本地变量 |
| synchronized 原理 | 基于 Monitor，对象头 Mark Word 存储锁状态，支持锁升级 |
| synchronized 和 ReentrantLock 区别 | synchronized 简单自动释放；ReentrantLock 功能更强，基于 AQS |
| volatile 作用 | 保证可见性，禁止指令重排序，不保证原子性 |
| CAS 是什么 | 比较并交换，乐观锁思想 |
| AQS 原理 | state 表示同步状态，FIFO 队列保存等待线程 |
| 线程池核心参数 | 核心线程数、最大线程数、存活时间、队列、线程工厂、拒绝策略等 |
| 线程池执行流程 | 核心线程 -> 队列 -> 非核心线程 -> 拒绝策略 |
| 线程池拒绝策略 | Abort、CallerRuns、Discard、DiscardOldest |
| 如何防止线程池 OOM | 使用有界队列、合理参数、限流、监控、拒绝策略 |
| CountDownLatch、CyclicBarrier、Semaphore 区别 | 倒计时器、循环屏障、信号量 |

---

# 十六、完整面试回答模板

Java 中，进程是操作系统资源分配的基本单位，线程是 CPU 调度执行的基本单位。一个进程可以包含多个线程，线程之间共享进程资源，因此通信方便，但也容易产生线程安全问题。

创建线程的方式主要有四种：继承 `Thread` 类、实现 `Runnable` 接口、实现 `Callable` 接口配合 `FutureTask` 使用，以及使用线程池。实际开发中更推荐使用线程池，因为线程池可以复用线程，减少线程创建和销毁的开销。

`ThreadLocal` 用于保存线程本地变量。它的底层原理是每个 `Thread` 对象中都有一个 `ThreadLocalMap`，`ThreadLocal` 作为 key，具体变量作为 value。每个线程访问的都是自己线程中的变量副本。由于 `ThreadLocalMap` 的 key 是弱引用，value 是强引用，在线程池场景下如果不调用 `remove()`，可能导致内存泄漏。

`synchronized` 是 Java 中的同步关键字，可以保证原子性、可见性和有序性。它底层基于 Monitor 实现，同步代码块通过 `monitorenter` 和 `monitorexit` 实现，同步方法通过 `ACC_SYNCHRONIZED` 标志实现。JDK 1.6 之后，`synchronized` 引入了偏向锁、轻量级锁和重量级锁等优化。

`synchronized` 和 `ReentrantLock` 都是可重入锁。`synchronized` 是 JVM 关键字，使用简单，可以自动释放锁；`ReentrantLock` 是 JDK 提供的显式锁，底层基于 AQS，需要手动释放锁，但支持公平锁、可中断锁、超时获取锁、尝试获取锁和多个 Condition。

`volatile` 是轻量级同步机制，主要作用是保证变量可见性和禁止指令重排序。但它不能保证复合操作的原子性，例如 `i++` 即使加了 `volatile`，也不是线程安全的。

CAS 是比较并交换，是一种乐观锁机制。它会比较内存值和预期值是否相等，如果相等就更新为新值，否则更新失败。CAS 常用于原子类中，但存在 ABA 问题、自旋开销大、只能保证单个变量原子操作等问题。

AQS 是抽象队列同步器，是很多并发工具类的基础。它内部通过一个 `volatile int state` 表示同步状态，并通过 FIFO 双向队列保存获取锁失败的线程。`ReentrantLock`、`Semaphore`、`CountDownLatch` 等都基于 AQS 实现。

线程池的核心参数有七个：核心线程数、最大线程数、非核心线程空闲存活时间、时间单位、任务队列、线程工厂和拒绝策略。线程池执行流程是：先创建核心线程，核心线程满了以后任务进入队列，队列满了以后创建非核心线程，线程数达到最大线程数且队列也满了，就执行拒绝策略。

线程池的拒绝策略有四种：`AbortPolicy` 直接抛异常，`CallerRunsPolicy` 由提交任务的线程执行任务，`DiscardPolicy` 直接丢弃任务，`DiscardOldestPolicy` 丢弃队列中最老的任务。实际开发中通常会自定义拒绝策略，记录日志、告警或持久化任务。

为了防止线程池 OOM，不建议直接使用 `Executors` 创建线程池，因为可能使用无界队列。应该使用 `ThreadPoolExecutor` 显式设置参数，使用有界队列，设置合理的拒绝策略，并对任务提交进行限流，同时做好线程池监控和告警。

`CountDownLatch` 是倒计时器，用于一个或多个线程等待其他线程完成，不能复用。`CyclicBarrier` 是循环屏障，用于多个线程互相等待，所有线程都到达屏障后一起继续执行，可以复用。`Semaphore` 是信号量，用于控制同时访问某个资源的线程数量，常用于限流和资源池控制。
