# MySQL MVCC 总结

MVCC 全称是 **Multi-Version Concurrency Control，多版本并发控制**。  
它的核心作用是：**在不加锁的情况下，提高数据库读写并发能力，并实现一致性读。**

---

## 1. MVCC 解决什么问题

在并发场景下，如果读写都加锁，会导致性能下降。

MVCC 通过保存数据的多个历史版本，让读操作可以读取某个历史版本，而不是阻塞正在修改数据的事务。

简单理解：

```text
写操作生成新版本
读操作读取旧版本
读写互不阻塞
```

---

## 2. MVCC 依赖的核心机制

MySQL InnoDB 的 MVCC 主要依赖：

- 隐藏字段
- undo log
- ReadView

---

## 3. 隐藏字段

InnoDB 每行记录中会维护一些隐藏字段。

| 隐藏字段 | 说明 |
|---|---|
| DB_TRX_ID | 最近一次修改该行记录的事务 ID |
| DB_ROLL_PTR | 回滚指针，指向 undo log 中的旧版本 |
| DB_ROW_ID | 如果表没有主键，InnoDB 会生成隐藏主键 |

重点关注：

```text
DB_TRX_ID：记录当前版本由哪个事务生成
DB_ROLL_PTR：指向上一个历史版本
```

---

## 4. undo log 版本链

当一条记录被多次修改时，InnoDB 会通过 undo log 保存旧版本数据。

例如原始数据：

```text
id = 1, name = A
```

事务 10 修改为：

```text
id = 1, name = B
```

事务 20 又修改为：

```text
id = 1, name = C
```

此时会形成版本链：

```text
当前版本 C，trx_id = 20
    ↓ roll_pointer
历史版本 B，trx_id = 10
    ↓ roll_pointer
历史版本 A，trx_id = 5
```

查询时，InnoDB 会根据 ReadView 判断当前事务能看到哪个版本。

---

## 5. ReadView 是什么

ReadView 可以理解为：**事务在某一时刻生成的数据可见性快照**。

它用于判断某个版本的数据，当前事务是否可以看到。

ReadView 中主要包含：

| 字段 | 说明 |
|---|---|
| creator_trx_id | 创建当前 ReadView 的事务 ID |
| m_ids | 创建 ReadView 时，当前活跃事务 ID 列表 |
| min_trx_id | 当前活跃事务中的最小事务 ID |
| max_trx_id | 下一个将要分配的事务 ID |

---

## 6. 数据版本可见性规则

假设当前数据版本的事务 ID 是：

```text
trx_id
```

判断规则如下：

### 1. `trx_id == creator_trx_id`

说明该版本是当前事务自己修改的。

```text
可见
```

---

### 2. `trx_id < min_trx_id`

说明该版本在 ReadView 创建前已经提交。

```text
可见
```

---

### 3. `trx_id >= max_trx_id`

说明该版本是在 ReadView 创建后才生成的。

```text
不可见
```

---

### 4. `min_trx_id <= trx_id < max_trx_id`

需要判断 `trx_id` 是否在活跃事务列表 `m_ids` 中。

```text
如果 trx_id 在 m_ids 中：
    说明事务还没提交，不可见

如果 trx_id 不在 m_ids 中：
    说明事务已经提交，可见
```

---

## 7. MVCC 查询流程

一次普通查询大致流程如下：

```text
1. 查询当前记录最新版本。
2. 根据 ReadView 判断当前版本是否可见。
3. 如果可见，直接返回。
4. 如果不可见，通过 roll_pointer 找到 undo log 中的上一个版本。
5. 继续判断旧版本是否可见。
6. 直到找到可见版本或版本链结束。
```

---

## 8. 快照读和当前读

### 快照读

普通 `SELECT` 属于快照读。

```sql
SELECT * FROM user WHERE id = 1;
```

快照读基于 MVCC 实现，不加锁，读取的是符合 ReadView 的历史版本。

### 当前读

当前读读取的是最新版本，并且通常会加锁。

常见当前读：

```sql
SELECT * FROM user WHERE id = 1 FOR UPDATE;

SELECT * FROM user WHERE id = 1 LOCK IN SHARE MODE;

UPDATE user SET name = 'Tom' WHERE id = 1;

DELETE FROM user WHERE id = 1;

INSERT INTO user VALUES (...);
```

当前读需要读取最新数据，所以不能只依赖 MVCC，通常要结合行锁或间隙锁。

---

## 9. 不同隔离级别下的 MVCC

### Read Committed

在 **读已提交** 隔离级别下：

```text
每次 SELECT 都会生成新的 ReadView
```

所以同一个事务中，两次查询可能读到不同结果。

这就是不可重复读。

---

### Repeatable Read

在 **可重复读** 隔离级别下：

```text
事务中第一次 SELECT 时生成 ReadView
后续 SELECT 复用同一个 ReadView
```

所以同一个事务中，多次查询结果一致。

这就是可重复读。

---

## 10. MVCC 能否解决幻读

在 MySQL InnoDB 的 **可重复读** 隔离级别下：

- 普通快照读通过 MVCC 解决幻读问题。
- 当前读通过 Next-Key Lock 解决幻读问题。

需要注意：

```text
MVCC 主要解决的是快照读的一致性问题。
当前读的幻读问题主要依赖间隙锁和临键锁解决。
```

---

## 11. MVCC 的优势

- 读写不互相阻塞。
- 提高并发性能。
- 支持一致性读。
- 实现可重复读。
- 减少锁竞争。

---

## 12. MVCC 的劣势

- 需要维护 undo log 版本链。
- 长事务会导致历史版本无法及时清理。
- undo log 过多会占用存储空间。
- 查询旧版本时可能需要沿版本链回溯，影响性能。

---

## 13. 长事务对 MVCC 的影响

如果一个事务长时间不提交，它创建的 ReadView 会一直存在。

这会导致：

```text
旧版本数据不能被清理
undo log 持续增长
版本链变长
查询性能下降
```

所以实际项目中要避免长事务。

---

## 14. 总结

MVCC 是 MySQL InnoDB 实现高并发读写的重要机制，它通过隐藏字段、undo log 和 ReadView 实现。

每行数据中有隐藏字段 `DB_TRX_ID` 和 `DB_ROLL_PTR`，`DB_TRX_ID` 表示最近修改该行的事务 ID，`DB_ROLL_PTR` 指向 undo log 中的历史版本，从而形成版本链。

普通查询属于快照读，会根据 ReadView 判断当前事务能看到哪个版本。如果当前版本不可见，就沿着 undo log 版本链向前查找，直到找到一个可见版本。

在读已提交隔离级别下，每次查询都会生成新的 ReadView；在可重复读隔离级别下，事务第一次查询生成 ReadView，后续查询复用同一个 ReadView，所以可以实现可重复读。

一句话总结：

```text
MVCC = 隐藏字段 + undo log 版本链 + ReadView 可见性判断。
```
