# MAT 如何分析内存泄露

MAT 全称是 **Eclipse Memory Analyzer Tool**，主要用于分析 JVM 堆转储文件 `heap dump`，定位 **哪些对象占用内存大、为什么没有被 GC 回收、是谁在引用它们**。

核心思路：

```text
导出 heap dump
  ↓
用 MAT 打开
  ↓
查看大对象
  ↓
查看引用链
  ↓
定位 GC Roots
  ↓
判断是否内存泄露
```

---

## 1. 先导出 heap dump

线上建议提前配置 JVM 参数：

```bash
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/data/logs/heapdump
```

如果进程还活着，也可以手动导出：

```bash
jmap -dump:format=b,file=heap.hprof <pid>
```

也可以导出存活对象：

```bash
jmap -dump:live,format=b,file=heap-live.hprof <pid>
```

注意：

```text
导出 heap dump 可能会造成 JVM 短暂停顿，线上要谨慎操作。
```

---

## 2. MAT 常用视图

| 视图 | 作用 |
|---|---|
| Leak Suspects | 自动分析疑似内存泄露 |
| Histogram | 按类统计对象数量和内存占用 |
| Dominator Tree | 按对象支配关系分析内存占用 |
| Top Consumers | 查看内存占用最大的对象 |
| Path To GC Roots | 查看对象为什么不能被回收 |
| OQL | 类似 SQL 的对象查询语言 |

---

## 3. 先看 Leak Suspects

打开 dump 后，MAT 会提示是否运行：

```text
Leak Suspects Report
```

它会自动分析可疑泄露点。

重点看：

```text
Problem Suspect
Accumulated Objects
Retained Heap
Shortest Paths To GC Roots
```

如果报告中出现：

```text
某个 Map / List / Cache 占用大量内存
```

通常就是重点排查对象。

---

## 4. 看 Histogram

Histogram 用于查看：

```text
每个类有多少对象
这些对象占用了多少内存
```

重点关注：

```text
Objects 数量异常大的类
Shallow Heap 较大的类
Retained Heap 较大的类
业务对象是否异常堆积
```

常见可疑对象：

```text
java.util.HashMap
java.util.ArrayList
byte[]
char[]
String
业务 DTO
缓存对象
ThreadLocalMap
```

---

## 5. Shallow Heap 和 Retained Heap

### Shallow Heap

表示对象本身占用的内存。

例如：

```text
一个 HashMap 对象本身占用的内存
```

不包含它引用的对象。

---

### Retained Heap

表示如果这个对象被回收，能够释放的总内存。

包含：

```text
对象本身
它直接或间接持有的对象
```

排查内存泄露时更关注：

```text
Retained Heap
```

因为它代表这个对象实际“拖住”了多少内存。

---

## 6. 看 Dominator Tree

Dominator Tree 是 MAT 中最重要的视图之一。

它可以帮助你找到：

```text
谁持有了最多内存
谁是大对象的上层引用者
删除谁可以释放最多内存
```

重点按：

```text
Retained Heap
```

倒序排序。

常见泄露形态：

```text
某个 ConcurrentHashMap 持有大量业务对象
某个 ArrayList 持有大量查询结果
某个 ThreadLocalMap 持有用户上下文
某个缓存对象没有过期策略
某个线程池队列堆积大量任务
```

---

## 7. 查看 Path To GC Roots

找到可疑对象后，右键：

```text
Path To GC Roots
  ↓
exclude weak/soft references
```

作用：

```text
查看这个对象为什么还活着
是谁从 GC Roots 一路引用到了它
```

建议选择：

```text
exclude weak/soft references
```

这样可以排除弱引用、软引用干扰，更容易看到真正强引用链。

---

## 8. 什么是 GC Roots

GC Roots 是垃圾回收的起点。

常见 GC Roots：

```text
线程栈中的局部变量
static 静态变量
JNI 引用
系统类加载器
运行中的线程
synchronized 锁持有对象
```

如果对象能从 GC Roots 访问到：

```text
对象不会被回收
```

所以内存泄露本质是：

```text
无用对象仍然被 GC Roots 间接引用
```

---

## 9. 常见内存泄露引用链

### 1. 静态集合泄露

```text
GC Roots
  ↓
static Map
  ↓
大量业务对象
```

常见代码：

```java
private static final Map<String, Object> CACHE = new HashMap<>();
```

问题：

```text
只放不删
没有容量限制
没有过期策略
```

---

### 2. ThreadLocal 泄露

