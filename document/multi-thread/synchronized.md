# synchronized 总结

`synchronized` 是 Java 提供的内置锁机制，用于保证多线程环境下共享资源访问的线程安全。

它可以保证：

- 原子性
- 可见性
- 有序性

---

## 1. synchronized 的作用

当多个线程同时访问共享变量或共享资源时，可能出现并发安全问题。

`synchronized` 可以保证同一时刻只有一个线程进入同步代码块或同步方法。

```java
public synchronized void increment() {
    count++;
}
```

等价理解：

```text
线程进入 synchronized 区域前，需要先获取锁
获取锁成功后才能执行代码
执行完成后释放锁
其他线程才能继续竞争锁
```

---

## 2. synchronized 的使用方式

### 1. 修饰实例方法

锁对象是当前实例对象 `this`。

```java
public synchronized void method() {
    // 临界区代码
}
```

等价于：

```java
public void method() {
    synchronized (this) {
        // 临界区代码
    }
}
```

---

### 2. 修饰静态方法

锁对象是当前类的 `Class` 对象。

```java
public static synchronized void method() {
    // 临界区代码
}
```

等价于：

```java
synchronized (UserService.class) {
    // 临界区代码
}
```

---

### 3. 修饰代码块

锁对象由自己指定。

```java
private final Object lock = new Object();

public void method() {
    synchronized (lock) {
        // 临界区代码
    }
}
```

推荐使用这种方式，锁粒度更容易控制。

---

## 3. 锁对象是什么

`synchronized` 锁的是对象，不是代码。

不同写法对应不同锁对象：

| 写法 | 锁对象 |
|---|---|
| `synchronized` 实例方法 | 当前对象 `this` |
| `synchronized` 静态方法 | 当前类的 `Class` 对象 |
| `synchronized(obj)` | 指定的 `obj` 对象 |

示例：

```java
public synchronized void methodA() {}

public synchronized void methodB() {}
```

`methodA` 和 `methodB` 锁的都是同一个对象 `this`，所以同一个实例下两个方法会互斥。

---

## 4. synchronized 的实现原理

`synchronized` 底层依赖 JVM 实现，主要通过 **对象头 Mark Word** 和 **Monitor 监视器锁** 实现。

每个 Java 对象都可以作为锁对象。

对象头中的 Mark Word 会记录锁相关信息，例如：

```text
无锁状态
偏向锁
轻量级锁
重量级锁
GC 标记
hashCode
```

当线程进入 `synchronized` 代码块时，会尝试获取对象关联的 Monitor。

```text
获取 Monitor 成功
  ↓
进入同步代码块

获取 Monitor 失败
  ↓
阻塞等待
```

---

## 5. 字节码层面实现

### 同步代码块

```java
synchronized (lock) {
    // code
}
```

编译后会生成：

```text
monitorenter
monitorexit
```

其中：

- `monitorenter`：尝试获取锁。
- `monitorexit`：释放锁。

---

### 同步方法

```java
public synchronized void method() {}
```

同步方法不会显式生成 `monitorenter` 和 `monitorexit`，而是通过方法访问标志：

```text
ACC_SYNCHRONIZED
```

JVM 执行方法时看到该标志，会自动加锁和释放锁。

---

## 6. Monitor 机制

每个对象都可以关联一个 Monitor。

Monitor 中主要有：

```text
Owner：当前持有锁的线程
EntryList：等待获取锁的线程队列
WaitSet：调用 wait() 后等待的线程集合
```

执行流程：

```text
线程尝试进入 synchronized
  ↓
如果 Monitor 没有 Owner，当前线程成为 Owner
  ↓
如果 Monitor 已有 Owner，当前线程进入 EntryList 阻塞
  ↓
Owner 执行完同步代码后释放锁
  ↓
EntryList 中线程重新竞争锁
```

---

## 7. synchronized 的锁升级

早期 JVM 为了优化 `synchronized` 性能，引入了锁升级机制。

锁状态大致如下：

```text
无锁 -> 偏向锁 -> 轻量级锁 -> 重量级锁
```

### 1. 无锁

对象还没有被任何线程加锁。

---

### 2. 偏向锁

偏向锁用于优化只有一个线程反复进入同步块的场景。

```text
第一次线程 A 获取锁
  ↓
对象头记录线程 A 的 ID
  ↓
之后线程 A 再进入同步块，不需要 CAS
```

优点：

- 减少无竞争场景下的加锁成本。

缺点：

- 如果有其他线程竞争，需要撤销偏向锁。

> 注意：较新的 JDK 中偏向锁已经逐渐被废弃或移除，面试中了解即可。

---

### 3. 轻量级锁

当多个线程交替进入同步块，但没有激烈竞争时，会使用轻量级锁。

实现方式主要依赖 CAS。

```text
线程尝试通过 CAS 把对象头 Mark Word 指向自己的锁记录
成功：获得锁
失败：说明存在竞争
```

如果竞争不激烈，线程可能自旋等待，避免直接阻塞。

---

### 4. 重量级锁

当竞争激烈，自旋失败后，锁会膨胀为重量级锁。

重量级锁依赖操作系统 Mutex。

特点：

- 线程会进入阻塞状态。
- 涉及用户态和内核态切换。
- 性能开销较大。

---

## 8. synchronized 的可重入性

`synchronized` 是可重入锁。

同一个线程已经获取某个对象锁后，可以再次进入该对象锁保护的代码。

示例：

```java
public synchronized void methodA() {
    methodB();
}

public synchronized void methodB() {
    // 同一个线程可以再次进入
}
```

如果不可重入，`methodA()` 调用 `methodB()` 时会自己把自己阻塞，产生死锁。

### 实现原理

Monitor 内部会记录：

