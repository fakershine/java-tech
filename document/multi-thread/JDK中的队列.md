# JDK 中的队列总结

JDK 中的队列主要在 `java.util` 和 `java.util.concurrent` 包下，常用于任务排队、线程通信、生产者消费者、线程池任务队列等场景。

常见队列可以分为：

- 普通队列
- 双端队列
- 优先级队列
- 阻塞队列
- 并发队列
- 延迟队列
- 传输队列

---

## 1. Queue 接口

### 实现原理

`Queue` 是 JDK 中队列的基础接口，遵循先进先出 FIFO 原则。

常用方法：

| 方法 | 失败时行为 | 说明 |
|---|---|---|
| `add(e)` | 抛异常 | 入队 |
| `offer(e)` | 返回 false | 入队 |
| `remove()` | 抛异常 | 出队 |
| `poll()` | 返回 null | 出队 |
| `element()` | 抛异常 | 查看队头 |
| `peek()` | 返回 null | 查看队头 |

### 推荐使用

实际开发中更推荐：

```java
offer()
poll()
peek()
```

因为这几个方法失败时不会直接抛异常，使用更安全。

---

## 2. Deque 双端队列

### 实现原理

`Deque` 是双端队列，支持从队头和队尾插入、删除元素。

常用方法：

```java
offerFirst(e);
offerLast(e);

pollFirst();
pollLast();

peekFirst();
peekLast();
```

### 适用场景

- 栈。
- 队列。
- 双端任务调度。
- 滑动窗口。
- 广度优先搜索 / 深度优先搜索。

---

## 3. ArrayDeque

### 实现原理

`ArrayDeque` 是基于数组实现的双端队列，底层使用循环数组。

### 优势

- 性能高。
- 无锁，单线程场景效率好。
- 可作为栈或队列使用。
- 通常比 `Stack` 和 `LinkedList` 更推荐。

### 劣势

- 线程不安全。
- 不允许存储 `null`。
- 扩容时需要数组拷贝。

### 适用场景

- 单线程队列。
- 单线程栈。
- BFS / DFS。
- 临时数据缓存。

---

## 4. LinkedList

### 实现原理

`LinkedList` 基于双向链表实现，同时实现了 `List` 和 `Deque` 接口。

### 优势

- 插入和删除方便。
- 支持双端操作。
- 可以作为队列或栈使用。

### 劣势

- 查询效率低。
- 内存占用较高。
- 线程不安全。
- 实际作为队列时通常不如 `ArrayDeque`。

### 适用场景

- 需要频繁双端插入删除的单线程场景。
- 不推荐作为高性能队列首选。

---

## 5. PriorityQueue

### 实现原理

`PriorityQueue` 是优先级队列，底层基于小顶堆实现。

默认情况下，元素按照自然顺序排序，也可以传入 `Comparator` 自定义优先级。

示例：

```java
PriorityQueue<Integer> queue = new PriorityQueue<>();

queue.offer(3);
queue.offer(1);
queue.offer(2);

System.out.println(queue.poll()); // 1
```

### 优势

- 可以按优先级出队。
- 插入和删除堆顶效率较高。
- 适合 Top K、任务优先级调度。

### 劣势

- 不是 FIFO 队列。
- 线程不安全。
- 不允许存储 `null`。
- 遍历结果不一定有序。

### 适用场景

- Top K 问题。
- 定时任务排序。
- 优先级任务调度。
- Dijkstra 等算法。

---

## 6. BlockingQueue 阻塞队列

### 实现原理

`BlockingQueue` 是阻塞队列接口，常用于生产者消费者模型。

当队列满时，生产者可以阻塞等待。  
当队列空时，消费者可以阻塞等待。

常用方法：

| 方法 | 队列满时 | 队列空时 |
|---|---|---|
| `add(e)` / `remove()` | 抛异常 | 抛异常 |
| `offer(e)` / `poll()` | 返回 false | 返回 null |
| `put(e)` / `take()` | 阻塞等待 | 阻塞等待 |
| `offer(e, timeout)` / `poll(timeout)` | 超时等待 | 超时等待 |

### 适用场景

- 生产者消费者。
- 线程池任务队列。
- 异步任务缓冲。
- 削峰填谷。

---

## 7. ArrayBlockingQueue

### 实现原理

`ArrayBlockingQueue` 是基于数组实现的有界阻塞队列。

内部使用一把 `ReentrantLock` 控制并发，并通过两个 `Condition` 分别控制队列非空和非满。

```text
notEmpty：队列非空条件
notFull：队列非满条件
```

### 优势

- 有界队列，容量固定。
- 内存可控。
- 适合线程池任务队列。
- 支持公平锁模式。

### 劣势

