# AbstractQueuedSynchronizer 总结

`AbstractQueuedSynchronizer`，简称 **AQS**，是 Java 并发包中非常核心的同步器框架。

很多并发工具底层都基于 AQS 实现，例如：

- ReentrantLock
- CountDownLatch
- Semaphore
- ReentrantReadWriteLock
- FutureTask
- ThreadPoolExecutor 中部分组件

AQS 的核心作用是：**用一个 state 状态变量 + 一个 FIFO 等待队列，来实现线程同步。**

---

## 1. AQS 的核心思想

AQS 把同步器的实现抽象成两个核心部分：

```text
state 状态变量
    +
CLH FIFO 等待队列
```

可以简单理解为：

```text
线程先尝试修改 state 获取锁
获取成功，继续执行
获取失败，进入等待队列阻塞
锁释放后，唤醒队列中的后继线程
```

---

## 2. AQS 的核心结构

### 1. state 状态变量

AQS 内部维护一个 `volatile int state`。

```java
private volatile int state;
```

`state` 表示同步状态。

不同组件中含义不同：

| 组件 | state 含义 |
|---|---|
| ReentrantLock | 锁重入次数 |
| Semaphore | 剩余许可证数量 |
| CountDownLatch | 剩余计数 |
| ReentrantReadWriteLock | 读写锁状态 |

---

### 2. FIFO 等待队列

当线程获取锁失败时，会被封装成一个 Node 节点，加入 AQS 的同步队列。

队列结构：

```text
head -> node1 -> node2 -> node3 -> tail
```

每个 Node 代表一个等待线程。

Node 中主要保存：

```text
thread：当前线程
prev：前驱节点
next：后继节点
waitStatus：等待状态
```

---

## 3. AQS 的两种模式

AQS 支持两种同步模式：

| 模式 | 说明 | 典型实现 |
|---|---|---|
| 独占模式 | 同一时刻只允许一个线程获取锁 | ReentrantLock |
| 共享模式 | 同一时刻允许多个线程获取资源 | Semaphore、CountDownLatch |

---

## 4. 独占模式实现原理

以 `ReentrantLock` 为例。

### 加锁流程

```text
1. 线程尝试通过 CAS 修改 state。
2. 如果 state = 0，说明锁空闲，当前线程获取锁成功。
3. 如果 state != 0，判断持锁线程是否是当前线程。
4. 如果是当前线程，state + 1，实现可重入。
5. 如果不是当前线程，获取锁失败，进入 AQS 队列等待。
```

简化流程：

```text
尝试获取锁
  ↓
成功：执行业务
  ↓
失败：加入等待队列
  ↓
阻塞当前线程
  ↓
等待被唤醒后重新竞争锁
```

---

### 解锁流程

```text
1. 当前线程释放锁。
2. state - 1。
3. 如果 state > 0，说明还有重入次数，不真正释放。
4. 如果 state = 0，说明锁完全释放。
5. 清空当前持锁线程。
6. 唤醒等待队列中的后继节点。
```

---

## 5. 共享模式实现原理

以 `Semaphore` 为例。

### 获取资源

```text
1. 判断 state 是否大于 0。
2. 如果 state > 0，说明还有许可证。
3. 通过 CAS 扣减 state。
4. 扣减成功，获取资源。
5. 如果 state = 0，说明没有许可证，线程进入队列等待。
```

### 释放资源

```text
1. 释放许可证。
2. state + 1。
3. 唤醒等待队列中的线程。
4. 多个线程可以同时获取资源。
```

---

## 6. AQS 中的 Node 节点

AQS 队列中的每个等待线程都会被封装成 Node。

Node 主要字段：

| 字段 | 说明 |
|---|---|
| thread | 当前等待的线程 |
| prev | 前驱节点 |
| next | 后继节点 |
| waitStatus | 节点等待状态 |
| nextWaiter | 用于区分共享模式或条件队列 |

常见 `waitStatus`：

| 状态 | 说明 |
|---|---|
| 0 | 默认状态 |
| CANCELLED | 节点取消等待 |
| SIGNAL | 当前节点释放后需要唤醒后继节点 |
| CONDITION | 节点在 Condition 队列中等待 |
| PROPAGATE | 共享模式下传播唤醒 |

---

## 7. AQS 如何阻塞和唤醒线程

AQS 底层通过 `LockSupport` 实现线程阻塞和唤醒。

阻塞线程：

```java
LockSupport.park();
```

唤醒线程：

```java
LockSupport.unpark(thread);
```

流程：

