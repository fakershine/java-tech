# SQL 慢查询分析总结

SQL 慢查询分析的核心思路是：**先定位慢 SQL，再看执行计划，判断慢在扫描、回表、排序、锁等待、数据量、IO 还是网络返回。**

---

## 1. 慢 SQL 分析流程

```text
定位慢 SQL
  ↓
查看执行计划 EXPLAIN
  ↓
分析索引是否命中
  ↓
分析扫描行数和返回行数
  ↓
分析是否回表、排序、临时表
  ↓
分析锁等待和事务
  ↓
优化 SQL / 索引 / 表结构 / 架构
```

---

## 2. 定位慢 SQL

### 方式一：开启慢查询日志

```sql
SHOW VARIABLES LIKE 'slow_query_log';
SHOW VARIABLES LIKE 'long_query_time';
```

开启慢查询日志：

```sql
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;
```

查看慢 SQL 后，可以关注：

- SQL 执行时间。
- 扫描行数。
- 返回行数。
- 是否频繁出现。
- 是否在高峰期集中出现。

---

### 方式二：查看当前正在执行的 SQL

```sql
SHOW FULL PROCESSLIST;
```

重点关注：

| 字段 | 说明 |
|---|---|
| Time | SQL 执行时间 |
| State | 当前状态 |
| Info | 正在执行的 SQL |

常见异常状态：

```text
Sending data
Creating tmp table
Copying to tmp table
Using filesort
Waiting for table lock
Locked
```

---

## 3. 使用 EXPLAIN 分析执行计划

### 基本命令

```sql
EXPLAIN SELECT * FROM user WHERE phone = '13800138000';
```

重点看以下字段：

| 字段 | 说明 | 关注点 |
|---|---|---|
| type | 访问类型 | 是否全表扫描 |
| possible_keys | 可能使用的索引 | 是否有可用索引 |
| key | 实际使用的索引 | 是否真正命中索引 |
| rows | 预估扫描行数 | 是否扫描过多 |
| filtered | 过滤比例 | 越高越好 |
| Extra | 额外信息 | 是否排序、临时表、回表 |

---

## 4. type 字段怎么看

`type` 表示 MySQL 访问数据的方式，性能从好到差大致如下：

```text
system > const > eq_ref > ref > range > index > ALL
```

| type | 说明 |
|---|---|
| const | 主键或唯一索引等值查询 |
| eq_ref | Join 时使用唯一索引 |
| ref | 普通索引等值查询 |
| range | 范围索引扫描 |
| index | 扫描整个索引树 |
| ALL | 全表扫描 |

一般来说：

```text
type = ALL，通常需要重点优化
type = index，也可能扫描数据过多
type = range/ref，一般较好
```

---

## 5. Extra 字段怎么看

| Extra | 含义 | 说明 |
|---|---|---|
| Using index | 使用覆盖索引 | 较好 |
| Using where | 使用 where 过滤 | 正常 |
| Using temporary | 使用临时表 | 需要关注 |
| Using filesort | 使用额外排序 | 需要关注 |
| Using index condition | 使用索引条件下推 | 一般较好 |
| Using join buffer | Join 没有很好使用索引 | 需要优化 |

重点关注：

```text
Using temporary
Using filesort
Using join buffer
```

这些通常说明 SQL 可能存在排序、分组、Join 性能问题。

---

## 6. 常见慢 SQL 原因

### 1. 没有命中索引

常见原因：

- 查询字段没有索引。
- 联合索引不满足最左前缀。
- 对索引字段使用函数。
- 隐式类型转换。
- `LIKE '%xxx'` 左模糊。
- `OR` 条件导致索引失效。

---

### 2. 扫描数据量太大

即使使用了索引，如果扫描行数太多，也会慢。

例如：

```sql
SELECT * FROM order_info WHERE status = 1;
```

如果 `status = 1` 占全表 80%，索引意义不大，MySQL 可能全表扫描。

优化思路：

- 提高索引区分度。
- 增加更合适的联合索引。
- 缩小查询范围。
- 避免一次查询过多数据。

---

### 3. 回表次数太多

二级索引查询时，如果查询字段不在索引中，需要回表。

例如：

```sql
SELECT * FROM user WHERE phone = '13800138000';
```

如果 `phone` 是普通索引，查询 `*` 可能需要回表。

优化方式：

