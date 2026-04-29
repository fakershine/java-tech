# MySQL 索引与 SQL 优化常见面试题总结

## 目录

- [一、MySQL 索引是什么](#一mysql-索引是什么)
- [二、B+ 树索引原理](#二b-树索引原理)
- [三、为什么 MySQL 使用 B+ 树](#三为什么-mysql-使用-b-树)
- [四、聚簇索引和非聚簇索引区别](#四聚簇索引和非聚簇索引区别)
- [五、覆盖索引是什么](#五覆盖索引是什么)
- [六、最左前缀原则是什么](#六最左前缀原则是什么)
- [七、索引失效场景有哪些](#七索引失效场景有哪些)
- [八、EXPLAIN 怎么看](#八explain-怎么看)
- [九、面试速记版](#九面试速记版)
- [十、总览表](#十总览表)
- [十一、完整面试回答模板](#十一完整面试回答模板)

---

# 一、MySQL 索引是什么

## 1. 索引是什么？

MySQL 索引是一种用于提高查询效率的数据结构。

简单来说：

> 索引类似于书的目录，可以帮助 MySQL 快速定位数据，避免全表扫描。

例如有一张用户表：

```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50),
    age INT,
    phone VARCHAR(20)
);
```

如果没有索引，执行：

```sql
SELECT * FROM user WHERE name = 'Tom';
```

MySQL 可能需要一行一行扫描整张表。

如果给 `name` 字段建立索引：

```sql
CREATE INDEX idx_name ON user(name);
```

MySQL 就可以通过索引快速定位到 `name = 'Tom'` 的记录。

---

## 2. 索引的作用

索引的主要作用包括：

1. 提高查询速度；
2. 减少全表扫描；
3. 提高排序效率；
4. 提高分组效率；
5. 加速表连接；
6. 保证数据唯一性；
7. 降低数据库 IO 成本。

---

## 3. 索引的代价

索引并不是越多越好。

索引会带来以下成本：

1. 占用额外磁盘空间；
2. 降低 `INSERT` 性能；
3. 降低 `UPDATE` 性能；
4. 降低 `DELETE` 性能；
5. 增加优化器选择成本；
6. 索引过多会增加维护成本。

原因是：

```text
数据写入、更新、删除时，不仅要修改表数据，还要维护对应索引结构。
```

---

## 4. 常见索引类型

MySQL 中常见索引类型包括：

| 索引类型 | 说明 |
|---|---|
| 主键索引 | `PRIMARY KEY`，唯一且不允许为 NULL |
| 唯一索引 | `UNIQUE`，值不能重复 |
| 普通索引 | 最基本的索引，没有唯一性限制 |
| 联合索引 | 多个字段组成的索引 |
| 前缀索引 | 对字符串字段的前几个字符建立索引 |
| 全文索引 | 用于全文检索 |
| 空间索引 | 用于地理空间数据 |

---

## 5. 主键索引

主键索引是一种特殊的唯一索引。

特点：

```text
唯一
非空
一张表只能有一个主键索引
InnoDB 中主键索引就是聚簇索引
```

示例：

```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50)
);
```

---

## 6. 唯一索引

唯一索引用于保证字段值唯一。

示例：

```sql
CREATE UNIQUE INDEX uk_phone ON user(phone);
```

适合场景：

```text
手机号
邮箱
身份证号
订单号
业务唯一编码
```

---

## 7. 普通索引

普通索引只用于提高查询效率，不保证唯一性。

示例：

```sql
CREATE INDEX idx_name ON user(name);
```

---

## 8. 联合索引

联合索引是多个字段组成的索引。

示例：

```sql
CREATE INDEX idx_name_age ON user(name, age);
```

适合多个字段经常一起作为查询条件的场景。

例如：

```sql
SELECT * FROM user WHERE name = 'Tom' AND age = 18;
```

---

## 9. 前缀索引

前缀索引是指只对字符串字段的前几个字符建立索引。

示例：

```sql
CREATE INDEX idx_email_prefix ON user(email(10));
```

适合字段较长，但前缀区分度较高的场景。

例如：

```text
email
url
长编码
```

---

## 10. 索引适合哪些字段？

适合建立索引的字段：

1. 经常出现在 `WHERE` 条件中的字段；
2. 经常用于 `ORDER BY` 的字段；
3. 经常用于 `GROUP BY` 的字段；
4. 经常用于 `JOIN` 关联的字段；
5. 区分度较高的字段；
6. 业务唯一字段；
7. 查询频率高、更新频率相对低的字段。

---

## 11. 不适合建立索引的字段

不适合建立索引的字段：

1. 区分度很低的字段；
2. 数据量很小的表；
3. 经常更新的字段；
4. 很少作为查询条件的字段；
5. 超长字段直接建完整索引；
6. 存在大量重复值的字段。

例如：

```text
性别
是否删除
状态标识
```

这些字段如果区分度很低，单独建索引效果通常不好。

---

## 12. 面试回答

MySQL 索引是一种提高查询效率的数据结构，类似书的目录，可以帮助数据库快速定位数据，减少全表扫描。

InnoDB 中常见索引底层通常使用 B+ 树。索引可以提高查询、排序、分组和连接效率，但也会占用额外空间，并降低写入、更新和删除性能。因此索引不是越多越好，要根据查询场景合理设计。

---

# 二、B+ 树索引原理

## 1. B+ 树是什么？

B+ 树是一种多路平衡搜索树。

它是 B 树的一种变种，非常适合磁盘存储系统。

MySQL InnoDB 中常见索引结构就是 B+ 树。

---

## 2. B+ 树的结构特点

B+ 树具有以下特点：

```text
1. 多路平衡树
2. 非叶子节点只存索引键和指针
3. 叶子节点存放完整索引记录
4. 所有数据都在叶子节点
5. 叶子节点之间通过链表连接
6. 树高度较低
7. 查询性能稳定
```

---

## 3. B+ 树结构示意

```text
             [30 | 60]
            /    |    \
           /     |     \
      [10|20] [40|50] [70|80]
        |       |       |
        v       v       v
叶子节点之间通过链表连接：

[10,20] <-> [30,40,50] <-> [60,70,80]
```

---

## 4. B+ 树查询过程

假设查询：

```sql
SELECT * FROM user WHERE id = 50;
```

B+ 树查询过程：

```text
1. 从根节点开始
2. 根据索引值判断进入哪个子节点
3. 一层一层向下查找
4. 最终到达叶子节点
5. 在叶子节点中找到目标记录
```

---

## 5. B+ 树为什么适合磁盘？

数据库数据通常存储在磁盘中。

磁盘 IO 是数据库查询中的主要成本。

B+ 树每个节点可以存储多个 key，使得树的高度非常低。

例如：

```text
三层 B+ 树可能就能存储上千万甚至上亿数据。
```

树高越低，查询时需要的磁盘 IO 次数越少。

---

## 6. InnoDB 页和 B+ 树

InnoDB 存储数据的基本单位是页。

常见页大小是：

```text
16KB
```

B+ 树中的每个节点通常对应一个数据页。

所以 B+ 树查询时，本质上是在不同页之间查找。

---

## 7. B+ 树叶子节点链表

B+ 树叶子节点之间通过链表连接。

这样对范围查询非常友好。

例如：

```sql
SELECT * FROM user WHERE id BETWEEN 100 AND 200;
```

查到 `100` 后，可以沿着叶子节点链表继续向后扫描，直到 `200`。

---

## 8. B+ 树索引查找方式

常见查找类型：

| 查询类型 | 是否适合 B+ 树 |
|---|---|
| 等值查询 | 适合 |
| 范围查询 | 适合 |
| 前缀匹配 | 适合 |
| 排序 | 适合 |
| 分组 | 适合 |
| 后缀模糊匹配 | 不适合 |

---

## 9. 面试回答

B+ 树是一种多路平衡搜索树，InnoDB 的索引通常使用 B+ 树实现。

B+ 树的非叶子节点只存索引键和指针，叶子节点存放完整索引记录，并且叶子节点之间通过链表连接。查询时从根节点开始逐层向下查找，最终定位到叶子节点。

B+ 树树高低，可以减少磁盘 IO；叶子节点有序并通过链表连接，非常适合范围查询和排序。

---

# 三、为什么 MySQL 使用 B+ 树

## 1. MySQL 为什么不用普通二叉树？

普通二叉树在极端情况下可能退化成链表。

例如按顺序插入：

```text
1, 2, 3, 4, 5, 6
```

可能形成：

```text
1
 \
  2
   \
    3
     \
      4
```

查询复杂度退化为：

```text
O(n)
```

不适合作为数据库索引。

---

## 2. 为什么不用红黑树？

红黑树是平衡二叉树，查询复杂度是：

```text
O(log n)
```

但是红黑树是二叉树，每个节点最多只有两个子节点。

如果数据量很大，树会比较高。

树高越高，磁盘 IO 次数越多。

数据库索引主要瓶颈是磁盘 IO，因此红黑树不适合作为大规模磁盘索引结构。

---

## 3. 为什么不用 Hash 索引？

Hash 索引适合等值查询。

例如：

```sql
SELECT * FROM user WHERE id = 1;
```

但是 Hash 索引不适合：

1. 范围查询；
2. 排序；
3. 分组；
4. 最左前缀匹配；
5. 模糊前缀查询。

例如：

```sql
SELECT * FROM user WHERE id BETWEEN 10 AND 100;
```

Hash 索引无法高效支持这种范围查询。

---

## 4. 为什么不用 B 树？

B 树和 B+ 树都属于多路搜索树。

B 树的特点是：

```text
非叶子节点和叶子节点都可能存储数据。
```

这会导致：

```text
每个节点能存储的 key 数量减少；
树的高度可能更高；
范围查询效率不如 B+ 树。
```

---

## 5. B+ 树相比 B 树的优势

B+ 树相比 B 树有以下优势：

1. 非叶子节点只存 key 和指针，可以存更多 key；
2. 树更矮，磁盘 IO 更少；
3. 所有数据都在叶子节点，查询性能更稳定；
4. 叶子节点有序链表连接，范围查询更高效；
5. 更适合数据库磁盘存储和范围扫描。

---

## 6. B+ 树相比 Hash 的优势

| 对比项 | B+ 树 | Hash |
|---|---|---|
| 等值查询 | 支持 | 支持 |
| 范围查询 | 支持 | 不适合 |
| 排序 | 支持 | 不支持 |
| 分组 | 支持 | 不支持 |
| 最左前缀 | 支持 | 不支持 |
| 模糊前缀 | 支持 | 不支持 |
| 查询稳定性 | 稳定 | 受哈希冲突影响 |

---

## 7. B+ 树相比红黑树的优势

| 对比项 | B+ 树 | 红黑树 |
|---|---|---|
| 节点分叉 | 多路 | 二叉 |
| 树高度 | 低 | 相对高 |
| 磁盘 IO | 少 | 多 |
| 范围查询 | 高效 | 一般 |
| 适合场景 | 磁盘索引 | 内存结构 |

---

## 8. 面试回答

MySQL 使用 B+ 树作为索引结构，主要是因为数据库查询瓶颈通常在磁盘 IO。

B+ 树是多路平衡树，树高很低，每次查询需要的磁盘 IO 次数较少。它的非叶子节点只存索引键和指针，可以容纳更多 key；所有数据都在叶子节点，查询性能稳定；叶子节点之间通过链表连接，非常适合范围查询、排序和分组。

相比红黑树，B+ 树树高更低，磁盘 IO 更少；相比 Hash 索引，B+ 树支持范围查询和排序；相比 B 树，B+ 树范围扫描更高效。

---

# 四、聚簇索引和非聚簇索引区别

## 1. 聚簇索引是什么？

聚簇索引是指：

> 索引结构和数据存放在一起。

在 InnoDB 中，表数据本身就是按照主键索引组织存储的。

也就是说：

```text
InnoDB 的主键索引就是聚簇索引。
```

---

## 2. 聚簇索引叶子节点存什么？

聚簇索引的叶子节点存放的是：

```text
完整行数据
```

例如表：

```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50),
    age INT
);
```

主键索引 `id` 的 B+ 树叶子节点中存放：

```text
id + name + age 等完整行数据
```

---

## 3. 非聚簇索引是什么？

非聚簇索引也叫二级索引、辅助索引。

它是除了聚簇索引之外的其他索引。

例如：

```sql
CREATE INDEX idx_name ON user(name);
```

`idx_name` 就是非聚簇索引。

---

## 4. 非聚簇索引叶子节点存什么？

InnoDB 非聚簇索引的叶子节点存放：

```text
索引字段值 + 主键值
```

例如：

```sql
CREATE INDEX idx_name ON user(name);
```

二级索引叶子节点中存放：

```text
name + id
```

其中 `id` 是主键值。

---

## 5. 回表是什么？

当使用非聚簇索引查询时，如果查询的字段不在二级索引中，就需要根据二级索引中保存的主键值，再去聚簇索引中查询完整行数据。

这个过程叫：

```text
回表
```

---

## 6. 回表示例

表结构：

```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50),
    age INT,
    address VARCHAR(100),
    INDEX idx_name(name)
);
```

查询：

```sql
SELECT age, address FROM user WHERE name = 'Tom';
```

执行过程：

```text
1. 先通过 idx_name 找到 name = 'Tom' 的记录
2. 在 idx_name 叶子节点中拿到主键 id
3. 再根据 id 到主键索引中查完整行数据
4. 返回 age、address
```

这个第二次查询主键索引的过程就是回表。

---

## 7. 聚簇索引和非聚簇索引对比

| 对比项 | 聚簇索引 | 非聚簇索引 |
|---|---|---|
| 典型代表 | 主键索引 | 普通索引、唯一索引、联合索引 |
| 叶子节点存储 | 完整行数据 | 索引字段 + 主键值 |
| 一张表数量 | 通常只有一个 | 可以有多个 |
| 是否需要回表 | 不需要 | 可能需要 |
| 查询效率 | 主键查询效率高 | 取决于是否覆盖索引 |
| 数据存储顺序 | 按聚簇索引组织 | 不决定数据物理组织 |

---

## 8. 为什么建议主键短小？

因为 InnoDB 的二级索引叶子节点会保存主键值。

如果主键很长，例如使用很长的字符串作为主键，会导致：

1. 二级索引占用空间变大；
2. 索引页能存放的记录变少；
3. B+ 树高度可能增加；
4. 查询 IO 成本变高；
5. 缓存命中率下降。

所以 InnoDB 中通常建议使用短小、递增、稳定的主键。

---

## 9. 面试回答

聚簇索引是索引和数据存放在一起的索引，InnoDB 中主键索引就是聚簇索引，它的叶子节点保存完整行数据。

非聚簇索引也叫二级索引，它的叶子节点保存的是索引字段值和主键值。如果通过二级索引查询的数据不在索引中，就需要拿到主键值后再去聚簇索引中查询完整数据，这个过程叫回表。

---

# 五、覆盖索引是什么

## 1. 覆盖索引是什么？

覆盖索引是指：

> 一个查询需要的所有字段，都可以从某个索引中直接获取，不需要回表查询。

也就是说：

```text
查询列都被索引覆盖了。
```

---

## 2. 覆盖索引示例

表结构：

```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50),
    age INT,
    address VARCHAR(100),
    INDEX idx_name_age(name, age)
);
```

查询：

```sql
SELECT name, age FROM user WHERE name = 'Tom';
```

因为 `idx_name_age` 中已经包含：

```text
name
age
```

所以 MySQL 可以直接从索引中返回结果，不需要回表。

这就是覆盖索引。

---

## 3. 非覆盖索引示例

还是上面的表：

```sql
SELECT name, age, address FROM user WHERE name = 'Tom';
```

虽然 `name` 和 `age` 在索引中，但 `address` 不在 `idx_name_age` 中。

所以需要：

```text
1. 先查 idx_name_age
2. 拿到主键 id
3. 再回表查询 address
```

这就不是覆盖索引。

---

## 4. EXPLAIN 中怎么看覆盖索引？

如果使用覆盖索引，`EXPLAIN` 的 `Extra` 中通常会出现：

```text
Using index
```

示例：

```sql
EXPLAIN SELECT name, age FROM user WHERE name = 'Tom';
```

可能看到：

```text
Extra: Using index
```

这表示查询可以直接通过索引完成。

---

## 5. 覆盖索引的优点

覆盖索引的优点：

1. 避免回表；
2. 减少磁盘 IO；
3. 提高查询性能；
4. 减少随机 IO；
5. 索引页通常比数据页更小，缓存命中率更高。

---

## 6. 覆盖索引和联合索引的关系

覆盖索引通常通过联合索引实现。

例如查询：

```sql
SELECT order_no, user_id, status
FROM orders
WHERE user_id = 1001;
```

可以建立联合索引：

```sql
CREATE INDEX idx_user_status_order 
ON orders(user_id, status, order_no);
```

如果查询字段都在这个索引中，就可能形成覆盖索引。

---

## 7. 覆盖索引设计注意事项

1. 不要为了覆盖所有查询盲目建立超宽索引；
2. 索引字段越多，维护成本越高；
3. 高频查询可以考虑覆盖索引；
4. 大字段不适合放入联合索引；
5. 要结合查询频率和写入成本综合权衡。

---

## 8. 面试回答

覆盖索引是指查询需要的字段都可以从索引中直接获取，不需要再回表查询。

例如有联合索引 `(name, age)`，执行 `SELECT name, age FROM user WHERE name = 'Tom'` 时，查询字段都在索引中，就可以直接从索引返回数据。

覆盖索引可以减少回表和磁盘 IO，提高查询性能。通过 `EXPLAIN` 查看时，`Extra` 中出现 `Using index` 通常表示使用了覆盖索引。

---

# 六、最左前缀原则是什么

## 1. 最左前缀原则是什么？

最左前缀原则主要针对联合索引。

它指的是：

> MySQL 使用联合索引时，会从索引最左边的字段开始连续匹配。

例如建立联合索引：

```sql
CREATE INDEX idx_name_age_city ON user(name, age, city);
```

这个索引可以支持：

```text
name
name + age
name + age + city
```

但通常不能直接高效支持：

```text
age
city
age + city
```

因为它们没有从最左边的 `name` 开始。

---

## 2. 联合索引结构如何理解？

联合索引：

```sql
(name, age, city)
```

可以理解为按照以下顺序排序：

```text
先按 name 排序
name 相同再按 age 排序
age 相同再按 city 排序
```

类似电话簿：

```text
先按姓排序
姓相同再按名排序
```

如果不知道姓，只知道名，就很难利用这个排序结构快速定位。

---

## 3. 能使用联合索引的情况

索引：

```sql
CREATE INDEX idx_name_age_city ON user(name, age, city);
```

以下查询可以使用索引：

```sql
SELECT * FROM user WHERE name = 'Tom';

SELECT * FROM user WHERE name = 'Tom' AND age = 18;

SELECT * FROM user WHERE name = 'Tom' AND age = 18 AND city = '北京';
```

---

## 4. 不能充分使用联合索引的情况

以下查询不符合最左前缀：

```sql
SELECT * FROM user WHERE age = 18;

SELECT * FROM user WHERE city = '北京';

SELECT * FROM user WHERE age = 18 AND city = '北京';
```

因为没有使用最左边字段 `name`。

---

## 5. 条件顺序是否影响最左前缀？

SQL 中 `WHERE` 条件顺序通常不影响最左前缀。

例如索引：

```sql
(name, age, city)
```

查询：

```sql
SELECT * FROM user 
WHERE age = 18 AND name = 'Tom';
```

虽然 SQL 中 `age` 写在前面，但优化器可以调整条件顺序，仍然可以使用 `name + age`。

---

## 6. 范围查询对最左前缀的影响

联合索引中，如果某个字段使用范围查询，它右边的字段通常不能继续用于精确定位。

索引：

```sql
(name, age, city)
```

查询：

```sql
SELECT * FROM user
WHERE name = 'Tom' AND age > 18 AND city = '北京';
```

通常可以使用：

```text
name
age
```

但 `city` 可能无法继续用于索引精确定位。

原因是：

```text
age 是范围查询后，age 后面的 city 已经不能保持整体有序匹配。
```

---

## 7. 常见范围查询

常见范围查询包括：

```text
>
>=
<
<=
BETWEEN
LIKE 'abc%'
```

注意：

```text
LIKE 'abc%' 可以使用索引，但它也是范围扫描。
```

---

## 8. 最左前缀和 ORDER BY

联合索引也可以用于排序。

索引：

```sql
CREATE INDEX idx_user_age ON user(user_id, age);
```

查询：

```sql
SELECT * FROM user 
WHERE user_id = 1001 
ORDER BY age;
```

因为 `user_id` 固定后，`age` 在索引中是有序的，所以可以利用索引排序。

---

## 9. 面试回答

最左前缀原则是指 MySQL 使用联合索引时，会从索引最左边的字段开始连续匹配。

例如联合索引 `(a, b, c)` 可以支持 `(a)`、`(a, b)`、`(a, b, c)` 的查询，但通常不能直接支持 `(b)` 或 `(b, c)`。

如果中间某个字段没有使用，后面的字段通常无法继续使用索引。如果某个字段使用了范围查询，那么它右边的字段通常也不能继续用于索引精确匹配。

---

# 七、索引失效场景有哪些

## 1. 什么是索引失效？

索引失效是指：

> SQL 中虽然有索引，但优化器没有使用索引，或者只使用了索引的一部分，导致查询效率下降。

需要注意：

```text
索引是否使用，最终由 MySQL 优化器决定。
```

即使满足某些规则，如果优化器认为全表扫描成本更低，也可能不使用索引。

---

## 2. 联合索引不满足最左前缀

索引：

```sql
CREATE INDEX idx_name_age ON user(name, age);
```

查询：

```sql
SELECT * FROM user WHERE age = 18;
```

没有使用最左字段 `name`，可能无法使用该联合索引。

---

## 3. 在索引列上使用函数

索引：

```sql
CREATE INDEX idx_name ON user(name);
```

错误写法：

```sql
SELECT * FROM user WHERE LOWER(name) = 'tom';
```

因为对索引列使用了函数，可能导致索引失效。

优化写法：

```sql
SELECT * FROM user WHERE name = 'tom';
```

或者使用函数索引、生成列等方式优化。

---

## 4. 在索引列上进行计算

索引：

```sql
CREATE INDEX idx_age ON user(age);
```

错误写法：

```sql
SELECT * FROM user WHERE age + 1 = 19;
```

优化写法：

```sql
SELECT * FROM user WHERE age = 18;
```

---

## 5. 在索引列上发生隐式类型转换

字段类型：

```sql
phone VARCHAR(20)
```

索引：

```sql
CREATE INDEX idx_phone ON user(phone);
```

错误写法：

```sql
SELECT * FROM user WHERE phone = 13800138000;
```

因为 `phone` 是字符串，但查询条件使用数字，可能发生隐式类型转换，导致索引失效。

优化写法：

```sql
SELECT * FROM user WHERE phone = '13800138000';
```

---

## 6. LIKE 以通配符开头

索引：

```sql
CREATE INDEX idx_name ON user(name);
```

可以使用索引：

```sql
SELECT * FROM user WHERE name LIKE 'Tom%';
```

通常不能有效使用普通 B+ 树索引：

```sql
SELECT * FROM user WHERE name LIKE '%Tom';
```

或者：

```sql
SELECT * FROM user WHERE name LIKE '%Tom%';
```

原因是：

```text
B+ 树索引按照从左到右排序，前缀不确定时无法快速定位。
```

---

## 7. OR 条件使用不当

示例：

```sql
SELECT * FROM user 
WHERE name = 'Tom' OR age = 18;
```

如果 `name` 有索引，但 `age` 没有索引，优化器可能放弃使用索引。

优化方式：

```text
给 OR 两边字段都建合适索引；
或者使用 UNION 拆分。
```

示例：

```sql
SELECT * FROM user WHERE name = 'Tom'
UNION
SELECT * FROM user WHERE age = 18;
```

---

## 8. 使用 != 或 <>

示例：

```sql
SELECT * FROM user WHERE status != 1;
```

`!=`、`<>` 通常选择性较差，可能导致索引效果不好。

但并不是绝对不走索引，最终由优化器根据成本决定。

---

## 9. 使用 IS NOT NULL

示例：

```sql
SELECT * FROM user WHERE name IS NOT NULL;
```

如果大部分数据都不为 NULL，选择性很低，优化器可能认为全表扫描更划算。

---

## 10. 索引字段区分度太低

例如：

```text
gender：男 / 女
deleted：0 / 1
status：少量状态值
```

这些字段单独建索引，区分度较低。

如果查询返回大量数据，优化器可能不使用索引。

---

## 11. 查询返回数据量太大

即使有索引，如果查询命中大量数据，例如超过表中很大比例，MySQL 可能选择全表扫描。

示例：

```sql
SELECT * FROM user WHERE status = 1;
```

如果 90% 的数据 `status = 1`，索引意义不大。

---

## 12. 范围查询后面的联合索引字段可能失效

索引：

```sql
CREATE INDEX idx_name_age_city ON user(name, age, city);
```

查询：

```sql
SELECT * FROM user
WHERE name = 'Tom' AND age > 18 AND city = '北京';
```

通常 `name` 和 `age` 可以使用索引，`city` 可能不能继续用于精确匹配。

---

## 13. ORDER BY 与索引顺序不一致

索引：

```sql
CREATE INDEX idx_age_name ON user(age, name);
```

查询：

```sql
SELECT * FROM user ORDER BY name;
```

由于排序字段没有遵守索引顺序，可能无法利用索引排序。

---

## 14. SELECT * 可能导致回表成本过高

示例：

```sql
SELECT * FROM user WHERE name = 'Tom';
```

如果使用二级索引查到大量数据，还需要大量回表。

优化器可能认为全表扫描更划算。

优化方式：

```sql
SELECT id, name FROM user WHERE name = 'Tom';
```

尽量只查询必要字段，争取覆盖索引。

---

## 15. 索引失效场景总结表

| 场景 | 示例 | 原因 |
|---|---|---|
| 不满足最左前缀 | `WHERE b = 1`，索引 `(a,b)` | 没有从最左列开始 |
| 索引列使用函数 | `LOWER(name)` | 无法直接使用原索引有序性 |
| 索引列参与计算 | `age + 1 = 20` | 索引值被表达式改变 |
| 隐式类型转换 | `phone = 138` | 类型不一致 |
| LIKE 左模糊 | `LIKE '%abc'` | 前缀不确定 |
| OR 使用不当 | 一边有索引一边无索引 | 优化器可能放弃索引 |
| `!=`、`<>` | `status != 1` | 选择性可能较差 |
| `IS NOT NULL` | `name IS NOT NULL` | 返回数据可能过多 |
| 区分度低 | `gender` | 过滤效果差 |
| 返回数据太多 | 命中大部分数据 | 全表扫描可能更划算 |
| 范围后字段 | `a=1 AND b>2 AND c=3` | `c` 可能无法精确利用 |
| 排序不匹配 | `ORDER BY b`，索引 `(a,b)` | 不符合索引顺序 |

---

## 16. 面试回答

常见索引失效场景包括：联合索引不满足最左前缀原则，在索引列上使用函数或计算，发生隐式类型转换，`LIKE` 以 `%` 开头，`OR` 条件使用不当，使用 `!=`、`<>`、`IS NOT NULL` 导致选择性较差，索引字段区分度低，查询返回数据量太大，以及范围查询后面的联合索引字段无法继续用于精确匹配。

不过索引是否真正使用，最终由 MySQL 优化器根据成本决定，所以实际排查时应该结合 `EXPLAIN` 查看执行计划。

---

# 八、EXPLAIN 怎么看

## 1. EXPLAIN 是什么？

`EXPLAIN` 用于查看 MySQL 的 SQL 执行计划。

它可以帮助我们分析：

```text
SQL 是否使用索引
使用了哪个索引
表连接顺序
扫描了多少行
访问类型好不好
是否出现文件排序
是否使用临时表
是否回表
```

示例：

```sql
EXPLAIN SELECT * FROM user WHERE name = 'Tom';
```

---

## 2. EXPLAIN 常见字段

执行：

```sql
EXPLAIN SELECT * FROM user WHERE name = 'Tom';
```

常见输出字段：

| 字段 | 说明 |
|---|---|
| id | 查询编号 |
| select_type | 查询类型 |
| table | 当前访问的表 |
| partitions | 匹配的分区 |
| type | 访问类型 |
| possible_keys | 可能使用的索引 |
| key | 实际使用的索引 |
| key_len | 使用的索引长度 |
| ref | 与索引比较的列或常量 |
| rows | 预计扫描行数 |
| filtered | 过滤比例 |
| Extra | 额外信息 |

---

## 3. id

`id` 表示查询中每个 SELECT 的编号。

一般规则：

```text
id 相同：从上到下执行
id 不同：id 越大，优先级越高
```

示例：

```sql
EXPLAIN
SELECT * FROM user
WHERE id IN (
    SELECT user_id FROM orders
);
```

---

## 4. select_type

`select_type` 表示查询类型。

常见值：

| select_type | 说明 |
|---|---|
| SIMPLE | 简单查询，不包含子查询或 UNION |
| PRIMARY | 最外层查询 |
| SUBQUERY | 子查询 |
| DERIVED | 派生表 |
| UNION | UNION 中后续查询 |
| UNION RESULT | UNION 结果 |

---

## 5. table

`table` 表示当前访问的是哪张表。

如果是派生表，可能显示：

```text
<derivedN>
```

如果是子查询结果，可能显示特殊名称。

---

## 6. type

`type` 是非常重要的字段，表示访问类型。

性能从好到差大致如下：

```text
system > const > eq_ref > ref > range > index > ALL
```

---

## 7. system

`system` 表示表中只有一行记录，是 `const` 的特殊情况。

---

## 8. const

`const` 表示通过主键或唯一索引一次就能定位一行数据。

示例：

```sql
SELECT * FROM user WHERE id = 1;
```

如果 `id` 是主键，通常是 `const`。

---

## 9. eq_ref

`eq_ref` 常见于多表 JOIN。

表示对于前表的每一行，后表通过主键或唯一索引只能匹配一行。

示例：

```sql
SELECT *
FROM orders o
JOIN user u ON o.user_id = u.id;
```

如果 `u.id` 是主键，访问 `user` 表可能是 `eq_ref`。

---

## 10. ref

`ref` 表示使用非唯一索引进行等值查询，可能匹配多行。

示例：

```sql
SELECT * FROM user WHERE name = 'Tom';
```

如果 `name` 是普通索引，可能是 `ref`。

---

## 11. range

`range` 表示使用索引范围扫描。

示例：

```sql
SELECT * FROM user WHERE age > 18;

SELECT * FROM user WHERE id BETWEEN 10 AND 100;
```

---

## 12. index

`index` 表示扫描整个索引树。

它比全表扫描稍好一些，因为索引通常比整行数据小。

但仍然扫描了大量索引记录。

---

## 13. ALL

`ALL` 表示全表扫描。

通常是需要重点优化的类型。

示例：

```sql
SELECT * FROM user WHERE address = '北京';
```

如果 `address` 没有索引，可能是 `ALL`。

---

## 14. possible_keys

`possible_keys` 表示优化器认为可能用到的索引。

注意：

```text
possible_keys 有值，不代表实际使用了索引。
```

---

## 15. key

`key` 表示实际使用的索引。

如果是 `NULL`，表示没有使用索引。

示例：

```text
key: idx_name
```

表示实际使用了 `idx_name` 索引。

---

## 16. key_len

`key_len` 表示使用的索引长度。

它可以帮助判断联合索引用到了几个字段。

例如联合索引：

```sql
CREATE INDEX idx_name_age ON user(name, age);
```

如果 `key_len` 只包含 `name` 的长度，说明只用到了部分索引。

如果包含 `name + age` 的长度，说明两个字段都用上了。

---

## 17. ref

`ref` 表示哪个列或常量被用来和索引比较。

常见值：

```text
const
表字段名
func
NULL
```

---

## 18. rows

`rows` 表示优化器预计需要扫描的行数。

注意：

```text
rows 是估算值，不是实际扫描行数。
```

一般来说：

```text
rows 越小越好。
```

---

## 19. filtered

`filtered` 表示经过条件过滤后剩余记录的比例估算。

结合 `rows` 可以估算最终参与后续操作的行数：

```text
rows * filtered / 100
```

---

## 20. Extra

`Extra` 表示额外执行信息，非常重要。

常见值：

| Extra | 说明 |
|---|---|
| Using index | 使用覆盖索引 |
| Using where | 使用 WHERE 条件过滤 |
| Using index condition | 使用索引条件下推 ICP |
| Using filesort | 使用文件排序 |
| Using temporary | 使用临时表 |
| Using join buffer | 使用 join buffer |
| Impossible WHERE | WHERE 条件恒为 false |
| Select tables optimized away | 优化器直接优化掉表访问 |

---

## 21. Using index

`Using index` 通常表示使用了覆盖索引。

说明查询字段可以直接从索引中获取，不需要回表。

---

## 22. Using where

`Using where` 表示 MySQL 读取记录后，还需要使用 `WHERE` 条件进一步过滤。

---

## 23. Using index condition

`Using index condition` 表示使用了索引条件下推。

索引条件下推可以在存储引擎层先根据索引过滤一部分数据，减少回表次数。

---

## 24. Using filesort

`Using filesort` 表示 MySQL 需要额外排序。

这通常说明：

```text
ORDER BY 没有很好利用索引顺序。
```

需要重点关注。

---

## 25. Using temporary

`Using temporary` 表示 MySQL 使用了临时表。

常见于：

```text
GROUP BY
ORDER BY
DISTINCT
UNION
```

如果数据量较大，可能影响性能。

---

## 26. EXPLAIN 优化重点

看 `EXPLAIN` 时，重点关注：

```text
1. type 是否为 ALL
2. key 是否为 NULL
3. rows 是否过大
4. Extra 是否有 Using filesort
5. Extra 是否有 Using temporary
6. 是否使用了覆盖索引
7. 联合索引是否只用了一部分
8. JOIN 顺序是否合理
```

---

## 27. EXPLAIN 示例

表结构：

```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50),
    age INT,
    city VARCHAR(50),
    INDEX idx_name_age(name, age)
);
```

SQL：

```sql
EXPLAIN SELECT name, age 
FROM user 
WHERE name = 'Tom' AND age = 18;
```

重点看：

```text
type: ref
key: idx_name_age
Extra: Using index
```

说明：

```text
使用了 idx_name_age 索引；
查询字段被索引覆盖；
不需要回表。
```

---

## 28. EXPLAIN FORMAT=JSON

MySQL 还支持：

```sql
EXPLAIN FORMAT=JSON
SELECT * FROM user WHERE name = 'Tom';
```

它可以看到更详细的执行计划信息，例如：

```text
成本估算
访问路径
过滤条件
使用索引情况
```

---

## 29. EXPLAIN ANALYZE

MySQL 8.0.18+ 支持：

```sql
EXPLAIN ANALYZE
SELECT * FROM user WHERE name = 'Tom';
```

它会实际执行 SQL，并返回实际执行耗时和行数信息。

注意：

```text
EXPLAIN ANALYZE 会真正执行 SQL。
```

因此在线上使用要谨慎。

---

## 30. 面试回答

`EXPLAIN` 用来查看 SQL 执行计划，常用于判断 SQL 是否走索引、扫描行数是否过多、是否发生文件排序或临时表。

重点看几个字段：`type` 表示访问类型，性能从好到差大致是 `system > const > eq_ref > ref > range > index > ALL`；`possible_keys` 表示可能使用的索引；`key` 表示实际使用的索引；`key_len` 可以判断联合索引用到了几个字段；`rows` 表示预计扫描行数；`Extra` 中要重点关注 `Using index`、`Using filesort`、`Using temporary` 等信息。

如果发现 `type = ALL`、`key = NULL`、`rows` 很大，或者 `Extra` 中出现 `Using filesort`、`Using temporary`，通常说明 SQL 还有优化空间。

---

# 九、面试速记版

## 1. MySQL 索引是什么？

索引是帮助 MySQL 快速查找数据的数据结构，类似书的目录。

优点是提高查询效率，缺点是占用空间并降低写入、更新、删除性能。

---

## 2. B+ 树索引原理

B+ 树是多路平衡搜索树。

非叶子节点存 key 和指针，叶子节点存索引记录，叶子节点之间通过链表连接。

查询从根节点逐层向下查找，最终定位到叶子节点。

---

## 3. 为什么 MySQL 使用 B+ 树？

因为 B+ 树树高低，磁盘 IO 少；所有数据在叶子节点，查询稳定；叶子节点有序链表连接，适合范围查询、排序和分组。

相比红黑树，B+ 树更低；相比 Hash，B+ 树支持范围查询；相比 B 树，B+ 树更适合范围扫描。

---

## 4. 聚簇索引和非聚簇索引区别

InnoDB 中主键索引是聚簇索引，叶子节点保存完整行数据。

非聚簇索引也叫二级索引，叶子节点保存索引字段和主键值。

通过二级索引查完整数据时，可能需要回表。

---

## 5. 覆盖索引是什么？

覆盖索引是指查询需要的字段都在索引中，可以直接从索引返回数据，不需要回表。

`EXPLAIN` 中 `Extra` 出现 `Using index` 通常表示使用了覆盖索引。

---

## 6. 最左前缀原则是什么？

最左前缀原则主要针对联合索引。

联合索引 `(a, b, c)` 可以支持：

```text
a
a + b
a + b + c
```

通常不能直接支持：

```text
b
b + c
```

如果中间断开，后面的字段通常无法继续使用索引。

---

## 7. 索引失效场景有哪些？

常见索引失效场景：

```text
不满足最左前缀
索引列使用函数
索引列参与计算
隐式类型转换
LIKE 左模糊
OR 使用不当
!= 或 <>
IS NOT NULL
字段区分度低
返回数据量太大
范围查询后字段无法继续精确匹配
ORDER BY 不符合索引顺序
```

---

## 8. EXPLAIN 怎么看？

重点看：

```text
type
possible_keys
key
key_len
rows
filtered
Extra
```

`type` 性能从好到差大致为：

```text
system > const > eq_ref > ref > range > index > ALL
```

重点警惕：

```text
type = ALL
key = NULL
rows 很大
Using filesort
Using temporary
```

---

# 十、总览表

| 问题 | 核心结论 |
|---|---|
| MySQL 索引是什么 | 提高查询效率的数据结构，类似书的目录 |
| B+ 树索引原理 | 多路平衡树，非叶子存 key，叶子存数据，叶子节点链表连接 |
| 为什么 MySQL 使用 B+ 树 | 树高低、IO 少、范围查询强、查询稳定 |
| 聚簇索引和非聚簇索引区别 | 聚簇索引叶子节点存完整行；非聚簇索引叶子节点存索引列和主键 |
| 覆盖索引是什么 | 查询字段都在索引中，不需要回表 |
| 最左前缀原则是什么 | 联合索引从最左列开始连续匹配 |
| 索引失效场景有哪些 | 函数、计算、隐式转换、左模糊、不满足最左前缀等 |
| EXPLAIN 怎么看 | 看 type、key、key_len、rows、Extra 等字段 |

---

# 十一、完整面试回答模板

MySQL 索引是一种提高查询效率的数据结构，类似书的目录，可以帮助数据库快速定位数据，减少全表扫描。索引可以提高查询、排序、分组和连接效率，但也会占用额外磁盘空间，并降低插入、更新和删除性能，所以索引不是越多越好。

InnoDB 中常见索引底层使用 B+ 树。B+ 树是一种多路平衡搜索树，非叶子节点只存索引键和指针，叶子节点存放索引记录，并且叶子节点之间通过链表连接。查询时从根节点开始逐层向下查找，最终定位到叶子节点。

MySQL 使用 B+ 树的原因主要是数据库查询瓶颈通常在磁盘 IO。B+ 树树高较低，可以减少磁盘 IO；叶子节点有序并通过链表连接，适合范围查询、排序和分组；所有数据都在叶子节点，查询性能稳定。相比红黑树，B+ 树更适合磁盘；相比 Hash 索引，B+ 树支持范围查询；相比 B 树，B+ 树范围扫描效率更高。

聚簇索引是索引和数据存放在一起的索引，InnoDB 中主键索引就是聚簇索引，它的叶子节点保存完整行数据。非聚簇索引也叫二级索引，它的叶子节点保存索引字段值和主键值。如果通过二级索引查询的数据不在索引中，就需要拿到主键值后再去聚簇索引中查询完整行数据，这个过程叫回表。

覆盖索引是指查询需要的字段都可以从索引中直接获取，不需要回表。例如有联合索引 `(name, age)`，执行 `SELECT name, age FROM user WHERE name = 'Tom'` 时，查询字段都在索引中，就可以直接返回结果。覆盖索引可以减少回表和磁盘 IO，提升查询性能。

最左前缀原则是指 MySQL 使用联合索引时，会从索引最左边的字段开始连续匹配。例如联合索引 `(a, b, c)` 可以支持 `(a)`、`(a, b)`、`(a, b, c)` 的查询，但通常不能直接支持 `(b)` 或 `(b, c)`。如果中间某个字段没有使用，后面的字段通常无法继续使用索引。如果某个字段使用范围查询，那么它右边的字段通常也不能继续用于索引精确匹配。

常见索引失效场景包括：联合索引不满足最左前缀原则，在索引列上使用函数或计算，发生隐式类型转换，`LIKE` 以 `%` 开头，`OR` 条件使用不当，使用 `!=`、`<>`、`IS NOT NULL` 导致选择性较差，索引字段区分度低，查询返回数据量太大，以及范围查询后面的联合索引字段无法继续用于精确匹配。实际是否使用索引，最终要看 MySQL 优化器的成本判断。

`EXPLAIN` 用来查看 SQL 执行计划，常用于判断 SQL 是否使用索引、扫描行数是否过多、是否出现文件排序或临时表。重点看 `type`、`possible_keys`、`key`、`key_len`、`rows`、`filtered` 和 `Extra`。其中 `type` 性能从好到差大致是 `system > const > eq_ref > ref > range > index > ALL`。如果发现 `type = ALL`、`key = NULL`、`rows` 很大，或者 `Extra` 中出现 `Using filesort`、`Using temporary`，通常说明 SQL 还有优化空间。
