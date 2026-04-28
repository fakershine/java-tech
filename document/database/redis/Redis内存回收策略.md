# Redis 内存回收策略总结

Redis 内存回收主要分为两类：

```text
过期删除策略：删除已经设置 TTL 且到期的 Key
内存淘汰策略：Redis 内存达到 maxmemory 后，淘汰部分 Key
```

---

## 1. 过期删除策略

Redis 对设置了过期时间的 Key，不是到期后立刻删除，而是通过 **惰性删除 + 定期删除** 结合处理。Redis 官方文档也说明，Key 过期删除分为被动方式和主动方式：访问时发现过期会删除，同时 Redis 会周期性随机检查部分设置了过期时间的 Key 并删除过期 Key。:contentReference[oaicite:0]{index=0}

---

## 2. 惰性删除

### 实现原理

当客户端访问某个 Key 时，Redis 会检查它是否过期。

```text
访问 Key
  ↓
判断是否过期
  ↓
如果过期，删除 Key
  ↓
返回 nil
```

### 优势

- 实现简单。
- 不额外消耗 CPU。
- 只在访问时检查。

### 劣势

- 如果过期 Key 一直不被访问，就不会被删除。
- 可能造成内存浪费。

---

## 3. 定期删除

### 实现原理

Redis 会定期随机抽取一部分设置了过期时间的 Key，检查是否过期。

```text
定期任务
  ↓
随机扫描部分 TTL Key
  ↓
删除已过期 Key
```

### 优势

- 能清理不再被访问的过期 Key。
- 避免过期 Key 长期占用内存。

### 劣势

- 不是全量扫描。
- 过期 Key 不一定马上删除。
- 扫描太频繁会增加 CPU 开销。

---

## 4. 内存淘汰策略

当 Redis 内存超过 `maxmemory` 时，会根据配置的 `maxmemory-policy` 淘汰 Key。Redis 官方文档说明，Redis 在新增数据导致内存超过 `maxmemory` 后，会按选定的淘汰策略删除 Key，直到内存回到限制以下。:contentReference[oaicite:1]{index=1}

配置示例：

```bash
maxmemory 4gb
maxmemory-policy allkeys-lru
```

---

## 5. 常见淘汰策略

| 策略 | 说明 |
|---|---|
| `noeviction` | 不淘汰，内存满后写请求报错 |
| `allkeys-lru` | 从所有 Key 中淘汰最近最少使用的 Key |
| `volatile-lru` | 从设置了过期时间的 Key 中淘汰最近最少使用的 Key |
| `allkeys-lfu` | 从所有 Key 中淘汰使用频率最低的 Key |
| `volatile-lfu` | 从设置了过期时间的 Key 中淘汰使用频率最低的 Key |
| `allkeys-random` | 从所有 Key 中随机淘汰 |
| `volatile-random` | 从设置了过期时间的 Key 中随机淘汰 |
| `volatile-ttl` | 从设置了过期时间的 Key 中优先淘汰 TTL 最短的 Key |

---

## 6. noeviction

### 实现原理

Redis 不主动淘汰 Key。

```text
内存达到 maxmemory
  ↓
写请求报错
  ↓
读请求通常不受影响
```

### 适用场景

```text
Redis 作为数据库使用
不能随便丢数据
```

---

## 7. allkeys-lru

### 实现原理

从所有 Key 中，淘汰最近最少使用的 Key。

```text
所有 Key
  ↓
选择最近最少访问的 Key
  ↓
淘汰
```

### 适用场景

```text
Redis 纯缓存场景
所有 Key 都可以被淘汰
```

---

## 8. volatile-lru

### 实现原理

只从设置了过期时间的 Key 中，淘汰最近最少使用的 Key。

```text
带 TTL 的 Key
  ↓
选择最近最少访问的 Key
  ↓
淘汰
```

### 适用场景

```text
部分 Key 是缓存
部分 Key 是重要数据
只希望淘汰缓存 Key
```

注意：

```text
如果没有设置 TTL 的 Key，就不会被 volatile 策略淘汰。
```

---

## 9. allkeys-lfu

### 实现原理

从所有 Key 中，淘汰访问频率最低的 Key。

```text
所有 Key
  ↓
选择访问频率最低的 Key
  ↓
淘汰
```

### 适用场景

```text
热点数据明显
希望保留高频访问 Key
```

---

## 10. volatile-ttl

### 实现原理

从设置了过期时间的 Key 中，优先淘汰剩余 TTL 最短的 Key。

```text
带 TTL 的 Key
  ↓
选择快过期的 Key
  ↓
优先淘汰
```

### 适用场景

```text
希望优先清理即将过期的数据
```

---

## 11. 策略选择建议

| 场景 | 推荐策略 |
|---|---|
| Redis 只做缓存 | `allkeys-lru` / `allkeys-lfu` |
| 热点访问明显 | `allkeys-lfu` |
| 只允许淘汰带 TTL 的缓存数据 | `volatile-lru` / `volatile-lfu` |
| 不能丢数据 | `noeviction` |
| 临时数据、会话、验证码 | `volatile-ttl` |
| 对淘汰准确性要求不高 | `allkeys-random` |

---

## 12. LRU 和 LFU 区别

| 对比项 | LRU | LFU |
|---|---|---|
| 含义 | 最近最少使用 | 最少频率使用 |
| 关注点 | 最近有没有访问 | 一段时间内访问次数 |
| 适合场景 | 最近热点变化快 | 长期热点明显 |
| 示例 | 最近没用就淘汰 | 访问次数少就淘汰 |

简单理解：

```text
LRU 看最近一次访问时间
LFU 看访问频率
```

---

## 13. 总结

Redis 内存回收主要包括两部分：过期删除和内存淘汰。

过期删除针对设置了 TTL 的 Key，Redis 使用惰性删除和定期删除结合的方式。惰性删除是在访问 Key 时检查是否过期，过期则删除；定期删除是后台定期随机扫描部分设置了过期时间的 Key，删除已经过期的 Key。

内存淘汰发生在 Redis 内存达到 `maxmemory` 后，通过 `maxmemory-policy` 决定淘汰哪些 Key。常见策略有 `noeviction`、`allkeys-lru`、`volatile-lru`、`allkeys-lfu`、`volatile-lfu`、`allkeys-random`、`volatile-random`、`volatile-ttl`。

一句话总结：

```text
Redis 过期删除 = 惰性删除 + 定期删除；
Redis 内存淘汰 = maxmemory 达到上限后，根据 maxmemory-policy 淘汰 Key。
```
