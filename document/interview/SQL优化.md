# MySQL 慢 SQL 与大表优化常见面试题总结

## 目录

- [一、慢 SQL 如何排查](#一慢-sql-如何排查)
- [二、如何优化分页查询](#二如何优化分页查询)
- [三、如何优化 ORDER BY](#三如何优化-order-by)
- [四、如何优化 GROUP BY](#四如何优化-group-by)
- [五、大表如何优化](#五大表如何优化)
- [六、索引建太多有什么问题](#六索引建太多有什么问题)
- [七、面试速记版](#七面试速记版)
- [八、总览表](#八总览表)
- [九、完整面试回答模板](#九完整面试回答模板)

---

# 一、慢 SQL 如何排查

## 1. 什么是慢 SQL？

慢 SQL 是指执行时间较长、消耗资源较多、影响数据库性能的 SQL。

常见表现：

```text
接口响应慢
数据库 CPU 飙高
磁盘 IO 高
连接数被打满
锁等待严重
慢查询日志大量增长
```

---

## 2. 慢 SQL 常见原因

慢 SQL 常见原因包括：

```text
1. 没有合适索引
2. 索引失效
3. 查询返回数据量太大
4. 使用 SELECT *
5. 深分页
6. ORDER BY 没有利用索引
7. GROUP BY 使用临时表或文件排序
8. JOIN 字段没有索引
9. 大事务导致锁等待
10. 表数据量过大
11. SQL 写法不合理
12. 数据库资源不足
```

---

## 3. 慢 SQL 排查整体流程

排查慢 SQL 可以按照以下流程：

```text
1. 发现慢 SQL
2. 定位具体 SQL
3. 查看执行计划
4. 分析索引使用情况
5. 分析扫描行数和返回行数
6. 分析是否存在排序、临时表、回表
7. 分析锁等待和事务情况
8. 优化 SQL 或索引
9. 压测验证
10. 上线观察
```

---

## 4. 开启慢查询日志

可以通过慢查询日志定位执行时间较长的 SQL。

查看是否开启：

```sql
SHOW VARIABLES LIKE 'slow_query_log';
```

开启慢查询日志：

```sql
SET GLOBAL slow_query_log = ON;
```

查看慢查询阈值：

```sql
SHOW VARIABLES LIKE 'long_query_time';
```

设置慢查询阈值：

```sql
SET GLOBAL long_query_time = 1;
```

表示执行时间超过 1 秒的 SQL 会被记录。

---

## 5. 查看慢查询日志文件位置

```sql
SHOW VARIABLES LIKE 'slow_query_log_file';
```

---

## 6. 记录未使用索引的 SQL

可以开启：

```sql
SET GLOBAL log_queries_not_using_indexes = ON;
```

注意：

```text
生产环境开启前要谨慎，因为可能产生大量日志。
```

---

## 7. 使用 mysqldumpslow 分析慢日志

MySQL 提供了 `mysqldumpslow` 工具，可以对慢查询日志进行汇总分析。

示例：

```bash
mysqldumpslow -s t -t 10 /var/lib/mysql/slow.log
```

含义：

```text
-s t：按总执行时间排序
-t 10：取前 10 条
```

常见参数：

| 参数 | 说明 |
|---|---|
| `-s t` | 按总执行时间排序 |
| `-s at` | 按平均执行时间排序 |
| `-s c` | 按执行次数排序 |
| `-s l` | 按锁等待时间排序 |
| `-t N` | 显示前 N 条 |

---

## 8. 使用 pt-query-digest

生产环境常用 Percona Toolkit 的 `pt-query-digest` 分析慢日志。

示例：

```bash
pt-query-digest /var/lib/mysql/slow.log > slow_report.txt
```

它可以分析：

```text
SQL 执行次数
平均耗时
最大耗时
总耗时
扫描行数
返回行数
锁等待时间
SQL 指纹
```

---

## 9. 使用 EXPLAIN 查看执行计划

示例：

```sql
EXPLAIN SELECT * FROM user WHERE name = 'Tom';
```

重点关注：

```text
type
possible_keys
key
key_len
rows
filtered
Extra
```

---

## 10. EXPLAIN 重点字段

### 10.1 type

`type` 表示访问类型，性能从好到差大致为：

```text
system > const > eq_ref > ref > range > index > ALL
```

如果出现：

```text
ALL
```

通常表示全表扫描，需要重点关注。

---

### 10.2 key

`key` 表示实际使用的索引。

如果：

```text
key = NULL
```

说明没有使用索引。

---

### 10.3 rows

`rows` 表示预计扫描行数。

一般来说：

```text
rows 越小越好。
```

---

### 10.4 Extra

`Extra` 中需要重点关注：

| Extra | 含义 |
|---|---|
| `Using index` | 使用覆盖索引 |
| `Using where` | 使用 WHERE 条件过滤 |
| `Using filesort` | 使用额外排序 |
| `Using temporary` | 使用临时表 |
| `Using index condition` | 使用索引条件下推 |

---

## 11. 使用 EXPLAIN ANALYZE

MySQL 8.0.18+ 支持：

```sql
EXPLAIN ANALYZE
SELECT * FROM user WHERE name = 'Tom';
```

它会实际执行 SQL，并返回实际耗时和实际扫描行数。

注意：

```text
EXPLAIN ANALYZE 会真正执行 SQL，生产环境要谨慎使用。
```

---

## 12. 慢 SQL 优化方向

常见优化方向：

```text
1. 给 WHERE 条件字段加合适索引
2. 使用联合索引
3. 尽量覆盖索引，减少回表
4. 避免 SELECT *
5. 避免函数、计算、隐式转换导致索引失效
6. 优化深分页
7. 优化 ORDER BY 和 GROUP BY
8. 减少返回数据量
9. 拆分复杂 SQL
10. 优化 JOIN
11. 避免大事务
12. 分库分表或归档历史数据
```

---

## 13. 慢 SQL 排查示例

慢 SQL：

```sql
SELECT *
FROM orders
WHERE user_id = 1001
ORDER BY create_time DESC
LIMIT 20;
```

分析：

```text
1. WHERE 条件是 user_id
2. ORDER BY 字段是 create_time
3. LIMIT 20 只需要前 20 条
```

可以建立联合索引：

```sql
CREATE INDEX idx_user_create_time ON orders(user_id, create_time DESC);
```

优化点：

```text
先按 user_id 过滤；
再利用 create_time 的索引顺序排序；
避免 filesort；
减少扫描行数。
```

---

## 14. 面试回答

慢 SQL 排查一般先通过慢查询日志定位具体 SQL，也可以结合监控、APM 或数据库性能视图发现问题。定位 SQL 后，使用 `EXPLAIN` 或 `EXPLAIN ANALYZE` 查看执行计划，重点看是否走索引、访问类型是否为 `ALL`、扫描行数是否过大、是否出现 `Using filesort` 或 `Using temporary`。

常见优化方向包括建立合适索引、优化联合索引顺序、避免索引失效、减少返回字段、避免 `SELECT *`、优化深分页、优化排序和分组、减少回表、拆分复杂 SQL，以及在数据量过大时进行归档、分区或分库分表。

---

# 二、如何优化分页查询

## 1. 普通分页写法

常见分页 SQL：

```sql
SELECT *
FROM orders
ORDER BY id
LIMIT 100, 20;
```

含义：

```text
跳过前 100 条，取 20 条。
```

---

## 2. 深分页问题

当偏移量很大时，例如：

```sql
SELECT *
FROM orders
ORDER BY id
LIMIT 1000000, 20;
```

MySQL 需要扫描：

```text
1000000 + 20
```

条记录，然后丢弃前 1000000 条，只返回 20 条。

这会导致：

```text
扫描行数大
排序成本高
回表次数多
响应时间长
数据库压力大
```

---

## 3. 优化方式一：使用延迟关联

原 SQL：

```sql
SELECT *
FROM orders
ORDER BY id
LIMIT 1000000, 20;
```

优化为：

```sql
SELECT o.*
FROM orders o
JOIN (
    SELECT id
    FROM orders
    ORDER BY id
    LIMIT 1000000, 20
) t ON o.id = t.id;
```

优化原理：

```text
子查询只扫描索引中的 id；
先找到需要的主键 id；
再回表查询完整数据；
减少大量无效回表。
```

---

## 4. 优化方式二：基于上一次最大 ID 翻页

如果是按照主键递增分页，可以使用游标分页。

第一页：

```sql
SELECT *
FROM orders
WHERE id > 0
ORDER BY id
LIMIT 20;
```

下一页：

```sql
SELECT *
FROM orders
WHERE id > 上一页最后一条记录的id
ORDER BY id
LIMIT 20;
```

例如：

```sql
SELECT *
FROM orders
WHERE id > 1000000
ORDER BY id
LIMIT 20;
```

优点：

```text
不需要跳过大量数据；
可以直接从索引位置向后扫描；
性能稳定。
```

缺点：

```text
不适合直接跳到第 N 页。
```

---

## 5. 优化方式三：使用业务游标

如果排序字段不是主键，例如按时间排序：

```sql
SELECT *
FROM orders
WHERE create_time < '2026-04-01 10:00:00'
ORDER BY create_time DESC
LIMIT 20;
```

为了避免时间重复导致分页错乱，可以加上主键：

```sql
SELECT *
FROM orders
WHERE 
    create_time < '2026-04-01 10:00:00'
    OR (
        create_time = '2026-04-01 10:00:00'
        AND id < 1000000
    )
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

对应索引：

```sql
CREATE INDEX idx_create_time_id ON orders(create_time DESC, id DESC);
```

---

## 6. 优化方式四：限制最大翻页深度

很多业务没有必要支持无限深分页。

可以限制：

```text
最多查询前 100 页
最多查询前 5000 条
超过后提示缩小筛选条件
```

适合场景：

```text
后台列表
搜索结果页
运营查询页
日志查询页
```

---

## 7. 优化方式五：增加筛选条件

深分页慢的本质是扫描范围太大。

可以增加条件缩小范围：

```sql
SELECT *
FROM orders
WHERE user_id = 1001
  AND create_time >= '2026-04-01'
ORDER BY create_time DESC
LIMIT 1000, 20;
```

配合索引：

```sql
CREATE INDEX idx_user_time ON orders(user_id, create_time DESC);
```

---

## 8. 优化方式六：使用覆盖索引

如果页面只需要少量字段，可以建立覆盖索引。

示例：

```sql
SELECT id, order_no, create_time
FROM orders
WHERE user_id = 1001
ORDER BY create_time DESC
LIMIT 1000, 20;
```

索引：

```sql
CREATE INDEX idx_user_time_order 
ON orders(user_id, create_time DESC, order_no);
```

如果查询字段都在索引中，就可以减少回表。

---

## 9. 优化方式七：冷热数据分离

对于历史数据很多的表，可以将冷数据归档。

例如：

```text
近 3 个月订单放在线上表
3 个月以前订单放历史表
```

这样分页查询主要查热数据表，扫描范围更小。

---

## 10. 分页优化方案对比

| 方案 | 优点 | 缺点 |
|---|---|---|
| 延迟关联 | 减少回表 | 仍然要扫描 offset |
| 游标分页 | 性能稳定 | 不支持任意跳页 |
| 业务游标 | 适合时间流分页 | SQL 较复杂 |
| 限制页数 | 简单有效 | 产品体验有限制 |
| 增加筛选条件 | 减少扫描范围 | 依赖业务条件 |
| 覆盖索引 | 减少回表 | 索引维护成本增加 |
| 冷热分离 | 降低数据量 | 架构复杂度增加 |

---

## 11. 面试回答

分页查询慢通常是因为深分页时 MySQL 需要扫描 `offset + size` 条数据，然后丢弃前面的 offset 条，只返回少量数据。

常见优化方式包括：使用延迟关联，先通过覆盖索引查出主键，再回表查完整数据；使用游标分页，也就是根据上一页最后一条记录的 id 或时间继续向后查；限制最大翻页深度；增加筛选条件缩小扫描范围；通过覆盖索引减少回表；对于大表可以做冷热数据分离或历史数据归档。

---

# 三、如何优化 ORDER BY

## 1. ORDER BY 为什么会慢？

`ORDER BY` 慢的常见原因：

```text
1. 排序字段没有索引
2. 排序字段不符合联合索引顺序
3. 返回数据量太大
4. 需要 filesort
5. 需要临时表
6. 排序字段和过滤字段索引不匹配
7. SELECT * 导致回表成本高
```

---

## 2. ORDER BY 的两种常见方式

MySQL 排序常见有两种方式：

```text
1. 使用索引顺序返回结果
2. 使用 filesort 额外排序
```

---

## 3. 使用索引排序

如果 `ORDER BY` 字段有合适索引，MySQL 可以直接按照索引顺序读取数据。

示例：

```sql
SELECT *
FROM orders
WHERE user_id = 1001
ORDER BY create_time DESC
LIMIT 20;
```

推荐索引：

```sql
CREATE INDEX idx_user_create_time 
ON orders(user_id, create_time DESC);
```

这样可以：

```text
先通过 user_id 定位数据；
再按 create_time 的索引顺序返回；
避免额外排序。
```

---

## 4. filesort 是什么？

如果不能利用索引顺序完成排序，MySQL 可能使用：

```text
Using filesort
```

`filesort` 并不一定表示磁盘排序，它表示 MySQL 需要额外排序。

如果排序数据量较大，可能使用磁盘临时文件，性能会明显下降。

---

## 5. ORDER BY 优化核心原则

优化 `ORDER BY` 的核心是：

```text
让排序字段尽量走索引顺序。
```

常见原则：

```text
1. WHERE 等值字段放在联合索引前面
2. ORDER BY 字段放在联合索引后面
3. 排序方向尽量保持一致
4. 避免对排序字段使用函数
5. 避免返回过多字段
6. 配合 LIMIT 使用
```

---

## 6. 联合索引优化 ORDER BY

SQL：

```sql
SELECT *
FROM orders
WHERE user_id = 1001
ORDER BY create_time DESC
LIMIT 20;
```

适合索引：

```sql
CREATE INDEX idx_user_time ON orders(user_id, create_time DESC);
```

---

## 7. 多字段排序

SQL：

```sql
SELECT *
FROM orders
WHERE user_id = 1001
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

适合索引：

```sql
CREATE INDEX idx_user_time_id 
ON orders(user_id, create_time DESC, id DESC);
```

---

## 8. 排序方向要一致

索引：

```sql
CREATE INDEX idx_time_id ON orders(create_time DESC, id DESC);
```

可以较好支持：

```sql
ORDER BY create_time DESC, id DESC
```

或者反向扫描支持：

```sql
ORDER BY create_time ASC, id ASC
```

但不适合：

```sql
ORDER BY create_time DESC, id ASC
```

因为排序方向混合，可能无法充分利用索引顺序。

---

## 9. WHERE 范围查询对 ORDER BY 的影响

索引：

```sql
CREATE INDEX idx_user_time ON orders(user_id, create_time);
```

SQL：

```sql
SELECT *
FROM orders
WHERE user_id > 1000
ORDER BY create_time;
```

由于 `user_id` 是范围查询，后面的 `create_time` 通常难以继续利用索引顺序进行全局排序。

更适合的情况是：

```sql
SELECT *
FROM orders
WHERE user_id = 1001
ORDER BY create_time;
```

---

## 10. 避免排序字段使用函数

错误写法：

```sql
SELECT *
FROM orders
ORDER BY DATE(create_time);
```

优化方式：

```sql
SELECT *
FROM orders
ORDER BY create_time;
```

或者通过生成列、函数索引等方式优化。

---

## 11. 避免 SELECT *

如果查询：

```sql
SELECT *
FROM orders
WHERE user_id = 1001
ORDER BY create_time DESC
LIMIT 20;
```

可能需要回表。

如果页面只需要部分字段，可以改成：

```sql
SELECT id, order_no, create_time
FROM orders
WHERE user_id = 1001
ORDER BY create_time DESC
LIMIT 20;
```

配合覆盖索引：

```sql
CREATE INDEX idx_user_time_order 
ON orders(user_id, create_time DESC, order_no);
```

---

## 12. EXPLAIN 怎么判断 ORDER BY 是否优化好？

重点看 `Extra`：

```text
Using filesort
```

如果出现 `Using filesort`，说明没有完全利用索引顺序排序。

理想情况：

```text
没有 Using filesort
```

如果还有：

```text
Using index
```

说明可能使用了覆盖索引，效果更好。

---

## 13. ORDER BY 优化示例

优化前：

```sql
SELECT *
FROM orders
WHERE status = 1
ORDER BY create_time DESC
LIMIT 20;
```

如果只有 `status` 索引，可能还需要 filesort。

优化索引：

```sql
CREATE INDEX idx_status_time 
ON orders(status, create_time DESC);
```

优化后：

```text
先按 status 过滤；
再按 create_time 索引顺序读取；
减少排序成本。
```

---

## 14. 面试回答

优化 `ORDER BY` 的核心是让排序尽量利用索引顺序，避免 `Using filesort`。

常见做法是根据 SQL 建立合适的联合索引，把等值过滤字段放在前面，把排序字段放在后面。例如 `WHERE user_id = ? ORDER BY create_time DESC` 可以建立 `(user_id, create_time)` 联合索引。

同时要注意排序字段顺序和方向要和索引一致，避免对排序字段使用函数，避免返回过多字段，并配合 `LIMIT` 减少排序数据量。通过 `EXPLAIN` 查看是否出现 `Using filesort` 来判断优化效果。

---

# 四、如何优化 GROUP BY

## 1. GROUP BY 为什么会慢？

`GROUP BY` 慢的常见原因：

```text
1. 分组字段没有索引
2. 分组前过滤条件不充分
3. 分组数据量太大
4. 使用临时表
5. 使用 filesort
6. 聚合计算成本高
7. 分组字段区分度过高
8. 返回结果集过大
```

---

## 2. GROUP BY 的核心优化思路

优化 `GROUP BY` 的核心是：

```text
减少参与分组的数据量，并尽量利用索引完成分组。
```

---

## 3. 先过滤，再分组

优化前：

```sql
SELECT user_id, COUNT(*)
FROM orders
GROUP BY user_id;
```

如果 `orders` 很大，会对全表分组。

优化后：

```sql
SELECT user_id, COUNT(*)
FROM orders
WHERE create_time >= '2026-04-01'
GROUP BY user_id;
```

通过 `WHERE` 先减少参与分组的数据量。

---

## 4. 给 GROUP BY 字段建立索引

SQL：

```sql
SELECT user_id, COUNT(*)
FROM orders
WHERE status = 1
GROUP BY user_id;
```

可以考虑索引：

```sql
CREATE INDEX idx_status_user ON orders(status, user_id);
```

作用：

```text
先按 status 过滤；
再按 user_id 分组；
减少临时表和排序成本。
```

---

## 5. GROUP BY 和联合索引

如果 SQL：

```sql
SELECT user_id, status, COUNT(*)
FROM orders
GROUP BY user_id, status;
```

可以建立索引：

```sql
CREATE INDEX idx_user_status ON orders(user_id, status);
```

MySQL 可以利用索引中已经排序的顺序进行分组。

---

## 6. 避免不必要字段

错误示例：

```sql
SELECT *
FROM orders
GROUP BY user_id;
```

这种写法不仅语义不清晰，还可能导致额外开销。

推荐写法：

```sql
SELECT user_id, COUNT(*)
FROM orders
GROUP BY user_id;
```

---

## 7. 使用覆盖索引

SQL：

```sql
SELECT user_id, COUNT(*)
FROM orders
WHERE status = 1
GROUP BY user_id;
```

索引：

```sql
CREATE INDEX idx_status_user ON orders(status, user_id);
```

如果查询只需要 `status`、`user_id` 和聚合结果，那么可以尽量减少回表。

---

## 8. 避免 GROUP BY 后再大量排序

例如：

```sql
SELECT user_id, COUNT(*) AS cnt
FROM orders
GROUP BY user_id
ORDER BY cnt DESC;
```

这种根据聚合结果排序，通常难以直接利用普通索引。

优化思路：

```text
减少参与分组的数据量；
限制返回条数；
使用汇总表；
使用缓存；
离线统计。
```

---

## 9. 使用汇总表

对于频繁统计的场景，可以建立汇总表。

例如订单统计：

```sql
CREATE TABLE user_order_stats (
    user_id BIGINT PRIMARY KEY,
    order_count BIGINT,
    total_amount DECIMAL(10,2),
    update_time DATETIME
);
```

查询时：

```sql
SELECT *
FROM user_order_stats
ORDER BY order_count DESC
LIMIT 20;
```

适合场景：

```text
报表统计
排行榜
运营看板
大屏数据
高频聚合查询
```

---

## 10. 分批聚合

对于大数据量统计，可以分批处理：

```text
按时间分批
按 id 范围分批
按分区分批
离线任务预聚合
```

---

## 11. EXPLAIN 怎么看 GROUP BY 是否有问题？

重点看 `Extra`：

```text
Using temporary
Using filesort
```

如果出现这两个，说明可能使用了临时表或额外排序。

需要考虑：

```text
是否可以加索引；
是否可以先过滤；
是否可以使用汇总表；
是否可以减少字段；
是否可以拆分 SQL。
```

---

## 12. GROUP BY 优化示例

优化前：

```sql
SELECT user_id, COUNT(*)
FROM orders
WHERE status = 1
GROUP BY user_id;
```

可能全表扫描或临时表分组。

优化索引：

```sql
CREATE INDEX idx_status_user ON orders(status, user_id);
```

优化后：

```text
利用 status 过滤；
利用 user_id 的索引顺序分组；
减少扫描和排序成本。
```

---

## 13. 面试回答

优化 `GROUP BY` 的核心是减少参与分组的数据量，并尽量让分组字段走索引。

常见做法包括：先通过 `WHERE` 条件过滤数据，再分组；给过滤字段和分组字段建立联合索引；只查询必要字段；使用覆盖索引减少回表；避免对大量数据进行实时聚合；对于高频统计场景，可以使用汇总表、缓存或离线计算。

通过 `EXPLAIN` 查看是否出现 `Using temporary` 和 `Using filesort`，如果出现，通常说明还有优化空间。

---

# 五、大表如何优化

## 1. 什么是大表？

大表没有绝对标准。

通常可以从以下角度判断：

```text
单表数据量千万级以上
单表容量几十 GB 以上
查询明显变慢
索引膨胀严重
DDL 变更耗时很长
备份恢复困难
```

---

## 2. 大表常见问题

大表常见问题包括：

```text
1. 查询慢
2. 索引大
3. 写入慢
4. DDL 慢
5. 备份恢复慢
6. 删除历史数据慢
7. 主从同步延迟
8. Buffer Pool 命中率下降
9. 磁盘 IO 压力大
10. 单表维护困难
```

---

## 3. 大表优化整体思路

大表优化可以从多个层面入手：

```text
1. SQL 优化
2. 索引优化
3. 字段设计优化
4. 归档历史数据
5. 分区表
6. 分库分表
7. 读写分离
8. 缓存
9. 汇总表
10. 异步化
11. 硬件和参数优化
```

---

## 4. SQL 优化

大表查询时要避免：

```text
SELECT *
无索引查询
深分页
大范围扫描
大事务
全表 ORDER BY
全表 GROUP BY
复杂子查询
低效 JOIN
```

建议：

```text
只查必要字段
通过索引过滤
限制返回条数
拆分复杂 SQL
使用覆盖索引
避免深分页
```

---

## 5. 索引优化

大表索引设计非常关键。

建议：

```text
1. 高频查询字段建索引
2. WHERE + ORDER BY 建联合索引
3. WHERE + GROUP BY 建联合索引
4. JOIN 字段建索引
5. 尽量使用覆盖索引
6. 删除无用索引
7. 避免重复索引
8. 避免低区分度字段单独建索引
```

---

## 6. 字段设计优化

字段设计会影响表大小和索引大小。

建议：

```text
1. 使用合适的数据类型
2. 能用 int 不用 bigint，前提是容量足够
3. 能用 varchar 不用 text
4. 字段尽量 NOT NULL
5. 避免超长字段放主表
6. 大字段拆到扩展表
7. 使用短小主键
```

---

## 7. 冷热数据分离

如果一张表中历史数据很多，但日常只查询近期数据，可以做冷热分离。

例如订单表：

```text
orders：保存近 3 个月数据
orders_history：保存 3 个月以前数据
```

优点：

```text
减少热表数据量
提高查询效率
降低索引大小
方便历史数据归档
```

---

## 8. 分区表

MySQL 分区表可以把一张逻辑表的数据按规则拆成多个物理分区。

常见分区方式：

```text
按时间分区
按范围分区
按 HASH 分区
按 LIST 分区
```

示例：

```sql
CREATE TABLE orders (
    id BIGINT NOT NULL,
    create_time DATE NOT NULL,
    amount DECIMAL(10,2),
    PRIMARY KEY(id, create_time)
)
PARTITION BY RANGE COLUMNS(create_time) (
    PARTITION p202401 VALUES LESS THAN ('2024-02-01'),
    PARTITION p202402 VALUES LESS THAN ('2024-03-01'),
    PARTITION pmax VALUES LESS THAN (MAXVALUE)
);
```

适合场景：

```text
按时间查询
按时间归档
按时间删除历史数据
```

注意：

```text
分区表不是万能的，如果查询条件不包含分区键，仍然可能扫描多个分区。
```

---

## 9. 分库分表

当单表数据量和写入压力都很大时，可以考虑分库分表。

常见拆分方式：

```text
水平分表
垂直分表
水平分库
垂直分库
```

---

### 9.1 水平分表

按照某个分片键把数据拆到多张结构相同的表中。

例如：

```text
orders_00
orders_01
orders_02
...
orders_15
```

按 `user_id` 取模：

```text
table_index = user_id % 16
```

---

### 9.2 垂直分表

把一张宽表拆成主表和扩展表。

例如：

```text
user：id、name、phone
user_ext：user_id、address、profile、remark
```

适合大字段、低频字段拆分。

---

### 9.3 水平分库

将数据分散到多个数据库实例中。

例如：

```text
db_00.orders_00
db_01.orders_00
db_02.orders_00
```

适合单库写入压力大、容量瓶颈明显的场景。

---

## 10. 读写分离

如果读多写少，可以使用主从复制做读写分离。

```text
写请求 -> 主库
读请求 -> 从库
```

优点：

```text
分摅读压力
提高查询能力
```

注意：

```text
主从延迟可能导致读到旧数据。
```

---

## 11. 缓存优化

对于热点数据，可以引入 Redis 缓存。

适合场景：

```text
热点商品
用户信息
系统配置
排行榜
字典数据
```

注意问题：

```text
缓存穿透
缓存击穿
缓存雪崩
缓存一致性
```

---

## 12. 汇总表和冗余字段

对于复杂统计，可以使用汇总表。

例如：

```text
每天订单数
用户订单总数
商品销量
账户余额快照
```

避免每次都扫大表聚合。

---

## 13. 历史数据归档

对于历史数据，可以定期归档到：

```text
历史表
归档库
数据仓库
对象存储
ES
ClickHouse
Hive
```

归档后，在线库只保留热数据。

---

## 14. 大表删除优化

不要一次性删除大量数据：

```sql
DELETE FROM orders WHERE create_time < '2024-01-01';
```

这样可能导致：

```text
大事务
锁等待
undo log 暴涨
主从延迟
磁盘 IO 抖动
```

建议分批删除：

```sql
DELETE FROM orders
WHERE create_time < '2024-01-01'
LIMIT 1000;
```

循环执行，并控制频率。

---

## 15. DDL 优化

大表执行 DDL 要谨慎。

例如：

```sql
ALTER TABLE orders ADD INDEX idx_user_id(user_id);
```

可能导致：

```text
耗时长
锁表
主从延迟
磁盘 IO 高
业务抖动
```

建议：

```text
低峰期执行
使用 online DDL
使用 pt-online-schema-change
使用 gh-ost
灰度执行
提前评估磁盘空间
```

---

## 16. 大表优化优先级

一般优化优先级：

```text
1. 先优化 SQL 和索引
2. 再做冷热数据归档
3. 再考虑分区表
4. 最后考虑分库分表
```

原因：

```text
分库分表会显著增加系统复杂度。
```

---

## 17. 面试回答

大表优化首先要看瓶颈在哪里。如果是查询慢，优先优化 SQL 和索引，比如避免全表扫描、避免深分页、避免 `SELECT *`、建立合适联合索引和覆盖索引。如果是历史数据过多，可以做冷热数据分离和归档。如果查询明显按时间范围，可以考虑分区表。

当单表数据量、写入压力或单库容量达到瓶颈时，可以考虑分库分表。分库分表能提升容量和并发能力，但会带来分布式事务、跨分片查询、全局 ID、分页排序、运维复杂度等问题，因此通常是最后手段。

---

# 六、索引建太多有什么问题

## 1. 索引是不是越多越好？

不是。

索引可以提高查询效率，但会降低写入效率，并增加存储和维护成本。

---

## 2. 索引太多的主要问题

索引太多会带来以下问题：

```text
1. 占用更多磁盘空间
2. 降低 INSERT 性能
3. 降低 UPDATE 性能
4. 降低 DELETE 性能
5. 增加优化器选择成本
6. 增加 Buffer Pool 压力
7. 增加主从复制压力
8. 增加 DDL 维护成本
9. 可能产生重复索引和冗余索引
```

---

## 3. 占用磁盘空间

每个索引本质上都是一棵 B+ 树。

索引越多，磁盘占用越大。

尤其是大表上多个联合索引，会显著增加存储空间。

---

## 4. 降低写入性能

执行：

```sql
INSERT INTO user(name, age, phone) VALUES('Tom', 18, '13800138000');
```

不仅要写入数据行，还要更新相关索引树。

如果一张表有很多索引：

```text
每插入一行，都要维护多个索引。
```

写入性能会下降。

---

## 5. 降低更新性能

如果更新了索引字段：

```sql
UPDATE user SET phone = '13900139000' WHERE id = 1;
```

MySQL 需要：

```text
修改数据行；
删除旧索引记录；
插入新索引记录。
```

索引越多，维护成本越高。

---

## 6. 降低删除性能

删除数据时：

```sql
DELETE FROM user WHERE id = 1;
```

不仅要删除数据，也要删除或标记删除对应索引记录。

索引越多，删除成本越高。

---

## 7. 增加优化器选择成本

MySQL 优化器执行 SQL 前，需要在多个可能的索引中选择成本最低的执行计划。

索引过多会增加优化器评估成本，也可能导致优化器选错索引。

---

## 8. 增加 Buffer Pool 压力

InnoDB 会把数据页和索引页缓存到 Buffer Pool。

索引越多，索引页越多，会占用更多 Buffer Pool。

可能导致：

```text
热点数据页被挤出
缓存命中率下降
磁盘 IO 增加
```

---

## 9. 增加主从同步压力

主库写入时维护索引会增加执行成本。

从库回放 binlog 时也需要执行同样的数据变更和索引维护。

索引太多可能导致：

```text
主从延迟增加
从库回放变慢
```

---

## 10. 增加 DDL 成本

大表加索引、删索引、改字段时，索引越多，DDL 维护成本越高。

可能出现：

```text
DDL 执行时间长
磁盘空间不足
主从延迟
业务抖动
```

---

## 11. 重复索引

重复索引是指完全相同字段顺序的索引。

例如：

```sql
CREATE INDEX idx_name ON user(name);
CREATE INDEX idx_name_2 ON user(name);
```

这两个索引重复，应该删除一个。

---

## 12. 冗余索引

冗余索引是指某个索引可以被另一个联合索引覆盖。

例如：

```sql
CREATE INDEX idx_name ON user(name);
CREATE INDEX idx_name_age ON user(name, age);
```

在很多情况下，`idx_name` 可以被 `idx_name_age` 覆盖。

因为联合索引 `(name, age)` 的最左前缀包含 `name`。

但是否删除单列索引，需要结合实际 SQL 和执行计划判断。

---

## 13. 如何判断索引是否无用？

可以从以下角度分析：

```text
1. 是否长期没有被使用
2. 是否和其他索引重复
3. 是否可以被联合索引覆盖
4. 区分度是否太低
5. 是否影响写入性能
6. 是否占用大量空间
7. 是否对核心 SQL 有帮助
```

---

## 14. 查看表索引

```sql
SHOW INDEX FROM user;
```

---

## 15. 查看索引大小

可以查询 `information_schema`：

```sql
SELECT 
    table_schema,
    table_name,
    index_length,
    data_length
FROM information_schema.tables
WHERE table_schema = 'your_database'
  AND table_name = 'your_table';
```

---

## 16. 索引设计原则

合理索引设计原则：

```text
1. 为高频查询建立索引
2. 为高选择性字段建立索引
3. 优先考虑联合索引
4. 遵循最左前缀原则
5. 尽量使用覆盖索引
6. 避免重复索引
7. 避免冗余索引
8. 控制索引数量
9. 定期评估索引使用情况
10. 写多读少的表谨慎加索引
```

---

## 17. 面试回答

索引不是越多越好。索引太多会占用更多磁盘空间，并且每次插入、更新、删除数据时都需要维护多个索引结构，从而降低写入性能。

此外，索引过多还会增加优化器选择执行计划的成本，占用更多 Buffer Pool，降低缓存命中率，增加主从同步压力和 DDL 维护成本。实际项目中要避免重复索引和冗余索引，根据核心查询场景建立必要索引，并定期清理无用索引。

---

# 七、面试速记版

## 1. 慢 SQL 如何排查？

慢 SQL 排查流程：

```text
开启慢查询日志
定位具体 SQL
使用 EXPLAIN 查看执行计划
看 type、key、rows、Extra
分析是否索引失效
分析是否回表、filesort、temporary
优化 SQL 和索引
压测验证
```

重点关注：

```text
type = ALL
key = NULL
rows 很大
Using filesort
Using temporary
```

---

## 2. 如何优化分页查询？

分页慢主要是深分页导致扫描大量数据。

优化方式：

```text
延迟关联
游标分页
基于上一次最大 id 查询
增加筛选条件
限制最大页数
覆盖索引
冷热数据分离
```

---

## 3. 如何优化 ORDER BY？

核心是让排序使用索引顺序，避免 `Using filesort`。

常见做法：

```text
WHERE 等值字段 + ORDER BY 字段建立联合索引
排序字段顺序和方向与索引一致
避免排序字段使用函数
减少返回字段
配合 LIMIT
```

---

## 4. 如何优化 GROUP BY？

核心是减少参与分组的数据量，并尽量利用索引分组。

常见做法：

```text
先 WHERE 过滤再 GROUP BY
过滤字段 + 分组字段建立联合索引
只查询必要字段
使用覆盖索引
高频统计使用汇总表
大数据统计走离线计算
```

---

## 5. 大表如何优化？

大表优化方向：

```text
SQL 优化
索引优化
字段设计优化
冷热数据分离
历史数据归档
分区表
读写分离
缓存
汇总表
分库分表
```

优先级：

```text
先 SQL 和索引
再归档和分区
最后考虑分库分表
```

---

## 6. 索引建太多有什么问题？

索引太多的问题：

```text
占用磁盘空间
降低 INSERT 性能
降低 UPDATE 性能
降低 DELETE 性能
增加优化器成本
增加 Buffer Pool 压力
增加主从延迟
增加 DDL 成本
产生重复索引和冗余索引
```

---

# 八、总览表

| 问题 | 核心结论 |
|---|---|
| 慢 SQL 如何排查 | 慢日志定位，EXPLAIN 分析，重点看索引、扫描行数、排序和临时表 |
| 如何优化分页查询 | 避免深分页，使用延迟关联、游标分页、覆盖索引 |
| 如何优化 ORDER BY | 建立匹配排序的联合索引，避免 filesort |
| 如何优化 GROUP BY | 先过滤再分组，利用联合索引，必要时使用汇总表 |
| 大表如何优化 | SQL、索引、归档、分区、缓存、读写分离、分库分表 |
| 索引建太多有什么问题 | 占空间，拖慢写入，增加维护成本和优化器成本 |

---

# 九、完整面试回答模板

慢 SQL 排查一般先通过慢查询日志、监控或 APM 定位具体 SQL，然后使用 `EXPLAIN` 或 `EXPLAIN ANALYZE` 查看执行计划。重点看 `type` 是否为 `ALL`，`key` 是否为 `NULL`，`rows` 是否过大，`Extra` 中是否出现 `Using filesort` 或 `Using temporary`。然后分析是否存在索引缺失、索引失效、回表过多、排序分组成本高、深分页、大事务或锁等待等问题。优化时可以通过建立合适索引、调整联合索引顺序、减少查询字段、避免 `SELECT *`、优化分页、拆分复杂 SQL 等方式解决。

分页查询慢通常是因为深分页时 MySQL 需要扫描 `offset + size` 条数据，然后丢弃前面的 offset 条。优化方式包括延迟关联，先通过覆盖索引查出主键，再回表查完整数据；使用游标分页，根据上一页最后一条记录的 id 或时间继续向后查；限制最大翻页深度；增加筛选条件缩小扫描范围；使用覆盖索引减少回表；对于历史数据很多的场景，可以做冷热数据分离。

优化 `ORDER BY` 的核心是让排序尽量利用索引顺序，避免 `Using filesort`。常见做法是根据查询条件建立联合索引，把等值过滤字段放在前面，把排序字段放在后面。例如 `WHERE user_id = ? ORDER BY create_time DESC` 可以建立 `(user_id, create_time)` 联合索引。同时要注意排序字段顺序和方向要和索引一致，避免对排序字段使用函数，减少返回字段，并配合 `LIMIT` 控制结果集大小。

优化 `GROUP BY` 的核心是减少参与分组的数据量，并尽量利用索引完成分组。常见做法包括先通过 `WHERE` 条件过滤数据，再进行分组；为过滤字段和分组字段建立联合索引；只查询必要字段；使用覆盖索引减少回表。如果是高频统计或大数据量统计，可以使用汇总表、缓存或离线计算，避免每次实时扫描大表聚合。

大表优化要先看瓶颈。如果是查询慢，优先优化 SQL 和索引，比如避免全表扫描、深分页、`SELECT *`，建立合适的联合索引和覆盖索引。如果是历史数据过多，可以做冷热数据分离和历史归档。如果查询天然按时间范围，可以考虑分区表。如果单表数据量、写入压力或单库容量达到瓶颈，再考虑分库分表。分库分表能提升容量和并发能力，但会带来分布式事务、跨分片查询、全局 ID、分页排序和运维复杂度等问题，所以通常是最后手段。

索引不是越多越好。索引太多会占用更多磁盘空间，并且每次 `INSERT`、`UPDATE`、`DELETE` 都要维护多个索引结构，导致写入性能下降。索引过多还会增加优化器选择成本，占用更多 Buffer Pool，降低缓存命中率，增加主从同步压力和 DDL 维护成本。实际项目中要避免重复索引和冗余索引，根据核心查询场景建立必要索引，并定期评估和清理无用索引。
