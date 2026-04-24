# MySQL 调优总结

MySQL 调优的核心目标是：**减少磁盘 IO、减少锁等待、提高索引命中率、提升 SQL 执行效率、降低数据库资源压力**。

---

## 1. MySQL 调优整体思路

```text
慢 SQL 分析
  ↓
SQL 和索引优化
  ↓
表结构优化
  ↓
事务和锁优化
  ↓
MySQL 参数优化
  ↓
架构优化：缓存、读写分离、分库分表
```

---

## 2. SQL 优化

### 核心思路

- 避免全表扫描。
- 避免索引失效。
- 避免 `SELECT *`。
- 避免深分页。
- 避免大事务。
- 避免复杂 Join。
- 减少返回数据量。

### 常见优化方式

```sql
EXPLAIN SELECT * FROM user WHERE phone = '13800138000';
```

重点关注：

| 字段 | 说明 |
|---|---|
| type | 是否全表扫描 |
| key | 是否命中索引 |
| rows | 扫描行数 |
| Extra | 是否有 filesort、temporary |

---

## 3. 索引优化

### 建索引原则

- 高频查询字段建索引。
- 区分度高的字段适合建索引。
- 多条件查询使用联合索引。
- 遵守最左前缀原则。
- 尽量使用覆盖索引。
- 索引不是越多越好。

### 示例

```sql
SELECT *
FROM order_info
WHERE user_id = 1001
AND status = 1
ORDER BY create_time DESC;
```

可以建立联合索引：

```sql
CREATE INDEX idx_user_status_time
ON order_info(user_id, status, create_time);
```

---

## 4. 表结构优化

### 优化方向

- 字段类型尽量小。
- 能用 `INT` 不用 `BIGINT`。
- 能用 `VARCHAR(64)` 不用 `VARCHAR(255)`。
- 避免大字段和热点字段放在一起。
- 冷热字段分离。
- 大表定期归档。
- 合理冗余字段减少 Join。

### 示例

```text
user_base：id、name、phone
user_extend：user_id、avatar、profile、remark
```

---

## 5. 事务优化

### 常见问题

- 事务过大。
- 事务中调用远程接口。
- 长时间不提交事务。
- 大批量更新导致锁等待。
- 热点行频繁更新。

### 优化方式

- 控制事务范围。
- 事务中只做数据库操作。
- 避免事务中调用 RPC、HTTP、MQ。
- 大批量操作分批提交。
- 尽量缩短锁持有时间。

### 不推荐

```text
开启事务
  ↓
调用远程接口
  ↓
更新数据库
  ↓
提交事务
```

### 推荐

```text
先调用远程接口
  ↓
开启事务
  ↓
更新数据库
  ↓
提交事务
```

---

## 6. 锁优化

### 常见问题

- 行锁升级为表锁。
- 索引失效导致锁范围变大。
- 间隙锁导致插入阻塞。
- 大事务长时间持锁。
- 热点数据竞争严重。

### 优化方式

- 更新条件必须命中索引。
- 避免大范围更新。
- 控制事务执行时间。
- 减少热点行更新。
- 必要时使用乐观锁。
- 高并发扣减场景可以用 Redis、MQ 削峰。

---

## 7. Buffer Pool 优化

### 作用

`innodb_buffer_pool_size` 是 InnoDB 最重要的参数，用于缓存数据页和索引页。

```text
Buffer Pool 越大，命中率越高，磁盘 IO 越少。
```

### 常见配置

```ini
innodb_buffer_pool_size = 物理内存的 60% ~ 75%
```

如果服务器主要运行 MySQL，可以设置为内存的 60% 到 75%。

---

## 8. Buffer Pool Instance 优化

### 作用

`innodb_buffer_pool_instances` 用于将 Buffer Pool 拆成多个实例，减少并发访问 Buffer Pool 时的锁竞争。

### 适用场景

当 Buffer Pool 较大、并发较高时，可以配置多个实例。

```ini
innodb_buffer_pool_instances = 4
```

或：

```ini
innodb_buffer_pool_instances = 8
```

### 注意

- Buffer Pool 较小时，不建议设置太多实例。
- 一般每个实例至少保持几个 GB 比较合理。
- MySQL 8.0 中部分版本对该参数做了优化或弱化，不一定需要过度调整。

---

## 9. Redo Log 优化

### 关键参数

```ini
innodb_log_file_size
innodb_log_files_in_group
innodb_flush_log_at_trx_commit
```

### 作用

Redo Log 用于保证事务持久性，写入是顺序 IO。

### 参数说明

| 参数 | 说明 |
|---|---|
| innodb_log_file_size | 单个 redo log 文件大小 |
| innodb_log_files_in_group | redo log 文件数量 |
| innodb_flush_log_at_trx_commit | 事务提交时 redo log 刷盘策略 |

### `innodb_flush_log_at_trx_commit`

| 值 | 说明 | 特点 |
|---|---|---|
| 1 | 每次提交都刷盘 | 最安全，性能较低 |
| 2 | 每次提交写 OS Cache，每秒刷盘 | 性能较好，可能丢 1 秒数据 |
| 0 | 每秒写入并刷盘 | 性能最好，安全性最低 |

生产环境默认推荐：

```ini
innodb_flush_log_at_trx_commit = 1
```

如果可以接受极端情况下丢失少量数据，可以考虑：

