# MySQL 事务总结

事务是指：**一组 SQL 要么全部成功，要么全部失败**。

典型场景：

```text
下单
  ↓
扣库存
  ↓
扣余额
  ↓
生成订单
```

这些操作必须作为一个整体执行，不能只成功一部分。

---

## 1. 事务的 ACID 特性

| 特性 | 说明 |
|---|---|
| 原子性 Atomicity | 一个事务中的操作要么全部成功，要么全部失败 |
| 一致性 Consistency | 事务执行前后，数据必须保持正确状态 |
| 隔离性 Isolation | 多个事务并发执行时互不干扰 |
| 持久性 Durability | 事务提交后，数据修改必须永久保存 |

---

## 2. 原子性

原子性表示：

```text
事务中的 SQL 要么全部执行成功，要么全部回滚
```

例如转账：

```text
A 扣 100
B 加 100
```

不能出现：

```text
A 扣了钱，B 没收到钱
```

InnoDB 主要通过：

```text
undo log
```

实现事务回滚。

---

## 3. 一致性

一致性表示事务执行前后，数据必须符合业务规则。

例如：

```text
转账前：A + B = 1000
转账后：A + B = 1000
```

一致性依赖：

```text
原子性
隔离性
持久性
业务约束
数据库约束
```

---

## 4. 隔离性

隔离性表示多个事务并发执行时，一个事务不能随意影响另一个事务。

如果没有隔离性，可能出现：

```text
脏读
不可重复读
幻读
```

InnoDB 主要通过：

```text
锁
MVCC
```

实现事务隔离。

---

## 5. 持久性

持久性表示：

```text
事务提交后，数据即使数据库宕机也不能丢失
```

InnoDB 主要通过：

```text
redo log
```

保证事务提交后的数据可以恢复。

---

## 6. 事务基本操作

开启事务：

```sql
START TRANSACTION;
```

提交事务：

```sql
COMMIT;
```

回滚事务：

```sql
ROLLBACK;
```

示例：

```sql
START TRANSACTION;

UPDATE account SET balance = balance - 100 WHERE id = 1;
UPDATE account SET balance = balance + 100 WHERE id = 2;

COMMIT;
```

如果中途异常：

```sql
ROLLBACK;
```

---

## 7. 自动提交

MySQL 默认开启自动提交：

```sql
SHOW VARIABLES LIKE 'autocommit';
```

默认：

```text
autocommit = 1
```

表示每条 SQL 都是一个独立事务。

关闭自动提交：

```sql
SET autocommit = 0;
```

---

## 8. 并发事务问题

## 8.1 脏读

### 含义

一个事务读到了另一个事务 **未提交** 的数据。

```text
事务 A 修改数据但未提交
事务 B 读取到了这个修改
事务 A 回滚
事务 B 读到的数据就是脏数据
```

---

## 8.2 不可重复读

### 含义

同一个事务中，多次读取同一行数据，结果不一致。

```text
事务 A 第一次读 balance = 100
事务 B 修改 balance = 200 并提交
事务 A 第二次读 balance = 200
```

重点：

```text
同一行数据被修改
```

---

## 8.3 幻读

### 含义

同一个事务中，多次按条件查询，结果集数量不一致。

```text
事务 A 查询 age > 18，有 10 条
事务 B 插入一条 age = 20 并提交
事务 A 再查 age > 18，有 11 条
```

重点：

```text
新增或删除了符合条件的记录
```

---

## 9. 事务隔离级别

| 隔离级别 | 脏读 | 不可重复读 | 幻读 |
|---|---|---|---|
| Read Uncommitted | 可能 | 可能 | 可能 |
| Read Committed | 不会 | 可能 | 可能 |
| Repeatable Read | 不会 | 不会 | InnoDB 基本可避免 |
| Serializable | 不会 | 不会 | 不会 |

MySQL InnoDB 默认隔离级别是：

```text
Repeatable Read
```

---

## 10. Read Uncommitted

