# SQL 索引失效场景总结

索引失效指的是：SQL 中虽然给字段建立了索引，但查询时优化器没有使用索引，或者没有充分使用索引，导致走全表扫描或扫描数据量变大。

---

## 1. 不满足最左前缀原则

### 场景

联合索引需要从最左边的字段开始使用。

例如有联合索引：

```sql
KEY idx_name_age_status(name, age, status)
```

可以命中索引：

```sql
WHERE name = 'Tom'
WHERE name = 'Tom' AND age = 18
WHERE name = 'Tom' AND age = 18 AND status = 1
```

可能无法充分命中索引：

```sql
WHERE age = 18
WHERE status = 1
WHERE age = 18 AND status = 1
```

### 总结

联合索引必须按照最左字段开始匹配，否则索引可能失效。

---

## 2. 联合索引中间字段断开

### 场景

联合索引：

```sql
KEY idx_name_age_status(name, age, status)
```

SQL：

```sql
WHERE name = 'Tom' AND status = 1
```

这里跳过了 `age` 字段，索引只能用到 `name`，后面的 `status` 不能充分利用索引。

### 总结

联合索引中如果跳过中间字段，后面的字段无法继续利用索引。

---

## 3. 联合索引中范围查询后面的字段失效

### 场景

联合索引：

```sql
KEY idx_name_age_status(name, age, status)
```

SQL：

```sql
WHERE name = 'Tom' 
AND age > 18 
AND status = 1
```

索引可以用到：

```text
name、age
```

但 `status` 通常无法继续用于索引过滤。

### 总结

联合索引中，范围查询字段后面的字段通常无法继续利用索引。

常见范围条件：

```sql
>、<、>=、<=、BETWEEN、LIKE 'xxx%'
```

---

## 4. 对索引字段使用函数

### 场景

```sql
WHERE DATE(create_time) = '2026-04-25'
```

如果 `create_time` 上有索引，对字段使用函数后，索引可能失效。

### 推荐写法

```sql
WHERE create_time >= '2026-04-25 00:00:00'
AND create_time < '2026-04-26 00:00:00'
```

### 总结

不要在索引字段上使用函数计算。

---

## 5. 对索引字段进行表达式计算

### 场景

```sql
WHERE age + 1 = 19
```

如果 `age` 上有索引，这种写法可能导致索引失效。

### 推荐写法

```sql
WHERE age = 18
```

### 总结

索引字段不要参与计算，应把计算放到常量侧。

---

## 6. 索引字段发生隐式类型转换

### 场景

字段类型是 `VARCHAR`：

```sql
phone VARCHAR(20)
```

错误写法：

```sql
WHERE phone = 13800138000
```

MySQL 可能会把字段转换为数字再比较，导致索引失效。

### 推荐写法

```sql
WHERE phone = '13800138000'
```

### 总结

查询条件的数据类型要和字段类型保持一致。

---

## 7. 字符集或排序规则不一致

### 场景

两个表 Join 时，关联字段字符集不同：

```sql
t1.name utf8mb4
t2.name utf8
```

SQL：

```sql
SELECT *
FROM t1
JOIN t2 ON t1.name = t2.name;
```

可能因为字符集转换导致索引失效。

### 总结

关联字段的字符集和排序规则要保持一致。

---

## 8. LIKE 左模糊查询

### 场景

可以使用索引：

```sql
WHERE name LIKE 'Tom%'
```

无法使用普通 B+Tree 索引：

```sql
WHERE name LIKE '%Tom'
WHERE name LIKE '%Tom%'
```

### 总结

`LIKE` 以 `%` 开头时，普通索引通常失效。

---

## 9. 使用 `OR` 连接条件

### 场景

```sql
WHERE name = 'Tom' OR age = 18
```

如果 `name` 有索引，但 `age` 没有索引，可能导致整体不走索引。

### 优化方式

- 给 `OR` 两边字段都加合适索引。
- 使用 `UNION` 拆分查询。

```sql
SELECT * FROM user WHERE name = 'Tom'
UNION
SELECT * FROM user WHERE age = 18;
```

### 总结

`OR` 条件中只要有一边无法使用索引，整体可能索引失效。

---

## 10. 使用 `!=` 或 `<>`

### 场景

```sql
WHERE status != 1
WHERE status <> 1
```

这种条件选择性较差，MySQL 可能不走索引。

### 总结

不等于查询通常不利于索引命中。

---

## 11. 使用 `NOT IN`、`NOT LIKE`

### 场景

```sql
WHERE id NOT IN (1, 2, 3)
WHERE name NOT LIKE 'Tom%'
```

这类反向查询通常选择性较差，可能导致索引失效。

### 总结

`NOT IN`、`NOT LIKE` 一般不利于索引使用。

---

## 12. `IS NULL` / `IS NOT NULL`

### 场景