```text
GC Roots
  ↓
Thread
  ↓
ThreadLocalMap
  ↓
Entry
  ↓
value
```

常见原因：

```text
线程池线程长期存活
ThreadLocal 用完没有 remove
```

修复：

```java
try {
    threadLocal.set(value);
} finally {
    threadLocal.remove();
}
```

---

### 3. 本地缓存泄露

```text
GC Roots
  ↓
CacheManager
  ↓
Cache
  ↓
大量缓存对象
```

常见问题：

```text
缓存无最大容量
缓存无过期时间
缓存 key 维度过细
缓存 value 太大
```

修复：

```text
设置 maximumSize
设置 expireAfterWrite
避免缓存大对象
```

---

### 4. 线程池队列堆积

```text
GC Roots
  ↓
ThreadPoolExecutor
  ↓
workQueue
  ↓
大量 Runnable
  ↓
业务对象
```

常见原因：

```text
消费速度慢
队列设置过大
任务生产过快
任务内部持有大对象
```

---

### 5. 一次性查询大量数据

```text
GC Roots
  ↓
Controller / Service 栈帧
  ↓
ArrayList
  ↓
大量业务对象
```

常见原因：

```text
select * 全量查询
导出 Excel 一次加载全部数据
分页参数失效
```

---

## 10. 用 OQL 查询对象

MAT 支持 OQL 查询。

### 查询某个类的对象

```sql
SELECT * FROM com.demo.UserDTO
```

### 查询大数组

```sql
SELECT * FROM byte[] b WHERE b.@length > 1024 * 1024
```

### 查询大字符串

```sql
SELECT * FROM java.lang.String s WHERE s.value.@length > 10000
```

### 查询 HashMap

```sql
SELECT * FROM java.util.HashMap
```

OQL 适合精确定位某类对象是否异常增长。

---

## 11. 分析步骤推荐

```text
1. 打开 heap dump。
2. 先跑 Leak Suspects Report。
3. 查看 Dominator Tree。
4. 按 Retained Heap 排序。
5. 找到占用最大的业务对象或集合。
6. 右键查看 Path To GC Roots。
7. 判断引用链是否合理。
8. 如果是缓存、ThreadLocal、队列、静态集合，重点排查代码。
9. 结合 GC 日志和业务日志确认发生时间。
10. 修复后压测验证。
```

---

## 12. 如何判断是不是内存泄露

不是所有大对象都是泄露。

### 可能不是泄露

```text
正在执行大查询
正在导出大文件
正在处理大批量任务
缓存设计本来就比较大
```

---

### 更像内存泄露

```text
Full GC 后内存不下降
对象数量持续增长
对象生命周期明显超过业务需要
大量对象被 static / ThreadLocal / Cache 持有
队列持续堆积不释放
同类对象数量远超预期
```

---

## 13. MAT 分析重点指标

| 指标 | 重点看什么 |
|---|---|
| Retained Heap | 谁间接持有最多内存 |
| Objects | 某类对象数量是否异常 |
| GC Roots | 对象为什么不能回收 |
| Dominator Tree | 最大内存支配对象 |
| Histogram | 哪些类对象最多 |
| Thread Overview | 是否线程持有大量对象 |

---

## 14. 常见修复方案

| 问题 | 修复方式 |
|---|---|
| static Map 泄露 | 删除无用数据，改用有界缓存 |
| ThreadLocal 泄露 | finally 中 remove |
| 缓存过大 | 设置最大容量和过期时间 |
| 队列堆积 | 限流、扩容消费者、缩小队列 |
| 大查询 | 分页、游标、流式处理 |
| 大文件 | 分片读写，不一次加载到内存 |
| 监听器未注销 | 生命周期结束时 remove listener |
| 动态类过多 | 复用类加载器，避免重复生成代理类 |

---

## 15. 总结

MAT 分析内存泄露时，首先要导出 heap dump，然后用 MAT 打开，先看 Leak Suspects 报告，再重点看 Dominator Tree 和 Histogram。

Dominator Tree 按 Retained Heap 排序，可以找到真正持有大量内存的对象。找到可疑对象后，通过 Path To GC Roots 查看它为什么没有被回收，也就是看它被谁引用。

如果发现大量对象被 static Map、ThreadLocal、缓存、线程池队列、监听器等长期引用，就很可能是内存泄露。

一句话总结：

```text
MAT 分析内存泄露 = 看 Retained Heap 找大对象 + Path To GC Roots 找引用链 + 判断对象是否该被释放。
```
