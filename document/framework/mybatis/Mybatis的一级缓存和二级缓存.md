# MyBatis 一级缓存和二级缓存总结

MyBatis 缓存用于减少数据库查询次数，提高查询性能。  
MyBatis 缓存分为：

```text
一级缓存：SqlSession 级别
二级缓存：Mapper / namespace 级别
```

---

## 1. 一级缓存

### 实现原理

一级缓存是 MyBatis 默认开启的缓存，作用范围是同一个 `SqlSession`。

同一个 `SqlSession` 中，执行相同 SQL 和相同参数时，第一次查询会访问数据库，后续相同查询会优先从一级缓存中获取。

```text
第一次查询
  ↓
查询数据库
  ↓
结果放入一级缓存

第二次相同查询
  ↓
直接从一级缓存返回
```

示例：

```java
SqlSession sqlSession = sqlSessionFactory.openSession();

User user1 = userMapper.selectById(1);
User user2 = userMapper.selectById(1);
```

如果两次查询在同一个 `SqlSession` 中，并且中间没有执行增删改操作，第二次查询可能直接走一级缓存。

---

## 2. 一级缓存失效场景

一级缓存虽然默认开启，但以下情况会失效：

- 不同的 `SqlSession`。
- SQL 不同。
- 参数不同。
- 查询之间执行了 `insert`、`update`、`delete`。
- 手动调用了 `clearCache()`。
- Mapper 配置了 `flushCache=true`。
- 事务提交或回滚后缓存被清理。

示例：

```java
User user1 = userMapper.selectById(1);

userMapper.updateUser(user);

User user2 = userMapper.selectById(1);
```

中间执行了更新操作，一级缓存会被清空，第二次查询会重新访问数据库。

---

## 3. 一级缓存优势

- 默认开启，不需要额外配置。
- 减少同一个 `SqlSession` 内重复查询。
- 实现简单，性能开销小。

---

## 4. 一级缓存劣势

- 作用范围较小，只在同一个 `SqlSession` 内有效。
- 在 Spring 项目中，每次请求或事务通常对应不同 `SqlSession`，缓存效果有限。
- 如果同一个 `SqlSession` 中数据被外部修改，可能读到旧数据。
- 不能跨会话共享。

---

## 5. 二级缓存

### 实现原理

二级缓存是 Mapper 级别，也可以理解为 `namespace` 级别缓存。

多个 `SqlSession` 可以共享同一个 Mapper 的二级缓存。

```text
SqlSession 1 查询数据
  ↓
事务提交后，结果写入二级缓存

SqlSession 2 查询相同 Mapper、相同 SQL
  ↓
优先从二级缓存读取
```

注意：

```text
二级缓存的数据只有在 SqlSession 提交或关闭后，才会写入二级缓存。
```

---

## 6. 二级缓存开启方式

### 全局开启

```xml
<settings>
    <setting name="cacheEnabled" value="true"/>
</settings>
```

默认通常是开启的。

### Mapper 中开启

在对应 Mapper XML 中增加：

```xml
<cache/>
```

示例：

```xml
<mapper namespace="com.demo.UserMapper">

    <cache/>

    <select id="selectById" resultType="User">
        SELECT * FROM user WHERE id = #{id}
    </select>

</mapper>
```

---

## 7. 二级缓存执行流程

```text
查询请求
  ↓
先查二级缓存
  ↓
二级缓存没有，再查一级缓存
  ↓
一级缓存没有，查询数据库
  ↓
结果放入一级缓存
  ↓
SqlSession 提交或关闭后，写入二级缓存
```

简单理解：

```text
二级缓存跨 SqlSession 共享
一级缓存只在当前 SqlSession 内有效
```

---

## 8. 二级缓存失效场景

以下情况会导致二级缓存失效或被清空：

- Mapper 中没有配置 `<cache/>`。
- 查询语句设置了 `useCache=false`。
- 增删改语句默认会刷新缓存。
- SQL 参数不同。
- 查询条件不同。
- 不同 namespace。
- `SqlSession` 未提交或关闭，数据还没进入二级缓存。

示例：

```xml
<select id="selectById" resultType="User" useCache="false">
    SELECT * FROM user WHERE id = #{id}
</select>
```

这个查询不会使用二级缓存。

---

## 9. 二级缓存注意点

### 1. 实体类需要可序列化

如果使用 MyBatis 默认二级缓存，缓存对象通常需要实现 `Serializable`。

```java
public class User implements Serializable {
    private Long id;
    private String name;
}
```

---

### 2. 二级缓存以 namespace 为单位

二级缓存是 Mapper namespace 级别。

```text
UserMapper 有自己的缓存
OrderMapper 有自己的缓存
```

如果多个 Mapper 操作同一张表，可能出现缓存不一致。

例如：

```text
UserMapper 查询 user 表并缓存
UserOtherMapper 更新 user 表
UserMapper 的缓存不会自动感知
```

---

### 3. 更新操作会清空缓存

MyBatis 中 `insert`、`update`、`delete` 默认会刷新缓存。

```xml
<update id="updateUser" flushCache="true">
    UPDATE user SET name = #{name} WHERE id = #{id}
</update>
```

默认就是：

```text
flushCache=true
```

---

## 10. 一级缓存和二级缓存对比

| 对比项 | 一级缓存 | 二级缓存 |
|---|---|---|
| 作用范围 | SqlSession 级别 | Mapper / namespace 级别 |
| 是否默认开启 | 默认开启 | 需要 Mapper 配置 `<cache/>` |
| 是否跨 SqlSession | 否 | 是 |
| 生命周期 | SqlSession 生命周期内 | Mapper namespace 生命周期内 |
| 清理时机 | 增删改、提交、回滚、手动清理 | 增删改、flushCache、配置失效 |
| 使用风险 | 同会话内脏读 | 多 Mapper 操作同表可能不一致 |
| 实际使用 | 默认存在 | 实际项目较少直接使用 |

---

## 11. 为什么实际项目中很少用二级缓存

二级缓存虽然可以跨 `SqlSession` 共享，但实际项目中使用较少。

主要原因：

- 缓存粒度是 Mapper namespace，不够灵活。
- 多个 Mapper 操作同一张表时容易数据不一致。
- 分布式环境下本地二级缓存无法跨服务共享。
- 缓存更新策略不好控制。
- 实际项目通常使用 Redis 作为统一缓存。

因此实际项目更常见：

```text
MyBatis 一级缓存默认使用
业务缓存使用 Redis
MyBatis 二级缓存谨慎使用或不用
```

---

## 12. 总结

MyBatis 缓存分为一级缓存和二级缓存。

一级缓存是 `SqlSession` 级别，默认开启。同一个 `SqlSession` 中执行相同 SQL 和参数时，第一次查询数据库，后续相同查询可以直接从一级缓存获取。一级缓存会在执行增删改、提交、回滚或手动清理时失效。

二级缓存是 Mapper 的 `namespace` 级别，可以在多个 `SqlSession` 之间共享，需要在 Mapper XML 中配置 `<cache/>`。查询结果会先进入一级缓存，等 `SqlSession` 提交或关闭后才会写入二级缓存。增删改操作默认会清空对应 namespace 的二级缓存。

实际项目中，一级缓存默认使用即可，二级缓存因为存在数据一致性和分布式共享问题，使用较少，更多会使用 Redis 来做业务缓存。

一句话总结：

```text
MyBatis 一级缓存是 SqlSession 级别，默认开启；
二级缓存是 namespace 级别，需要配置开启，但实际项目中一般谨慎使用。
```