```sql
WHERE deleted_at IS NULL
WHERE deleted_at IS NOT NULL
```

是否使用索引取决于数据分布。

如果大量数据都是 `NULL`，即使有索引，优化器也可能选择全表扫描。

### 总结

`IS NULL` 不一定失效，关键看字段区分度和数据量。

---

## 13. 查询条件选择性太低

### 场景

例如 `gender` 字段只有两个值：

```sql
WHERE gender = '男'
```

即使 `gender` 上有索引，MySQL 也可能不使用，因为命中数据太多，全表扫描反而更快。

### 总结

区分度低的字段不适合单独建索引。

---

## 14. 使用索引字段排序但顺序不匹配

### 场景

联合索引：

```sql
KEY idx_age_score(age, score)
```

可以较好利用索引：

```sql
ORDER BY age, score
```

可能无法充分利用索引：

```sql
ORDER BY score
ORDER BY score, age
```

### 总结

`ORDER BY` 使用联合索引时，也要遵守最左前缀原则。

---

## 15. `ORDER BY` 排序方向不一致

### 场景

联合索引：

```sql
KEY idx_age_score(age, score)
```

SQL：

```sql
ORDER BY age ASC, score DESC
```

在部分 MySQL 版本或索引设计不匹配时，可能无法充分利用索引排序。

### 优化方式

MySQL 8.0 支持降序索引：

```sql
KEY idx_age_score(age ASC, score DESC)
```

### 总结

排序字段和索引字段顺序、方向要尽量一致。

---

## 16. 查询返回数据量过大

### 场景

```sql
SELECT * FROM user WHERE status = 1;
```

如果 `status = 1` 命中大量数据，优化器可能认为走索引后还要大量回表，不如直接全表扫描。

### 总结

是否使用索引由优化器根据成本决定，不是有索引就一定会走。

---

## 17. 使用 `SELECT *` 导致大量回表

### 场景

```sql
SELECT * FROM user WHERE name = 'Tom';
```

如果查询字段很多，普通二级索引命中后还要回表查询完整数据。

### 优化方式

尽量使用覆盖索引：

```sql
SELECT id, name FROM user WHERE name = 'Tom';
```

### 总结

能查必要字段就不要 `SELECT *`，尽量让查询走覆盖索引。

---

## 18. 表数据量太小

### 场景

小表只有几十或几百条数据，MySQL 可能直接全表扫描。

### 总结

小表全表扫描成本很低，优化器可能不走索引。

---

## 19. 统计信息不准确

### 场景

表数据变化很大，但统计信息没有及时更新，优化器可能选择错误执行计划。

### 处理方式

```sql
ANALYZE TABLE user;
```

### 总结

统计信息不准确可能导致索引选择异常。

---

## 20. 索引字段上使用类型不匹配的排序或比较

### 场景

字段是字符串，但按数字逻辑比较：

```sql
WHERE code > 100
```

如果 `code` 是 `VARCHAR` 类型，可能发生隐式转换，影响索引使用。

### 总结

字段类型、查询条件、排序方式要保持一致。

---

## 21. 常见索引失效对比表

| 场景 | 是否容易索引失效 | 原因 |
|---|---|---|
| 不满足最左前缀 | 是 | 联合索引必须从左到右匹配 |
| 跳过联合索引中间字段 | 是 | 后续字段无法继续使用索引 |
| 范围查询后面的字段 | 是 | 范围字段后索引利用受限 |
| 索引字段使用函数 | 是 | 字段值被计算后无法直接走索引 |
| 索引字段参与表达式 | 是 | 字段被计算 |
| 隐式类型转换 | 是 | 字段可能被转换 |
| LIKE `%xxx` | 是 | 无法从索引左侧开始匹配 |
| OR 条件一边无索引 | 是 | 可能导致全表扫描 |
| `!=` / `<>` | 可能 | 选择性差 |
| `NOT IN` / `NOT LIKE` | 可能 | 反向查询选择性差 |
| `IS NULL` | 不一定 | 取决于数据分布 |
| 低区分度字段 | 可能 | 命中数据太多 |
| SELECT * | 可能 | 回表成本高 |
| 表太小 | 可能 | 全表扫描成本更低 |

---

## 22. 总结

SQL 索引失效常见场景包括：不满足最左前缀原则、联合索引中跳过中间字段、范围查询后面的字段无法继续使用索引、对索引字段使用函数或表达式、发生隐式类型转换、`LIKE` 左模糊查询、`OR` 条件中部分字段无索引、使用 `!=`、`NOT IN`、`NOT LIKE`、字段区分度太低、返回数据量过大以及 `SELECT *` 导致回表成本过高等。

实际优化时，可以通过 `EXPLAIN` 查看执行计划，重点关注 `type`、`key`、`rows`、`Extra` 等字段，判断 SQL 是否真正使用索引以及是否发生全表扫描、回表、文件排序等问题。
