# Redis 架构模式总结

Redis 常见架构模式：

```text
单机模式
主从复制
哨兵模式
Cluster 集群模式
分片集群 / 客户端分片
```

---

## 1. 单机模式

### 架构

```text
Client
  ↓
Redis 单节点
```

### 特点

- 部署简单。
- 性能高。
- 没有网络复制开销。

### 缺点

- 单点故障。
- 容量受单机内存限制。
- 无法水平扩展。
- 故障需要人工恢复。

### 适用场景

```text
开发环境
测试环境
数据量小
非核心缓存
```

---

## 2. 主从复制模式

### 架构

```text
Client
  ↓
Master
  ↓
Slave / Replica
```

也可以一主多从：

```text
        Master
       /   |   \
   Slave Slave Slave
```

### 原理

```text
写请求走 Master
读请求可走 Slave
Master 将数据同步给 Slave
```

### 优点

- 支持读写分离。
- 从节点可分担读压力。
- 从节点可作为数据备份。
- 为哨兵故障转移打基础。

### 缺点

- Master 仍然是单点。
- 主从复制异步，可能有数据延迟。
- Master 宕机后不能自动切换。
- 写能力仍受单 Master 限制。

### 适用场景

```text
读多写少
需要数据备份
可接受主从延迟
```

---

## 3. 哨兵模式 Sentinel

### 架构

```text
          Sentinel 集群
        /      |      \
Client -> Master -> Slave
              ↓
            Slave
```

### 核心作用

```text
监控
故障发现
自动选主
故障转移
通知客户端
```

### 故障转移流程

```text
1. Sentinel 监控 Master 和 Slave。
2. Master 宕机。
3. 多个 Sentinel 判断 Master 客观下线。
4. Sentinel 选举一个 Leader。
5. Leader 从 Slave 中选一个新 Master。
6. 其他 Slave 切换复制新 Master。
7. 通知客户端新的 Master 地址。
```

### 优点

- 支持自动故障转移。
- 解决 Master 单点问题。
- 架构相对简单。
- 适合中小规模 Redis 高可用。

### 缺点

- 不支持数据自动分片。
- 写能力仍受单 Master 限制。
- 主从切换期间可能短暂不可用。
- 异步复制下仍可能丢少量数据。

### 适用场景

```text
数据量不大
需要高可用
写压力不特别大
读多写少缓存场景
```

---

## 4. Cluster 集群模式

### 架构

```text
Redis Cluster
├── Master1 -> Slave1
├── Master2 -> Slave2
└── Master3 -> Slave3
```

### 核心原理

Redis Cluster 使用：

```text
16384 个 hash slot
```

每个 key 根据 hash 计算所属 slot：

```text
slot = CRC16(key) % 16384
```

不同 slot 分布在不同 Master 节点上。

```text
Master1：0 ~ 5460
Master2：5461 ~ 10922
Master3：10923 ~ 16383
```

---

## 5. Cluster 请求路由

客户端访问 key：

```text
Client
  ↓
计算 key 所属 slot
  ↓
请求对应 Redis 节点
```

如果请求到了错误节点，Redis 会返回：

```text
MOVED
ASK
```

客户端根据提示重定向到正确节点。

---

## 6. Cluster 的优点

- 支持数据分片。
- 支持水平扩展。
- 支持高可用。
- 多 Master 分担写压力。
- 节点故障可自动切换。
- 适合大容量、高并发场景。

---

## 7. Cluster 的缺点

- 架构更复杂。
- 运维成本更高。
- 多 key 操作受 slot 限制。
- 跨 slot 事务不支持。
- 批量操作需要注意 key 分布。
- 客户端需要支持 Cluster 协议。

---

## 8. Hash Tag

Redis Cluster 中，如果希望多个 key 落到同一个 slot，可以使用 Hash Tag。

```text
user:{1001}:name
user:{1001}:order
user:{1001}:cart
```

Redis 只对 `{}` 中的内容计算 slot：

```text
1001
```

所以这些 key 会落到同一个 slot。

适合：

```text
同一用户相关 key 做批量操作
Lua 脚本需要多个 key 在同一 slot
```

---

## 9. 客户端分片模式

### 架构

```text
Client
  ↓
根据 Hash 算法选择 Redis 节点
  ↓
Redis1 / Redis2 / Redis3
```

### 原理

客户端自己决定 key 存到哪个 Redis 节点。

常见算法：

```text
hash(key) % N
一致性 Hash
```

### 优点

- 实现简单。
- 不依赖 Redis Cluster。
- 客户端可控。

### 缺点

- 客户端逻辑复杂。
- 扩容迁移麻烦。
- 节点故障需要客户端处理。
- 不如 Redis Cluster 标准。

### 适用场景

```text
早期架构
自研缓存组件
简单分片需求
```

---

## 10. 各模式对比

| 模式 | 高可用 | 水平扩展 | 自动故障转移 | 复杂度 | 适用场景 |
|---|---|---|---|---|---|
| 单机 | 否 | 否 | 否 | 低 | 测试、小数据 |
| 主从 | 部分 | 读扩展 | 否 | 低 | 读多写少 |
| 哨兵 | 是 | 读扩展 | 是 | 中 | 高可用缓存 |
| Cluster | 是 | 是 | 是 | 高 | 大容量、高并发 |
| 客户端分片 | 取决于实现 | 是 | 取决于实现 | 中高 | 自研分片 |

---

## 11. 选型建议

| 场景 | 推荐 |
|---|---|
| 本地开发 / 测试 | 单机模式 |
| 简单读扩展 | 主从复制 |
| 中小规模高可用 | 哨兵模式 |
| 大数据量缓存 | Cluster |
| 高并发读写 | Cluster |
| 需要水平扩容 | Cluster |
| 老系统已有分片逻辑 | 客户端分片 |
| 核心业务缓存 | 哨兵 / Cluster |

---

## 12. 总结

Redis 常见架构模式有单机、主从、哨兵和 Cluster。

单机模式部署简单，但存在单点故障；主从复制可以实现读写分离和数据备份，但 Master 宕机后不能自动切换；哨兵模式在主从复制基础上增加监控和自动故障转移，适合中小规模高可用场景；Cluster 模式通过 16384 个 hash slot 实现数据分片和水平扩展，同时支持自动故障转移，适合大容量、高并发场景。

一句话总结：

```text
单机简单但不高可用；
主从能读写分离但不能自动切换；
哨兵解决高可用；
Cluster 解决高可用 + 水平扩展。
```
