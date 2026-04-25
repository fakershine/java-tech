# MySQL 占用 CPU 超过 100% 怎么排查

MySQL CPU 超过 100% 不一定异常。  
在 Linux 中，`top` 显示的是 **单核百分比**，如果机器是多核：

```text
100% = 占满 1 个 CPU 核
400% = 占满 4 个 CPU 核
```

所以要结合机器 CPU 核数判断是否真的异常。

---

## 1. 常见原因

| 原因 | 说明 |
|---|---|
| 慢 SQL | SQL 扫描大量数据，CPU 计算高 |
| 索引失效 | 没走索引，导致全表扫描 |
| QPS 突增 | 请求量突然升高 |
| 大量排序 / 分组 | `order by`、`group by` 消耗 CPU |
| 大表 Join | 多表关联数据量大 |
| 连接数过多 | 并发 SQL 太多 |
| 锁等待 | 事务阻塞导致大量线程堆积 |
| 频繁刷脏页 | IO 压力间接导致 CPU 升高 |
| 主从复制压力 | SQL 线程或 IO 线程压力大 |
| 后台任务 | 定时任务、报表、批量更新 |

---

## 2. 排查思路

```text
先看 CPU 是否真的异常
  ↓
看当前正在执行的 SQL
  ↓
找慢 SQL
  ↓
分析执行计划
  ↓
看连接数和锁等待
  ↓
看 Buffer Pool 和 IO
  ↓
定位业务来源
```

---

## 3. 查看当前连接和 SQL

```sql
SHOW FULL PROCESSLIST;
```

重点关注：

```text
Command
Time
State
Info
```

常见异常状态：

```text
Sending data
Creating sort index
Copying to tmp table
Locked
Waiting for table metadata lock
```

如果发现某条 SQL 执行时间很长，优先分析它。

---

## 4. 查看慢 SQL

确认慢查询是否开启：

```sql
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';
```

查看慢 SQL 日志路径：

```sql
SHOW VARIABLES LIKE 'slow_query_log_file';
```

重点分析：

```text
执行时间长的 SQL
扫描行数多的 SQL
执行频率高的 SQL
返回行数多的 SQL
```

---

## 5. 分析执行计划

对可疑 SQL 执行：

```sql
EXPLAIN SELECT ...
```

重点看：

| 字段 | 说明 |
|---|---|
| `type` | 访问类型，是否全表扫描 |
| `key` | 是否命中索引 |
| `rows` | 预估扫描行数 |
| `Extra` | 是否出现临时表、文件排序 |

重点关注：

```text
type = ALL
key = NULL
rows 很大
Using filesort
Using temporary
```

这些通常说明 SQL 性能较差。

---

## 6. 常见 SQL 问题

### 1. 没有索引

```sql
SELECT * FROM order_info WHERE user_id = 1001;
```

如果 `user_id` 没有索引，会全表扫描。

优化：

```sql
CREATE INDEX idx_user_id ON order_info(user_id);
```

---

### 2. 索引失效

常见场景：

```sql
WHERE id + 1 = 100
WHERE DATE(create_time) = '2026-04-25'
WHERE name LIKE '%abc'
WHERE status != 1
WHERE OR 条件使用不当
```

优化：

```text
避免对索引列做函数或计算
避免左模糊查询
遵守最左前缀原则
合理设计联合索引
```

---

### 3. 排序分组消耗 CPU

```sql
SELECT *
FROM order_info
WHERE status = 1
ORDER BY create_time DESC;
```

如果排序字段没有合适索引，可能出现：

```text
Using filesort
```

优化：

```sql
CREATE INDEX idx_status_create_time
ON order_info(status, create_time);
```

---

### 4. 深分页

```sql
SELECT *
FROM order_info
ORDER BY id
LIMIT 1000000, 20;
```

问题：

```text
需要扫描并丢弃大量数据
```

优化：

```sql
SELECT *
FROM order_info
WHERE id > last_id
ORDER BY id
LIMIT 20;
```

---

## 7. 查看连接数

```sql
SHOW STATUS LIKE 'Threads%';
```