- 容量固定，不能动态扩容。
- 生产和消费共用一把锁，并发性能一般。
- 队列满时可能阻塞或触发拒绝策略。

### 适用场景

- 线程池任务队列。
- 生产者消费者。
- 限制内存使用的异步任务队列。

---

## 8. LinkedBlockingQueue

### 实现原理

`LinkedBlockingQueue` 是基于链表实现的阻塞队列。

默认容量是：

```java
Integer.MAX_VALUE
```

所以如果不指定容量，近似无界队列。

内部通常使用两把锁：

```text
putLock：控制生产者入队
takeLock：控制消费者出队
```

生产和消费可以并发执行。

### 优势

- 吞吐量较高。
- 生产和消费锁分离。
- 可指定容量。
- 适合任务缓冲。

### 劣势

- 不指定容量容易导致任务堆积。
- 可能导致 OOM。
- 链表节点有额外内存开销。

### 适用场景

- 生产者消费者。
- 任务缓冲队列。
- 线程池队列，但生产环境建议指定容量。

---

## 9. SynchronousQueue

### 实现原理

`SynchronousQueue` 是一个不存储元素的阻塞队列。

每个 `put` 操作必须等待一个 `take` 操作。  
每个 `take` 操作也必须等待一个 `put` 操作。

```text
生产者直接把任务交给消费者
队列本身不保存任务
```

### 优势

- 不占用队列存储空间。
- 任务直接交接。
- 适合快速创建线程处理任务。

### 劣势

- 没有缓冲能力。
- 消费能力不足时容易创建大量线程。
- 使用不当可能导致线程数过多。

### 适用场景

- `Executors.newCachedThreadPool()`。
- 任务不需要排队，希望直接交给线程处理。
- 高响应、短任务场景。

---

## 10. PriorityBlockingQueue

### 实现原理

`PriorityBlockingQueue` 是阻塞版优先级队列，底层基于堆实现。

元素会按照优先级出队。

### 优势

- 支持优先级排序。
- 出队会阻塞等待。
- 适合优先级任务调度。

### 劣势

- 默认无界，可能 OOM。
- 不保证同优先级元素的 FIFO 顺序。
- 元素需要可比较或指定比较器。

### 适用场景

- 优先级任务调度。
- 定时任务排序。
- 高优先级任务优先执行。

---

## 11. DelayQueue

### 实现原理

`DelayQueue` 是延迟队列，只有元素到期后才能被取出。

元素必须实现 `Delayed` 接口。

```java
class DelayTask implements Delayed {
    private long executeTime;

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(executeTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return Long.compare(this.getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
    }
}
```

### 优势

- 支持延迟任务。
- 到期后才能消费。
- 适合超时控制。

### 劣势

- 元素必须实现 `Delayed`。
- 默认无界。
- 不适合高精度定时任务。

### 适用场景

- 订单超时取消。
- 缓存过期清理。
- 延迟任务执行。
- 连接超时检测。

---

## 12. LinkedTransferQueue

### 实现原理

`LinkedTransferQueue` 是基于链表实现的无界传输队列，实现了 `TransferQueue` 接口。

它既可以像普通队列一样存储元素，也可以直接把元素传递给消费者。

核心方法：

```java
transfer(e);
tryTransfer(e);
```

### transfer

```java
queue.transfer(e);
```

如果当前有消费者等待，直接交给消费者。  
如果没有消费者，生产者会阻塞，直到有消费者接收。

### tryTransfer

```java
queue.tryTransfer(e);
```

如果有消费者等待，立即传递成功。  
如果没有消费者，立即返回失败。

### 优势

- 吞吐量高。
- 支持直接传递。
- 适合高并发生产消费。
- 比普通阻塞队列更灵活。

### 劣势

- 无界队列，可能堆积。
- 使用复杂度较高。
- 不适合需要严格容量控制的场景。

### 适用场景

- 高性能生产者消费者。
- 直接任务交接。
- 高并发异步处理。

---

## 13. ConcurrentLinkedQueue

### 实现原理

`ConcurrentLinkedQueue` 是基于链表实现的无界非阻塞并发队列。

底层使用 CAS 保证线程安全。

### 优势

- 线程安全。
- 非阻塞。
- 并发性能较好。
- 适合高并发入队出队。

### 劣势

- 无界队列，可能导致内存增长。
- 不支持阻塞等待。
- size() 计算成本较高，不适合频繁调用。

### 适用场景

- 高并发任务队列。
- 非阻塞消息队列。
- 多线程日志缓冲。
- 不需要阻塞等待的生产消费场景。

---

## 14. ConcurrentLinkedDeque

### 实现原理

`ConcurrentLinkedDeque` 是线程安全的无界双端队列。

底层基于链表和 CAS 实现。

### 优势

