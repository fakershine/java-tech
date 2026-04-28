# Redis 中的大 Key 总结

Redis 大 Key 是指：**某个 Key 对应的 Value 过大，或者集合类型中元素过多**。

大 Key 会影响 Redis 性能、网络传输、持久化、主从复制和集群稳定性。

---

## 1. 什么是大 Key

常见判断标准：

```text
String 类型：value 超过 10KB / 100KB
Hash/List/Set/ZSet：元素数量过多，例如超过 5000 / 10000
```

实际标准要根据业务和 Redis 性能要求判断。

---

## 2. 常见大 Key 场景

| 类型 | 大 Key 示例 |
|---|---|
| String | 存储超大 JSON、图片、文件内容 |
| Hash | 一个用户 Hash 中字段过多 |
| List | 消息队列积压大量元素 |
| Set | 某个标签下用户 ID 过多 |
| ZSet | 排行榜元素过多 |
| Bitmap | 用户量极大时位图过大 |

---

## 3. 大 Key 的危害

### 1. 阻塞 Redis

Redis 主线程执行命令，如果操作大 Key，可能阻塞其他请求。

例如：

```bash
DEL bigKey
HGETALL bigHash
LRANGE bigList 0 -1
SMEMBERS bigSet
ZRANGE bigZSet 0 -1
```

这些命令可能一次处理大量数据，导致 Redis 卡顿。

---

### 2. 网络传输压力大

一次返回大量数据：

```text
Redis -> 客户端
```

会导致：

```text
网络带宽升高
客户端处理变慢
Redis 响应时间变长
```

---

### 3. 内存分布不均

Redis Cluster 中，大 Key 可能集中在某个节点。

```text
节点 A：一个大 Key 占用大量内存
节点 B：正常
节点 C：正常
```

会导致：

```text
集群内存倾斜
某个节点压力过大
```

---

### 4. 影响持久化和复制

大 Key 会影响：

```text
RDB 持久化
AOF 重写
主从复制
数据迁移
故障恢复
```

数据越大，复制和恢复成本越高。

---

## 4. 如何发现大 Key

### 方式一：redis-cli 扫描

```bash
redis-cli --bigkeys
```

作用：

```text
扫描 Redis 中较大的 Key
```

---

### 方式二：SCAN + MEMORY USAGE

```bash
SCAN 0 COUNT 1000
MEMORY USAGE key
```

作用：

```text
逐步扫描 Key，并查看单个 Key 占用内存
```

注意：

```text
不要使用 KEYS *
```

因为 `KEYS *` 会阻塞 Redis。

---

### 方式三：监控分析

通过监控观察：

```text
慢查询
网络流量
内存使用
单节点内存倾斜
命令耗时
```

---

## 5. 如何解决大 Key

### 1. 拆分大 Key

把一个大 Key 拆成多个小 Key。

例如原来：

```text
user:followers -> Set 存所有粉丝
```

拆分为：

```text
user:followers:1001:0
user:followers:1001:1
user:followers:1001:2
```

按 Hash 或分页拆分：

```text
hash(userId) % N
```

---

### 2. 控制集合大小

不要让一个集合无限增长。

可以：

```text
限制 List 长度
限制 ZSet 排行榜数量
定期清理历史数据
只保留最近 N 条
```

例如：

```bash
LTRIM listKey 0 999
```

只保留最新 1000 条。

---

### 3. 避免一次性全量读取

不要使用：

```bash
HGETALL
SMEMBERS
LRANGE 0 -1
ZRANGE 0 -1
```

推荐使用分页或游标：

```bash
HSCAN
SSCAN
ZSCAN
LRANGE start end
ZRANGE start end
```

---

### 4. 删除大 Key 使用异步删除

不要直接：

```bash
DEL bigKey
```

推荐：

```bash
UNLINK bigKey
```

`UNLINK` 会异步释放内存，减少阻塞。

---

### 5. 大对象不要放 Redis

不建议 Redis 存：

```text
大 JSON
图片
文件
超大文本
大量嵌套对象
```

可以改为：

```text
对象存储保存大文件
数据库保存大字段
Redis 只保存索引、状态、热点小数据
```

---

## 6. 不同类型优化建议

| 类型 | 优化方式 |
|---|---|
| String | 拆分字段，避免大 JSON |
| Hash | 按业务维度拆成多个 Hash |
| List | 控制长度，分页读取 |
| Set | 分桶存储，避免 SMEMBERS |
| ZSet | 只保留 TopN，分页查询 |
| Bitmap | 按时间或业务拆分 |

---

## 7. 生产建议

- Redis 只存热点小数据。
- 避免单个 Key 过大。
- 集合类型要限制元素数量。
- 查询集合时使用分页或 scan。
- 删除大 Key 使用 `UNLINK`。
- 定期扫描大 Key。
- Redis Cluster 要关注内存倾斜。
- 大 Value 不要直接放 Redis。

---

## 8. 总结

Redis 大 Key 是指单个 Key 对应的数据过大，例如 String Value 很大，或者 Hash、List、Set、ZSet 中元素数量过多。

大 Key 会导致 Redis 主线程阻塞、网络传输变慢、内存分布不均、持久化和主从复制变慢，严重时会造成 Redis 卡顿甚至超时。

排查大 Key 可以使用 `redis-cli --bigkeys`、`SCAN + MEMORY USAGE`，不要使用 `KEYS *`。解决方式主要是拆分大 Key、限制集合大小、分页读取、异步删除和避免存储大对象。

一句话总结：

```text
Redis 大 Key = 单个 Key 数据过大；
危害是阻塞、慢查询、网络压力、内存倾斜；
解决方式是拆分、限长、分页、异步删除和定期扫描。
```
