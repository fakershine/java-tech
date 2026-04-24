# ReentrantLock 总结

`ReentrantLock` 是 Java 提供的可重入锁，位于 `java.util.concurrent.locks` 包下。  
它和 `synchronized` 类似，都可以保证多线程访问共享资源时的线程安全，但 `ReentrantLock` 功能更灵活。

---

## 1. ReentrantLock 的作用

`ReentrantLock` 用于控制多个线程对共享资源的并发访问。

```java
private final ReentrantLock lock = new ReentrantLock();

public void update() {
    lock.lock();
    try {
        // 临界区代码
    } finally {
        lock.unlock();
    }
}
```

核心特点：

- 可重入。
- 可中断。
- 可超时获取锁。
- 支持公平锁和非公平锁。
- 支持多个条件队列。

---

## 2. 基本使用

### 标准写法

```java
private final ReentrantLock lock = new ReentrantLock();

public void method() {
    lock.lock();
    try {
        // 执行业务逻辑
    } finally {
        lock.unlock();
    }
}
```

必须在 `finally` 中释放锁，避免异常导致锁无法释放。

---

## 3. 可重入性

### 实现原理

可重入指的是：同一个线程已经获取锁后，可以再次获取同一把锁，不会被自己阻塞。

示例：

```java
public void methodA() {
    lock.lock();
    try {
        methodB();
    } finally {
        lock.unlock();
    }
}

public void methodB() {
    lock.lock();
    try {
        // 同一个线程可以再次获取锁
    } finally {
        lock.unlock();
    }
}
```

内部会维护一个重入次数：

```text
第一次加锁：state = 1
第二次重入：state = 2
释放一次：state = 1
再次释放：state = 0，真正释放锁
```

---

## 4. 底层实现原理

`ReentrantLock` 底层基于 **AQS** 实现。

AQS 全称是：

```text
AbstractQueuedSynchronizer
```

核心结构：

```text
state：表示锁状态
CLH 队列：保存等待获取锁的线程
exclusiveOwnerThread：当前持有锁的线程
```

### 加锁流程

```text
线程尝试 CAS 修改 state
  ↓
如果 state = 0，说明锁空闲，抢锁成功
  ↓
如果 state != 0，判断持锁线程是否是当前线程
  ↓
如果是当前线程，state + 1，实现可重入
  ↓
如果不是当前线程，进入 AQS 队列等待
```

### 解锁流程

```text
释放锁时 state - 1
  ↓
如果 state > 0，说明还有重入，锁不释放
  ↓
如果 state = 0，清空持锁线程
  ↓
唤醒 AQS 队列中的后继线程
```

---

## 5. 公平锁和非公平锁

### 非公平锁

默认是非公平锁：

```java
ReentrantLock lock = new ReentrantLock();
```

非公平锁允许新来的线程直接竞争锁，不一定按照排队顺序获取锁。

特点：

- 吞吐量高。
- 可能导致部分线程等待时间较长。
- 默认使用。

---

### 公平锁

创建公平锁：

```java
ReentrantLock lock = new ReentrantLock(true);
```

公平锁会尽量按照线程排队顺序获取锁。

特点：

- 更公平。
- 减少线程饥饿。
- 性能通常低于非公平锁。

---

### 对比

| 类型 | 特点 | 优势 | 劣势 |
|---|---|---|---|
| 非公平锁 | 允许插队 | 吞吐量高 | 可能线程饥饿 |
| 公平锁 | 按队列顺序获取锁 | 更公平 | 性能较低 |

---

## 6. lockInterruptibly 可中断获取锁

### 实现原理

普通 `lock()` 获取锁时，如果线程阻塞，不能响应中断。

```java
lock.lock();
```

`lockInterruptibly()` 可以在等待锁时响应中断。

```java
try {
    lock.lockInterruptibly();
    try {
        // 临界区代码
    } finally {
        lock.unlock();
    }
} catch (InterruptedException e) {
    // 等锁过程中被中断
}
```

### 适用场景

- 避免线程长时间等待锁。
- 支持任务取消。
- 避免死锁时线程无法退出。

---

## 7. tryLock 尝试获取锁

### 立即尝试获取锁