- 支持双端操作。
- 线程安全。
- 非阻塞。
- 适合并发环境。

### 劣势

- 无界队列，可能内存膨胀。
- size() 成本较高。
- 不支持阻塞操作。

### 适用场景

- 并发双端队列。
- 工作窃取类场景。
- 多线程任务缓存。

---

## 15. LinkedBlockingDeque

### 实现原理

`LinkedBlockingDeque` 是基于链表实现的阻塞双端队列。

支持从队头和队尾进行阻塞式插入和删除。

### 优势

- 支持双端阻塞操作。
- 可以指定容量。
- 适合复杂生产消费模型。

### 劣势

- 链表节点额外占用内存。
- 并发性能不如部分非阻塞队列。
- 使用场景相对较少。

### 适用场景

- 双端任务队列。
- 工作窃取。
- 复杂生产者消费者模型。

---

## 16. 常见队列对比

| 队列 | 是否线程安全 | 是否阻塞 | 是否有界 | 底层结构 | 适用场景 |
|---|---|---|---|---|---|
| ArrayDeque | 否 | 否 | 否 | 循环数组 | 单线程队列/栈 |
| LinkedList | 否 | 否 | 否 | 双向链表 | 普通双端队列 |
| PriorityQueue | 否 | 否 | 否 | 堆 | 优先级排序 |
| ArrayBlockingQueue | 是 | 是 | 是 | 数组 | 线程池、生产消费 |
| LinkedBlockingQueue | 是 | 是 | 可有界 | 链表 | 任务缓冲 |
| SynchronousQueue | 是 | 是 | 无容量 | 直接交接 | CachedThreadPool |
| PriorityBlockingQueue | 是 | 是 | 无界 | 堆 | 优先级任务 |
| DelayQueue | 是 | 是 | 无界 | 堆 | 延迟任务 |
| LinkedTransferQueue | 是 | 是/非阻塞 | 无界 | 链表 | 高并发传输 |
| ConcurrentLinkedQueue | 是 | 否 | 无界 | 链表 + CAS | 非阻塞并发队列 |
| ConcurrentLinkedDeque | 是 | 否 | 无界 | 链表 + CAS | 并发双端队列 |
| LinkedBlockingDeque | 是 | 是 | 可有界 | 链表 | 阻塞双端队列 |

---

## 17. 线程池中常用队列

`ThreadPoolExecutor` 常用队列：

| 队列 | 特点 | 影响 |
|---|---|---|
| ArrayBlockingQueue | 有界队列 | 容量可控，推荐 |
| LinkedBlockingQueue | 默认无界 | 可能任务堆积导致 OOM |
| SynchronousQueue | 不存任务 | 容易创建更多线程 |
| PriorityBlockingQueue | 优先级队列 | 可按优先级执行任务 |

生产环境建议：

```text
优先使用有界队列
避免无界队列
配合拒绝策略和监控告警
```

---

## 18. 使用建议

- 单线程普通队列优先使用 `ArrayDeque`。
- 需要优先级排序使用 `PriorityQueue`。
- 生产者消费者模型使用 `BlockingQueue`。
- 线程池任务队列推荐使用有界 `ArrayBlockingQueue`。
- 高并发非阻塞场景使用 `ConcurrentLinkedQueue`。
- 延迟任务可以使用 `DelayQueue`。
- 需要直接传递任务可以使用 `SynchronousQueue` 或 `LinkedTransferQueue`。
- 尽量避免无界队列导致内存无限增长。

---

## 19. 总结

JDK 中常见队列主要包括普通队列、双端队列、优先级队列、阻塞队列和并发队列。

`ArrayDeque` 是基于循环数组实现的双端队列，适合单线程下替代 `Stack` 和 `LinkedList`。`PriorityQueue` 基于堆实现，用于优先级排序。

并发场景下常用 `BlockingQueue`，例如 `ArrayBlockingQueue`、`LinkedBlockingQueue`、`SynchronousQueue`、`PriorityBlockingQueue`、`DelayQueue` 等。`ArrayBlockingQueue` 是有界数组队列，内存可控；`LinkedBlockingQueue` 基于链表，默认近似无界，使用时要指定容量；`SynchronousQueue` 不存储元素，生产者和消费者直接交接，常用于 `newCachedThreadPool`；`DelayQueue` 用于延迟任务。

如果需要非阻塞线程安全队列，可以使用 `ConcurrentLinkedQueue`，它基于 CAS 实现，适合高并发入队出队，但不支持阻塞等待。

一句话总结：

```text
单线程用 ArrayDeque，优先级用 PriorityQueue，生产消费用 BlockingQueue，高并发非阻塞用 ConcurrentLinkedQueue，延迟任务用 DelayQueue，线程池中尽量使用有界队列。
```
