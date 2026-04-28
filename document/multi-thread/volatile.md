# volatile 总结

`volatile` 是 Java 提供的轻量级同步机制，主要用于保证变量在多线程之间的 **可见性** 和一定程度的 **有序性**。

它适合用于状态标记、开关变量等场景，但不能替代锁。

---

## 1. volatile 的作用

`volatile` 主要有两个作用：

- 保证可见性。
- 禁止指令重排序。

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

如果 `running` 不加 `volatile`，一个线程修改了它，另一个线程可能无法及时感知。

---

## 2. 可见性

### 问题背景

每个线程都有自己的工作内存，线程可能从工作内存中读取变量，而不是每次都从主内存读取。

```text
线程 A 修改变量
  ↓
变量写入主内存

线程 B 读取变量
  ↓
可能仍然读取旧值
```

### volatile 如何解决

被 `volatile` 修饰的变量：

```text
写 volatile 变量时，会立即刷新到主内存
读 volatile 变量时，会从主内存读取最新值
```

因此可以保证一个线程对 volatile 变量的修改，其他线程能够立即看到。

---

## 3. 有序性

### 什么是指令重排序

为了提高执行效率，编译器和 CPU 可能会在不影响单线程结果的前提下调整指令执行顺序。

例如：

```java
a = 1;
b = 2;
```

在单线程下，只要最终结果一致，底层可能调整执行顺序。

### volatile 如何解决

`volatile` 会通过内存屏障禁止特定类型的指令重排序。

简单理解：

```text
volatile 写之前的操作，不能重排到 volatile 写之后
volatile 读之后的操作，不能重排到 volatile 读之前
```

---

## 4. volatile 的底层原理

`volatile` 底层主要依赖 **内存屏障** 实现。

内存屏障可以理解为一条特殊指令，用于限制 CPU 和编译器的重排序，并保证内存可见性。

### volatile 写

```java
volatileVar = 1;
```

作用：

```text
1. volatile 写之前的普通写，不能重排到 volatile 写之后。
2. volatile 写会立即刷新到主内存。
```

### volatile 读

```java
int value = volatileVar;
```

作用：

```text
1. volatile 读会从主内存读取最新值。
2. volatile 读之后的普通读写，不能重排到 volatile 读之前。
```

---

## 5. volatile 不能保证原子性

`volatile` 不能保证复合操作的原子性。

例如：

```java
private volatile int count = 0;

public void increment() {
    count++;
}
```

`count++` 不是一个原子操作，它包含：

```text
1. 读取 count
2. count + 1
3. 写回 count
```

多个线程同时执行时，仍然可能出现并发问题。

### 解决方式

可以使用：

```java
AtomicInteger count = new AtomicInteger(0);

count.incrementAndGet();
```

或者使用锁：

```java
synchronized void increment() {
    count++;
}
```

---

## 6. volatile 适合的场景

### 1. 状态标记

```java
private volatile boolean running = true;
```

用于控制线程停止。

---

### 2. 开关变量

```java
private volatile boolean enabled = false;
```

用于动态控制某个功能是否开启。

---

### 3. 双重检查锁 DCL

单例模式中常见用法：

```java
public class Singleton {

    private static volatile Singleton instance;

    private Singleton() {}

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

这里 `volatile` 的作用是防止对象创建过程发生指令重排序。

对象创建大致分为：

```text
1. 分配内存
2. 初始化对象
3. 将引用赋值给 instance
```

如果没有 `volatile`，可能发生重排序：

```text
1. 分配内存
2. 将引用赋值给 instance
3. 初始化对象
```

这样其他线程可能拿到一个还没有初始化完成的对象。

---

## 7. volatile 不适合的场景

### 1. 复合操作

不适合：

```java
count++;
```

因为它不是原子操作。

---

### 2. 多变量状态一致性

不适合：

```java
volatile int a;
volatile int b;
```

如果业务要求 `a` 和 `b` 必须一起更新并保持一致，`volatile` 无法保证整体原子性。

---

### 3. 需要互斥访问的场景

如果多个线程需要对共享资源进行读写互斥，应该使用：

- `synchronized`
- `ReentrantLock`
- 原子类
- 并发容器

---

## 8. volatile 和 synchronized 区别

| 对比项 | volatile | synchronized |
|---|---|---|
| 是否保证可见性 | 是 | 是 |
| 是否保证有序性 | 是 | 是 |
| 是否保证原子性 | 不能保证复合操作 | 可以保证 |
| 是否加锁 | 不加锁 | 加锁 |
| 是否阻塞线程 | 不阻塞 | 可能阻塞 |
| 性能 | 较轻量 | 相对较重 |
| 适用场景 | 状态标记、开关变量 | 临界区互斥、复合操作 |

简单理解：

```text
volatile 解决变量可见性问题。
synchronized 解决临界区互斥问题。
```

---

## 9. volatile 和 AtomicInteger 区别

| 对比项 | volatile | AtomicInteger |
|---|---|---|
| 可见性 | 保证 | 保证 |
| 原子性 | 不保证复合操作 | 保证原子更新 |
| 底层实现 | 内存屏障 | CAS + volatile |
| 适用场景 | 状态标记 | 计数、自增、自减 |

示例：

不安全：

```java
private volatile int count = 0;

count++;
```

安全：

```java
private AtomicInteger count = new AtomicInteger(0);

count.incrementAndGet();
```

---

## 10. volatile 的 happens-before 规则

Java 内存模型规定：

```text
对一个 volatile 变量的写操作，happens-before 后续对这个 volatile 变量的读操作。
```

意思是：

```text
线程 A 写 volatile 变量之前的所有操作
对线程 B 读到这个 volatile 变量之后都是可见的
```

示例：

```java
int data = 0;
volatile boolean ready = false;

// 线程 A
data = 100;
ready = true;

// 线程 B
if (ready) {
    System.out.println(data); // 一定能看到 data = 100
}
```

当线程 B 看到 `ready = true` 时，也能看到线程 A 在写 `ready` 之前对 `data` 的修改。

---

## 11. 使用注意点

- `volatile` 适合一写多读的状态标记。
- `volatile` 不适合计数器自增。
- `volatile` 不能保证多个操作的原子性。
- 多个变量之间的一致性不能只靠 `volatile`。
- DCL 单例中必须使用 `volatile` 防止重排序。
- 如果需要互斥访问，应该使用锁或原子类。

---

## 12. 总结

`volatile` 是 Java 中的轻量级同步机制，主要保证变量的可见性和有序性。

可见性是指一个线程修改了 volatile 变量后，其他线程能够立即看到最新值；有序性是指 volatile 会通过内存屏障禁止特定指令重排序。

但是 `volatile` 不能保证复合操作的原子性，例如 `count++` 仍然不是线程安全的，因为它包含读取、计算、写回三个步骤。

因此，`volatile` 适合用于状态标记、开关变量、DCL 单例中的对象引用等场景。如果需要保证复合操作的线程安全，应该使用 `synchronized`、`ReentrantLock` 或 `AtomicInteger` 等工具。

一句话总结：

```text
volatile 保证可见性和有序性，但不保证复合操作的原子性。
```
