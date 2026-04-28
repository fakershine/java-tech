# SQL 调优总结

SQL 调优的核心目标是：**减少扫描数据量、减少回表、减少排序临时表、减少锁等待、提高索引命中率**。

---

## 1. SQL 调优整体思路

```text
定位慢 SQL
  ↓
查看执行计划 EXPLAIN
  ↓
分析索引是否命中
  ↓
分析扫描行数、回表、排序、临时表
  ↓
优化 SQL 写法
  ↓
优化索引设计
  ↓
优化表结构和数据量
  ↓
必要时做缓存、读写分离、分库分表
```

---

## 2. 使用 EXPLAIN 分析 SQL

### 常用命令

```sql
EXPLAIN SELECT * FROM user WHERE phone = '13800138000';
```

### 重点字段

| 字段 | 说明 | 关注点 |
|---|---|---|
| type | 访问类型 | 是否全表扫描 |
| key | 实际使用的索引 | 是否命中索引 |
| rows | 预估扫描行数 | 是否扫描过多 |
| filtered | 过滤比例 | 越高越好 |
| Extra | 额外信息 | 是否有排序、临时表、回表 |

### type 性能从好到差

```text
system > const > eq_ref > ref > range > index > ALL
```

如果出现：

```text
type = ALL
```

通常说明可能全表扫描，需要重点优化。

---

## 3. 索引优化

### 1. 给高频查询字段加索引

```sql
SELECT * FROM user WHERE phone = '13800138000';
```

可以给 `phone` 建索引：

```sql
CREATE INDEX idx_phone ON user(phone);
```

### 2. 使用联合索引

如果经常按照多个字段查询：

```sql
SELECT * FROM order_info
WHERE user_id = 1001
AND status = 1
ORDER BY create_time DESC;
```

可以建立联合索引：

```sql
CREATE INDEX idx_user_status_time 
ON order_info(user_id, status, create_time);
```

### 3. 遵守最左前缀原则

联合索引：

```sql
KEY idx_user_status_time(user_id, status, create_time)
```

可以命中：

```sql
WHERE user_id = ?
WHERE user_id = ? AND status = ?
WHERE user_id = ? AND status = ? ORDER BY create_time
```

不容易命中：

```sql
WHERE status = ?
WHERE create_time = ?
```

### 4. 使用覆盖索引

尽量避免回表。

不推荐：

```sql
SELECT * FROM user WHERE phone = '13800138000';
```

推荐：

```sql
SELECT id, phone FROM user WHERE phone = '13800138000';
```

如果索引包含查询字段，就可以减少回表。

---

## 4. 避免索引失效

常见索引失效写法：

### 1. 对索引字段使用函数

不推荐：

```sql
WHERE DATE(create_time) = '2026-04-25'
```

推荐：

```sql
WHERE create_time >= '2026-04-25 00:00:00'
AND create_time < '2026-04-26 00:00:00'
```

### 2. 对索引字段进行计算

不推荐：

```sql
WHERE age + 1 = 19
```

推荐：

```sql
WHERE age = 18
```

### 3. 隐式类型转换

字段是 `VARCHAR`：

```sql
phone VARCHAR(20)
```

不推荐：

```sql
WHERE phone = 13800138000
```

推荐：

```sql
WHERE phone = '13800138000'
```

### 4. LIKE 左模糊

可以使用索引：

```sql
WHERE name LIKE 'Tom%'
```

不容易使用索引：

```sql
WHERE name LIKE '%Tom%'
```

### 5. OR 条件不合理

不推荐：

```sql
WHERE phone = '138' OR age = 18
```

如果 `age` 没有索引，可能导致整体索引效果变差。

可以改成：

```sql
SELECT * FROM user WHERE phone = '138'
UNION
SELECT * FROM user WHERE age = 18;
```

---

## 5. 减少扫描数据量

### 1. 避免查询无用数据

不推荐：

```sql
SELECT * FROM user;
```

推荐：

```sql
SELECT id, name, phone FROM user;
```

### 2. 加必要条件

不推荐：

```sql
SELECT * FROM order_info;
```

推荐：

```sql
SELECT * FROM order_info
WHERE user_id = 1001
AND create_time >= '2026-04-01';
```

### 3. 控制返回条数

```sql
SELECT * FROM order_info
WHERE user_id = 1001
ORDER BY create_time DESC
LIMIT 20;
```

---

## 6. 优化排序和分组

### 排序优化

SQL：

```sql
SELECT * FROM order_info
WHERE user_id = 1001
ORDER BY create_time DESC;
```

可以建立索引：

```sql
CREATE INDEX idx_user_time 
ON order_info(user_id, create_time);
```

避免出现：

```text
Using filesort
```

---

### 分组优化

SQL：

```sql
SELECT status, COUNT(*)
FROM order_info
WHERE user_id = 1001
GROUP BY status;
```

可以建立索引：

```sql
CREATE INDEX idx_user_status 
ON order_info(user_id, status);
```

避免大量临时表和排序。

---

## 7. 优化分页查询

### 深分页问题

不推荐：

```sql
SELECT * FROM order_info
ORDER BY id
LIMIT 100000, 20;
```

MySQL 需要扫描前 100000 条数据，再丢弃。

### 推荐方式：游标分页