```java
if (lock.tryLock()) {
    try {
        // 获取锁成功
    } finally {
        lock.unlock();
    }
} else {
    // 获取锁失败，直接返回
}
```

### 超时尝试获取锁

```java
if (lock.tryLock(3, TimeUnit.SECONDS)) {
    try {
        // 获取锁成功
    } finally {
        lock.unlock();
    }
} else {
    // 3 秒内没拿到锁，放弃
}
```

### 优势

- 避免无限等待。
- 可以做降级处理。
- 适合高并发下快速失败场景。

---

## 8. Condition 条件队列

### 实现原理

`ReentrantLock` 可以配合 `Condition` 实现线程等待和唤醒。

类似于：

```text
synchronized + wait/notify
```

但 `Condition` 更灵活，可以创建多个条件队列。

示例：

```java
private final ReentrantLock lock = new ReentrantLock();
private final Condition notEmpty = lock.newCondition();
private final Condition notFull = lock.newCondition();
```

等待：

```java
lock.lock();
try {
    while (queue.isEmpty()) {
        notEmpty.await();
    }
} finally {
    lock.unlock();
}
```

唤醒：

```java
lock.lock();
try {
    notEmpty.signal();
} finally {
    lock.unlock();
}
```

---

## 9. Condition 和 wait/notify 区别

| 对比项 | Condition | wait/notify |
|---|---|---|
| 所属 | ReentrantLock | synchronized |
| 等待队列 | 可以有多个 | 一个对象只有一个 WaitSet |
| 唤醒方式 | signal / signalAll | notify / notifyAll |
| 灵活性 | 更高 | 较低 |
| 使用前提 | 必须先获取 Lock | 必须进入 synchronized |

简单理解：

```text
Condition 是 wait/notify 的增强版。
```

---

## 10. ReentrantLock 和 synchronized 区别

| 对比项 | ReentrantLock | synchronized |
|---|---|---|
| 实现方式 | JDK 层面，基于 AQS | JVM 内置，基于 Monitor |
| 是否可重入 | 是 | 是 |
| 是否需要手动释放锁 | 是 | 否 |
| 是否支持公平锁 | 支持 | 不支持 |
| 是否支持中断等待 | 支持 | 不支持 |
| 是否支持超时获取锁 | 支持 | 不支持 |
| 条件队列 | 支持多个 Condition | 一个 WaitSet |
| 使用复杂度 | 较高 | 简单 |
| 异常安全 | 需要 finally unlock | JVM 自动释放 |

### 选择建议

- 简单同步场景：优先使用 `synchronized`。
- 需要公平锁、可中断、超时获取锁、多个条件队列：使用 `ReentrantLock`。

---

## 11. 常见使用场景

- 复杂并发控制。
- 需要尝试获取锁的场景。
- 需要超时等待锁的场景。
- 需要响应中断的场景。
- 需要公平锁的场景。
- 生产者消费者模型。
- 多条件队列场景。

---

## 12. 使用注意点

- 加锁后必须在 `finally` 中释放锁。
- 不要在锁内执行耗时操作。
- 避免锁粒度过大。
- 避免多个锁嵌套导致死锁。
- 使用 `Condition.await()` 时要放在 `while` 中判断条件。
- 公平锁性能通常低于非公平锁，不要盲目使用。
- `tryLock()` 获取失败时要有明确处理逻辑。

---

## 13. 总结

`ReentrantLock` 是 Java 中基于 AQS 实现的可重入锁。它通过 `state` 表示锁状态，通过 CAS 尝试获取锁，通过 AQS 队列管理等待线程。

当 `state = 0` 时，线程可以通过 CAS 获取锁；如果锁已经被当前线程持有，则 `state + 1`，实现可重入；释放锁时 `state - 1`，只有减到 `0` 时才真正释放锁并唤醒后续等待线程。

相比 `synchronized`，`ReentrantLock` 功能更灵活，支持公平锁、非公平锁、可中断获取锁、超时获取锁以及多个 `Condition` 条件队列。但它需要手动释放锁，所以必须在 `finally` 中调用 `unlock()`。

一句话总结：

```text
ReentrantLock = AQS + CAS + state + 阻塞队列，实现了功能更灵活的可重入独占锁。
```
