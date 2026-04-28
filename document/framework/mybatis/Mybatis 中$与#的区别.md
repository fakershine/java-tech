# MyBatis 中 #{} 和 ${} 的区别

MyBatis 中 `#{}` 和 `${}` 都可以用于参数传递，但它们的处理方式完全不同。

核心区别：

```text
#{} 是预编译占位符，安全，能防止 SQL 注入。
${} 是字符串拼接，不安全，存在 SQL 注入风险。
```

---

## 1. #{} 的实现原理

`#{}` 会被 MyBatis 解析成 JDBC 的 `?` 占位符。

示例：

```sql
SELECT * FROM user WHERE id = #{id}
```

最终会变成：

```sql
SELECT * FROM user WHERE id = ?
```

然后通过 `PreparedStatement` 设置参数：

```java
preparedStatement.setLong(1, id);
```

### 特点

- 使用预编译。
- 参数会作为值处理。
- 自动处理字符串引号。
- 可以防止 SQL 注入。
- 推荐优先使用。

---

## 2. ${} 的实现原理

`${}` 是直接进行字符串替换。

示例：

```sql
SELECT * FROM user WHERE id = ${id}
```

如果传入：

```text
id = 1
```

最终 SQL 是：

```sql
SELECT * FROM user WHERE id = 1
```

如果传入恶意参数：

```text
id = 1 OR 1=1
```

最终 SQL 可能变成：

```sql
SELECT * FROM user WHERE id = 1 OR 1=1
```

这就存在 SQL 注入风险。

### 特点

- 直接字符串拼接。
- 不会预编译。
- 不会自动加引号。
- 存在 SQL 注入风险。
- 只能在少数动态 SQL 场景中使用。

---

## 3. 对比总结

| 对比项 | `#{}` | `${}` |
|---|---|---|
| 处理方式 | 预编译占位符 | 字符串拼接 |
| 最终 SQL | `?` | 直接替换成参数值 |
| 是否防 SQL 注入 | 是 | 否 |
| 是否自动加引号 | 是 | 否 |
| 使用场景 | 参数值 | 表名、字段名、排序字段 |
| 推荐程度 | 优先使用 | 谨慎使用 |

---

## 4. 使用示例

### 使用 #{}

```sql
SELECT * FROM user WHERE name = #{name}
```

传入：

```text
name = Tom
```

最终 SQL：

```sql
SELECT * FROM user WHERE name = ?
```

参数由 JDBC 设置：

```text
'TOM'
```

安全。

---

### 使用 ${}

```sql
SELECT * FROM user WHERE name = '${name}'
```

传入：

```text
name = Tom
```

最终 SQL：

```sql
SELECT * FROM user WHERE name = 'Tom'
```

如果传入：

```text
name = Tom' OR '1'='1
```

最终 SQL 可能变成：

```sql
SELECT * FROM user WHERE name = 'Tom' OR '1'='1'
```

存在 SQL 注入风险。

---

## 5. ${} 的适用场景

虽然 `${}` 不安全，但有些场景必须使用字符串拼接。

例如：

### 1. 动态表名

```sql
SELECT * FROM ${tableName}
WHERE id = #{id}
```

因为表名不能使用 `?` 占位符。

---

### 2. 动态字段名

```sql
SELECT ${columnName}
FROM user
WHERE id = #{id}
```

---

### 3. 动态排序字段

```sql
SELECT * FROM user
ORDER BY ${orderBy}
```

`ORDER BY` 后面的字段名不能用 `#{}` 作为字段名。

---

## 6. 使用 ${} 的安全建议

使用 `${}` 时，必须做白名单校验。

### 动态排序示例

不推荐：

```sql
ORDER BY ${orderBy}
```

如果用户传入：

```text
id desc; drop table user;
```

就可能有风险。

推荐在代码中限制：

```java
private static final Set<String> ALLOW_ORDER_FIELDS =
        Set.of("id", "create_time", "update_time");

if (!ALLOW_ORDER_FIELDS.contains(orderBy)) {
    throw new IllegalArgumentException("非法排序字段");
}
```

然后再传入 MyBatis。

---

## 7. 为什么表名、字段名不能用 #{}

因为 `#{}` 会被当成参数值，而不是 SQL 结构。

错误示例：

```sql
SELECT * FROM #{tableName}
```

最终会变成：

```sql
SELECT * FROM ?
```

这在 SQL 语法上是不合法的。

同理：

```sql
ORDER BY #{orderBy}
```

最终会变成：

```sql
ORDER BY ?
```

数据库会把它当成一个普通参数值，而不是排序字段，达不到动态排序效果。

---

## 8. 总结

MyBatis 中 `#{}` 和 `${}` 的区别主要在于处理方式不同。

`#{}` 会被解析成 JDBC 的 `?` 占位符，通过 `PreparedStatement` 预编译设置参数，所以可以防止 SQL 注入，适合传普通参数值。

`${}` 是直接字符串替换，相当于 SQL 拼接，不会预编译，也不会自动加引号，因此存在 SQL 注入风险。它一般只用于动态表名、字段名、排序字段等 SQL 结构无法使用占位符的场景。

实际开发中，能用 `#{}` 就不要用 `${}`；如果必须使用 `${}`，一定要做白名单校验。

一句话总结：

```text
#{} 传值，${} 拼 SQL；#{} 安全，${} 有注入风险。
```
