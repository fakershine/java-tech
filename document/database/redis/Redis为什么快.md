# Redis 为什么快

Redis 快的核心原因是：**基于内存操作、单线程模型避免锁竞争、IO 多路复用处理高并发连接，并且使用了高效的数据结构**。

---

## 1. 基于内存操作

Redis 数据主要存储在内存中。

```text
客户端请求
  ↓
Redis 直接操作内存
  ↓
返回结果
```

相比磁盘数据库：

```text
内存访问速度远高于磁盘 IO
```

所以 Redis 读写性能非常高。

---

## 2. 单线程模型

Redis 命令执行主要是单线程。

单线程的好处：

```text
没有线程切换开销
没有多线程锁竞争
不会出现复杂并发问题
执行逻辑简单高效
```

注意：

```text
Redis 单线程主要指命令执行线程是单线程
并不是 Redis 所有功能都是单线程
```

例如持久化、异步删除、网络 IO 等部分能力也可能使用后台线程。

---

## 3. IO 多路复用

Redis 使用 IO 多路复用处理大量客户端连接。

```text
一个线程监听多个 Socket
  ↓
哪个连接有事件就处理哪个连接
```

这样可以用少量线程处理大量连接，避免一个连接一个线程带来的巨大开销。

常见底层机制：

```text
epoll
kqueue
select
```

Linux 下主要依赖 `epoll`。

---

## 4. 高效数据结构

Redis 为不同数据类型设计了高效底层结构。

| 数据类型 | 底层结构 |
|---|---|
| String | SDS |
| Hash | listpack / hashtable |
| List | quicklist |
| Set | intset / hashtable |
| ZSet | listpack / skiplist + dict |

这些结构针对内存和性能做了优化。

例如：

```text
SDS 比普通 C 字符串更安全高效
Hash 小对象用 listpack 节省内存
ZSet 用 skiplist 支持快速范围查询
```

---

## 5. 简单高效的协议

Redis 使用 RESP 协议。

特点：

```text
格式简单
解析成本低
传输效率高
支持多种数据类型
```

协议越简单，网络解析和编码成本越低。

---

## 6. 避免复杂 SQL 解析

Redis 是 Key-Value 数据库。

操作方式通常是：

```bash
GET key
SET key value
HGET user:1 name
ZADD rank 100 user1
```

相比关系型数据库：

```text
不需要复杂 SQL 解析
不需要执行计划优化
不需要多表 Join
不需要复杂事务隔离
```

所以单次命令执行路径更短。

---

## 7. 命令执行时间短

Redis 大多数命令时间复杂度较低。

例如：

```text
GET / SET：O(1)
HGET：O(1)
LPUSH：O(1)
ZADD：O(logN)
```

只要避免大 Key 和慢命令，Redis 单次操作通常非常快。

---

## 8. 支持 Pipeline

Pipeline 可以把多个命令一次性发送给 Redis，减少网络往返次数。

普通方式：

```text
发送命令 1 -> 等响应
发送命令 2 -> 等响应
发送命令 3 -> 等响应
```

Pipeline：

```text
一次发送多个命令
  ↓
一次读取多个响应
```

优势：

```text
减少 RTT
提升批量操作性能
```

---

## 9. 为什么单线程还能高并发

因为 Redis 的瓶颈通常不是 CPU，而是：

```text
网络 IO
内存访问
客户端连接管理
```

Redis 使用：

```text
内存操作 + IO 多路复用 + 单线程无锁模型
```

所以即使单线程也能支撑很高 QPS。

---

## 10. Redis 快的原因总结

| 原因 | 说明 |
|---|---|
| 内存存储 | 避免磁盘 IO |
| 单线程执行命令 | 避免锁竞争和线程切换 |
| IO 多路复用 | 一个线程处理大量连接 |
| 高效数据结构 | 针对不同场景优化 |
| RESP 协议简单 | 编解码成本低 |
| Key-Value 模型 | 无复杂 SQL 和 Join |
| 命令复杂度低 | 大多数操作是 O(1) 或 O(logN) |
| Pipeline | 减少网络往返 |

---

## 11. 注意点

Redis 快不代表所有操作都快。

以下操作可能导致 Redis 变慢：

```text
大 Key
慢命令
KEYS *
HGETALL 大 Hash
SMEMBERS 大 Set
LRANGE 0 -1
复杂 Lua 脚本
网络带宽打满
持久化 fork 开销
主从复制压力
```

所以 Redis 使用时要避免大 Key 和阻塞命令。

---

## 12. 总结

Redis 快主要有几个原因：第一，Redis 基于内存操作，避免了磁盘 IO；第二，Redis 命令执行采用单线程模型，避免了多线程锁竞争和线程上下文切换；第三，Redis 使用 IO 多路复用，可以用少量线程处理大量客户端连接；第四，Redis 内部使用了 SDS、quicklist、skiplist、hashtable 等高效数据结构；第五，Redis 协议简单，命令执行路径短，没有复杂 SQL 解析和多表 Join。

一句话总结：

```text
Redis 快 = 内存操作 + 单线程无锁 + IO 多路复用 + 高效数据结构 + 简单协议。
```