```text
线程获取锁失败
  ↓
加入 AQS 队列
  ↓
LockSupport.park() 阻塞线程
  ↓
前驱节点释放锁
  ↓
LockSupport.unpark() 唤醒后继线程
```

---

## 8. AQS 为什么使用 CAS

AQS 中对 `state` 的修改通常通过 CAS 完成。

例如：

```java
compareAndSetState(expect, update)
```

作用：

```text
保证多个线程同时修改 state 时的原子性
```

例如 ReentrantLock 加锁时：

```text
多个线程同时抢锁
只有一个线程 CAS 成功
其他线程 CAS 失败后进入队列
```

---

## 9. ReentrantLock 和 AQS 的关系

`ReentrantLock` 内部有一个 Sync 类继承 AQS。

```java
abstract static class Sync extends AbstractQueuedSynchronizer {
}
```

它通过重写 AQS 的方法实现加锁逻辑：

```java
tryAcquire()
tryRelease()
```

AQS 负责：

```text
排队
阻塞
唤醒
状态维护
```

ReentrantLock 负责：

```text
定义 state 的含义
定义如何获取锁
定义如何释放锁
```

---

## 10. AQS 的模板方法

AQS 使用模板方法设计模式。

AQS 已经封装好了：

- 入队逻辑
- 阻塞逻辑
- 唤醒逻辑
- CAS 修改状态
- 队列维护

子类只需要实现：

| 方法 | 说明 |
|---|---|
| tryAcquire | 独占模式获取资源 |
| tryRelease | 独占模式释放资源 |
| tryAcquireShared | 共享模式获取资源 |
| tryReleaseShared | 共享模式释放资源 |
| isHeldExclusively | 判断是否独占持有 |

---

## 11. Condition 实现原理

AQS 还支持 `Condition` 条件队列。

`Condition` 用于实现类似：

```text
wait / notify
```

但它比 `wait/notify` 更灵活，可以有多个条件队列。

### await 流程

```text
1. 当前线程必须先持有锁。
2. 调用 await 后，线程加入 Condition 队列。
3. 释放当前持有的锁。
4. 当前线程阻塞等待。
```

### signal 流程

```text
1. 当前线程必须先持有锁。
2. 调用 signal。
3. 将 Condition 队列中的节点转移到 AQS 同步队列。
4. 等待重新竞争锁。
```

注意：

```text
signal 只是把线程从 Condition 队列转移到同步队列，
并不是马上执行。
```

---

## 12. AQS 和 synchronized 的区别

| 对比项 | AQS | synchronized |
|---|---|---|
| 实现层面 | JDK 代码实现 | JVM 内置实现 |
| 底层机制 | state + 队列 + CAS + LockSupport | Monitor |
| 是否可扩展 | 可扩展 | 不可扩展 |
| 支持公平锁 | 可以支持 | 不支持 |
| 支持中断 | 可以支持 | 不支持等待锁时中断 |
| 支持超时 | 可以支持 | 不支持 |
| 条件队列 | 支持多个 Condition | 一个 WaitSet |
| 典型实现 | ReentrantLock、Semaphore | synchronized 方法/代码块 |

---

## 13. AQS 常见实现类

| 工具类 | 模式 | state 含义 |
|---|---|---|
| ReentrantLock | 独占 | 锁重入次数 |
| ReentrantReadWriteLock | 独占 + 共享 | 高 16 位读锁，低 16 位写锁 |
| Semaphore | 共享 | 剩余许可证数量 |
| CountDownLatch | 共享 | 剩余计数 |
| FutureTask | 独占/状态机 | 任务执行状态 |

---

## 14. 总结

AQS 是 Java 并发包的核心同步器框架，很多并发工具底层都基于它实现。

AQS 的核心是一个 `volatile int state` 状态变量和一个 FIFO 双向等待队列。线程获取锁或资源时，先尝试通过 CAS 修改 state，成功则继续执行，失败则封装成 Node 节点加入等待队列，并通过 `LockSupport.park()` 阻塞。资源释放后，会通过 `LockSupport.unpark()` 唤醒后继节点重新竞争。

AQS 支持独占模式和共享模式。`ReentrantLock` 使用独占模式，`Semaphore` 和 `CountDownLatch` 使用共享模式。

AQS 本身只负责同步框架，包括排队、阻塞、唤醒和状态管理；具体 state 代表什么、如何获取资源、如何释放资源，由子类通过重写 `tryAcquire`、`tryRelease`、`tryAcquireShared`、`tryReleaseShared` 等方法实现。

一句话总结：

```text
AQS = volatile state + CAS + FIFO 等待队列 + LockSupport，是 Java 并发工具类的底层同步框架。
```
