# MySQL 三大日志总结

MySQL 中常说的三大日志是：**binlog、redo log、undo log**。  
它们分别用于 **主从复制、事务持久性、事务回滚和 MVCC**。

---

## 1. binlog

### 实现原理

`binlog` 是 MySQL Server 层的日志，记录数据库的逻辑变更操作。

例如：

```sql
UPDATE user SET name = 'Tom' WHERE id = 1;
```

binlog 记录的是类似 SQL 语句或行变更内容，而不是具体的数据页修改。

binlog 常见格式：

| 格式 | 说明 |
|---|---|
| STATEMENT | 记录 SQL 语句 |
| ROW | 记录每一行数据变更 |
| MIXED | SQL 和 ROW 混合模式 |

实际项目中一般使用：

```text
ROW 格式
```

因为它更准确，适合主从复制和数据恢复。

### 主要作用

- 主从复制。
- 数据恢复。
- 增量备份。
- 监听数据变更，例如 Canal 监听 binlog。

### 优势

- Server 层日志，所有存储引擎都可以使用。
- 可以用于主从同步。
- 可以用于数据恢复和审计。
- 支持增量备份。

### 劣势

- 不负责事务回滚。
- 不负责崩溃恢复。
- ROW 格式日志量较大。

---

## 2. redo log

### 实现原理

`redo log` 是 InnoDB 存储引擎层的日志，用于保证事务的持久性。

MySQL 修改数据时，不会每次都立刻把数据页刷到磁盘，而是先修改内存中的 Buffer Pool，然后记录 redo log。

流程：

```text
修改数据
  ↓
修改 Buffer Pool 中的数据页
  ↓
写入 redo log
  ↓
事务提交
  ↓
后台异步刷脏页到磁盘
```

如果 MySQL 宕机，重启后可以通过 redo log 恢复已经提交但还没刷盘的数据。

### 主要作用

- 保证事务持久性。
- 支持崩溃恢复。
- 提升写入性能。

### 为什么 redo log 能提升性能？

如果每次更新都直接刷数据页到磁盘，会产生大量随机 IO。

redo log 是顺序写：

```text
随机写数据页 -> 顺序写 redo log
```

顺序写性能更高。

### 优势

- 保证已提交事务不丢失。
- 支持崩溃恢复。
- 顺序写日志，性能较好。
- 减少数据页频繁刷盘。

### 劣势

- 只属于 InnoDB。
- 空间有限，循环写。
- 不能用于主从复制。
- 不能直接做逻辑数据恢复。

---

## 3. undo log

### 实现原理

`undo log` 是 InnoDB 存储引擎层的日志，记录数据修改前的旧值。

例如：

```sql
UPDATE user SET name = 'Tom' WHERE id = 1;
```

undo log 会记录修改前的数据：

```text
id = 1, name = 原来的值
```

如果事务回滚，就可以根据 undo log 恢复旧数据。

### 主要作用

- 事务回滚。
- 实现 MVCC 多版本并发控制。
- 支持一致性读。

### undo log 和 MVCC

在可重复读隔离级别下，普通查询不会加锁，而是通过 undo log 构造历史版本数据。

例如：

```text
事务 A 修改了一行数据但未提交
事务 B 查询时，可以通过 undo log 看到修改前的旧版本
```

这就是 MVCC 的核心基础之一。

### 优势

- 支持事务回滚。
- 支持 MVCC。
- 提高并发读写能力。
- 保证事务原子性。

### 劣势

- 长事务会导致 undo log 长时间无法清理。
- undo log 过多会增加存储压力。
- 长事务可能影响 MVCC 版本链清理。

---

## 4. 三大日志对比

| 日志 | 所属层级 | 记录内容 | 主要作用 | 是否用于崩溃恢复 | 是否用于主从复制 |
|---|---|---|---|---|---|
| binlog | Server 层 | 逻辑变更 | 主从复制、数据恢复 | 否 | 是 |
| redo log | InnoDB 层 | 物理页修改 | 崩溃恢复、持久性 | 是 | 否 |
| undo log | InnoDB 层 | 修改前旧值 | 回滚、MVCC | 间接相关 | 否 |

---

## 5. 一条 Update SQL 的日志流程

以这条 SQL 为例：

```sql
UPDATE user SET name = 'Tom' WHERE id = 1;
```

大致流程：

```text
1. 查询数据页是否在 Buffer Pool 中。
2. 如果不在，从磁盘加载到 Buffer Pool。
3. 修改 Buffer Pool 中的数据。
4. 记录 undo log，用于回滚。
5. 记录 redo log，状态为 prepare。
6. 记录 binlog。
7. 提交事务，将 redo log 状态改为 commit。
```

这里涉及 **两阶段提交**：

```text
redo log prepare
  ↓
写 binlog
  ↓
redo log commit
```

---

## 6. 为什么需要两阶段提交？

因为 redo log 和 binlog 属于不同层级：

```text
redo log：InnoDB 引擎层
binlog：MySQL Server 层
```

如果不使用两阶段提交，可能出现数据不一致。

### 场景一：redo log 写成功，binlog 没写成功

```text
redo log 有记录
binlog 没记录
```

结果：

- MySQL 本机崩溃恢复后数据存在。
- 从库没有 binlog，无法同步这次修改。
- 主从数据不一致。

### 场景二：binlog 写成功，redo log 没写成功

```text
binlog 有记录
redo log 没记录
```

结果：

- 从库同步了这次修改。
- 主库崩溃恢复后没有这次修改。
- 主从数据不一致。

### 两阶段提交的作用

通过两阶段提交保证：

```text
redo log 和 binlog 要么都成功，要么都失败
```

从而保证主库数据恢复和主从复制的一致性。

---

## 7. 总结

MySQL 三大日志分别是 `binlog`、`redo log` 和 `undo log`。

`binlog` 是 Server 层日志，记录逻辑变更，主要用于主从复制和数据恢复；`redo log` 是 InnoDB 引擎层日志，记录物理页修改，主要用于崩溃恢复，保证事务持久性；`undo log` 也是 InnoDB 引擎层日志，记录修改前的数据，主要用于事务回滚和 MVCC。

一条更新语句执行时，会先记录 undo log，然后修改 Buffer Pool，再写 redo log，最后写 binlog，并通过 redo log 的 prepare 和 commit 两阶段提交保证 redo log 和 binlog 的一致性。

一句话总结：

```text
binlog 负责复制和恢复，redo log 负责崩溃恢复，undo log 负责回滚和 MVCC。
```