```ini
innodb_flush_log_at_trx_commit = 2
```

---

## 10. Binlog 优化

### 关键参数

```ini
sync_binlog
binlog_format
expire_logs_days
binlog_expire_logs_seconds
```

### `sync_binlog`

| 值 | 说明 |
|---|---|
| 1 | 每次事务提交都刷 binlog 到磁盘，最安全 |
| 0 | 由操作系统决定何时刷盘，性能好但风险高 |
| N | 每 N 次事务刷盘一次 |

强一致场景推荐：

```ini
sync_binlog = 1
```

性能优先场景可以适当调大。

---

## 11. 连接数优化

### 关键参数

```ini
max_connections
wait_timeout
interactive_timeout
thread_cache_size
```

### 说明

| 参数 | 作用 |
|---|---|
| max_connections | 最大连接数 |
| wait_timeout | 空闲连接超时时间 |
| thread_cache_size | 线程缓存数量 |

### 注意

- `max_connections` 不是越大越好。
- 连接数过大会导致 CPU 切换、内存消耗增加。
- 应该结合应用连接池一起调优。

应用侧也要配置合理连接池，例如：

```text
最大连接数不要超过数据库承受能力
避免大量空闲连接
避免连接泄漏
```

---

## 12. 临时表和排序优化

### 关键参数

```ini
tmp_table_size
max_heap_table_size
sort_buffer_size
join_buffer_size
```

### 说明

| 参数 | 作用 |
|---|---|
| tmp_table_size | 内存临时表大小 |
| max_heap_table_size | MEMORY 表最大大小 |
| sort_buffer_size | 排序缓冲区 |
| join_buffer_size | Join 缓冲区 |

### 注意

这些参数通常是 **每个连接独享** 的，不要盲目设置太大，否则高并发下容易占用大量内存。

---

## 13. 慢查询日志优化

### 开启慢查询

```sql
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;
```

### 关注指标

- SQL 执行时间。
- 扫描行数。
- 返回行数。
- 执行频率。
- 是否使用索引。
- 是否有锁等待。

### 常用分析工具

```text
mysqldumpslow
pt-query-digest
```

---

## 14. 主从和读写分离优化

### 适用场景

当读请求压力较大时，可以通过主从复制实现读写分离。

```text
主库：负责写
从库：负责读
```

### 注意问题

- 主从延迟。
- 读到旧数据。
- 主库故障切换。
- 强一致读必须走主库。

---

## 15. 缓存优化

### 适用场景

热点数据可以放入 Redis，减少数据库查询压力。

常见缓存数据：

- 用户信息。
- 商品详情。
- 字典配置。
- 首页数据。
- 热点排行榜。

### 注意问题

- 缓存穿透。
- 缓存击穿。
- 缓存雪崩。
- 缓存和数据库一致性。

---

## 16. 分库分表优化

### 适用场景

当单库、单表已经成为瓶颈时，可以考虑分库分表。

常见场景：

- 订单表。
- 支付流水表。
- 消息表。
- 日志表。
- 交易明细表。

### 注意问题

- 分片键选择。
- 分布式 ID。
- 跨库 Join。
- 跨库事务。
- 跨库分页。
- 数据迁移和扩容。

---

## 17. 常见调优参数总结

| 参数 | 作用 | 调优方向 |
|---|---|---|
| innodb_buffer_pool_size | 缓存数据页和索引页 | 通常设置为内存 60%~75% |
| innodb_buffer_pool_instances | Buffer Pool 分片 | 大内存高并发可设置 4~8 |
| innodb_log_file_size | redo log 文件大小 | 写入压力大可适当增大 |
| innodb_flush_log_at_trx_commit | redo log 刷盘策略 | 安全优先设 1，性能优先可设 2 |
| sync_binlog | binlog 刷盘策略 | 强一致设 1 |
| max_connections | 最大连接数 | 结合连接池合理设置 |
| thread_cache_size | 线程缓存 | 减少线程创建销毁 |
| tmp_table_size | 内存临时表大小 | 减少磁盘临时表 |
| max_heap_table_size | 内存表大小 | 配合 tmp_table_size |
| sort_buffer_size | 排序缓冲 | 不宜过大 |
| join_buffer_size | Join 缓冲 | 优先优化索引，不要盲目调大 |

---

## 18. 总结

MySQL 调优可以从 SQL、索引、表结构、事务、锁、参数和架构几个层面展开。

首先通过慢查询日志和 `EXPLAIN` 定位慢 SQL，分析是否命中索引、扫描行数是否过多、是否出现 `Using filesort`、`Using temporary` 等问题。

SQL 层面要避免索引失效、`SELECT *`、深分页、大事务和复杂 Join；索引层面要合理设计联合索引，遵守最左前缀原则，尽量使用覆盖索引。

参数层面最重要的是 `innodb_buffer_pool_size`，它决定数据页和索引页的缓存能力；高并发大内存场景下可以关注 `innodb_buffer_pool_instances`；同时还要关注 redo log、binlog、连接数、临时表和排序相关参数。

如果单机优化后仍然无法满足性能要求，可以从架构层面引入 Redis 缓存、读写分离、数据归档、分库分表等方案。

一句话总结：

```text
MySQL 调优 = SQL 优化 + 索引优化 + 表结构优化 + 事务锁优化 + 参数优化 + 架构优化。
```