```sql
SELECT id, phone FROM user WHERE phone = '13800138000';
```

或者建立覆盖索引：

```sql
KEY idx_phone_name(phone, name)
```

---

### 4. 排序或分组慢

常见 SQL：

```sql
SELECT * FROM order_info 
WHERE user_id = 1001 
ORDER BY create_time DESC;
```

如果没有合适索引，可能出现：

```text
Using filesort
```

优化方式：

```sql
KEY idx_user_time(user_id, create_time)
```

让查询和排序都走索引。

---

### 5. 深分页慢

典型 SQL：

```sql
SELECT * FROM order_info 
ORDER BY id 
LIMIT 100000, 20;
```

MySQL 需要先扫描前 `100000` 条，再丢弃，只返回后 20 条。

优化方式：

```sql
SELECT * FROM order_info 
WHERE id > 上一页最大 id
ORDER BY id
LIMIT 20;
```

也就是使用游标分页或 `last_id` 分页。

---

### 6. Join 慢

常见原因：

- Join 字段没有索引。
- 两表字符集不一致。
- 大表 Join 大表。
- Join 顺序不合理。
- 返回字段过多。

优化方式：

- 给关联字段加索引。
- 小表驱动大表。
- 保证 Join 字段类型和字符集一致。
- 拆分复杂 Join。
- 必要时做字段冗余。

---

### 7. 锁等待导致慢

SQL 本身可能不慢，但被锁阻塞。

常见情况：

- 大事务长时间未提交。
- 更新同一行热点数据。
- 表锁或行锁等待。
- DDL 和 DML 冲突。

排查方式：

```sql
SHOW FULL PROCESSLIST;
SHOW ENGINE INNODB STATUS;
```

关注：

```text
lock wait
deadlock
transaction
```

---

### 8. 返回数据量太大

例如：

```sql
SELECT * FROM log_info WHERE create_time >= '2026-04-01';
```

如果一次返回几十万行，即使命中索引也会慢。

优化方式：

- 分页查询。
- 限制返回字段。
- 避免 `SELECT *`。
- 大数据导出走异步任务。
- 使用专门的日志或分析系统。

---

## 7. 常见优化手段

| 问题 | 优化方式 |
|---|---|
| 没有索引 | 添加合适索引 |
| 索引失效 | 改写 SQL，避免函数、隐式转换、左模糊 |
| 扫描行数多 | 增加高区分度联合索引 |
| 回表多 | 使用覆盖索引，避免 `SELECT *` |
| 排序慢 | 建立符合 `WHERE + ORDER BY` 的联合索引 |
| 分组慢 | 建立符合 `WHERE + GROUP BY` 的索引 |
| 深分页慢 | 使用游标分页 / last_id 分页 |
| Join 慢 | 关联字段加索引，小表驱动大表 |
| 锁等待 | 缩短事务，减少热点更新 |
| 数据量太大 | 分库分表、归档、冷热分离 |

---

## 8. 索引设计原则

- 区分度高的字段适合建索引。
- 高频查询条件适合建索引。
- 联合索引要遵守最左前缀原则。
- 尽量使用覆盖索引。
- 不要给低区分度字段单独建索引。
- 索引不是越多越好，过多索引会影响写入性能。
- 联合索引顺序一般按：等值条件字段、范围字段、排序字段设计。

示例：

```sql
WHERE user_id = ?
AND status = ?
ORDER BY create_time DESC
```

可以考虑：

```sql
KEY idx_user_status_time(user_id, status, create_time)
```

---

## 9. 总结

SQL 慢查询分析一般先通过慢查询日志或监控定位慢 SQL，然后使用 `EXPLAIN` 查看执行计划，重点关注 `type`、`key`、`rows`、`filtered` 和 `Extra`。

如果 `type` 是 `ALL`，说明可能发生全表扫描；如果 `rows` 很大，说明扫描数据量过多；如果 `Extra` 中出现 `Using filesort` 或 `Using temporary`，说明排序或分组可能存在性能问题。

常见慢 SQL 原因包括：没有命中索引、索引失效、扫描行数过多、回表次数过多、排序分组慢、深分页、Join 字段无索引、锁等待以及返回数据量过大。

优化时通常从 SQL 改写、索引优化、减少返回字段、避免深分页、优化 Join、缩短事务、归档历史数据和分库分表等方面入手。