```sql
SELECT * FROM order_info
WHERE id > 100000
ORDER BY id
LIMIT 20;
```

或者按时间分页：

```sql
SELECT * FROM order_info
WHERE create_time < '2026-04-25 10:00:00'
ORDER BY create_time DESC
LIMIT 20;
```

---

## 8. 优化 JOIN

### 常见问题

- Join 字段没有索引。
- 两表字段类型不一致。
- 两表字符集不一致。
- 大表 Join 大表。
- 返回字段太多。

### 优化方式

- 给关联字段建立索引。
- 小表驱动大表。
- Join 字段类型保持一致。
- Join 字段字符集保持一致。
- 必要时拆分复杂 Join。
- 高频查询可以适当冗余字段。

示例：

```sql
SELECT *
FROM order_info o
JOIN user u ON o.user_id = u.id
WHERE o.user_id = 1001;
```

索引建议：

```sql
order_info(user_id)
user(id)
```

---

## 9. 优化子查询

### 不推荐

```sql
SELECT * FROM order_info
WHERE user_id IN (
    SELECT id FROM user WHERE status = 1
);
```

### 可优化为 JOIN

```sql
SELECT o.*
FROM order_info o
JOIN user u ON o.user_id = u.id
WHERE u.status = 1;
```

但是否一定更快，需要结合执行计划判断。

---

## 10. 减少回表

### 什么是回表

二级索引中只存储索引字段和主键 ID，如果查询字段不在索引中，需要再根据主键回到聚簇索引查询完整数据。

### 优化方式

使用覆盖索引。

不推荐：

```sql
SELECT * FROM user WHERE phone = '13800138000';
```

推荐：

```sql
SELECT id, phone FROM user WHERE phone = '13800138000';
```

索引：

```sql
CREATE INDEX idx_phone ON user(phone);
```

---

## 11. 避免大事务

### 大事务问题

- 长时间持有锁。
- 阻塞其他 SQL。
- 回滚成本高。
- undo log 膨胀。
- 影响主从同步。

### 优化方式

- 控制事务范围。
- 避免事务中调用远程接口。
- 大批量更新分批执行。
- 尽快提交事务。

不推荐：

```text
开启事务
  ↓
查询数据
  ↓
调用远程接口
  ↓
更新数据库
  ↓
提交事务
```

推荐：

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

## 12. 大批量数据处理优化

### 不推荐一次性更新大量数据

```sql
UPDATE user SET status = 0 WHERE create_time < '2025-01-01';
```

### 推荐分批处理

```sql
UPDATE user 
SET status = 0 
WHERE id > 0 
ORDER BY id 
LIMIT 1000;
```

循环执行，减少锁持有时间和数据库压力。

---

## 13. 表结构优化

### 常见方式

- 字段类型尽量小。
- 能用 `INT` 不用 `BIGINT`。
- 能用 `VARCHAR(64)` 不用 `VARCHAR(255)`。
- 避免过多 `TEXT`、`BLOB` 字段。
- 冷热字段分离。
- 大表按时间归档。
- 合理使用冗余字段减少 Join。

### 示例

用户基础信息和扩展信息拆分：

```text
user_base：id、name、phone
user_extend：user_id、avatar、profile、remark
```

---

## 14. 架构层面优化

当 SQL 和索引优化后仍然无法满足性能要求，可以考虑架构优化。

| 方案 | 作用 |
|---|---|
| Redis 缓存 | 减少数据库读压力 |
| 读写分离 | 提升读能力 |
| 分库分表 | 解决单库单表瓶颈 |
| 数据归档 | 减少热表数据量 |
| Elasticsearch | 承接复杂搜索 |
| ClickHouse | 承接统计分析 |
| MQ 异步化 | 削峰填谷，减少同步写压力 |

---

## 15. SQL 调优常见手段对比

| 问题 | 优化方式 |
|---|---|
| 全表扫描 | 建索引、改写 SQL |
| 索引失效 | 避免函数、计算、隐式转换、左模糊 |
| 扫描行数多 | 增加高区分度联合索引 |
| 回表多 | 使用覆盖索引，避免 `SELECT *` |
| 排序慢 | 建立符合 `WHERE + ORDER BY` 的索引 |
| 分组慢 | 建立符合 `WHERE + GROUP BY` 的索引 |
| 深分页慢 | 使用游标分页 |
| Join 慢 | 关联字段建索引，小表驱动大表 |
| 锁等待 | 缩短事务，减少热点更新 |
| 数据量大 | 归档、分库分表 |
| 读压力大 | 缓存、读写分离 |
| 写压力大 | 分库分表、批量写、异步化 |

---

## 16. 总结

SQL 调优一般从执行计划入手，先用 `EXPLAIN` 分析 SQL 是否命中索引，重点关注 `type`、`key`、`rows` 和 `Extra`。

常见优化方向包括：合理设计索引、避免索引失效、减少扫描行数、避免 `SELECT *`、使用覆盖索引、优化排序和分组、避免深分页、优化 Join、减少大事务和锁等待。

如果单条 SQL 优化后仍然无法满足性能要求，可以从架构层面优化，例如使用 Redis 缓存、读写分离、数据归档、分库分表、Elasticsearch 或 MQ 异步化。

一句话总结：**SQL 调优的核心是让数据库少扫数据、少回表、少排序、少加锁。**