```text
Owner：持有锁的线程
recursions：重入次数
```

同一个线程重复进入时，重入次数加一；退出一次，重入次数减一；减到零时真正释放锁。

---

## 9. synchronized 的可见性

`synchronized` 不仅保证互斥，也保证内存可见性。

规则：

```text
线程释放锁前，对共享变量的修改会刷新到主内存
线程获取锁后，会从主内存读取最新值
```

所以：

```text
同一把锁保护的代码块中，共享变量对其他线程可见
```

---

## 10. synchronized 和 wait/notify

`wait()`、`notify()`、`notifyAll()` 必须在 synchronized 中使用。

示例：

```java
synchronized (lock) {
    while (!condition) {
        lock.wait();
    }

    // 条件满足，继续执行
}
```

唤醒：

```java
synchronized (lock) {
    condition = true;
    lock.notifyAll();
}
```

### wait 和 notify 说明

| 方法 | 说明 |
|---|---|
| `wait()` | 释放锁并进入等待队列 |
| `notify()` | 唤醒一个等待线程 |
| `notifyAll()` | 唤醒所有等待线程 |

注意：

```text
wait() 会释放锁
sleep() 不会释放锁
```

---

## 11. wait 和 sleep 区别

| 对比项 | wait | sleep |
|---|---|---|
| 所属类 | Object | Thread |
| 是否释放锁 | 会释放锁 | 不释放锁 |
| 是否需要 synchronized | 需要 | 不需要 |
| 唤醒方式 | notify / notifyAll / 超时 | 时间到 / interrupt |
| 主要用途 | 线程间通信 | 线程休眠 |

---

## 12. synchronized 和 ReentrantLock 区别

| 对比项 | synchronized | ReentrantLock |
|---|---|---|
| 实现层面 | JVM 内置 | JDK AQS 实现 |
| 是否可重入 | 是 | 是 |
| 是否可中断 | 不支持等待锁时中断 | 支持 lockInterruptibly |
| 是否支持公平锁 | 不支持 | 支持 |
| 是否支持超时获取锁 | 不支持 | 支持 tryLock |
| 是否需要手动释放 | 不需要 | 需要 finally unlock |
| 条件队列 | wait/notify | Condition |
| 使用复杂度 | 简单 | 较复杂 |
| 性能 | JDK 1.6 后优化较好 | 功能更灵活 |

### 选择建议

- 简单同步场景用 `synchronized`。
- 需要公平锁、可中断、超时等待、多条件队列时，用 `ReentrantLock`。

---

## 13. synchronized 和 volatile 区别

| 对比项 | synchronized | volatile |
|---|---|---|
| 是否保证原子性 | 可以保证 | 不能保证复合操作原子性 |
| 是否保证可见性 | 保证 | 保证 |
| 是否保证有序性 | 保证 | 保证一定程度有序性 |
| 是否加锁 | 加锁 | 不加锁 |
| 使用场景 | 临界区互斥 | 状态标记、开关变量 |

示例：

```java
volatile boolean running = true;
```

适合做状态标记。

但下面这种不适合只用 volatile：

```java
count++;
```

因为 `count++` 不是原子操作。

---

## 14. synchronized 常见问题

### 1. 锁对象选择不当

不推荐锁字符串常量：

```java
synchronized ("lock") {
}
```

因为字符串常量池可能导致不同代码锁住同一个对象。

推荐：

```java
private final Object lock = new Object();
```

---

### 2. 锁粒度过大

不推荐：

```java
public synchronized void process() {
    // 大量业务逻辑
}
```

推荐：

```java
public void process() {
    // 非共享资源逻辑

    synchronized (lock) {
        // 只锁共享资源操作
    }

    // 非共享资源逻辑
}
```

---

### 3. 锁中调用远程接口

不推荐：

```java
synchronized (lock) {
    remoteService.call();
    updateLocalData();
}
```

远程接口慢会导致锁长时间不释放。

---

### 4. 死锁

示例：

```java
synchronized (lockA) {
    synchronized (lockB) {
    }
}
```

另一个线程：

```java
synchronized (lockB) {
    synchronized (lockA) {
    }
}
```

两个线程加锁顺序不一致，可能产生死锁。

### 避免死锁

- 保持固定加锁顺序。
- 减少嵌套锁。
- 缩短锁持有时间。
- 尽量避免锁中调用外部方法。

---

## 15. synchronized 使用建议

- 锁对象尽量使用私有 final 对象。
- 尽量缩小同步代码块范围。
- 不要锁字符串常量和公共对象。
- 不要在锁内执行耗时操作。
- 避免嵌套锁。
- 同一资源用同一把锁保护。
- wait 条件判断要用 `while`，不要用 `if`。

---

## 16. 总结

`synchronized` 是 Java 内置的同步机制，可以修饰实例方法、静态方法和代码块。实例方法锁的是当前对象 `this`，静态方法锁的是当前类的 `Class` 对象，代码块锁的是指定对象。

`synchronized` 底层通过对象头 Mark Word 和 Monitor 实现。同步代码块在字节码层面对应 `monitorenter` 和 `monitorexit` 指令，同步方法通过 `ACC_SYNCHRONIZED` 标志实现。

`synchronized` 是可重入锁，同一个线程重复获取同一把锁不会被阻塞。它既能保证原子性，也能保证可见性和有序性。

JDK 对 `synchronized` 做了很多优化，包括偏向锁、轻量级锁、自旋锁和重量级锁。无竞争时开销较小，竞争激烈时会膨胀为重量级锁。

一句话总结：

```text
synchronized 的核心是：基于对象 Monitor 实现互斥同步，通过 JVM 保证同一时刻只有一个线程进入临界区，并保证共享变量的可见性。
```
