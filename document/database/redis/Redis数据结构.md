# Redis 数据结构总结

Redis 常见数据结构包括：**String、Hash、List、Set、ZSet、Bitmap、HyperLogLog、Geo、Stream**。

---

## 1. String

### 特点

最基础的数据结构，可以存字符串、数字、JSON、二进制数据。

### 底层结构

```text
SDS
```

### 常见场景

- 缓存
- 计数器
- 分布式锁
- 验证码
- Token / Session

---

## 2. Hash

### 特点

键值对结构，适合存对象。

```text
user:1 -> name=Tom, age=18
```

### 底层结构

```text
listpack / hashtable
```

### 常见场景

- 用户信息
- 商品信息
- 购物车
- 配置信息

---

## 3. List

### 特点

有序列表，支持两端插入和弹出。

### 底层结构

```text
quicklist
```

### 常见场景

- 消息队列
- 任务队列
- 最新列表
- 栈 / 队列

---

## 4. Set

### 特点

无序集合，元素不重复，支持交集、并集、差集。

### 底层结构

```text
intset / hashtable
```

### 常见场景

- 去重
- 标签
- 共同好友
- 抽奖
- 黑名单

---

## 5. ZSet

### 特点

有序集合，元素不重复，每个元素有一个 `score`，按 `score` 排序。

### 底层结构

```text
listpack / skiplist + dict
```

### 常见场景

- 排行榜
- 延迟队列
- 热搜榜
- 优先级队列
- 滑动窗口限流

---

## 6. Bitmap

### 特点

基于 bit 位存储状态，本质是 String。

### 常见场景

- 用户签到
- 在线状态
- 活跃用户统计
- 布尔状态统计

---

## 7. HyperLogLog

### 特点

用于海量数据基数统计，有一定误差，但占用内存很小。

### 常见场景

- UV 统计
- 独立访客统计
- 搜索关键词去重统计

---

## 8. Geo

### 特点

用于存储地理位置，支持距离计算和附近查询。

### 底层结构

```text
ZSet
```

### 常见场景

- 附近的人
- 附近门店
- 打车派单
- 地理位置搜索

---

## 9. Stream

### 特点

Redis 提供的消息流结构，支持消息 ID、消费组、ACK。

### 常见场景

- 消息队列
- 异步任务
- 事件流
- 日志流

---

## 10. 对比总结

| 数据结构 | 特点 | 常见场景 |
|---|---|---|
| String | 简单 Key-Value | 缓存、计数器、锁 |
| Hash | 对象结构 | 用户、商品、购物车 |
| List | 有序列表 | 队列、最新列表 |
| Set | 无序去重 | 标签、抽奖、共同好友 |
| ZSet | 按分数排序 | 排行榜、延迟队列 |
| Bitmap | 位状态 | 签到、在线状态 |
| HyperLogLog | 基数统计 | UV 统计 |
| Geo | 地理位置 | 附近的人、门店 |
| Stream | 消息流 | 消息队列、事件流 |

---

## 11. 总结

Redis 常见数据结构包括 String、Hash、List、Set、ZSet、Bitmap、HyperLogLog、Geo 和 Stream。

String 适合缓存、计数器和分布式锁；Hash 适合存对象；List 适合队列；Set 适合去重；ZSet 适合排行榜和延迟队列；Bitmap 适合签到和状态统计；HyperLogLog 适合 UV 统计；Geo 适合地理位置查询；Stream 适合消息队列。

一句话总结：

```text
String 做缓存，Hash 存对象，List 做队列，Set 做去重，ZSet 做排序，Bitmap 做状态，HyperLogLog 做 UV，Geo 做位置，Stream 做消息。
```
