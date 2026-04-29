# Java 集合常见面试题总结

## 目录

- [一、ArrayList 和 LinkedList 区别](#一arraylist-和-linkedlist-区别)
- [二、HashMap 底层原理](#二hashmap-底层原理)
- [三、HashMap 为什么线程不安全](#三hashmap-为什么线程不安全)
- [四、HashMap 扩容机制](#四hashmap-扩容机制)
- [五、HashMap put 流程](#五hashmap-put-流程)
- [六、HashMap 为什么容量是 2 的幂](#六hashmap-为什么容量是-2-的幂)
- [七、HashMap 在 JDK 1.7 和 JDK 1.8 的区别](#七hashmap-在-jdk-17-和-jdk-18-的区别)
- [八、ConcurrentHashMap 原理](#八concurrenthashmap-原理)
- [九、HashSet 如何保证元素不重复](#九hashset-如何保证元素不重复)
- [十、TreeMap 和 HashMap 区别](#十treemap-和-hashmap-区别)
- [十一、面试速记版](#十一面试速记版)

---

# 一、ArrayList 和 LinkedList 区别

## 1. 底层数据结构不同

### ArrayList

`ArrayList` 底层基于 **动态数组** 实现。

```java
transient Object[] elementData;
```

它的元素在内存中是连续存储的。

---

### LinkedList

`LinkedList` 底层基于 **双向链表** 实现。

每个节点中保存：

1. 当前元素；
2. 上一个节点引用；
3. 下一个节点引用。

结构类似：

```java
private static class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;
}
```

---

## 2. 查询性能不同

### ArrayList 查询快

`ArrayList` 支持通过下标直接访问元素。

```java
list.get(index);
```

时间复杂度：

```text
O(1)
```

原因是数组可以通过下标直接定位元素。

---

### LinkedList 查询慢

`LinkedList` 查询元素时，需要从头节点或尾节点开始遍历。

时间复杂度：

```text
O(n)
```

---

## 3. 插入和删除性能不同

### ArrayList

如果在末尾添加元素，效率较高：

```text
O(1)
```

但如果在中间或开头插入、删除元素，需要移动大量元素：

```text
O(n)
```

---

### LinkedList

如果已经定位到目标节点，插入和删除只需要修改前后节点指针：

```text
O(1)
```

但如果需要先查找目标位置，查找过程仍然是：

```text
O(n)
```

所以实际开发中，`LinkedList` 并不一定比 `ArrayList` 插入删除快。

---

## 4. 内存占用不同

### ArrayList

`ArrayList` 只存储元素本身，额外开销较小。

---

### LinkedList

`LinkedList` 每个节点除了存储元素，还需要存储 `prev` 和 `next` 两个引用，因此内存占用更大。

---

## 5. 是否支持随机访问

| 集合 | 是否支持随机访问 |
|---|---|
| ArrayList | 支持 |
| LinkedList | 不支持 |

`ArrayList` 实现了 `RandomAccess` 标记接口，表示支持快速随机访问。

---

## 6. ArrayList 和 LinkedList 对比表

| 对比项 | ArrayList | LinkedList |
|---|---|---|
| 底层结构 | 动态数组 | 双向链表 |
| 查询效率 | 高，`O(1)` | 低，`O(n)` |
| 末尾添加 | 快，通常 `O(1)` | 快，`O(1)` |
| 中间插入删除 | 需要移动元素，`O(n)` | 定位后为 `O(1)`，但查找位置是 `O(n)` |
| 内存占用 | 较小 | 较大 |
| 随机访问 | 支持 | 不支持 |
| 适用场景 | 查询多、遍历多 | 头尾操作较多 |

---

## 7. 面试回答

`ArrayList` 底层是动态数组，查询快，支持随机访问，但中间插入和删除需要移动元素。

`LinkedList` 底层是双向链表，插入和删除节点本身较快，但定位节点需要遍历，查询效率较低，并且每个节点都要维护前后指针，内存占用更大。

实际开发中，如果没有特殊需求，通常优先使用 `ArrayList`。

---

# 二、HashMap 底层原理

## 1. HashMap 是什么？

`HashMap` 是 Java 中非常常用的键值对集合。

它用于存储：

```text
key-value
```

形式的数据。

示例：

```java
Map<String, Integer> map = new HashMap<>();

map.put("Tom", 18);
map.put("Jerry", 20);
```

---

## 2. HashMap 底层数据结构

在 JDK 1.8 中，`HashMap` 底层结构是：

```text
数组 + 链表 + 红黑树
```

结构示意：

```text
HashMap
 └── table 数组
      ├── Node
      ├── Node -> Node -> Node
      ├── TreeNode 红黑树
      └── null
```

核心数组：

```java
transient Node<K,V>[] table;
```

数组中的每个位置称为一个 **桶** 或 **bucket**。

---

## 3. Node 节点结构

`HashMap` 中每个键值对会封装成一个 `Node` 节点。

```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    V value;
    Node<K,V> next;
}
```

每个节点包含：

| 字段 | 含义 |
|---|---|
| hash | key 的 hash 值 |
| key | 键 |
| value | 值 |
| next | 下一个节点引用 |

---

## 4. HashMap 如何存储数据？

当执行：

```java
map.put("name", "Tom");
```

大致流程如下：

1. 根据 key 计算 hash 值；
2. 根据 hash 值计算数组下标；
3. 如果该位置为空，直接插入；
4. 如果该位置不为空，说明发生哈希冲突；
5. 如果 key 已存在，覆盖旧 value；
6. 如果 key 不存在，插入到链表或红黑树中；
7. 如果链表长度过长，可能转为红黑树；
8. 如果元素数量超过阈值，触发扩容。

---

## 5. HashMap 如何解决哈希冲突？

HashMap 使用 **链地址法** 解决哈希冲突。

也就是说：

> 如果多个 key 计算出来的数组下标相同，就把它们放到同一个桶中，通过链表或红黑树连接起来。

JDK 1.8 中：

```text
桶中元素较少：链表
桶中元素较多：红黑树
```

---

## 6. 链表什么时候转红黑树？

JDK 1.8 中，链表转红黑树需要满足两个条件：

```text
链表长度 >= 8
并且
数组容量 >= 64
```

相关常量：

```java
static final int TREEIFY_THRESHOLD = 8;
static final int UNTREEIFY_THRESHOLD = 6;
static final int MIN_TREEIFY_CAPACITY = 64;
```

如果链表长度达到 8，但数组容量小于 64，HashMap 会优先扩容，而不是树化。

---

## 7. 为什么要引入红黑树？

链表查询的时间复杂度是：

```text
O(n)
```

红黑树查询的时间复杂度是：

```text
O(log n)
```

当哈希冲突严重时，链表会变长，查询效率会下降。

JDK 1.8 引入红黑树，是为了提高极端情况下的查询性能。

---

## 8. HashMap 的重要参数

| 参数 | 默认值 | 说明 |
|---|---|---|
| 默认容量 | 16 | 默认数组长度 |
| 最大容量 | 2^30 | 最大容量 |
| 加载因子 | 0.75 | 控制扩容时机 |
| 扩容阈值 | 容量 * 加载因子 | 超过阈值触发扩容 |
| 树化阈值 | 8 | 链表长度达到 8 可能转红黑树 |
| 退化阈值 | 6 | 红黑树节点较少时退化为链表 |
| 最小树化容量 | 64 | 数组容量至少为 64 才允许树化 |

---

## 9. 面试回答

JDK 1.8 中，`HashMap` 底层是数组、链表和红黑树。

插入元素时，先根据 key 计算 hash 值，再通过 `(n - 1) & hash` 计算数组下标。如果该位置为空，直接插入；如果不为空，则说明发生哈希冲突，会通过链表或红黑树存储。

当链表长度大于等于 8，并且数组容量大于等于 64 时，链表会转换成红黑树，从而提高查询效率。

---

# 三、HashMap 为什么线程不安全

## 1. HashMap 不是线程安全集合

`HashMap` 本身没有使用锁机制，也没有使用 CAS 等并发控制手段。

所以多个线程同时操作 `HashMap` 时，可能会出现数据不一致问题。

---

## 2. 线程不安全的主要原因

### 2.1 多线程 put 可能导致数据覆盖

假设两个线程同时执行：

```java
map.put("a", 1);
map.put("b", 2);
```

如果两个 key 计算出的桶位置相同，两个线程都发现该位置为空，那么它们可能都会向这个位置插入数据。

后插入的节点可能覆盖前一个线程插入的节点，导致数据丢失。

---

### 2.2 扩容时可能出现数据丢失

HashMap 在扩容时，需要将旧数组中的元素迁移到新数组中。

如果多个线程同时触发扩容，可能会导致：

1. 数据丢失；
2. 链表断裂；
3. 元素覆盖；
4. 结构异常。

---

### 2.3 JDK 1.7 扩容时可能形成环形链表

JDK 1.7 中，HashMap 扩容迁移链表时使用的是 **头插法**。

在多线程并发扩容时，可能导致链表形成环。

一旦链表成环，调用 `get()` 时可能出现死循环，CPU 占用飙升。

---

### 2.4 JDK 1.8 仍然线程不安全

JDK 1.8 改用了 **尾插法**，解决了 JDK 1.7 中扩容时容易形成环的问题。

但是 JDK 1.8 的 `HashMap` 仍然不是线程安全的。

仍然可能出现：

1. 数据覆盖；
2. 数据丢失；
3. size 统计不准确；
4. 扩容期间读到不一致数据；
5. 并发修改导致结构异常。

---

## 3. 示例问题

```java
Map<Integer, Integer> map = new HashMap<>();

Thread t1 = new Thread(() -> {
    for (int i = 0; i < 10000; i++) {
        map.put(i, i);
    }
});

Thread t2 = new Thread(() -> {
    for (int i = 10000; i < 20000; i++) {
        map.put(i, i);
    }
});

t1.start();
t2.start();
```

上面代码在多线程环境下操作 `HashMap`，可能会出现最终元素数量不正确的问题。

---

## 4. 如何解决 HashMap 线程不安全问题？

可以使用以下方式：

### 4.1 使用 ConcurrentHashMap

```java
Map<String, String> map = new ConcurrentHashMap<>();
```

这是并发场景下最常用的选择。

---

### 4.2 使用 Collections.synchronizedMap

```java
Map<String, String> map = Collections.synchronizedMap(new HashMap<>());
```

这种方式通过同步包装保证线程安全，但性能通常不如 `ConcurrentHashMap`。

---

### 4.3 使用 Hashtable

```java
Map<String, String> map = new Hashtable<>();
```

`Hashtable` 是线程安全的，但它对方法整体加锁，并发性能较差，现在不推荐优先使用。

---

## 5. 面试回答

`HashMap` 线程不安全的原因是它没有加锁，也没有并发控制。

多个线程同时 `put` 时可能导致数据覆盖、数据丢失、size 不准确等问题。JDK 1.7 中并发扩容还可能因为头插法导致链表成环，从而出现死循环。

JDK 1.8 虽然使用尾插法改善了扩容问题，但 `HashMap` 仍然不是线程安全的。并发场景下应该使用 `ConcurrentHashMap`。

---

# 四、HashMap 扩容机制

## 1. 为什么需要扩容？

HashMap 底层数组容量有限。

当元素越来越多时，哈希冲突会增多，链表或红黑树会变长，查询效率下降。

因此，当元素数量超过一定阈值时，HashMap 会进行扩容。

---

## 2. 扩容触发条件

HashMap 是否扩容由以下两个参数决定：

```text
threshold = capacity * loadFactor
```

默认情况下：

```text
capacity = 16
loadFactor = 0.75
threshold = 16 * 0.75 = 12
```

也就是说：

> 当 HashMap 中的元素数量超过 12 时，会触发扩容。

---

## 3. 扩容后容量变化

HashMap 每次扩容，容量变为原来的 2 倍。

例如：

```text
16 -> 32 -> 64 -> 128 -> 256
```

---

## 4. 扩容时做了什么？

扩容时主要做两件事：

1. 创建一个更大的新数组；
2. 将旧数组中的元素迁移到新数组中。

---

## 5. JDK 1.8 的扩容优化

JDK 1.8 中，扩容后元素的位置判断更加高效。

因为容量是 2 的幂，所以扩容后，元素的位置只有两种可能：

```text
1. 仍然在原来的位置
2. 移动到 原位置 + oldCap
```

判断依据是：

```java
(hash & oldCap) == 0
```

如果结果为 0：

```text
元素位置不变
```

否则：

```text
元素位置 = 原位置 + oldCap
```

---

## 6. 示例

假设旧容量是：

```text
16
```

扩容后容量是：

```text
32
```

某个元素在旧数组中的位置是：

```text
5
```

扩容后它的位置只有两种可能：

```text
5
或者
5 + 16 = 21
```

---

## 7. 为什么加载因子默认是 0.75？

加载因子是空间和时间之间的折中。

### 加载因子太小

例如：

```text
0.5
```

优点：

```text
哈希冲突少，查询效率高
```

缺点：

```text
扩容频繁，空间浪费
```

---

### 加载因子太大

例如：

```text
1.0
```

优点：

```text
空间利用率高
```

缺点：

```text
哈希冲突增多，查询效率下降
```

---

### 默认 0.75

`0.75` 是一个比较均衡的选择：

```text
兼顾查询效率和空间利用率
```

---

## 8. 面试回答

HashMap 默认容量是 16，默认加载因子是 0.75，因此默认扩容阈值是 12。

当元素数量超过阈值时，HashMap 会扩容为原来的 2 倍，并将旧数组中的元素迁移到新数组中。

JDK 1.8 中扩容时不需要重新计算完整下标，只需要判断 `(hash & oldCap)`，元素要么留在原位置，要么移动到 `原位置 + oldCap`。

---

# 五、HashMap put 流程

## 1. put 方法作用

`put()` 用于向 HashMap 中添加或更新键值对。

```java
map.put(key, value);
```

---

## 2. put 的整体流程

JDK 1.8 中 `HashMap` 的 `put` 流程大致如下：

1. 计算 key 的 hash 值；
2. 判断数组是否为空，如果为空则初始化；
3. 根据 hash 值计算数组下标；
4. 如果对应桶为空，直接插入新节点；
5. 如果对应桶不为空，判断是否 key 相同；
6. 如果 key 相同，覆盖旧 value；
7. 如果 key 不同，判断当前节点是链表还是红黑树；
8. 如果是红黑树，按照红黑树方式插入；
9. 如果是链表，遍历链表后插入；
10. 插入后判断是否需要树化；
11. 插入成功后 size 加 1；
12. 判断是否超过扩容阈值；
13. 如果超过阈值，进行扩容。

---

## 3. 第一步：计算 hash 值

HashMap 会对 key 的 `hashCode()` 做扰动处理。

```java
static final int hash(Object key) {
    int h;
    return key == null ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

这样做是为了让 hash 的高 16 位也参与下标计算，减少哈希冲突。

---

## 4. 第二步：初始化数组

HashMap 是懒加载的。

刚创建 HashMap 时，底层数组还没有真正初始化。

```java
Map<String, String> map = new HashMap<>();
```

只有第一次调用 `put()` 时，才会初始化数组。

---

## 5. 第三步：计算数组下标

数组下标计算方式：

```java
index = (n - 1) & hash;
```

其中：

```text
n 表示数组长度
```

例如默认数组长度为 16，则：

```java
index = (16 - 1) & hash;
```

也就是：

```java
index = 15 & hash;
```

---

## 6. 第四步：桶为空，直接插入

如果计算出的数组位置为空：

```java
table[index] == null
```

则直接创建新节点放入该位置。

---

## 7. 第五步：桶不为空，处理哈希冲突

如果该位置已经有元素，说明发生了哈希冲突。

此时会判断：

1. 当前桶中第一个节点的 key 是否和新 key 相同；
2. 如果相同，直接覆盖 value；
3. 如果不同，继续向后查找。

判断 key 是否相同的逻辑：

```java
if (p.hash == hash &&
    ((k = p.key) == key || (key != null && key.equals(k)))) {
    // key 相同
}
```

---

## 8. 第六步：判断是链表还是红黑树

如果桶中第一个节点是红黑树节点：

```java
p instanceof TreeNode
```

则按照红黑树方式插入。

否则按照链表方式插入。

---

## 9. 第七步：链表插入

如果是链表，会遍历链表：

1. 如果找到相同 key，覆盖 value；
2. 如果没有找到相同 key，将新节点插入链表尾部。

JDK 1.8 中链表插入采用 **尾插法**。

---

## 10. 第八步：判断是否树化

如果插入后链表长度达到树化阈值：

```text
链表长度 >= 8
```

会尝试树化。

但是树化还要求数组容量：

```text
数组容量 >= 64
```

如果数组容量小于 64，则优先扩容。

---

## 11. 第九步：size 加 1 并判断扩容

插入新节点后：

```java
size++;
```

如果：

```java
size > threshold
```

则触发扩容。

---

## 12. put 流程图

```text
put(key, value)
     |
     v
计算 hash
     |
     v
table 是否为空？
     |
     ├── 是：初始化 table
     |
     v
根据 (n - 1) & hash 计算下标
     |
     v
桶位置是否为空？
     |
     ├── 是：直接插入新节点
     |
     └── 否：发生哈希冲突
              |
              v
        第一个节点 key 是否相同？
              |
              ├── 是：覆盖 value
              |
              └── 否：
                    |
                    v
              是红黑树吗？
                    |
                    ├── 是：按红黑树插入
                    |
                    └── 否：遍历链表
                              |
                              v
                        找到相同 key？
                              |
                              ├── 是：覆盖 value
                              |
                              └── 否：尾插新节点
                                        |
                                        v
                                  判断是否树化
                                        |
                                        v
                                  size + 1
                                        |
                                        v
                                  判断是否扩容
```

---

## 13. 面试回答

HashMap 的 `put` 流程是：先计算 key 的 hash 值，然后根据 `(n - 1) & hash` 计算数组下标。

如果桶为空，直接插入；如果桶不为空，先判断 key 是否相同，相同则覆盖旧值。否则判断当前桶是链表还是红黑树，分别按照链表或红黑树方式插入。

插入后如果链表长度达到 8，并且数组容量达到 64，会转成红黑树。最后 size 加 1，如果超过扩容阈值，则触发扩容。

---

# 六、HashMap 为什么容量是 2 的幂

## 1. 为了高效计算数组下标

HashMap 计算数组下标的方式是：

```java
index = (n - 1) & hash;
```

其中：

```text
n 是数组长度
```

当 `n` 是 2 的幂时：

```text
n - 1 的二进制低位全是 1
```

例如：

```text
n = 16
n - 1 = 15

16 的二进制：10000
15 的二进制：01111
```

这样可以通过位运算快速计算下标。

---

## 2. 位运算比取模效率更高

如果不用位运算，也可以使用取模：

```java
index = hash % n;
```

但取模运算效率不如位运算。

当容量是 2 的幂时：

```java
hash % n
```

等价于：

```java
hash & (n - 1)
```

所以 HashMap 使用位运算提升性能。

---

## 3. 为了让元素分布更均匀

当容量是 2 的幂时，`(n - 1)` 的二进制低位都是 1。

这样可以让 hash 的低位充分参与运算，减少哈希冲突。

如果容量不是 2 的幂，某些下标可能更容易被命中，导致分布不均匀。

---

## 4. 为了扩容迁移更高效

JDK 1.8 中，HashMap 扩容后容量翻倍。

由于容量是 2 的幂，扩容后元素的位置只有两种可能：

```text
原位置
原位置 + oldCap
```

只需要判断：

```java
(hash & oldCap) == 0
```

即可决定元素新位置。

---

## 5. 面试回答

HashMap 的容量设计成 2 的幂，主要是为了让 `(n - 1) & hash` 等价于 `hash % n`，从而用位运算代替取模运算，提高计算效率。

同时，2 的幂可以让元素分布更加均匀，减少哈希冲突。扩容时也可以通过 `(hash & oldCap)` 快速判断元素新位置，要么留在原位置，要么移动到 `原位置 + oldCap`。

---

# 七、HashMap 在 JDK 1.7 和 JDK 1.8 的区别

## 1. 底层数据结构不同

### JDK 1.7

JDK 1.7 中，HashMap 底层结构是：

```text
数组 + 链表
```

---

### JDK 1.8

JDK 1.8 中，HashMap 底层结构是：

```text
数组 + 链表 + 红黑树
```

当链表过长时，会转成红黑树，提高查询性能。

---

## 2. 插入方式不同

### JDK 1.7

JDK 1.7 使用 **头插法**。

新元素会插入到链表头部。

```text
新节点 -> 原链表
```

---

### JDK 1.8

JDK 1.8 使用 **尾插法**。

新元素会插入到链表尾部。

```text
原链表 -> 新节点
```

---

## 3. 扩容迁移方式不同

### JDK 1.7

JDK 1.7 扩容时，需要重新计算每个元素在新数组中的下标。

并且由于使用头插法，在多线程环境下可能导致链表成环。

---

### JDK 1.8

JDK 1.8 扩容时不需要重新完整计算下标。

元素位置只有两种情况：

```text
原位置
原位置 + oldCap
```

判断依据：

```java
(hash & oldCap) == 0
```

---

## 4. 是否支持红黑树

| JDK 版本 | 是否支持红黑树 |
|---|---|
| JDK 1.7 | 不支持 |
| JDK 1.8 | 支持 |

JDK 1.8 中，当链表长度达到 8，并且数组容量达到 64 时，链表会转成红黑树。

---

## 5. hash 计算方式不同

### JDK 1.7

JDK 1.7 的 hash 扰动函数较复杂，做了多次位运算。

---

### JDK 1.8

JDK 1.8 的 hash 扰动函数更简洁：

```java
static final int hash(Object key) {
    int h;
    return key == null ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

---

## 6. 创建数组时机不同

JDK 1.7 和 JDK 1.8 都有懒加载思想，但实现细节不同。

JDK 1.8 中，`HashMap` 创建对象时不会立即初始化数组，第一次 `put` 时才初始化 `table`。

---

## 7. JDK 1.7 和 JDK 1.8 对比表

| 对比项 | JDK 1.7 | JDK 1.8 |
|---|---|---|
| 数据结构 | 数组 + 链表 | 数组 + 链表 + 红黑树 |
| 插入方式 | 头插法 | 尾插法 |
| 扩容迁移 | 重新计算下标 | 原位置或原位置 + oldCap |
| 红黑树 | 不支持 | 支持 |
| 查询性能 | 冲突严重时退化为 O(n) | 冲突严重时可优化为 O(log n) |
| 并发扩容问题 | 可能形成环形链表 | 不容易形成环，但仍线程不安全 |
| hash 扰动 | 较复杂 | 更简洁 |

---

## 8. 面试回答

JDK 1.7 中，HashMap 底层是数组加链表，链表插入使用头插法，并发扩容时可能导致链表成环。

JDK 1.8 中，HashMap 底层变为数组、链表和红黑树。当链表长度达到 8 且数组容量达到 64 时，链表会转为红黑树。JDK 1.8 使用尾插法，扩容时元素要么留在原位置，要么移动到原位置加旧容量的位置，效率更高。

---

# 八、ConcurrentHashMap 原理

## 1. ConcurrentHashMap 是什么？

`ConcurrentHashMap` 是线程安全的哈希表，常用于高并发场景。

```java
Map<String, String> map = new ConcurrentHashMap<>();
```

它相比 `Hashtable` 或 `Collections.synchronizedMap()`，并发性能更好。

---

## 2. JDK 1.7 ConcurrentHashMap 原理

JDK 1.7 中，`ConcurrentHashMap` 使用：

```text
Segment 数组 + HashEntry 数组 + 链表
```

核心思想是：

```text
分段锁
```

---

### 2.1 Segment 是什么？

`Segment` 可以理解为一个小型的 HashMap。

每个 `Segment` 继承了 `ReentrantLock`，每次加锁只锁住一个 Segment。

结构类似：

```text
ConcurrentHashMap
 └── Segment[]
      ├── Segment
      │    └── HashEntry[]
      ├── Segment
      │    └── HashEntry[]
      └── Segment
           └── HashEntry[]
```

---

### 2.2 JDK 1.7 的优势

多个线程操作不同的 Segment 时，可以并发执行。

相比 `Hashtable` 整个方法加锁，性能更好。

---

## 3. JDK 1.8 ConcurrentHashMap 原理

JDK 1.8 中，`ConcurrentHashMap` 取消了 Segment 分段锁，底层结构变为：

```text
数组 + 链表 + 红黑树
```

和 HashMap 类似，但通过并发控制保证线程安全。

---

## 4. JDK 1.8 如何保证线程安全？

JDK 1.8 中主要依靠：

1. `volatile`；
2. CAS；
3. `synchronized`；
4. 分桶锁；
5. 扩容协助机制。

---

## 5. volatile 的作用

ConcurrentHashMap 中数组和节点的关键字段使用了 `volatile`，保证多线程之间的可见性。

例如节点中的 value 和 next：

```java
volatile V val;
volatile Node<K,V> next;
```

---

## 6. CAS 的作用

如果某个桶为空，ConcurrentHashMap 会通过 CAS 插入节点。

CAS 成功则插入成功，失败则说明有其他线程抢先插入，需要重新尝试。

---

## 7. synchronized 的作用

如果桶不为空，则会锁住当前桶的头节点。

```java
synchronized (f) {
    // 插入链表或红黑树
}
```

锁粒度是桶级别，而不是整个 Map。

所以不同线程操作不同桶时，可以并发执行。

---

## 8. 扩容协助机制

ConcurrentHashMap 扩容时，多个线程可以一起协助迁移数据。

这比单线程扩容效率更高。

扩容过程中会使用特殊节点 `ForwardingNode` 表示该桶已经迁移完成。

---

## 9. JDK 1.8 put 流程

ConcurrentHashMap 的 `put` 流程大致如下：

1. 判断 key 和 value 是否为 null，如果是则抛出异常；
2. 计算 hash 值；
3. 如果数组未初始化，则初始化数组；
4. 如果桶为空，使用 CAS 插入；
5. 如果桶正在扩容，则当前线程协助扩容；
6. 如果桶不为空，使用 `synchronized` 锁住桶头节点；
7. 判断是链表还是红黑树；
8. 插入或覆盖节点；
9. 判断是否需要树化；
10. 更新元素个数；
11. 判断是否需要扩容。

---

## 10. ConcurrentHashMap 为什么不允许 null？

`ConcurrentHashMap` 不允许 key 或 value 为 null。

原因是：

> 在并发环境下，无法区分返回 null 是因为 key 不存在，还是 value 本身就是 null。

例如：

```java
map.get(key);
```

如果返回 `null`，在并发环境下可能有两种情况：

1. key 不存在；
2. key 存在，但 value 是 null。

为了避免歧义，ConcurrentHashMap 直接禁止 null key 和 null value。

---

## 11. JDK 1.7 和 JDK 1.8 ConcurrentHashMap 对比

| 对比项 | JDK 1.7 | JDK 1.8 |
|---|---|---|
| 底层结构 | Segment + HashEntry + 链表 | 数组 + 链表 + 红黑树 |
| 加锁方式 | Segment 分段锁 | CAS + synchronized |
| 锁粒度 | Segment 级别 | 桶级别 |
| 是否支持红黑树 | 不支持 | 支持 |
| 并发性能 | 较好 | 更好 |
| 扩容方式 | Segment 内部扩容 | 多线程协助扩容 |

---

## 12. 面试回答

JDK 1.7 中，`ConcurrentHashMap` 使用 Segment 分段锁，每个 Segment 类似一个小 HashMap，不同 Segment 可以并发访问。

JDK 1.8 中，取消了 Segment，底层变为数组、链表和红黑树。它主要通过 CAS、`volatile` 和 `synchronized` 保证线程安全。桶为空时使用 CAS 插入，桶不为空时只锁当前桶的头节点，锁粒度更细，并发性能更好。

---

# 九、HashSet 如何保证元素不重复

## 1. HashSet 底层是什么？

`HashSet` 底层其实是基于 `HashMap` 实现的。

```java
private transient HashMap<E,Object> map;
```

HashSet 中的元素会作为 HashMap 的 key 存储。

value 使用一个固定的 Object 对象：

```java
private static final Object PRESENT = new Object();
```

---

## 2. HashSet 添加元素的本质

当执行：

```java
set.add("Tom");
```

底层实际执行类似：

```java
map.put("Tom", PRESENT);
```

也就是说：

```text
HashSet 的元素 = HashMap 的 key
```

---

## 3. HashSet 如何判断重复？

HashSet 判断元素是否重复，依赖 HashMap 的 key 判断逻辑。

主要依赖两个方法：

```text
hashCode()
equals()
```

判断过程：

1. 先计算元素的 `hashCode()`；
2. 根据 hash 值定位数组下标；
3. 如果该位置没有元素，说明不重复；
4. 如果该位置有元素，再使用 `equals()` 判断内容是否相同；
5. 如果 `equals()` 返回 true，说明元素重复；
6. 如果 `equals()` 返回 false，说明不是重复元素。

---

## 4. 示例

```java
Set<String> set = new HashSet<>();

set.add("Tom");
set.add("Tom");

System.out.println(set.size());
```

输出结果：

```text
1
```

因为两个 `"Tom"` 的 `hashCode()` 相同，`equals()` 也返回 `true`，所以 HashSet 认为它们是重复元素。

---

## 5. 自定义对象去重要重写 equals 和 hashCode

如果 HashSet 中存储自定义对象，需要重写 `equals()` 和 `hashCode()`。

错误示例：

```java
public class User {

    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

测试：

```java
Set<User> set = new HashSet<>();

set.add(new User("Tom", 18));
set.add(new User("Tom", 18));

System.out.println(set.size());
```

输出结果可能是：

```text
2
```

因为默认的 `equals()` 和 `hashCode()` 使用对象地址判断。

---

## 6. 正确写法

```java
import java.util.Objects;

public class User {

    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        User user = (User) o;

        return age == user.age &&
                Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }
}
```

---

## 7. 面试回答

`HashSet` 底层是通过 `HashMap` 实现的。

HashSet 添加元素时，实际上是把元素作为 HashMap 的 key，把一个固定对象作为 value 存进去。

HashSet 通过 `hashCode()` 和 `equals()` 判断元素是否重复。如果两个元素的 hash 值相同，并且 `equals()` 返回 true，就认为是重复元素，不会重复添加。

---

# 十、TreeMap 和 HashMap 区别

## 1. 底层数据结构不同

### HashMap

`HashMap` 底层是：

```text
数组 + 链表 + 红黑树
```

---

### TreeMap

`TreeMap` 底层是：

```text
红黑树
```

TreeMap 会根据 key 进行排序。

---

## 2. 是否有序不同

### HashMap

`HashMap` 不保证元素顺序。

遍历顺序既不是插入顺序，也不是排序顺序。

---

### TreeMap

`TreeMap` 会根据 key 排序。

排序方式有两种：

1. key 实现 `Comparable` 接口；
2. 创建 TreeMap 时传入 `Comparator` 比较器。

---

## 3. 查询、插入、删除复杂度不同

### HashMap

理想情况下：

```text
O(1)
```

哈希冲突严重时，链表情况下可能退化为：

```text
O(n)
```

JDK 1.8 红黑树情况下可以优化为：

```text
O(log n)
```

---

### TreeMap

由于底层是红黑树，所以查询、插入、删除时间复杂度都是：

```text
O(log n)
```

---

## 4. 对 null key 的支持不同

### HashMap

`HashMap` 允许一个 `null` key，允许多个 `null` value。

```java
Map<String, String> map = new HashMap<>();

map.put(null, "value");
map.put("a", null);
```

---

### TreeMap

`TreeMap` 默认使用自然排序时，不允许 `null` key。

因为 `null` 无法调用 `compareTo()` 进行比较。

如果自定义 `Comparator` 能够处理 `null`，则可以支持 `null` key。

---

## 5. 使用场景不同

### HashMap 适合场景

如果只需要快速存取键值对，不关心顺序，优先使用 `HashMap`。

---

### TreeMap 适合场景

如果需要按照 key 排序，或者需要范围查询，可以使用 `TreeMap`。

例如：

```java
TreeMap<Integer, String> map = new TreeMap<>();

map.put(3, "C");
map.put(1, "A");
map.put(2, "B");

System.out.println(map);
```

输出结果：

```text
{1=A, 2=B, 3=C}
```

---

## 6. TreeMap 支持范围操作

TreeMap 实现了 `NavigableMap` 接口，支持很多范围查询方法。

例如：

```java
treeMap.firstKey();
treeMap.lastKey();
treeMap.ceilingKey(key);
treeMap.floorKey(key);
treeMap.subMap(fromKey, toKey);
```

---

## 7. TreeMap 和 HashMap 对比表

| 对比项 | HashMap | TreeMap |
|---|---|---|
| 底层结构 | 数组 + 链表 + 红黑树 | 红黑树 |
| 是否有序 | 无序 | 按 key 排序 |
| 查询效率 | 平均 O(1) | O(log n) |
| 插入效率 | 平均 O(1) | O(log n) |
| 删除效率 | 平均 O(1) | O(log n) |
| null key | 允许一个 null key | 默认不允许 null key |
| null value | 允许多个 null value | 允许 null value |
| 是否支持范围查询 | 不支持 | 支持 |
| 适用场景 | 快速查找，不关心顺序 | 需要排序或范围查询 |

---

## 8. 面试回答

`HashMap` 底层是数组、链表和红黑树，元素无序，查询、插入、删除平均时间复杂度是 `O(1)`，适合快速存取。

`TreeMap` 底层是红黑树，会按照 key 排序，查询、插入、删除时间复杂度是 `O(log n)`，适合需要排序或范围查询的场景。

---

# 十一、面试速记版

## 1. ArrayList 和 LinkedList 区别

`ArrayList` 底层是动态数组，查询快，支持随机访问，插入删除可能需要移动元素。

`LinkedList` 底层是双向链表，查询慢，插入删除节点本身快，但定位节点需要遍历，内存占用更大。

---

## 2. HashMap 底层原理

JDK 1.8 中，`HashMap` 底层是数组、链表和红黑树。

通过 key 的 hash 值计算数组下标，如果发生哈希冲突，则使用链表或红黑树存储。

---

## 3. HashMap 为什么线程不安全

`HashMap` 没有加锁，多线程 put 时可能出现数据覆盖、数据丢失、size 不准确等问题。

JDK 1.7 中并发扩容还可能因为头插法导致链表成环。

---

## 4. HashMap 扩容机制

默认容量是 16，加载因子是 0.75，默认阈值是 12。

当元素数量超过阈值时，容量扩展为原来的 2 倍。

JDK 1.8 中扩容后元素要么在原位置，要么在原位置加旧容量的位置。

---

## 5. HashMap put 流程

先计算 key 的 hash，再通过 `(n - 1) & hash` 计算下标。

桶为空直接插入；桶不为空则判断 key 是否相同，相同覆盖，不同则插入链表或红黑树。

插入后判断是否树化和是否扩容。

---

## 6. HashMap 为什么容量是 2 的幂

为了让 `(n - 1) & hash` 等价于 `hash % n`，用位运算提升性能。

同时可以让数据分布更均匀，并且扩容迁移更高效。

---

## 7. HashMap JDK 1.7 和 JDK 1.8 区别

JDK 1.7 是数组加链表，使用头插法。

JDK 1.8 是数组加链表加红黑树，使用尾插法，扩容更高效，链表过长时会转红黑树。

---

## 8. ConcurrentHashMap 原理

JDK 1.7 使用 Segment 分段锁。

JDK 1.8 取消 Segment，使用数组、链表和红黑树，通过 CAS、volatile 和 synchronized 保证线程安全。

---

## 9. HashSet 如何保证元素不重复

`HashSet` 底层是 `HashMap`。

元素作为 HashMap 的 key 存储，通过 `hashCode()` 和 `equals()` 判断元素是否重复。

---

## 10. TreeMap 和 HashMap 区别

`HashMap` 无序，底层是数组、链表和红黑树，平均查询效率是 `O(1)`。

`TreeMap` 有序，底层是红黑树，会按照 key 排序，查询、插入、删除复杂度是 `O(log n)`。

---

# 十二、总览表

| 问题 | 核心结论 |
|---|---|
| ArrayList 和 LinkedList 区别 | ArrayList 是动态数组，查询快；LinkedList 是双向链表，节点插入删除快但查询慢 |
| HashMap 底层原理 | JDK 1.8 是数组 + 链表 + 红黑树 |
| HashMap 为什么线程不安全 | 没有并发控制，多线程 put 和 resize 可能导致数据异常 |
| HashMap 扩容机制 | 超过阈值后容量变为原来的 2 倍 |
| HashMap put 流程 | 计算 hash、定位桶、插入或覆盖、树化、扩容 |
| HashMap 为什么容量是 2 的幂 | 位运算取下标更快，分布更均匀，扩容更高效 |
| JDK 1.7 和 JDK 1.8 HashMap 区别 | JDK 1.7 数组 + 链表 + 头插法；JDK 1.8 数组 + 链表 + 红黑树 + 尾插法 |
| ConcurrentHashMap 原理 | JDK 1.7 分段锁；JDK 1.8 CAS + synchronized + volatile |
| HashSet 如何保证不重复 | 底层使用 HashMap，依靠 key 的 hashCode 和 equals 判断重复 |
| TreeMap 和 HashMap 区别 | TreeMap 有序且支持范围查询；HashMap 无序但平均性能更高 |

---

# 十三、完整面试回答模板

`ArrayList` 和 `LinkedList` 的区别主要在底层结构。`ArrayList` 底层是动态数组，支持随机访问，查询效率高，但中间插入删除需要移动元素。`LinkedList` 底层是双向链表，插入删除节点本身较快，但查找节点需要遍历，查询效率低，内存占用也更大。

`HashMap` 在 JDK 1.8 中底层是数组、链表和红黑树。put 元素时，会先计算 key 的 hash 值，再通过 `(n - 1) & hash` 计算数组下标。如果桶为空就直接插入；如果桶不为空，就判断 key 是否相同，相同则覆盖 value，不同则插入链表或红黑树。当链表长度达到 8，并且数组容量达到 64 时，链表会转成红黑树。

`HashMap` 是线程不安全的，因为它没有加锁，也没有并发控制。多线程同时 put 时可能出现数据覆盖、数据丢失、size 不准确等问题。JDK 1.7 中并发扩容时，因为使用头插法，还可能导致链表成环。JDK 1.8 改为尾插法后减少了这个问题，但仍然不是线程安全的。

`HashMap` 默认容量是 16，默认加载因子是 0.75，所以默认扩容阈值是 12。当元素数量超过阈值时，会扩容为原来的 2 倍。JDK 1.8 扩容时，元素位置要么保持不变，要么移动到原位置加旧容量的位置。

HashMap 的容量之所以是 2 的幂，是因为可以通过 `(n - 1) & hash` 快速计算数组下标，等价于 `hash % n`，但位运算效率更高。同时 2 的幂也有利于元素均匀分布和扩容迁移。

JDK 1.7 和 JDK 1.8 的 HashMap 主要区别是：JDK 1.7 使用数组加链表，链表插入使用头插法；JDK 1.8 使用数组、链表和红黑树，链表插入使用尾插法，并且链表过长时会转成红黑树。

`ConcurrentHashMap` 是线程安全的 HashMap。JDK 1.7 中它使用 Segment 分段锁；JDK 1.8 中取消了 Segment，使用数组、链表和红黑树，通过 CAS、volatile 和 synchronized 保证线程安全。桶为空时使用 CAS 插入，桶不为空时锁住桶头节点，因此锁粒度更细，并发性能更好。

`HashSet` 底层是 `HashMap`，HashSet 中的元素作为 HashMap 的 key 存储，value 是一个固定对象。它通过元素的 `hashCode()` 和 `equals()` 判断是否重复。

`TreeMap` 底层是红黑树，会按照 key 排序，查询、插入、删除时间复杂度都是 `O(log n)`，适合需要排序或范围查询的场景。`HashMap` 是无序的，平均查询、插入、删除时间复杂度是 `O(1)`，适合不关心顺序、只追求快速存取的场景。