读未提交。

特点：

```text
可以读到其他事务未提交的数据
```

问题：

```text
可能出现脏读
```

实际很少使用。

---

## 11. Read Committed

读已提交。

特点：

```text
只能读到其他事务已经提交的数据
```

可以避免：

```text
脏读
```

但可能出现：

```text
不可重复读
幻读
```

Oracle 默认隔离级别通常是 RC。

---

## 12. Repeatable Read

可重复读。

特点：

```text
同一个事务中，多次读取同一数据结果一致
```

可以避免：

```text
脏读
不可重复读
```

InnoDB 在 RR 下通过：

```text
MVCC
Next-Key Lock
```

基本解决幻读问题。

---

## 13. Serializable

串行化。

特点：

```text
事务串行执行
隔离级别最高
```

优点：

```text
安全性最高
```

缺点：

```text
并发性能最差
```

实际项目中很少使用。

---

## 14. MVCC 和事务

MVCC 是多版本并发控制。

核心思想：

```text
读不加锁
通过版本链读取符合当前事务视图的数据
```

普通快照读：

```sql
SELECT * FROM user WHERE id = 1;
```

通常走 MVCC。

当前读：

```sql
SELECT * FROM user WHERE id = 1 FOR UPDATE;
UPDATE user SET name = 'Tom' WHERE id = 1;
```

会读取最新数据，并加锁。

---

## 15. 快照读和当前读

| 类型 | 示例 | 特点 |
|---|---|---|
| 快照读 | 普通 `SELECT` | 读历史版本，不加锁 |
| 当前读 | `UPDATE`、`DELETE`、`SELECT FOR UPDATE` | 读最新版本，并加锁 |

---

## 16. 事务和锁

事务中常见锁：

```text
行锁
间隙锁
Next-Key Lock
表锁
意向锁
```

例如：

```sql
SELECT * FROM order_info WHERE id = 1 FOR UPDATE;
```

会对符合条件的数据加锁，防止其他事务修改。

---

## 17. 事务提交过程

简化流程：

```text
执行 SQL
  ↓
修改 Buffer Pool 中的数据页
  ↓
记录 undo log
  ↓
记录 redo log
  ↓
提交事务
  ↓
redo log 持久化
  ↓
binlog 写入
  ↓
事务提交成功
```

核心：

```text
undo log 保证回滚
redo log 保证崩溃恢复
binlog 用于主从复制和恢复
```

---

## 18. 事务失效常见场景

如果在 Spring 中使用 `@Transactional`，事务可能失效：

```text
方法不是 public
同类内部方法调用
异常被 catch 没抛出
默认只回滚 RuntimeException
数据库表不支持事务
没有被 Spring 管理
多线程中执行事务方法
事务传播行为设置不当
```

---

## 19. 使用事务注意事项

- 事务不要太大。
- 事务中不要做远程调用。
- 事务中不要执行耗时操作。
- 尽量使用索引条件更新。
- 避免长事务。
- 避免事务中等待用户输入。
- 锁粒度尽量小。
- 及时提交或回滚。
- 写接口要保证幂等。
- 高并发下注意死锁和锁等待。

---

## 20. 总结

MySQL 事务是指一组 SQL 操作要么全部成功，要么全部失败。事务具备 ACID 特性：原子性、一致性、隔离性和持久性。

InnoDB 中，原子性主要依赖 undo log，持久性主要依赖 redo log，隔离性主要依赖锁和 MVCC，一致性则由数据库机制和业务约束共同保证。

事务并发执行时可能出现脏读、不可重复读和幻读。MySQL 支持四种隔离级别：读未提交、读已提交、可重复读和串行化。InnoDB 默认是可重复读，在该级别下通过 MVCC 和 Next-Key Lock 基本解决幻读问题。

一句话总结：

```text
MySQL 事务 = ACID + undo log 回滚 + redo log 持久化 + MVCC/锁隔离；
默认隔离级别是 Repeatable Read。
```
