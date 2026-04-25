# ConcurrentHashMap 总结

`ConcurrentHashMap` 是 Java 中线程安全的 Hash 表，适合高并发场景下替代 `HashMap`。

核心特点：

```text
线程安全
高并发性能好
不允许 null key / null value
适合多线程读写
```

---

## 1. 为什么需要 ConcurrentHashMap

`HashMap` 在多线程下不安全，可能出现：

```text
数据覆盖
数据丢失
size 不准确
扩容异常
JDK 1.7 并发扩容死循环
```

`Hashtable` 虽然线程安全，但它对整个方法加 `synchronized`，锁粒度太大，并发性能差。

所以需要：

```text
ConcurrentHashMap = 线程安全 + 更高并发性能
```

---

## 2. JDK 1.7 实现原理

JDK 1.7 中，`ConcurrentHashMap` 使用：

```text
Segment 分段锁
```

整体结构：

```text
ConcurrentHashMap
  ↓
Segment[]
  ↓
HashEntry[]
  ↓
链表
```

每个 `Segment` 类似一个小的 HashMap，并且继承了 `ReentrantLock`。

```text
不同 Segment 可以并发写
同一个 Segment 写入需要加锁
```

### 优势

- 锁粒度比 Hashtable 小。
- 多个 Segment 可以并发操作。
- 并发性能比 Hashtable 好。

### 劣势

- Segment 数量固定，扩展性有限。
- 结构复杂。
- 并发度受 Segment 数量影响。

---

## 3. JDK 1.8 实现原理

JDK 1.8 中取消了 Segment，改为：

```text
CAS + synchronized + Node 数组 + 链表 + 红黑树
```

整体结构类似 HashMap：

```text
ConcurrentHashMap
  ↓
Node[]
  ↓
链表 / 红黑树
```

核心思想：

```text
读操作无锁
写操作尽量 CAS
冲突时锁住桶头节点
```

---

## 4. JDK 1.8 put 流程

```text
put key-value
  ↓
判断 key/value 是否为 null
  ↓
计算 hash
  ↓
如果数组未初始化，先初始化
  ↓
根据 hash 定位桶下标
  ↓
如果桶为空，CAS 插入
  ↓
如果桶不为空，对桶头节点加 synchronized
  ↓
插入链表或红黑树
  ↓
更新元素数量
  ↓
判断是否需要扩容
```

简单理解：

```text
空桶用 CAS
非空桶锁桶头
```

---

## 5. get 为什么不加锁

`ConcurrentHashMap` 的 `get` 操作通常不加锁。

原因：

```text
Node 的 key 和 value 使用 volatile / final 保证可见性
数组引用也通过 volatile 保证可见性
```

读取流程：

```text
计算 hash
  ↓
定位桶
  ↓
遍历链表或红黑树
  ↓
找到对应 key 返回 value
```

所以：

```text
读操作几乎无锁，性能很高
```

---

## 6. put 如何保证线程安全

JDK 1.8 中主要靠：

```text
CAS + synchronized
```

### 桶为空

```text
使用 CAS 插入节点
```

CAS 成功：

```text
插入成功
```

CAS 失败：

```text
说明有其他线程插入，重新尝试
```

---

### 桶不为空

```text
对桶头节点加 synchronized
```

只锁当前桶，不锁整个 Map。

```text
不同桶之间可以并发写
```

---

## 7. 扩容机制

### 触发条件

当元素数量超过阈值时，会触发扩容。

```text
size > capacity * loadFactor
```

默认负载因子：

```text
0.75
```

---

### 扩容特点

JDK 1.8 中支持多个线程协助扩容。

流程：

```text
发现需要扩容
  ↓
创建新数组
  ↓
多个线程一起迁移不同桶的数据
  ↓
迁移完成后切换到新数组
```

扩容期间，桶中会放入特殊节点：

```text
ForwardingNode
```

表示：

```text
当前桶已经迁移到新数组
```

其他线程遇到 `ForwardingNode` 时，会帮助迁移或去新数组查找。

---

## 8. size 统计原理

`ConcurrentHashMap` 中 `size()` 不是简单维护一个全局变量。

因为高并发下一个全局计数器竞争会很激烈。

