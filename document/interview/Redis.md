# Redis 常见面试题总结

## 目录

- [一、Redis 常用数据结构有哪些](#一redis-常用数据结构有哪些)
- [二、String、Hash、List、Set、ZSet 使用场景](#二stringhashlistsetzset-使用场景)
- [三、Redis 为什么快](#三redis-为什么快)
- [四、Redis 单线程为什么还能高性能](#四redis-单线程为什么还能高性能)
- [五、Redis 持久化机制 RDB 和 AOF 区别](#五redis-持久化机制-rdb-和-aof-区别)
- [六、Redis 过期删除策略](#六redis-过期删除策略)
- [七、Redis 内存淘汰策略](#七redis-内存淘汰策略)
- [八、缓存穿透、缓存击穿、缓存雪崩是什么](#八缓存穿透缓存击穿缓存雪崩是什么)
- [九、如何实现分布式锁](#九如何实现分布式锁)
- [十、Redisson 分布式锁原理](#十redisson-分布式锁原理)
- [十一、Redis 主从、哨兵、集群区别](#十一redis-主从哨兵集群区别)
- [十二、面试速记版](#十二面试速记版)
- [十三、总览表](#十三总览表)
- [十四、完整面试回答模板](#十四完整面试回答模板)

---

# 一、Redis 常用数据结构有哪些

## 1. Redis 是什么？

Redis 是一个基于内存的高性能 Key-Value 数据库。

它不仅支持简单的字符串，还支持多种数据结构，因此也常被称为：

```text
内存数据库
缓存数据库
数据结构服务器
```

Redis 常用于：

```text
缓存
分布式锁
计数器
排行榜
消息队列
会话存储
限流
点赞收藏
延迟任务
实时统计
```

---

## 2. Redis 常用数据结构

Redis 常用数据结构包括：

```text
1. String：字符串
2. Hash：哈希
3. List：列表
4. Set：集合
5. ZSet / Sorted Set：有序集合
6. Bitmap：位图
7. HyperLogLog：基数统计
8. Geospatial：地理位置
9. Stream：消息流
```

---

## 3. String

String 是 Redis 最基础的数据结构。

它可以存储：

```text
字符串
数字
JSON 字符串
序列化对象
二进制数据
```

常用命令：

```bash
SET key value
GET key
INCR key
DECR key
MGET key1 key2
SETEX key seconds value
```

示例：

```bash
SET user:name "Tom"
GET user:name
```

---

## 4. Hash

Hash 是一个 field-value 结构，类似 Java 中的 `HashMap`。

适合存储对象。

常用命令：

```bash
HSET key field value
HGET key field
HMGET key field1 field2
HGETALL key
HDEL key field
HINCRBY key field increment
```

示例：

```bash
HSET user:1 name Tom age 18 city Beijing
HGET user:1 name
```

---

## 5. List

List 是有序列表，底层可以理解为双端队列。

支持从两端插入和弹出元素。

常用命令：

```bash
LPUSH key value
RPUSH key value
LPOP key
RPOP key
LRANGE key start stop
BLPOP key timeout
BRPOP key timeout
```

示例：

```bash
LPUSH queue:order order1
RPUSH queue:order order2
LPOP queue:order
```

---

## 6. Set

Set 是无序集合，元素不能重复。

适合去重、交集、并集、差集等场景。

常用命令：

```bash
SADD key member
SREM key member
SISMEMBER key member
SMEMBERS key
SINTER key1 key2
SUNION key1 key2
SDIFF key1 key2
```

示例：

```bash
SADD user:1:tags java redis mysql
SISMEMBER user:1:tags redis
```

---

## 7. ZSet / Sorted Set

ZSet 是有序集合。

它和 Set 类似，元素不重复，但每个元素都会关联一个 score。

Redis 会按照 score 排序。

常用命令：

```bash
ZADD key score member
ZRANGE key start stop
ZREVRANGE key start stop
ZRANGEBYSCORE key min max
ZREM key member
ZINCRBY key increment member
ZRANK key member
ZREVRANK key member
```

示例：

```bash
ZADD rank:game 100 Tom
ZADD rank:game 200 Jerry
ZREVRANGE rank:game 0 9 WITHSCORES
```

---

## 8. Bitmap

Bitmap 本质上是基于 String 的位操作。

适合存储布尔状态。

常用命令：

```bash
SETBIT key offset value
GETBIT key offset
BITCOUNT key
BITOP operation destkey key1 key2
```

常见场景：

```text
用户签到
用户是否在线
活跃用户统计
布尔状态记录
```

---

## 9. HyperLogLog

HyperLogLog 用于基数统计。

基数统计就是统计不重复元素数量。

常用命令：

```bash
PFADD key element
PFCOUNT key
PFMERGE destkey sourcekey
```

适合场景：

```text
UV 统计
独立访客统计
大规模去重计数
```

特点：

```text
占用内存极小；
结果是近似值；
适合允许少量误差的统计场景。
```

---

## 10. Geospatial

Geospatial 用于地理位置相关计算。

常用命令：

```bash
GEOADD key longitude latitude member
GEODIST key member1 member2
GEOSEARCH key FROMLONLAT longitude latitude BYRADIUS radius unit
```

常见场景：

```text
附近的人
附近门店
外卖配送距离
车辆位置查询
```

---

## 11. Stream

Stream 是 Redis 5.0 引入的消息流数据结构。

适合消息队列、事件流场景。

常用命令：

```bash
XADD key * field value
XREAD COUNT count STREAMS key id
XGROUP CREATE key group id
XREADGROUP GROUP group consumer STREAMS key id
XACK key group id
```

常见场景：

```text
消息队列
事件流
日志流
异步任务
消费组处理
```

---

## 12. 面试回答

Redis 常用数据结构有 String、Hash、List、Set、ZSet，此外还有 Bitmap、HyperLogLog、Geospatial 和 Stream。

String 适合缓存简单值、计数器和分布式锁；Hash 适合存储对象；List 适合队列和列表；Set 适合去重、共同好友、标签等场景；ZSet 适合排行榜、延迟队列和按分数排序的场景。

---

# 二、String、Hash、List、Set、ZSet 使用场景

## 1. String 使用场景

String 是最常用的数据结构。

适合场景：

```text
1. 缓存简单字符串
2. 缓存 JSON 对象
3. 计数器
4. 分布式锁
5. 验证码
6. Token
7. Session
8. 限流计数
```

---

### 1.1 缓存对象 JSON

```bash
SET user:1 '{"id":1,"name":"Tom","age":18}'
GET user:1
```

适合：

```text
对象整体读取多；
对象字段不频繁单独修改。
```

---

### 1.2 计数器

```bash
INCR article:1:view
INCRBY article:1:view 10
```

适合：

```text
文章浏览量
点赞数
下载次数
接口调用次数
```

---

### 1.3 分布式锁

```bash
SET lock:order:1 requestId NX EX 30
```

含义：

```text
只有 key 不存在时才设置成功；
设置过期时间 30 秒；
避免死锁。
```

---

## 2. Hash 使用场景

Hash 适合存储结构化对象。

例如用户信息：

```bash
HSET user:1 name Tom age 18 city Beijing
HGET user:1 name
```

适合场景：

```text
用户信息
商品信息
购物车
对象字段缓存
配置信息
```

---

### 2.1 用户信息

```bash
HSET user:1001 id 1001 name Tom age 18
HGET user:1001 name
```

优点：

```text
可以单独读取某个字段；
可以单独修改某个字段；
比频繁反序列化 JSON 更方便。
```

---

### 2.2 购物车

```bash
HSET cart:user:1 product:1001 2
HSET cart:user:1 product:1002 1
HINCRBY cart:user:1 product:1001 1
```

其中：

```text
key：cart:user:1
field：productId
value：商品数量
```

---

## 3. List 使用场景

List 是有序列表，适合队列和列表场景。

适合场景：

```text
1. 消息队列
2. 任务队列
3. 最新消息列表
4. 评论列表
5. 时间线列表
```

---

### 3.1 队列

先进先出：

```bash
LPUSH queue:order order1
RPOP queue:order
```

或者：

```bash
RPUSH queue:order order1
LPOP queue:order
```

---

### 3.2 阻塞队列

```bash
BRPOP queue:order 10
```

如果队列为空，会阻塞等待最多 10 秒。

---

### 3.3 最新列表

```bash
LPUSH news:list news1
LPUSH news:list news2
LRANGE news:list 0 9
```

适合展示最新 10 条数据。

---

## 4. Set 使用场景

Set 是无序不重复集合。

适合场景：

```text
1. 去重
2. 标签
3. 点赞用户集合
4. 收藏用户集合
5. 共同好友
6. 抽奖
7. 黑名单白名单
```

---

### 4.1 点赞去重

```bash
SADD article:1:like:user 1001
SADD article:1:like:user 1002
SISMEMBER article:1:like:user 1001
SCARD article:1:like:user
```

特点：

```text
同一个用户重复点赞不会重复计数。
```

---

### 4.2 共同好友

```bash
SINTER user:1:friends user:2:friends
```

---

### 4.3 标签系统

```bash
SADD user:1:tags java redis mysql
SADD user:2:tags java spring cloud
SINTER user:1:tags user:2:tags
```

---

## 5. ZSet 使用场景

ZSet 是带分数的有序集合。

适合场景：

```text
1. 排行榜
2. 延迟队列
3. 热搜榜
4. 权重排序
5. 按时间排序
6. 滑动窗口限流
```

---

### 5.1 排行榜

```bash
ZADD rank:game 100 Tom
ZADD rank:game 200 Jerry
ZINCRBY rank:game 50 Tom
ZREVRANGE rank:game 0 9 WITHSCORES
```

---

### 5.2 延迟队列

将执行时间作为 score：

```bash
ZADD delay:queue 1714380000000 order:1
```

消费者查询当前时间之前的任务：

```bash
ZRANGEBYSCORE delay:queue 0 当前时间戳 LIMIT 0 10
```

取出后删除：

```bash
ZREM delay:queue order:1
```

---

### 5.3 滑动窗口限流

以时间戳作为 score：

```bash
ZADD limit:user:1 当前时间戳 requestId
ZREMRANGEBYSCORE limit:user:1 0 当前时间戳-窗口大小
ZCARD limit:user:1
```

通过统计窗口内请求数量判断是否限流。

---

## 6. 数据结构使用场景总览

| 数据结构 | 特点 | 典型场景 |
|---|---|---|
| String | 简单 KV，可自增 | 缓存、计数器、锁、Token |
| Hash | field-value | 用户对象、购物车、配置 |
| List | 有序，可两端操作 | 队列、消息列表、时间线 |
| Set | 无序去重 | 标签、点赞、共同好友 |
| ZSet | 去重且有序 | 排行榜、延迟队列、限流 |
| Bitmap | 位操作 | 签到、在线状态 |
| HyperLogLog | 近似去重计数 | UV 统计 |
| GEO | 地理位置 | 附近的人、门店 |
| Stream | 消息流 | 消息队列、事件流 |

---

## 7. 面试回答

String 适合缓存简单值、JSON、计数器、验证码和分布式锁。Hash 适合存储对象，例如用户信息、商品信息和购物车。List 适合队列、消息列表和时间线。Set 适合去重、点赞、标签、共同好友等场景。ZSet 适合排行榜、延迟队列、热搜榜和滑动窗口限流。

选择 Redis 数据结构时，核心是根据业务访问模式选择：是否需要排序、是否需要去重、是否需要按字段访问、是否需要计数、是否需要范围查询。

---

# 三、Redis 为什么快

## 1. Redis 快的核心原因

Redis 快主要有以下原因：

```text
1. 基于内存操作
2. 数据结构简单高效
3. 单线程执行命令，避免锁竞争
4. I/O 多路复用
5. 网络模型高效
6. C 语言实现
7. 命令执行路径短
8. 支持批量操作和 Pipeline
```

---

## 2. 基于内存操作

Redis 的数据主要存储在内存中。

内存访问速度远高于磁盘访问速度。

因此 Redis 查询不需要像传统数据库一样频繁访问磁盘。

---

## 3. 数据结构高效

Redis 为不同数据类型设计了高效的底层编码。

例如：

```text
String：SDS
Hash：listpack / hashtable
List：quicklist
Set：intset / hashtable
ZSet：listpack / skiplist + hashtable
```

这些结构针对内存使用和访问效率做了优化。

---

## 4. 单线程避免锁竞争

Redis 命令执行主要由单线程完成。

好处：

```text
没有多线程上下文切换成本；
没有复杂锁竞争；
命令天然具备原子性；
代码实现更简单。
```

---

## 5. I/O 多路复用

Redis 使用 I/O 多路复用处理大量客户端连接。

一个线程可以同时监听多个 socket 事件。

当某个连接可读或可写时，再处理对应事件。

常见底层机制：

```text
epoll
kqueue
select
```

---

## 6. 网络模型高效

Redis 采用事件驱动模型。

大致流程：

```text
监听 socket 事件
读取客户端请求
解析命令
执行命令
写回响应
```

通过事件循环可以高效处理大量连接。

---

## 7. Pipeline 减少网络往返

普通请求：

```text
客户端发送命令 -> Redis 返回结果
客户端再发送下一条命令 -> Redis 再返回结果
```

Pipeline 可以一次性发送多条命令：

```text
客户端批量发送命令 -> Redis 批量返回结果
```

这样可以减少网络 RTT，提高吞吐量。

---

## 8. Redis 快不代表所有命令都快

Redis 单条简单命令通常很快。

但一些复杂命令可能阻塞 Redis 主线程。

例如：

```bash
KEYS *
SMEMBERS 大集合
LRANGE 大列表 0 -1
HGETALL 大 Hash
大 key 删除
复杂 Lua 脚本
```

这些操作可能导致 Redis 卡顿。

---

## 9. 面试回答

Redis 快主要是因为它基于内存操作，避免了频繁磁盘 IO；同时 Redis 使用高效的数据结构，命令执行路径短。Redis 命令执行主要是单线程的，避免了多线程锁竞争和上下文切换。再加上 I/O 多路复用和事件驱动网络模型，使得 Redis 可以用较少线程处理大量连接。

不过 Redis 快并不代表所有命令都可以随便用，大 key、复杂命令、`KEYS *`、大范围查询和耗时 Lua 脚本都可能阻塞主线程。

---

# 四、Redis 单线程为什么还能高性能

## 1. Redis 真的是单线程吗？

Redis 常说的单线程，主要指：

```text
Redis 命令执行主要由单个主线程完成。
```

但 Redis 并不是所有事情都只有一个线程。

例如：

```text
持久化
异步删除
AOF 重写
部分网络 I/O
后台任务
```

这些可能由后台线程或子进程处理。

Redis 6 以后也支持 I/O 多线程，但命令执行仍然保持主线程模型。

---

## 2. 单线程为什么还能快？

核心原因：

```text
Redis 的瓶颈通常不在 CPU，而在内存、网络和数据结构操作。
```

Redis 单条命令通常非常短小，内存操作速度很快。

单线程反而避免了：

```text
锁竞争
线程切换
并发安全复杂度
```

---

## 3. I/O 多路复用

虽然命令执行是单线程，但 Redis 可以通过 I/O 多路复用同时管理大量客户端连接。

模型类似：

```text
一个线程监听多个 socket；
哪个 socket 准备好了，就处理哪个 socket；
不会为每个连接创建一个线程。
```

---

## 4. 单线程的优势

单线程模型优势：

```text
1. 避免锁竞争
2. 避免线程上下文切换
3. 保证命令原子性
4. 代码简单稳定
5. 事件模型高效
```

---

## 5. 单线程的劣势

单线程也有明显缺点：

```text
1. 单个耗时命令会阻塞所有请求
2. 无法充分利用多核 CPU 执行命令
3. 大 key 操作风险高
4. 慢 Lua 脚本会阻塞
5. 网络或内存带宽可能成为瓶颈
```

---

## 6. Redis 6 I/O 多线程

Redis 6 引入 I/O 多线程，主要用于：

```text
网络数据读取
协议解析
响应写回
```

但核心命令执行仍然是单线程。

这样做的目的：

```text
提高网络 I/O 处理能力；
保持命令执行的简单性和原子性。
```

---

## 7. 面试回答

Redis 所说的单线程主要是指命令执行由单个主线程完成。它仍然能高性能，是因为 Redis 数据主要在内存中，单条命令执行很快；同时 Redis 使用 I/O 多路复用和事件驱动模型，一个线程就可以处理大量连接。

单线程还避免了多线程锁竞争和上下文切换，并保证命令执行的原子性。但缺点是如果执行大 key 操作、复杂命令或耗时 Lua 脚本，会阻塞主线程，影响所有请求。

---

# 五、Redis 持久化机制 RDB 和 AOF 区别

## 1. Redis 为什么需要持久化？

Redis 数据主要存储在内存中。

如果没有持久化，Redis 进程重启或机器宕机后，内存数据会丢失。

持久化的作用是：

```text
将内存数据保存到磁盘；
Redis 重启后可以恢复数据。
```

---

## 2. Redis 持久化方式

Redis 主要有两种持久化机制：

```text
1. RDB
2. AOF
```

也可以：

```text
RDB + AOF 混合使用
```

---

## 3. RDB 是什么？

RDB 全称：

```text
Redis Database
```

它是快照持久化。

含义：

> Redis 在某个时间点，把内存中的数据生成一份快照文件保存到磁盘。

RDB 文件通常是：

```text
dump.rdb
```

---

## 4. RDB 触发方式

RDB 常见触发方式：

```text
1. save 配置规则自动触发
2. 手动执行 SAVE
3. 手动执行 BGSAVE
4. 主从复制时生成 RDB
5. Redis 正常关闭时生成 RDB
```

---

## 5. SAVE 和 BGSAVE 区别

### SAVE

```bash
SAVE
```

特点：

```text
同步执行；
阻塞 Redis 主线程；
不推荐生产环境手动使用。
```

---

### BGSAVE

```bash
BGSAVE
```

特点：

```text
fork 子进程生成 RDB；
主线程继续处理请求；
生产中更常用。
```

---

## 6. RDB 优点

RDB 优点：

```text
1. 文件紧凑，体积较小
2. 适合备份和全量复制
3. 恢复速度较快
4. 对主线程影响相对小，BGSAVE 由子进程完成
```

---

## 7. RDB 缺点

RDB 缺点：

```text
1. 可能丢失最近一次快照后的数据
2. fork 子进程时可能有性能开销
3. 数据量大时生成快照较重
```

例如每 5 分钟生成一次 RDB，宕机时可能丢失最近几分钟数据。

---

## 8. AOF 是什么？

AOF 全称：

```text
Append Only File
```

它是追加日志持久化。

含义：

> Redis 将每一条写命令追加到 AOF 文件中，重启时重新执行这些命令恢复数据。

---

## 9. AOF 写入流程

AOF 大致流程：

```text
1. 客户端执行写命令
2. Redis 执行命令修改内存
3. 将写命令追加到 AOF 缓冲区
4. 根据 appendfsync 策略刷盘
5. Redis 重启时加载 AOF 文件恢复数据
```

---

## 10. AOF 刷盘策略

AOF 常见刷盘策略：

| 策略 | 说明 | 数据安全性 | 性能 |
|---|---|---|---|
| always | 每条写命令都刷盘 | 最高 | 最低 |
| everysec | 每秒刷盘一次 | 较高 | 较好 |
| no | 由操作系统决定刷盘 | 较低 | 较高 |

常用配置：

```text
appendfsync everysec
```

这是性能和安全性的折中方案。

---

## 11. AOF 优点

AOF 优点：

```text
1. 数据安全性比 RDB 更高
2. 最多通常丢失 1 秒左右数据
3. 日志追加写，顺序 IO
4. 文件可读性相对较好
```

---

## 12. AOF 缺点

AOF 缺点：

```text
1. 文件通常比 RDB 大
2. 恢复速度可能比 RDB 慢
3. AOF rewrite 有额外开销
4. 持续写日志会带来 IO 压力
```

---

## 13. AOF 重写

随着写命令越来越多，AOF 文件会越来越大。

AOF 重写用于压缩 AOF 文件。

例如原来有：

```bash
INCR count
INCR count
INCR count
```

重写后可以变成：

```bash
SET count 3
```

重写后的 AOF 文件更小，恢复更快。

---

## 14. RDB 和 AOF 对比

| 对比项 | RDB | AOF |
|---|---|---|
| 持久化方式 | 快照 | 追加写命令 |
| 文件内容 | 某个时间点的数据快照 | 写操作命令日志 |
| 数据安全性 | 较低，可能丢最近快照后的数据 | 较高，everysec 通常最多丢约 1 秒 |
| 文件大小 | 较小 | 较大 |
| 恢复速度 | 较快 | 较慢 |
| 性能影响 | fork 子进程有开销 | 持续追加和刷盘有开销 |
| 适合场景 | 备份、灾备、全量复制 | 更高数据安全要求 |

---

## 15. 混合持久化

Redis 支持 RDB 和 AOF 混合使用。

思路：

```text
AOF 文件前半部分使用 RDB 格式保存全量数据；
后半部分追加增量写命令。
```

优点：

```text
兼顾 RDB 恢复快和 AOF 数据更安全。
```

---

## 16. 面试回答

Redis 持久化主要有 RDB 和 AOF。

RDB 是快照持久化，会在某个时间点将内存数据生成快照文件保存到磁盘，优点是文件小、恢复快，适合备份和全量复制，缺点是可能丢失最近一次快照后的数据。

AOF 是追加日志持久化，会把每条写命令追加到 AOF 文件中，重启时重新执行命令恢复数据。AOF 数据安全性更高，常用 `everysec` 策略，通常最多丢失 1 秒左右数据，但文件更大，恢复可能更慢。

生产中可以根据业务要求选择 RDB、AOF 或混合持久化。

---

# 六、Redis 过期删除策略

## 1. 什么是过期 key？

Redis 可以给 key 设置过期时间。

例如：

```bash
SET token:1 abc EX 3600
```

表示：

```text
token:1 在 3600 秒后过期。
```

也可以使用：

```bash
EXPIRE token:1 3600
TTL token:1
```

---

## 2. Redis 过期删除策略

Redis 删除过期 key 主要有两种策略：

```text
1. 惰性删除 / 被动删除
2. 定期删除 / 主动删除
```

---

## 3. 惰性删除

惰性删除是指：

> 当客户端访问某个 key 时，Redis 检查这个 key 是否已经过期，如果过期就删除并返回空。

示例：

```bash
GET token:1
```

如果 `token:1` 已经过期，Redis 会删除它，并返回：

```text
nil
```

---

## 4. 惰性删除优点

优点：

```text
节省 CPU；
只有访问时才检查。
```

---

## 5. 惰性删除缺点

缺点：

```text
如果一个过期 key 一直不被访问，它可能长时间占用内存。
```

---

## 6. 定期删除

定期删除是指：

> Redis 周期性地从设置了过期时间的 key 中抽样检查，删除已经过期的 key。

它不是一次性扫描所有 key。

原因是：

```text
全量扫描所有过期 key 会阻塞 Redis。
```

---

## 7. 为什么不直接定时删除所有过期 key？

如果 Redis 给每个 key 都设置一个定时器，到期立刻删除，会带来大量定时器管理成本。

如果定期全量扫描，又会造成 CPU 开销过大，甚至阻塞 Redis。

所以 Redis 采用：

```text
惰性删除 + 定期抽样删除
```

在 CPU 和内存之间做平衡。

---

## 8. 过期 key 一定会立刻删除吗？

不会。

key 到期后，并不代表立即从内存中删除。

它可能在以下情况下被删除：

```text
1. 被访问时发现过期，被惰性删除
2. 被定期抽样检查到，被主动删除
3. 内存不足触发内存淘汰
```

---

## 9. 过期删除和内存淘汰的区别

| 对比项 | 过期删除 | 内存淘汰 |
|---|---|---|
| 触发条件 | key 到达过期时间 | Redis 内存达到 maxmemory |
| 作用对象 | 设置了 TTL 的 key | 根据淘汰策略决定 |
| 目的 | 删除过期数据 | 释放内存 |
| 是否一定配置 TTL | 是 | 不一定 |

---

## 10. 面试回答

Redis 删除过期 key 主要使用惰性删除和定期删除。

惰性删除是客户端访问 key 时，Redis 检查它是否过期，如果过期就删除。定期删除是 Redis 周期性地从设置了过期时间的 key 中随机抽样，删除已经过期的 key。

Redis 不会给每个 key 都设置一个定时器，也不会频繁全量扫描所有 key，因为这样会带来很高的 CPU 开销。惰性删除和定期删除结合，可以在 CPU 和内存之间取得平衡。

---

# 七、Redis 内存淘汰策略

## 1. 为什么需要内存淘汰？

Redis 数据主要存储在内存中。

当 Redis 使用内存达到 `maxmemory` 限制时，如果继续写入数据，就需要根据配置的策略淘汰一些 key。

配置：

```conf
maxmemory 4gb
maxmemory-policy allkeys-lru
```

---

## 2. 内存淘汰和过期删除区别

过期删除处理的是：

```text
已经到期的 key
```

内存淘汰处理的是：

```text
内存不足时，为了腾出空间而删除 key
```

即使 key 没有过期，也可能被淘汰。

---

## 3. 常见内存淘汰策略

Redis 常见内存淘汰策略包括：

```text
1. noeviction
2. allkeys-lru
3. volatile-lru
4. allkeys-lfu
5. volatile-lfu
6. allkeys-random
7. volatile-random
8. volatile-ttl
```

---

## 4. noeviction

含义：

```text
不淘汰任何 key。
```

当内存达到上限后，写命令会返回错误。

适合场景：

```text
不能接受缓存被自动删除；
希望应用显式感知写入失败。
```

---

## 5. allkeys-lru

含义：

```text
从所有 key 中淘汰最近最少使用的 key。
```

适合场景：

```text
典型缓存场景；
所有 key 都可以被淘汰。
```

这是缓存场景中非常常用的策略。

---

## 6. volatile-lru

含义：

```text
只从设置了过期时间的 key 中淘汰最近最少使用的 key。
```

适合场景：

```text
只允许淘汰带 TTL 的缓存数据；
不希望永久 key 被淘汰。
```

---

## 7. allkeys-lfu

含义：

```text
从所有 key 中淘汰最近最不常使用的 key。
```

LRU 关注最近是否使用过。

LFU 关注使用频率。

适合场景：

```text
访问频率差异明显；
希望保留高频热点数据。
```

---

## 8. volatile-lfu

含义：

```text
只从设置了过期时间的 key 中淘汰最近最不常使用的 key。
```

---

## 9. allkeys-random

含义：

```text
从所有 key 中随机淘汰。
```

适合场景：

```text
访问模式比较均匀；
对淘汰准确性要求不高。
```

---

## 10. volatile-random

含义：

```text
只从设置了过期时间的 key 中随机淘汰。
```

---

## 11. volatile-ttl

含义：

```text
从设置了过期时间的 key 中，优先淘汰 TTL 最短的 key。
```

适合场景：

```text
希望快过期的数据优先被删除。
```

---

## 12. 淘汰策略怎么选？

| 场景 | 推荐策略 |
|---|---|
| 纯缓存，所有 key 都可淘汰 | `allkeys-lru` 或 `allkeys-lfu` |
| 热点访问明显 | `allkeys-lfu` |
| 只淘汰设置过期时间的 key | `volatile-lru` 或 `volatile-lfu` |
| 不允许自动淘汰 | `noeviction` |
| 访问较均匀，对准确性要求低 | `allkeys-random` |
| 希望快过期 key 先淘汰 | `volatile-ttl` |

---

## 13. LRU 和 LFU 区别

| 对比项 | LRU | LFU |
|---|---|---|
| 全称 | Least Recently Used | Least Frequently Used |
| 关注点 | 最近是否使用 | 使用频率 |
| 淘汰对象 | 最近最少使用 | 最近最不常使用 |
| 适合场景 | 最近访问代表热点 | 长期频率代表热点 |

---

## 14. 面试回答

Redis 内存达到 `maxmemory` 后，会根据 `maxmemory-policy` 选择淘汰策略。

常见策略有 `noeviction`、`allkeys-lru`、`volatile-lru`、`allkeys-lfu`、`volatile-lfu`、`allkeys-random`、`volatile-random` 和 `volatile-ttl`。

如果 Redis 主要作为缓存，通常使用 `allkeys-lru` 或 `allkeys-lfu`。如果只希望淘汰设置了过期时间的 key，可以选择 `volatile-lru` 或 `volatile-lfu`。如果不希望 Redis 自动淘汰数据，可以选择 `noeviction`。

---

# 八、缓存穿透、缓存击穿、缓存雪崩是什么

## 1. 三者概览

缓存系统常见三大问题：

```text
1. 缓存穿透
2. 缓存击穿
3. 缓存雪崩
```

它们都会导致大量请求打到数据库。

---

## 2. 缓存穿透是什么？

缓存穿透是指：

> 请求查询一个缓存和数据库中都不存在的数据，导致每次请求都绕过缓存，直接打到数据库。

例如：

```text
查询 id = -1 的用户；
缓存没有；
数据库也没有；
每次请求都查数据库。
```

如果攻击者大量请求不存在的数据，就会造成数据库压力暴增。

---

## 3. 缓存穿透解决方案

常见解决方案：

```text
1. 缓存空值
2. 布隆过滤器
3. 参数校验
4. 接口限流
5. 黑名单
```

---

### 3.1 缓存空值

如果数据库查不到，也写入缓存：

```text
key: user:-1
value: null
TTL: 60s
```

下次再查同一个不存在的 key，直接返回空值。

注意：

```text
空值缓存 TTL 不宜太长；
避免缓存大量无效 key。
```

---

### 3.2 布隆过滤器

布隆过滤器用于判断某个 key 是否可能存在。

流程：

```text
1. 请求先经过布隆过滤器
2. 如果判断一定不存在，直接返回
3. 如果判断可能存在，再查缓存和数据库
```

特点：

```text
可以判断一定不存在；
不能保证一定存在；
存在一定误判率。
```

---

### 3.3 参数校验

例如用户 ID 必须大于 0：

```text
id <= 0 直接拒绝
```

可以拦截明显非法请求。

---

## 4. 缓存击穿是什么？

缓存击穿是指：

> 某个热点 key 过期瞬间，大量并发请求同时访问这个 key，导致请求全部打到数据库。

例如：

```text
热点商品详情 key 过期；
上万请求同时进来；
缓存都没命中；
数据库瞬间被打爆。
```

---

## 5. 缓存击穿解决方案

常见解决方案：

```text
1. 互斥锁
2. 逻辑过期
3. 热点 key 永不过期
4. 后台异步刷新
5. 请求合并
```

---

### 5.1 互斥锁

当热点 key 失效时，只允许一个线程查数据库并重建缓存。

其他线程等待或快速失败。

伪代码：

```java
Object value = redis.get(key);

if (value == null) {
    boolean locked = tryLock(lockKey);

    if (locked) {
        try {
            value = queryDb();
            redis.set(key, value, ttl);
        } finally {
            unlock(lockKey);
        }
    } else {
        Thread.sleep(50);
        return retry();
    }
}
```

---

### 5.2 逻辑过期

缓存值中保存过期时间。

例如：

```json
{
  "data": {
    "id": 1,
    "name": "商品"
  },
  "expireTime": "2026-04-29 12:00:00"
}
```

流程：

```text
1. 查询缓存
2. 如果未逻辑过期，直接返回
3. 如果逻辑过期，先返回旧数据
4. 后台异步刷新缓存
```

优点：

```text
用户请求不会阻塞；
可以保护数据库。
```

缺点：

```text
短时间可能读到旧数据。
```

---

## 6. 缓存雪崩是什么？

缓存雪崩是指：

> 大量缓存 key 在同一时间过期，或者 Redis 整体不可用，导致大量请求同时打到数据库。

常见原因：

```text
大量 key 设置了相同过期时间；
Redis 宕机；
Redis 网络故障；
缓存集群大面积不可用。
```

---

## 7. 缓存雪崩解决方案

常见解决方案：

```text
1. 过期时间加随机值
2. 热点数据预热
3. 多级缓存
4. Redis 高可用
5. 限流降级
6. 熔断保护
7. 本地缓存兜底
```

---

### 7.1 过期时间加随机值

错误做法：

```text
所有 key TTL 都是 3600 秒
```

优化：

```text
TTL = 3600 + random(0, 600)
```

这样可以避免大量 key 同一时间失效。

---

### 7.2 Redis 高可用

使用：

```text
主从复制
哨兵
Redis Cluster
云 Redis 高可用版
```

避免单点故障。

---

### 7.3 限流降级

当缓存不可用时，可以：

```text
限制访问数据库的请求量；
返回默认值；
返回兜底数据；
关闭非核心功能。
```

---

## 8. 三者区别

| 问题 | 核心原因 | 典型场景 | 解决方案 |
|---|---|---|---|
| 缓存穿透 | 查不存在的数据 | 恶意请求不存在 ID | 空值缓存、布隆过滤器 |
| 缓存击穿 | 热点 key 过期 | 热点商品缓存失效 | 互斥锁、逻辑过期 |
| 缓存雪崩 | 大量 key 同时失效或 Redis 故障 | 批量缓存同 TTL | 随机 TTL、高可用、限流 |

---

## 9. 面试回答

缓存穿透是查询缓存和数据库都不存在的数据，导致请求每次都打到数据库。常见解决方案是缓存空值、布隆过滤器和参数校验。

缓存击穿是热点 key 在过期瞬间被大量并发访问，导致请求同时打到数据库。常见解决方案是互斥锁、逻辑过期、热点 key 永不过期和后台异步刷新。

缓存雪崩是大量 key 同时过期，或者 Redis 整体不可用，导致大量请求打到数据库。常见解决方案是 TTL 加随机值、缓存预热、多级缓存、Redis 高可用、限流和降级。

---

# 九、如何实现分布式锁

## 1. 分布式锁是什么？

分布式锁用于在分布式系统中控制多个进程或服务实例对共享资源的互斥访问。

例如：

```text
防止重复下单
防止库存超卖
防止定时任务重复执行
防止重复处理 MQ 消息
防止并发修改同一资源
```

---

## 2. Redis 实现分布式锁核心命令

Redis 实现分布式锁常用命令：

```bash
SET lock:key uniqueValue NX EX 30
```

含义：

| 参数 | 说明 |
|---|---|
| `lock:key` | 锁 key |
| `uniqueValue` | 当前线程或请求的唯一标识 |
| `NX` | key 不存在时才设置成功 |
| `EX 30` | 设置 30 秒过期时间 |

---

## 3. 为什么要使用 NX？

`NX` 保证：

```text
只有第一个线程能加锁成功；
后续线程加锁失败。
```

这就是互斥的基础。

---

## 4. 为什么要设置过期时间？

如果不设置过期时间，持有锁的服务宕机后，锁永远不会释放。

设置过期时间可以避免死锁。

---

## 5. 为什么 value 要唯一？

value 必须是唯一标识，例如：

```text
UUID
requestId
线程 ID + UUID
```

用于释放锁时判断：

```text
只能释放自己加的锁，不能误删别人的锁。
```

---

## 6. 错误释放锁示例

线程 A 加锁成功，锁过期时间 10 秒。

线程 A 执行业务超过 10 秒，锁自动过期。

线程 B 加锁成功。

线程 A 执行完后直接删除锁：

```bash
DEL lock:key
```

此时会误删线程 B 的锁。

所以释放锁前必须判断 value。

---

## 7. 正确释放锁

释放锁逻辑：

```text
先判断 value 是否是自己的；
如果是，才删除；
否则不删除。
```

但判断和删除必须保证原子性。

---

## 8. Lua 脚本释放锁

使用 Lua 脚本保证判断和删除原子性：

```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
```

Java 中释放锁时执行这段 Lua 脚本。

---

## 9. 分布式锁基本流程

```text
1. 生成唯一 value
2. 执行 SET lockKey value NX EX expireTime
3. 如果返回成功，表示获取锁成功
4. 执行业务逻辑
5. 使用 Lua 脚本判断 value 并删除锁
6. 如果获取锁失败，可以等待、重试或快速失败
```

---

## 10. 简单 Java 伪代码

```java
String lockKey = "lock:order:" + orderId;
String requestId = UUID.randomUUID().toString();

Boolean success = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, requestId, 30, TimeUnit.SECONDS);

if (Boolean.TRUE.equals(success)) {
    try {
        // 执行业务逻辑
    } finally {
        // 执行 Lua 脚本：判断 value 是自己的才删除
        unlock(lockKey, requestId);
    }
} else {
    // 获取锁失败，重试或直接返回
}
```

---

## 11. 分布式锁需要满足什么条件？

一个可靠的分布式锁通常需要满足：

```text
1. 互斥性
2. 防死锁
3. 只能自己释放自己的锁
4. 加锁和设置过期时间原子性
5. 解锁判断和删除原子性
6. 支持锁续期
7. 支持可重入，视业务需要
8. 高可用，视业务要求
```

---

## 12. Redis 分布式锁的问题

自己手写 Redis 锁时常见问题：

```text
1. 忘记设置过期时间导致死锁
2. 加锁和设置过期时间不是原子操作
3. 业务没执行完锁提前过期
4. 释放锁误删别人的锁
5. 不支持可重入
6. 不支持自动续期
7. Redis 主从切换时可能出现锁丢失
```

---

## 13. Redis 主从切换下的问题

Redis 主从复制通常是异步的。

可能出现：

```text
1. 线程 A 在主节点加锁成功
2. 锁还没同步到从节点
3. 主节点宕机
4. 从节点升级为新主
5. 线程 B 在新主上又加锁成功
```

此时可能出现两个客户端都认为自己拿到了锁。

如果业务对锁安全性要求极高，需要考虑更强一致性的方案。

---

## 14. 面试回答

Redis 实现分布式锁通常使用 `SET key value NX EX seconds`。`NX` 保证只有 key 不存在时才能加锁成功，`EX` 设置过期时间避免死锁，value 使用唯一标识，避免释放别人的锁。

释放锁时不能直接 `DEL`，必须先判断 value 是否是自己的，如果是才删除，并且判断和删除要用 Lua 脚本保证原子性。

需要注意的是，手写 Redis 锁要考虑锁过期、业务执行超时、误删锁、不可重入、无法自动续期以及 Redis 主从切换导致锁丢失等问题。实际项目中通常使用 Redisson。

---

# 十、Redisson 分布式锁原理

## 1. Redisson 是什么？

Redisson 是一个 Redis Java 客户端。

它封装了很多分布式对象和工具，例如：

```text
分布式锁
可重入锁
公平锁
读写锁
信号量
闭锁
限流器
延迟队列
分布式集合
```

---

## 2. Redisson 分布式锁使用示例

```java
RLock lock = redissonClient.getLock("lock:order:1");

lock.lock();

try {
    // 执行业务逻辑
} finally {
    lock.unlock();
}
```

---

## 3. Redisson 锁的核心能力

Redisson 分布式锁支持：

```text
1. 互斥
2. 可重入
3. 自动续期
4. Lua 脚本保证原子性
5. 看门狗机制
6. 阻塞等待
7. 超时等待
8. 自动释放
```

---

## 4. Redisson 加锁原理

Redisson 加锁时，底层会执行 Lua 脚本。

核心逻辑大致是：

```text
1. 如果锁 key 不存在，创建锁
2. 设置持有锁的线程标识
3. 设置过期时间
4. 如果锁已经被当前线程持有，重入次数 +1
5. 如果锁被其他线程持有，加锁失败并返回剩余过期时间
```

---

## 5. Redisson 锁的数据结构

Redisson 可重入锁通常使用 Redis Hash 存储。

结构类似：

```text
key：lock:order:1
field：clientId:threadId
value：重入次数
```

示例：

```text
lock:order:1
    clientA:thread-1 -> 1
```

同一个线程再次加锁：

```text
lock:order:1
    clientA:thread-1 -> 2
```

---

## 6. 为什么用 Hash？

Hash 可以方便地记录：

```text
当前锁属于哪个客户端哪个线程；
当前线程重入了几次。
```

这样就能实现可重入锁。

---

## 7. Redisson 可重入原理

同一个线程多次调用：

```java
lock.lock();
lock.lock();
```

Redisson 不会阻塞自己，而是把重入次数加 1。

释放时：

```java
lock.unlock();
lock.unlock();
```

每释放一次，重入次数减 1。

只有重入次数减到 0，才真正删除锁。

---

## 8. 看门狗机制 WatchDog

如果调用：

```java
lock.lock();
```

没有指定锁过期时间，Redisson 会启动看门狗机制。

默认锁过期时间通常是：

```text
30 秒
```

只要持锁客户端还活着，看门狗会定期续期。

作用：

```text
避免业务还没执行完，锁就过期释放。
```

---

## 9. 看门狗续期流程

大致流程：

```text
1. 加锁成功，设置锁过期时间为 30 秒
2. Redisson 启动定时续期任务
3. 每隔一段时间检查锁是否仍由当前线程持有
4. 如果仍持有，则刷新过期时间为 30 秒
5. 如果业务执行完成，unlock 后取消续期
6. 如果客户端宕机，看门狗停止，锁最终过期释放
```

---

## 10. 指定 leaseTime 时是否续期？

如果调用：

```java
lock.lock(10, TimeUnit.SECONDS);
```

表示指定锁 10 秒后自动释放。

这种情况下通常不会启用看门狗自动续期。

适合：

```text
能明确估算业务执行时间的场景。
```

---

## 11. Redisson 解锁原理

解锁时 Redisson 也会执行 Lua 脚本。

核心逻辑：

```text
1. 判断锁是否由当前线程持有
2. 如果不是当前线程持有，抛出异常或返回失败
3. 如果是当前线程持有，重入次数 -1
4. 如果重入次数大于 0，刷新过期时间
5. 如果重入次数等于 0，删除锁
6. 发布解锁消息，通知等待线程
```

---

## 12. 发布订阅机制

当一个线程加锁失败时，Redisson 不会一直空转自旋。

它会订阅锁释放消息。

当持锁线程释放锁时，会发布消息通知等待线程。

等待线程收到通知后，再尝试获取锁。

这样可以减少无效轮询。

---

## 13. Redisson 锁原理图

```text
线程 A 加锁
    |
    v
Lua 脚本判断锁是否存在
    |
    ├── 不存在：创建 Hash，设置过期时间，加锁成功
    |
    ├── 当前线程已持有：重入次数 +1
    |
    └── 其他线程持有：返回剩余 TTL，加锁失败

加锁成功
    |
    v
启动 WatchDog 自动续期

线程 A 解锁
    |
    v
Lua 脚本判断是否自己持有
    |
    ├── 重入次数 > 1：次数 -1
    |
    └── 重入次数 = 1：删除锁，发布解锁消息
```

---

## 14. Redisson 分布式锁注意事项

使用 Redisson 锁时也要注意：

```text
1. 锁粒度不能过大
2. 锁时间不能过长
3. finally 中必须释放锁
4. 避免锁内执行慢 SQL 或远程调用
5. 注意 Redis 主从切换下的一致性问题
6. 高一致场景可以考虑 RedLock、Zookeeper、数据库锁等方案
```

---

## 15. 面试回答

Redisson 分布式锁底层基于 Redis 和 Lua 脚本实现。它通常使用 Hash 结构保存锁信息，key 是锁名称，field 是客户端 ID 和线程 ID，value 是重入次数。

加锁时通过 Lua 脚本保证原子性：如果锁不存在就创建锁并设置过期时间；如果当前线程已经持有锁，就重入次数加 1；如果锁被其他线程持有，则加锁失败。

Redisson 有看门狗机制，如果加锁时没有指定 leaseTime，默认会给锁设置一个过期时间，并在业务执行期间自动续期，防止业务没执行完锁提前过期。解锁时也通过 Lua 脚本判断是否当前线程持有锁，只有重入次数减到 0 才真正删除锁，并通过发布订阅通知等待线程。

---

# 十一、Redis 主从、哨兵、集群区别

## 1. Redis 主从复制是什么？

主从复制是 Redis 的数据复制机制。

结构：

```text
Master
  |
  ├── Replica 1
  ├── Replica 2
  └── Replica 3
```

主节点负责写，从节点复制主节点数据。

---

## 2. 主从复制作用

主从复制主要作用：

```text
1. 读写分离
2. 数据备份
3. 提高读能力
4. 为高可用提供基础
```

---

## 3. 主从复制流程

从节点启动后，会连接主节点并同步数据。

大致流程：

```text
1. 从节点连接主节点
2. 发送同步请求
3. 主节点生成 RDB 快照
4. 主节点将 RDB 发给从节点
5. 从节点加载 RDB
6. 主节点继续将增量写命令发送给从节点
7. 主从保持同步
```

---

## 4. 主从复制的问题

主从复制本身不能自动故障转移。

如果主节点宕机：

```text
从节点不会自动升级为主节点；
需要人工切换或借助哨兵。
```

---

## 5. Redis 哨兵是什么？

Redis Sentinel 是 Redis 的高可用方案。

它用于监控主从节点，并在主节点故障时自动完成故障转移。

结构：

```text
Sentinel 1
Sentinel 2
Sentinel 3
      |
      v
Master + Replicas
```

---

## 6. 哨兵核心功能

哨兵主要功能：

```text
1. 监控
2. 主观下线
3. 客观下线
4. 自动故障转移
5. 通知客户端新主节点地址
```

---

## 7. 主观下线

某个 Sentinel 认为主节点不可用，称为主观下线。

```text
一个哨兵觉得主节点挂了。
```

---

## 8. 客观下线

多个 Sentinel 达成一致，认为主节点不可用，称为客观下线。

```text
多个哨兵都认为主节点挂了。
```

达到 quorum 后，才会触发故障转移。

---

## 9. 哨兵故障转移流程

主节点故障后：

```text
1. Sentinel 监控到主节点无响应
2. 标记主观下线
3. 多个 Sentinel 确认客观下线
4. 选举一个 Sentinel 作为 Leader
5. Leader 从从节点中选择一个新的主节点
6. 将该从节点提升为主节点
7. 让其他从节点复制新主节点
8. 通知客户端新的主节点地址
```

---

## 10. Redis Cluster 是什么？

Redis Cluster 是 Redis 的分布式集群方案。

它主要解决：

```text
1. 数据分片
2. 容量扩展
3. 高可用
```

---

## 11. Redis Cluster 哈希槽

Redis Cluster 将整个 key 空间划分为：

```text
16384 个 hash slot
```

每个 key 会根据 CRC16 计算所属槽位：

```text
slot = CRC16(key) % 16384
```

每个主节点负责一部分槽位。

---

## 12. Redis Cluster 示例

```text
Master A：负责 slot 0 - 5460
Master B：负责 slot 5461 - 10922
Master C：负责 slot 10923 - 16383
```

每个 Master 可以有 Replica。

```text
Master A -> Replica A1
Master B -> Replica B1
Master C -> Replica C1
```

---

## 13. Cluster 如何路由请求？

客户端访问某个 key 时：

```text
1. 根据 key 计算 slot
2. 找到负责该 slot 的节点
3. 发送命令
```

如果请求发错节点，Redis 会返回：

```text
MOVED
```

客户端根据 MOVED 重定向到正确节点。

---

## 14. Cluster 的高可用

Redis Cluster 中，每个主节点通常有从节点。

如果某个主节点宕机：

```text
它的从节点可以被提升为新的主节点。
```

但如果某个主节点和它的所有从节点都不可用，那么对应槽位不可用，集群可能不可用。

---

## 15. 主从、哨兵、集群对比

| 架构 | 作用 | 是否自动故障转移 | 是否分片 | 适合场景 |
|---|---|---|---|---|
| 主从复制 | 数据复制、读写分离 | 否 | 否 | 简单读扩展 |
| 哨兵 | 高可用、自动主从切换 | 是 | 否 | 单主多从高可用 |
| 集群 | 分片扩容、高可用 | 是 | 是 | 大容量、高并发 |

---

## 16. 主从复制适合什么场景？

适合：

```text
读多写少；
需要读扩展；
需要数据副本；
对自动故障转移要求不高。
```

---

## 17. 哨兵适合什么场景？

适合：

```text
数据量单机能放下；
需要自动故障转移；
不需要分片；
希望架构相对简单。
```

---

## 18. 集群适合什么场景？

适合：

```text
单机内存不够；
写入压力大；
需要水平扩展；
需要自动分片；
需要更高吞吐。
```

---

## 19. Redis Cluster 注意事项

Redis Cluster 使用时要注意：

```text
1. 多 key 操作要求 key 在同一个 slot
2. 可以使用 hash tag 保证同槽
3. 客户端需要支持 Cluster
4. 不支持多数据库选择
5. 扩缩容涉及槽位迁移
6. 跨槽事务受限
```

---

## 20. hash tag 示例

如果想让多个 key 落到同一个 slot，可以使用 `{}`。

例如：

```text
order:{1001}:info
order:{1001}:items
order:{1001}:pay
```

Redis Cluster 只会对 `{1001}` 计算 hash slot。

这样这些 key 会落到同一个槽位。

---

## 21. 面试回答

Redis 主从复制是最基础的数据复制机制，主节点负责写，从节点复制主节点数据，可以用于读写分离和数据备份，但主节点宕机后不能自动故障转移。

Redis Sentinel 是高可用方案，它会监控主从节点。当主节点故障时，多个 Sentinel 会判断主节点客观下线，并选举出一个从节点升级为新的主节点，实现自动故障转移。

Redis Cluster 是分布式集群方案，它将 key 空间划分为 16384 个 hash slot，不同主节点负责不同槽位，实现数据分片和水平扩展。Cluster 同时支持主从复制和自动故障转移，适合大容量、高并发场景。

---

# 十二、面试速记版

## 1. Redis 常用数据结构有哪些？

Redis 常用数据结构：

```text
String
Hash
List
Set
ZSet
Bitmap
HyperLogLog
GEO
Stream
```

最常问的是：

```text
String、Hash、List、Set、ZSet
```

---

## 2. String、Hash、List、Set、ZSet 使用场景

```text
String：缓存、计数器、验证码、Token、分布式锁
Hash：用户信息、商品信息、购物车、对象缓存
List：队列、消息列表、时间线
Set：去重、点赞、标签、共同好友
ZSet：排行榜、延迟队列、热搜、滑动窗口限流
```

---

## 3. Redis 为什么快？

Redis 快的原因：

```text
基于内存
数据结构高效
单线程避免锁竞争
I/O 多路复用
事件驱动模型
命令执行路径短
支持 Pipeline
```

---

## 4. Redis 单线程为什么还能高性能？

Redis 单线程主要指命令执行单线程。

它还能高性能是因为：

```text
数据在内存
单条命令很快
I/O 多路复用处理大量连接
避免锁竞争和线程切换
```

但大 key、慢命令、Lua 脚本会阻塞主线程。

---

## 5. RDB 和 AOF 区别

```text
RDB：快照持久化，文件小，恢复快，但可能丢最近数据
AOF：追加写命令，数据更安全，但文件更大，恢复可能更慢
```

常用：

```text
appendfsync everysec
```

---

## 6. Redis 过期删除策略

Redis 过期删除：

```text
惰性删除：访问 key 时检查过期并删除
定期删除：周期性随机抽样删除过期 key
```

过期 key 不一定到期立刻删除。

---

## 7. Redis 内存淘汰策略

常见策略：

```text
noeviction
allkeys-lru
volatile-lru
allkeys-lfu
volatile-lfu
allkeys-random
volatile-random
volatile-ttl
```

缓存场景常用：

```text
allkeys-lru
allkeys-lfu
```

---

## 8. 缓存穿透、击穿、雪崩

```text
缓存穿透：查不存在的数据，缓存和数据库都没有
解决：空值缓存、布隆过滤器、参数校验

缓存击穿：热点 key 过期，大量请求打到数据库
解决：互斥锁、逻辑过期、热点 key 永不过期

缓存雪崩：大量 key 同时过期或 Redis 故障
解决：随机 TTL、高可用、限流降级、多级缓存
```

---

## 9. 如何实现分布式锁？

核心命令：

```bash
SET lock:key uniqueValue NX EX 30
```

释放锁用 Lua 脚本：

```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
```

注意：

```text
加锁和过期要原子
解锁只能删自己的锁
解锁判断和删除要原子
```

---

## 10. Redisson 分布式锁原理

Redisson 使用 Hash 保存锁：

```text
key：锁名
field：clientId:threadId
value：重入次数
```

特点：

```text
Lua 脚本保证原子性
支持可重入
支持 WatchDog 自动续期
解锁时发布订阅通知等待线程
```

---

## 11. Redis 主从、哨兵、集群区别

```text
主从：数据复制、读写分离，不自动故障转移
哨兵：监控主从，自动故障转移，不做分片
集群：数据分片 + 高可用，支持水平扩展
```

---

# 十三、总览表

| 问题 | 核心结论 |
|---|---|
| Redis 常用数据结构 | String、Hash、List、Set、ZSet、Bitmap、HyperLogLog、GEO、Stream |
| String 使用场景 | 缓存、计数器、验证码、Token、分布式锁 |
| Hash 使用场景 | 用户对象、商品对象、购物车 |
| List 使用场景 | 队列、消息列表、时间线 |
| Set 使用场景 | 去重、点赞、标签、共同好友 |
| ZSet 使用场景 | 排行榜、延迟队列、热搜、滑动窗口限流 |
| Redis 为什么快 | 内存、单线程、I/O 多路复用、高效数据结构 |
| Redis 单线程为什么高性能 | 避免锁竞争和线程切换，通过事件驱动处理大量连接 |
| RDB 和 AOF 区别 | RDB 是快照，AOF 是追加命令日志 |
| 过期删除策略 | 惰性删除 + 定期删除 |
| 内存淘汰策略 | noeviction、LRU、LFU、random、TTL 等 |
| 缓存穿透 | 查不存在数据，解决方案是空值缓存、布隆过滤器 |
| 缓存击穿 | 热点 key 过期，解决方案是互斥锁、逻辑过期 |
| 缓存雪崩 | 大量 key 同时过期或 Redis 故障，解决方案是随机 TTL、高可用 |
| 分布式锁 | `SET NX EX` 加锁，Lua 判断 value 解锁 |
| Redisson 锁原理 | Hash + Lua + 可重入 + WatchDog 续期 |
| 主从、哨兵、集群 | 主从复制、哨兵高可用、集群分片扩容 |

---

# 十四、完整面试回答模板

Redis 常用数据结构有 String、Hash、List、Set、ZSet，此外还有 Bitmap、HyperLogLog、Geospatial 和 Stream。String 适合缓存简单值、JSON、计数器、验证码和分布式锁；Hash 适合存储对象，比如用户信息、商品信息和购物车；List 适合队列、消息列表和时间线；Set 适合去重、点赞、标签和共同好友；ZSet 适合排行榜、延迟队列、热搜榜和滑动窗口限流。

Redis 快主要是因为它基于内存操作，避免了频繁磁盘 IO；同时 Redis 使用高效的数据结构，命令执行路径短。Redis 命令执行主要是单线程的，避免了多线程锁竞争和上下文切换。再加上 I/O 多路复用和事件驱动网络模型，使得 Redis 可以用较少线程处理大量连接。不过大 key、复杂命令、`KEYS *`、大范围查询和耗时 Lua 脚本都可能阻塞主线程。

Redis 持久化主要有 RDB 和 AOF。RDB 是快照持久化，会在某个时间点将内存数据生成快照保存到磁盘，优点是文件小、恢复快，适合备份和全量复制，缺点是可能丢失最近一次快照后的数据。AOF 是追加日志持久化，会把写命令追加到 AOF 文件中，重启时重新执行命令恢复数据，数据安全性更高，但文件更大，恢复可能更慢。生产中可以根据业务要求选择 RDB、AOF 或混合持久化。

Redis 删除过期 key 主要使用惰性删除和定期删除。惰性删除是客户端访问 key 时检查是否过期，过期则删除；定期删除是 Redis 周期性地从设置了过期时间的 key 中随机抽样，删除已经过期的 key。Redis 不会给每个 key 都设置定时器，也不会频繁全量扫描所有 key，因为这样会带来很高的 CPU 开销。

Redis 内存达到 `maxmemory` 后，会根据 `maxmemory-policy` 选择淘汰策略。常见策略有 `noeviction`、`allkeys-lru`、`volatile-lru`、`allkeys-lfu`、`volatile-lfu`、`allkeys-random`、`volatile-random` 和 `volatile-ttl`。如果 Redis 主要作为缓存，通常使用 `allkeys-lru` 或 `allkeys-lfu`。

缓存穿透是查询缓存和数据库都不存在的数据，导致请求每次都打到数据库，常见解决方案是缓存空值、布隆过滤器和参数校验。缓存击穿是热点 key 在过期瞬间被大量并发访问，导致请求同时打到数据库，常见解决方案是互斥锁、逻辑过期、热点 key 永不过期和后台异步刷新。缓存雪崩是大量 key 同时过期，或者 Redis 整体不可用，导致大量请求打到数据库，常见解决方案是 TTL 加随机值、缓存预热、多级缓存、Redis 高可用、限流和降级。

Redis 实现分布式锁通常使用 `SET key value NX EX seconds`。`NX` 保证只有 key 不存在时才能加锁成功，`EX` 设置过期时间避免死锁，value 使用唯一标识，避免释放别人的锁。释放锁时不能直接 `DEL`，必须先判断 value 是否是自己的，如果是才删除，并且判断和删除要用 Lua 脚本保证原子性。

Redisson 分布式锁底层基于 Redis 和 Lua 脚本实现。它通常使用 Hash 结构保存锁信息，key 是锁名称，field 是客户端 ID 和线程 ID，value 是重入次数。加锁时通过 Lua 脚本保证原子性，如果锁不存在就创建锁并设置过期时间；如果当前线程已经持有锁，就重入次数加 1；如果锁被其他线程持有，则加锁失败。Redisson 还提供 WatchDog 看门狗机制，如果加锁时没有指定 leaseTime，会自动续期，防止业务没执行完锁提前过期。

Redis 主从复制是最基础的数据复制机制，主节点负责写，从节点复制主节点数据，可以用于读写分离和数据备份，但主节点宕机后不能自动故障转移。Redis Sentinel 是高可用方案，它会监控主从节点，当主节点故障时，多个 Sentinel 会判断主节点客观下线，并选举一个从节点升级为新的主节点。Redis Cluster 是分布式集群方案，它将 key 空间划分为 16384 个 hash slot，不同主节点负责不同槽位，实现数据分片和水平扩展，同时支持主从复制和自动故障转移。
