# Java 四种引用总结

Java 中对象引用分为四种：**强引用、软引用、弱引用、虚引用**。  
它们主要用于配合 GC 管理对象生命周期。

---

## 1. 强引用

### 实现原理

普通对象引用就是强引用。

```java
Object obj = new Object();
```

只要强引用还存在，GC 就不会回收该对象。

```text
强引用存在
  ↓
对象不会被 GC 回收
```

### 特点

- 最常见的引用方式。
- 只要引用存在，对象就不会被回收。
- 可能导致内存泄漏。

### 适用场景

- 普通业务对象。
- 必须长期存在的对象。
- Spring Bean、缓存配置、业务数据等。

---

## 2. 软引用

### 实现原理

软引用通过 `SoftReference` 实现。

```java
SoftReference<Object> softRef =
        new SoftReference<>(new Object());
```

当内存充足时，软引用对象不会被回收；当内存不足时，GC 会回收软引用对象。

```text
内存充足：不回收
内存不足：可能回收
```

### 特点

- 比强引用弱。
- 内存不足时才会被回收。
- 适合做缓存。

### 适用场景

- 本地缓存。
- 图片缓存。
- 大对象缓存。

### 注意

软引用缓存并不完全可靠，因为对象什么时候被回收由 JVM 决定。

---

## 3. 弱引用

### 实现原理

弱引用通过 `WeakReference` 实现。

```java
WeakReference<Object> weakRef =
        new WeakReference<>(new Object());
```

只要发生 GC，不管内存是否充足，弱引用对象都会被回收。

```text
发生 GC
  ↓
弱引用对象被回收
```

### 特点

- 生命周期比软引用更短。
- 只要 GC 发生，就可能被回收。
- 常用于避免内存泄漏。

### 适用场景

- `ThreadLocalMap` 中的 key。
- `WeakHashMap`。
- 不希望影响对象回收的引用场景。

### 示例

```java
WeakHashMap<Object, String> map = new WeakHashMap<>();
```

当 key 没有强引用时，GC 后对应 Entry 会被清理。

---

## 4. 虚引用

### 实现原理

虚引用通过 `PhantomReference` 实现，必须配合 `ReferenceQueue` 使用。

```java
ReferenceQueue<Object> queue = new ReferenceQueue<>();

PhantomReference<Object> phantomRef =
        new PhantomReference<>(new Object(), queue);
```

虚引用无法通过 `get()` 获取对象：

```java
phantomRef.get(); // 永远返回 null
```

它的主要作用不是访问对象，而是跟踪对象被回收的时机。

```text
对象即将被回收
  ↓
虚引用进入 ReferenceQueue
  ↓
程序可以做资源清理
```

### 特点

- 最弱的引用。
- 不能通过引用获取对象。
- 主要用于对象回收通知。
- 常用于堆外内存、直接内存清理。

### 适用场景

- 跟踪对象被 GC 回收。
- 管理堆外内存。
- 资源释放监控。
- Cleaner / DirectByteBuffer 相关场景。

---

## 5. 四种引用对比

| 引用类型 | 是否影响 GC 回收 | 回收时机 | 是否能获取对象 | 典型场景 |
|---|---|---|---|---|
| 强引用 | 是 | 只要强引用存在就不回收 | 能 | 普通对象 |
| 软引用 | 较弱影响 | 内存不足时回收 | 能 | 缓存 |
| 弱引用 | 不强保留 | 发生 GC 时回收 | 能，回收前可获取 | WeakHashMap、ThreadLocal |
| 虚引用 | 不影响 | 对象回收前进入队列 | 不能 | 回收通知、堆外内存 |

---

## 6. 引用强度顺序

```text
强引用 > 软引用 > 弱引用 > 虚引用
```

对象越靠右，越容易被 GC 回收。

---

## 7. ReferenceQueue

### 实现原理

`ReferenceQueue` 用于接收即将被回收或已经被回收的引用对象。

常和软引用、弱引用、虚引用配合使用。

```java
ReferenceQueue<Object> queue = new ReferenceQueue<>();

WeakReference<Object> ref =
        new WeakReference<>(new Object(), queue);
```

当对象被 GC 回收后，对应的引用会进入 `ReferenceQueue`。

### 作用

- 监听对象回收。
- 清理关联资源。
- 防止引用对象堆积。
- 实现缓存清理机制。

---

## 8. 总结

Java 中有四种引用：强引用、软引用、弱引用和虚引用。

强引用是最常见的引用，只要强引用存在，对象就不会被 GC 回收。

软引用通过 `SoftReference` 实现，内存充足时不会回收，内存不足时会被回收，适合做缓存。

弱引用通过 `WeakReference` 实现，只要发生 GC，弱引用对象就会被回收，常用于 `WeakHashMap` 和 `ThreadLocalMap` 的 key。

虚引用通过 `PhantomReference` 实现，无法通过 `get()` 获取对象，主要用于跟踪对象回收时机，通常配合 `ReferenceQueue` 使用，适合堆外内存释放和资源清理。

一句话总结：

```text
强引用不回收，软引用内存不足才回收，弱引用一 GC 就回收，虚引用只用于回收通知。
```