JDK 1.8 使用类似 LongAdder 的思想：

```text
baseCount + CounterCell[]
```

低并发时：

```text
CAS 更新 baseCount
```

高并发时：

```text
分散更新 CounterCell
```

最终统计：

```text
总数 = baseCount + 所有 CounterCell 之和
```

---

## 9. 为什么不允许 null

`ConcurrentHashMap` 不允许：

```text
null key
null value
```

原因：

```text
多线程环境下无法区分 key 不存在还是 value 为 null
```

例如：

```java
map.get(key) == null
```

在 `HashMap` 中可能表示：

```text
key 不存在
或者
key 存在但 value 是 null
```

但在并发场景下，这种语义会造成歧义。

---

## 10. 链表转红黑树

JDK 1.8 中，当某个桶中的链表过长时，会转成红黑树。

条件大致是：

```text
链表长度 >= 8
数组长度 >= 64
```

如果数组长度还小于 64，优先扩容，而不是树化。

原因：

```text
链表过长可能是数组太小导致冲突多
先扩容更合适
```

---

## 11. ConcurrentHashMap 常见方法

```java
put(key, value)
get(key)
remove(key)
putIfAbsent(key, value)
computeIfAbsent(key, mappingFunction)
replace(key, oldValue, newValue)
containsKey(key)
size()
```

常用原子方法：

```java
putIfAbsent()
computeIfAbsent()
replace()
remove(key, value)
```

---

## 12. 使用注意点

### 1. 复合操作仍需注意

虽然单个方法线程安全，但多个操作组合不一定线程安全。

不推荐：

```java
if (!map.containsKey(key)) {
    map.put(key, value);
}
```

推荐：

```java
map.putIfAbsent(key, value);
```

---

### 2. size 不是强一致

高并发修改时：

```text
size() 结果只是近似准确
```

不要在强一致并发逻辑中依赖 `size()`。

---

### 3. computeIfAbsent 里的逻辑不要太重

```java
map.computeIfAbsent(key, k -> loadData(k));
```

注意：

```text
mappingFunction 不要执行太慢
不要做复杂阻塞操作
不要递归修改当前 Map
```

否则可能影响当前桶上的并发性能。

---

## 13. HashMap、Hashtable、ConcurrentHashMap 对比

| 对比项 | HashMap | Hashtable | ConcurrentHashMap |
|---|---|---|---|
| 线程安全 | 否 | 是 | 是 |
| 锁机制 | 无锁 | 方法级 synchronized | CAS + synchronized / 分段锁 |
| 并发性能 | 不安全 | 差 | 好 |
| null key/value | 允许 | 不允许 | 不允许 |
| 适用场景 | 单线程 | 老代码兼容 | 高并发读写 |

---

## 14. JDK 1.7 和 JDK 1.8 对比

| 对比项 | JDK 1.7 | JDK 1.8 |
|---|---|---|
| 核心结构 | Segment + HashEntry | Node 数组 + 链表 + 红黑树 |
| 锁机制 | 分段锁 ReentrantLock | CAS + synchronized |
| 锁粒度 | Segment | 桶头节点 |
| 查询 | 大多无锁 | 无锁 |
| 扩容 | Segment 内扩容 | 多线程协助扩容 |
| 性能 | 较好 | 更好 |

---

## 15. 总结

`ConcurrentHashMap` 是线程安全的高性能 Map。

JDK 1.7 中，它通过 `Segment` 分段锁实现并发控制，每个 Segment 类似一个小 HashMap，不同 Segment 可以并发写。

JDK 1.8 中取消了 Segment，改为 `Node 数组 + 链表 + 红黑树` 的结构，线程安全主要通过 `CAS + synchronized` 实现。桶为空时用 CAS 插入，桶不为空时只锁桶头节点，不会锁整个 Map，因此并发性能较好。读操作通常不加锁，通过 `volatile` 保证可见性。

它不允许 `null key` 和 `null value`，原因是在并发环境下无法区分 key 不存在还是 value 为 null。

一句话总结：

```text
ConcurrentHashMap = 线程安全 HashMap；
JDK 1.7 用 Segment 分段锁；
JDK 1.8 用 CAS + synchronized + 链表/红黑树，读无锁，写锁桶。
```
