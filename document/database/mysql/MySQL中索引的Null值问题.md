# MySQL 中索引 NULL 和 NOT NULL 的区别

MySQL 索引列是否允许 `NULL`，会影响 **存储、索引使用、查询语义、唯一索引行为和优化器判断**。

核心结论：

```text
索引列可以为 NULL，但一般建议业务字段尽量定义为 NOT NULL。
```

---

## 1. NULL 和 NOT NULL 的含义

### NULL

```text
表示未知、没有值、不确定
```

例如：

```sql
phone IS NULL
```

表示手机号未知或未填写。

---

### NOT NULL

```text
表示字段必须有值
```

例如：

```sql
phone VARCHAR(20) NOT NULL DEFAULT ''
```

表示手机号不能为空，没值时用空字符串或默认值表示。

---

## 2. 索引列可以为 NULL 吗？

可以。

例如：

```sql
CREATE INDEX idx_phone ON user(phone);
```

即使 `phone` 允许为 `NULL`，MySQL 也可以对它建立索引。

查询时：

```sql
SELECT * FROM user WHERE phone IS NULL;
```

也可能走索引。

---

## 3. NULL 和普通值的查询区别

### 查询 NULL

不能用：

```sql
WHERE phone = NULL
```

因为 `NULL` 不能用 `=` 判断。

应该用：

```sql
WHERE phone IS NULL
```

---

### 查询非 NULL

```sql
WHERE phone IS NOT NULL
```

也可以使用索引，但如果非 NULL 数据占比很高，优化器可能认为走全表扫描更划算。

---

## 4. NULL 对索引存储的影响

允许 `NULL` 的列，InnoDB 需要额外记录这个字段是否为 `NULL`。

简单理解：

```text
NOT NULL：只需要存字段值
NULL：除了字段值，还要额外标记是否为 NULL
```

所以从存储角度看：

```text
NOT NULL 通常更节省空间
```

不过实际差距通常不大。

---

## 5. NULL 对唯一索引的影响

这是最容易考的点。

MySQL 中，唯一索引允许多个 `NULL` 值。

例如：

```sql
CREATE UNIQUE INDEX uk_email ON user(email);
```

如果 `email` 允许为 `NULL`，那么可以插入多条：

```text
email = NULL
email = NULL
email = NULL
```

原因：

```text
MySQL 认为 NULL 表示未知，多个未知值不认为相等。
```

但非 NULL 值不能重复：

```text
email = 'a@test.com'
email = 'a@test.com'  -- 报唯一索引冲突
```

---

## 6. NULL 对联合索引的影响

假设有联合索引：

```sql
CREATE INDEX idx_name_age ON user(name, age);
```

如果 `age` 允许为 `NULL`：

```sql
WHERE name = 'Tom' AND age IS NULL
```

仍然可以使用联合索引。

但如果查询条件中对索引列做了复杂判断，例如：

```sql
WHERE IFNULL(age, 0) = 18
```

可能导致索引失效。

推荐写法：

```sql
WHERE age = 18 OR age IS NULL
```

或者业务上尽量使用默认值，避免频繁对索引列使用函数。

---

## 7. NULL 和索引失效

`IS NULL` 本身不一定导致索引失效。

可以走索引：

```sql
WHERE phone IS NULL
```

可能走索引：

```sql
WHERE phone IS NOT NULL
```

容易导致索引失效的是：

```sql
WHERE IFNULL(phone, '') = ''
```

或者：

```sql
WHERE COALESCE(phone, '') = ''
```

原因：

```text
对索引列使用函数，可能无法直接利用索引有序性。
```

---

## 8. NULL 对统计信息和优化器的影响

MySQL 优化器会根据索引区分度、数据分布、统计信息来判断是否走索引。

如果某个字段大量为 `NULL`：

```text
索引区分度变低
```

例如：

```text
1000 万数据中，900 万 phone 都是 NULL
```

此时：

```sql
WHERE phone IS NULL
```

即使有索引，也可能不走索引，因为扫描数据太多。

---

## 9. 为什么建议字段尽量 NOT NULL

推荐 `NOT NULL` 的原因：

- 语义更明确。
- 减少 NULL 判断。
- 避免三值逻辑问题。
- 节省一点存储空间。
- 唯一索引行为更符合直觉。
- 查询条件更简单。
- 对优化器更友好。

---

## 10. NULL 的三值逻辑问题

SQL 中有三种逻辑结果：

```text
TRUE
FALSE
UNKNOWN
```

例如：

```sql
SELECT * FROM user WHERE age != 18;
```

如果 `age` 是 `NULL`，这条记录不会被查出来。

因为：

```text
NULL != 18 的结果不是 TRUE，而是 UNKNOWN
```

如果想查出非 18 或为空：

```sql
WHERE age != 18 OR age IS NULL
```

---

## 11. 实际设计建议

| 场景 | 建议 |
|---|---|
| 必填字段 | `NOT NULL` |
| 状态字段 | `NOT NULL DEFAULT 0` |
| 数字字段 | `NOT NULL DEFAULT 0` |
| 字符串字段 | `NOT NULL DEFAULT ''` 或按业务允许 NULL |
| 时间字段 | 如果确实未知，可以允许 NULL |
| 唯一字段 | 谨慎允许 NULL |
| 索引字段 | 尽量 `NOT NULL` |

---

## 12. 总结

MySQL 中索引列可以为 `NULL`，并且 `IS NULL` 查询也可能走索引。

但是 `NULL` 和普通值不同，它表示未知，不能用 `= NULL` 判断，必须使用 `IS NULL`。在唯一索引中，MySQL 允许多个 `NULL` 值，因为多个未知值不认为相等。

从设计角度看，索引字段一般建议定义为 `NOT NULL`，因为它语义更清晰，存储更简单，也能避免 SQL 三值逻辑、唯一索引多个 NULL、查询判断复杂等问题。

一句话总结：

```text
索引列可以为 NULL，IS NULL 也能走索引；
但 NULL 会带来语义复杂、唯一索引多个 NULL、统计信息不稳定等问题；
所以业务字段尤其是索引字段，通常建议 NOT NULL。
```