关注：

```text
Threads_connected
Threads_running
Threads_cached
Threads_created
```

如果 `Threads_running` 很高，说明很多 SQL 正在执行，CPU 高可能是并发压力导致。

查看最大连接数：

```sql
SHOW VARIABLES LIKE 'max_connections';
```

---

## 8. 查看锁等待

```sql
SHOW ENGINE INNODB STATUS\G
```

重点看：

```text
TRANSACTIONS
LATEST DETECTED DEADLOCK
lock wait
```

也可以查：

```sql
SELECT *
FROM information_schema.INNODB_TRX;
```

如果有长事务，会导致：

```text
锁等待
undo 堆积
CPU 和 IO 升高
SQL 执行变慢
```

---

## 9. 查看 Buffer Pool 命中率

```sql
SHOW ENGINE INNODB STATUS\G
```

关注：

```text
Buffer pool hit rate
Pages read
Pages written
Modified db pages
```

如果命中率低：

```text
大量数据从磁盘读取
SQL 执行慢
CPU 和 IO 都可能升高
```

优化方向：

```text
调大 innodb_buffer_pool_size
优化 SQL
减少全表扫描
减少大查询
```

---

## 10. 查看是否有大事务 / 批量任务

常见导致 CPU 飙高的业务：

```text
大批量 UPDATE
大批量 DELETE
报表统计
全表导出
定时任务扫描大表
数据同步任务
```

处理方式：

```text
分批执行
低峰执行
增加限速
避免大事务
使用索引条件
```

---

## 11. 临时止血方案

如果已经影响线上服务，可以先止血：

```text
kill 慢 SQL
限流入口请求
关闭异常定时任务
临时扩容只读实例
切走报表流量
回滚最近发布
降低并发任务数量
```

Kill 慢 SQL：

```sql
KILL 连接ID;
```

连接 ID 来自：

```sql
SHOW FULL PROCESSLIST;
```

---

## 12. 长期优化方案

| 问题 | 优化方式 |
|---|---|
| 慢 SQL | 优化 SQL、加索引 |
| 全表扫描 | 补充索引、改查询条件 |
| 深分页 | 改成游标分页 |
| 大表数据多 | 分库分表、冷热分离 |
| 报表查询重 | 走数仓 / ES / 从库 |
| 连接数过高 | 连接池限流、优化接口 |
| 锁冲突 | 缩短事务、降低锁粒度 |
| 读压力大 | 读写分离、缓存 |
| 写压力大 | 分库分表、异步削峰 |

---

## 13. 排查流程总结

```text
1. top 确认 mysqld CPU 占用。
2. SHOW FULL PROCESSLIST 查看当前慢 SQL。
3. 查看慢查询日志，找高频慢 SQL。
4. EXPLAIN 分析是否走索引。
5. 查看 Threads_running 判断并发压力。
6. SHOW ENGINE INNODB STATUS 查看锁等待和事务。
7. 查看 Buffer Pool 命中率和 IO 情况。
8. 定位业务来源，先止血再优化。
```

---

## 14. 总结

MySQL CPU 超过 100%，我会先确认机器 CPU 核数，因为 Linux 中 100% 表示占满一个核心。

排查时先用 `top` 确认是 `mysqld` 占用 CPU，再用 `SHOW FULL PROCESSLIST` 查看当前正在执行的 SQL，重点关注执行时间长、状态异常的 SQL。然后结合慢查询日志找出高频慢 SQL，用 `EXPLAIN` 分析执行计划，看是否存在全表扫描、索引失效、扫描行数过多、`Using filesort` 或 `Using temporary`。

同时还要查看连接数、锁等待、长事务、Buffer Pool 命中率和是否有批量任务。线上紧急情况下可以先 kill 异常 SQL、限流、关闭异常任务或切走流量，后续再通过加索引、改 SQL、分批处理、读写分离、缓存、分库分表等方式优化。

一句话总结：

```text
MySQL CPU 高 = 先看当前 SQL，再查慢 SQL，EXPLAIN 看索引，最后排查连接数、锁等待、大事务和批量任务。
```
