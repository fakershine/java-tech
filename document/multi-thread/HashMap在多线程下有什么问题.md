# HashMap 在多线程下的问题

`HashMap` 是 **非线程安全** 的集合。  
在多线程并发读写场景下，可能出现数据丢失、数据覆盖、读取异常、结构异常等问题。

---

## 1. 常见问题

| 问题 | 说明 |
|---|---|
| 数据覆盖 | 多个线程同时 put，相同位置数据互相覆盖 |
| 数据丢失 | 扩容或链表操作时，部分节点丢失 |
| 读到脏数据 | 一个线程正在修改，另一个线程读取到不完整数据 |
| size 不准确 | 多线程同时修改导致 size 统计错误 |
| 扩容异常 | 并发 resize 可能导致结构异常 |
| 死循环问题 | JDK 1.7 并发扩容时可能形成环形链表 |
| 并发修改异常 | 遍历时修改可能抛 `ConcurrentModificationException` |

---

## 2. put 数据覆盖问题

`HashMap` put 操作不是原子操作。

大致流程：

```text
计算 hash
  ↓
定位数组下标
  ↓
判断当前位置是否为空
  ↓
插入节点或追加链表 / 红黑树
  ↓
size++
```

如果两个线程同时 put：

```text
线程 A 判断 bucket 为空
线程 B 判断 bucket 为空
线程 A 插入数据
线程 B 插入数据
```

结果：

```text
线程 B 可能覆盖线程 A 的数据
```

---

## 3. size 不准确

`HashMap` 的 `size++` 不是原子操作。

它实际包含：

```text
读取 size
  ↓
size + 1
  ↓
写回 size
```

如果多个线程同时执行：

```text
线程 A 读取 size = 10
线程 B 读取 size = 10
线程 A 写回 11
线程 B 写回 11
```

实际插入了两个元素，但 `size` 只增加了 1。

---

## 4. 扩容数据丢失

`HashMap` 在元素数量超过阈值时会扩容。

```text
新建更大的数组
  ↓
重新计算元素位置
  ↓
迁移旧数组节点
```

如果多个线程同时扩容，可能出现：

```text
节点迁移混乱
部分节点丢失
链表结构异常
```

导致后续查询不到已经 put 的数据。

---

## 5. JDK 1.7 死循环问题

JDK 1.7 中，`HashMap` 扩容迁移链表时使用 **头插法**。

并发扩容时，多个线程同时迁移链表节点，可能导致链表形成环。

```text
A -> B -> C
```

异常情况下可能变成：

```text
A -> B -> A
```

后续执行 `get()` 时，会在链表中一直循环，导致 CPU 飙高。

---

## 6. JDK 1.8 是否还有死循环问题

JDK 1.8 对扩容迁移做了优化，链表迁移不再使用 JDK 1.7 的头插法，而是使用尾插法，并且引入红黑树优化。

所以：

```text
JDK 1.8 大幅降低了扩容死循环问题
```

但注意：

```text
JDK 1.8 的 HashMap 仍然不是线程安全的
```

仍可能出现：

- 数据覆盖。
- 数据丢失。
- size 不准确。
- 读写结果不一致。
- 并发修改异常。

---

## 7. 遍历时修改问题

如果一个线程正在遍历 `HashMap`，另一个线程修改了 `HashMap`，可能抛出：

```java
ConcurrentModificationException
```

示例：

```java
for (Map.Entry<String, String> entry : map.entrySet()) {
    map.put("newKey", "newValue");
}
```

原因：

```text
HashMap 迭代器是 fail-fast 机制
遍历时检测到结构被修改，就可能抛异常
```

---

## 8. 如何解决

### 1. 使用 ConcurrentHashMap

并发场景推荐：

```java
Map<String, String> map = new ConcurrentHashMap<>();
```

特点：

```text
线程安全
并发性能好
适合高并发读写
```

---

### 2. 使用 Collections.synchronizedMap

```java
Map<String, String> map =
        Collections.synchronizedMap(new HashMap<>());
```

特点：

```text
通过 synchronized 保证线程安全
实现简单
性能一般
```

---

### 3. 使用 Hashtable

```java
Map<String, String> map = new Hashtable<>();
```

特点：

```text
线程安全
方法级 synchronized
性能较差
老旧集合，不推荐新项目优先使用
```

---

### 4. 外部加锁

```java
synchronized (map) {
    map.put("key", "value");
}
```

适合：

```text
临时简单场景
```

不适合：

```text
高并发复杂场景
```

---

## 9. HashMap、Hashtable、ConcurrentHashMap 对比

| 对比项 | HashMap | Hashtable | ConcurrentHashMap |
|---|---|---|---|
| 线程安全 | 否 | 是 | 是 |
| 锁粒度 | 无锁 | 整个方法加锁 | 分段 / CAS + synchronized |
| 性能 | 单线程高 | 并发性能差 | 并发性能好 |
| 是否允许 null key/value | 允许一个 null key，多个 null value | 不允许 | 不允许 |
| 推荐场景 | 单线程 | 老代码兼容 | 高并发读写 |

---

## 10. 总结

`HashMap` 在多线程下是不安全的，因为它的 `put`、`resize`、`size++` 等操作都不是原子操作。

多线程并发写入时，可能出现数据覆盖、数据丢失、size 不准确等问题。JDK 1.7 中，`HashMap` 并发扩容时由于链表迁移使用头插法，可能形成环形链表，导致 `get()` 死循环和 CPU 飙高。JDK 1.8 虽然优化了扩容逻辑，降低了死循环风险，但 `HashMap` 仍然不是线程安全的。

并发场景应该使用 `ConcurrentHashMap`，或者使用 `Collections.synchronizedMap`、`Hashtable`、外部加锁等方式保证线程安全。

一句话总结：

```text
HashMap 多线程问题 = put 非原子 + resize 不安全 + size 不准确 + JDK 1.7 扩容可能死循环；
并发场景推荐使用 ConcurrentHashMap。
```
