# Spring 的事务

## 1. 什么是事务

事务是一组数据库操作，要么全部成功，要么全部失败。

事务主要解决数据一致性问题。

例如转账：

```text
A 账户扣 100
B 账户加 100
```

这两个操作必须同时成功或同时失败，不能只成功一个。

---

## 2. 事务的四大特性 ACID

| 特性 | 说明 |
|---|---|
| Atomicity 原子性 | 一个事务中的操作要么全部成功，要么全部失败 |
| Consistency 一致性 | 事务执行前后，数据必须保持一致 |
| Isolation 隔离性 | 多个事务并发执行时，互不干扰 |
| Durability 持久性 | 事务提交后，数据修改永久生效 |

---

## 3. Spring 事务的核心接口

Spring 事务核心接口主要有三个：

| 接口 | 作用 |
|---|---|
| `PlatformTransactionManager` | 事务管理器，负责开启、提交、回滚事务 |
| `TransactionDefinition` | 事务定义，描述传播行为、隔离级别、超时时间等 |
| `TransactionStatus` | 事务状态，记录当前事务是否新事务、是否回滚等 |

---

## 4. Spring 事务管理器

常见事务管理器：

| 事务管理器 | 说明 |
|---|---|
| `DataSourceTransactionManager` | JDBC、MyBatis 常用 |
| `JpaTransactionManager` | JPA、Hibernate 常用 |
| `HibernateTransactionManager` | Hibernate 专用 |
| `JtaTransactionManager` | 分布式事务场景 |

MyBatis / JDBC 项目中常用：

```java
DataSourceTransactionManager
```

---

## 5. Spring 事务使用方式

Spring 支持两种事务方式：

```text
编程式事务
声明式事务
```

---

## 6. 编程式事务

使用 `TransactionTemplate` 手动控制事务。

```java
@Service
public class OrderService {

    @Autowired
    private TransactionTemplate transactionTemplate;

    public void createOrder() {
        transactionTemplate.execute(status -> {
            try {
                // 1. 创建订单
                // 2. 扣减库存
                // 3. 记录日志

                return true;
            } catch (Exception e) {
                status.setRollbackOnly();
                return false;
            }
        });
    }
}
```

优点：

```text
控制精细
可以手动决定是否回滚
```

缺点：

```text
代码侵入性强
业务代码和事务代码耦合
```

---

## 7. 声明式事务

最常用方式是使用：

```java
@Transactional
```

示例：

```java
@Service
public class OrderService {

    @Transactional
    public void createOrder() {
        // 1. 创建订单
        // 2. 扣减库存
        // 3. 记录日志
    }
}
```

优点：

```text
使用简单
业务代码清晰
事务逻辑和业务逻辑解耦
```

实际项目中最常用的是声明式事务。

---

# 一、@Transactional

## 8. @Transactional 可以加在哪里

可以加在：

```text
类上
方法上
接口上
接口方法上
```

最常见写法：

```java
@Service
public class OrderService {

    @Transactional
    public void createOrder() {
        // 业务逻辑
    }
}
```

也可以加在类上：

```java
@Service
@Transactional
public class OrderService {

    public void createOrder() {
    }

    public void cancelOrder() {
    }
}
```

方法上的配置优先级高于类上的配置。

---

## 9. @Transactional 常用属性

| 属性 | 说明 |
|---|---|
| `propagation` | 事务传播行为 |
| `isolation` | 事务隔离级别 |
| `rollbackFor` | 指定哪些异常回滚 |
| `noRollbackFor` | 指定哪些异常不回滚 |
| `timeout` | 事务超时时间 |
| `readOnly` | 是否只读事务 |
| `transactionManager` | 指定事务管理器 |

示例：

```java
@Transactional(
    propagation = Propagation.REQUIRED,
    isolation = Isolation.READ_COMMITTED,
    rollbackFor = Exception.class,
    timeout = 5,
    readOnly = false
)
public void createOrder() {
}
```

---

# 二、事务传播行为

## 10. 什么是事务传播行为

事务传播行为指的是：

> 一个事务方法调用另一个事务方法时，事务应该如何传播。

例如：

```text
A 方法有事务
A 方法调用 B 方法
B 方法也有事务
```

这时 B 方法是加入 A 的事务，还是新开一个事务，就由传播行为决定。

---

## 11. Spring 常见传播行为

| 传播行为 | 说明 |
|---|---|
| `REQUIRED` | 默认值，有事务就加入，没有事务就新建 |
| `REQUIRES_NEW` | 总是新建事务，外层事务挂起 |
| `SUPPORTS` | 有事务就加入，没有事务就非事务执行 |
| `NOT_SUPPORTED` | 以非事务方式执行，有事务则挂起 |
| `MANDATORY` | 必须在事务中执行，没有事务就报错 |
| `NEVER` | 必须非事务执行，有事务就报错 |
| `NESTED` | 嵌套事务，依赖保存点 |

---

## 12. REQUIRED

默认传播行为。

```java
@Transactional(propagation = Propagation.REQUIRED)
public void methodA() {
}
```

规则：

```text
当前有事务：加入当前事务
当前没有事务：新建事务
```

最常用。

示例：

```java
@Transactional
public void createOrder() {
    saveOrder();
    reduceStock();
}
```

`saveOrder()` 和 `reduceStock()` 默认都会加入同一个事务。

---

## 13. REQUIRES_NEW

总是开启一个新事务。

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveLog() {
}
```

规则：

```text
当前有事务：挂起当前事务，新建事务
当前没有事务：新建事务
```

常见场景：

```text
操作日志
审计日志
失败记录
独立提交的数据
```

例如：

```java
@Transactional
public void createOrder() {
    try {
        // 创建订单
    } catch (Exception e) {
        logService.saveFailLog(); // REQUIRES_NEW
        throw e;
    }
}
```

即使外层订单事务回滚，日志事务也可以独立提交。

---

## 14. NESTED

嵌套事务，基于数据库保存点 Savepoint。

```java
@Transactional(propagation = Propagation.NESTED)
public void methodB() {
}
```

特点：

```text
外层事务回滚，内层事务一定回滚
内层事务回滚，不一定影响外层事务
```

简单理解：

```text
NESTED 是外层事务的一部分
REQUIRES_NEW 是完全独立的新事务
```

---

## 15. REQUIRED 和 REQUIRES_NEW 区别

| 对比项 | REQUIRED | REQUIRES_NEW |
|---|---|---|
| 是否新建事务 | 没有事务才新建 | 总是新建 |
| 是否加入外层事务 | 是 | 否 |
| 外层回滚是否影响内层 | 影响 | 不影响 |
| 内层回滚是否影响外层 | 通常影响 | 不一定影响 |
| 常见场景 | 普通业务事务 | 日志、审计、独立提交 |

---

# 三、事务隔离级别

## 16. 什么是隔离级别

事务隔离级别用于解决多个事务并发执行时的数据问题。

常见并发问题：

| 问题 | 说明 |
|---|---|
| 脏读 | 读到其他事务未提交的数据 |
| 不可重复读 | 同一事务中两次读取同一行数据结果不同 |
| 幻读 | 同一事务中两次范围查询，结果行数不同 |

---

## 17. Spring 支持的隔离级别

| 隔离级别 | 说明 |
|---|---|
| `DEFAULT` | 使用数据库默认隔离级别 |
| `READ_UNCOMMITTED` | 读未提交，可能脏读 |
| `READ_COMMITTED` | 读已提交，解决脏读 |
| `REPEATABLE_READ` | 可重复读，解决脏读、不可重复读 |
| `SERIALIZABLE` | 串行化，隔离最强，性能最低 |

示例：

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public void queryOrder() {
}
```

---

## 18. MySQL 默认隔离级别

MySQL InnoDB 默认隔离级别是：

```text
REPEATABLE READ
```

也就是可重复读。

Spring 中如果使用：

```java
@Transactional(isolation = Isolation.DEFAULT)
```

就会使用数据库默认隔离级别。

---

## 19. 隔离级别对比

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 性能 |
|---|---|---|---|---|
| READ_UNCOMMITTED | 可能 | 可能 | 可能 | 高 |
| READ_COMMITTED | 解决 | 可能 | 可能 | 较高 |
| REPEATABLE_READ | 解决 | 解决 | MySQL InnoDB 基本解决 | 中 |
| SERIALIZABLE | 解决 | 解决 | 解决 | 低 |

---

# 四、事务回滚规则

## 20. 默认回滚规则

Spring 默认只对以下异常回滚：

```text
RuntimeException
Error
```

默认不会对检查异常回滚。

例如：

```java
@Transactional
public void createOrder() {
    throw new RuntimeException();
}
```

会回滚。

但是：

```java
@Transactional
public void createOrder() throws Exception {
    throw new Exception();
}
```

默认不会回滚。

---

## 21. rollbackFor

如果希望所有异常都回滚，可以写：

```java
@Transactional(rollbackFor = Exception.class)
public void createOrder() throws Exception {
    throw new Exception();
}
```

推荐业务中经常使用：

```java
@Transactional(rollbackFor = Exception.class)
```

---

## 22. noRollbackFor

如果某些异常不想回滚，可以写：

```java
@Transactional(noRollbackFor = BusinessException.class)
public void createOrder() {
}
```

表示遇到 `BusinessException` 不回滚。

---

## 23. 异常被 catch 后事务会不会回滚

如果异常被捕获，并且没有继续抛出，事务不会回滚。

错误示例：

```java
@Transactional
public void createOrder() {
    try {
        // 执行业务
        int i = 1 / 0;
    } catch (Exception e) {
        log.error("异常", e);
    }
}
```

这里事务不会回滚，因为异常被吞掉了。

正确写法：

```java
@Transactional(rollbackFor = Exception.class)
public void createOrder() {
    try {
        // 执行业务
        int i = 1 / 0;
    } catch (Exception e) {
        log.error("异常", e);
        throw e;
    }
}
```

或者手动设置回滚：

```java
@Transactional
public void createOrder() {
    try {
        int i = 1 / 0;
    } catch (Exception e) {
        TransactionAspectSupport
            .currentTransactionStatus()
            .setRollbackOnly();
    }
}
```

---

# 五、Spring 事务原理

## 24. Spring 事务底层原理

Spring 声明式事务底层基于：

```text
AOP 动态代理
```

核心流程：

```text
调用代理对象方法
   ↓
事务拦截器 TransactionInterceptor
   ↓
获取事务属性
   ↓
开启事务
   ↓
调用目标方法
   ↓
方法正常返回：提交事务
   ↓
方法抛出异常：判断是否回滚
   ↓
回滚或提交
```

---

## 25. Spring 事务调用流程

```text
Controller
   ↓
OrderService 代理对象
   ↓
TransactionInterceptor
   ↓
PlatformTransactionManager 开启事务
   ↓
OrderServiceImpl.createOrder()
   ↓
执行 SQL
   ↓
提交事务 / 回滚事务
```

---

## 26. Spring 事务为什么依赖代理

因为 `@Transactional` 并不是直接修改方法代码。

Spring 会为目标类创建代理对象：

```text
目标对象：OrderServiceImpl
代理对象：OrderServiceProxy
```

调用代理对象时，代理对象在方法前后添加事务逻辑。

```text
代理对象开启事务
调用目标方法
代理对象提交/回滚事务
```

---

## 27. JDK 动态代理和 CGLIB

Spring 事务代理有两种方式：

| 代理方式 | 说明 |
|---|---|
| JDK 动态代理 | 基于接口 |
| CGLIB | 基于继承 |

默认规则：

```text
目标类实现接口：默认 JDK 动态代理
目标类没有接口：使用 CGLIB
```

强制使用 CGLIB：

```yaml
spring:
  aop:
    proxy-target-class: true
```

---

# 六、事务失效场景

## 28. 同类内部方法调用

最常见。

错误示例：

```java
@Service
public class OrderService {

    public void createOrder() {
        this.saveOrder();
    }

    @Transactional
    public void saveOrder() {
        // 保存订单
    }
}
```

原因：

```text
this.saveOrder() 调用的是当前对象方法
没有经过 Spring 代理对象
事务增强不会执行
```

解决方式：

```text
拆到另一个 Service
通过代理对象调用
使用 AopContext.currentProxy()
```

推荐方式：拆到另一个 Service。

```java
@Service
public class OrderService {

    @Autowired
    private OrderTxService orderTxService;

    public void createOrder() {
        orderTxService.saveOrder();
    }
}

@Service
public class OrderTxService {

    @Transactional
    public void saveOrder() {
        // 保存订单
    }
}
```

---

## 29. 方法不是 public

错误示例：

```java
@Transactional
private void saveOrder() {
}
```

或者：

```java
@Transactional
protected void saveOrder() {
}
```

原因：

```text
Spring 事务默认主要作用在 public 方法上
private 方法无法被代理正常增强
```

推荐：

```java
@Transactional
public void saveOrder() {
}
```

---

## 30. final 方法导致事务失效

```java
@Transactional
public final void saveOrder() {
}
```

原因：

```text
CGLIB 基于继承代理
final 方法不能被重写
所以无法增强
```

---

## 31. 类没有交给 Spring 管理

错误示例：

```java
OrderService orderService = new OrderService();
orderService.createOrder();
```

原因：

```text
自己 new 的对象不是 Spring Bean
不会生成代理对象
@Transactional 不生效
```

正确方式：

```java
@Autowired
private OrderService orderService;
```

---

## 32. 异常被捕获没有抛出

错误示例：

```java
@Transactional
public void createOrder() {
    try {
        int i = 1 / 0;
    } catch (Exception e) {
        log.error("异常", e);
    }
}
```

原因：

```text
异常被 catch 掉了
事务拦截器感知不到异常
所以不会回滚
```

解决：

```text
继续抛出异常
或手动设置 rollbackOnly
```

---

## 33. 抛出检查异常但没有配置 rollbackFor

错误示例：

```java
@Transactional
public void createOrder() throws Exception {
    throw new Exception();
}
```

默认不会回滚。

解决：

```java
@Transactional(rollbackFor = Exception.class)
public void createOrder() throws Exception {
    throw new Exception();
}
```

---

## 34. 数据库引擎不支持事务

例如 MySQL MyISAM 不支持事务。

如果表使用 MyISAM，即使加了 `@Transactional` 也不会真正回滚。

推荐使用：

```text
InnoDB
```

---

## 35. 多线程中事务失效

错误示例：

```java
@Transactional
public void createOrder() {
    new Thread(() -> {
        saveOrder();
    }).start();
}
```

原因：

```text
Spring 事务基于 ThreadLocal 保存事务上下文
新线程拿不到当前线程事务
```

解决：

```text
不要在事务中随意开新线程执行数据库操作
需要独立事务时，在新线程中调用 Spring Bean 的事务方法
```

---

## 36. 事务方法调用顺序不对

例如：

```java
@Transactional
public void outer() {
    inner();
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void inner() {
}
```

如果 `inner()` 是同类内部调用，那么 `REQUIRES_NEW` 不生效。

原因仍然是：

```text
没有经过代理对象
```

---

# 七、只读事务

## 37. readOnly

```java
@Transactional(readOnly = true)
public List<Order> listOrders() {
    return orderMapper.selectList();
}
```

作用：

```text
告诉 Spring 当前事务是只读事务
可以给数据库和 ORM 框架做优化提示
防止误写入
```

适合：

```text
查询方法
报表查询
列表查询
详情查询
```

注意：

```text
readOnly = true 不一定能强制阻止所有写操作
具体效果和数据库、驱动、ORM 实现有关
```

---

# 八、事务超时

## 38. timeout

设置事务超时时间，单位是秒。

```java
@Transactional(timeout = 5)
public void createOrder() {
}
```

表示事务超过 5 秒未完成，可能会被回滚。

适合：

```text
防止长事务
防止锁长时间占用
防止慢 SQL 拖垮系统
```

---

# 九、多数据源事务

## 39. 指定事务管理器

多数据源时，可能有多个事务管理器。

```java
@Transactional(transactionManager = "orderTransactionManager")
public void createOrder() {
}
```

如果不指定，可能使用默认事务管理器。

多数据源场景要注意：

```text
每个数据源对应自己的事务管理器
@Transactional 要指定正确 transactionManager
跨库事务不是普通本地事务能解决的
```

---

# 十、事务和锁

## 40. 事务和锁的关系

事务执行过程中，数据库可能会加锁。

例如：

```sql
update stock set count = count - 1 where product_id = 1001;
```

事务未提交前，相关行锁不会释放。

如果事务过长，会导致：

```text
锁等待
死锁
接口变慢
数据库连接占用
吞吐下降
```

所以事务中不要做耗时操作。

---

## 41. 事务中不建议做什么

事务中不建议做：

```text
远程 HTTP 调用
RPC 调用
发送 MQ 后等待结果
大文件 IO
复杂计算
长时间循环
用户交互等待
```

推荐：

```text
事务尽量短
只包住必要的数据库操作
外部调用尽量放在事务外
```

---

# 十一、事务最佳实践

## 42. 推荐写法

```java
@Transactional(rollbackFor = Exception.class)
public void createOrder() {
    // 只放核心数据库操作
}
```

---

## 43. 常见建议

```text
事务方法尽量 public
事务注解加在 Service 层
业务异常需要回滚时配置 rollbackFor
避免同类内部调用
避免事务中做远程调用
避免长事务
查询方法可以使用 readOnly = true
多数据源时指定 transactionManager
消费 MQ 时注意事务和幂等
```

---

# 十二、面试常问问题

## 44. Spring 事务原理是什么？

Spring 声明式事务底层基于 AOP 动态代理。

Spring 会为带有 `@Transactional` 的 Bean 创建代理对象。调用事务方法时，实际进入的是代理对象，代理对象通过 `TransactionInterceptor` 在目标方法执行前开启事务，在方法执行成功后提交事务，在方法抛出异常时根据回滚规则决定是否回滚。

---

## 45. Spring 事务什么时候会失效？

常见失效场景：

```text
同类内部方法调用
方法不是 public
final 方法
类没有交给 Spring 管理
异常被 catch 没有抛出
抛出检查异常但没有配置 rollbackFor
数据库不支持事务
多线程调用
事务传播行为使用不当
```

---

## 46. Spring 默认回滚哪些异常？

默认回滚：

```text
RuntimeException
Error
```

默认不回滚：

```text
检查异常 Exception
```

如果希望所有异常都回滚：

```java
@Transactional(rollbackFor = Exception.class)
```

---

## 47. REQUIRED 和 REQUIRES_NEW 区别？

```text
REQUIRED 是默认传播行为，有事务就加入，没有事务就新建。
REQUIRES_NEW 总是新建一个事务，如果当前已经有事务，会先挂起外层事务。
REQUIRED 内外层通常属于同一个事务，外层回滚会影响内层。
REQUIRES_NEW 是独立事务，内层提交后，外层回滚通常不会影响内层。
```

---

## 48. Spring 事务为什么同类调用会失效？

因为 Spring 事务是基于代理实现的。

同类内部调用使用的是：

```java
this.method()
```

它调用的是目标对象自身方法，没有经过代理对象，所以事务拦截器不会执行，事务也就不会生效。

---

## 49. @Transactional 加在类上和方法上有什么区别？

加在类上：

```text
类中所有 public 方法默认都使用该事务配置
```

加在方法上：

```text
只对该方法生效
方法上的事务配置优先级高于类上的配置
```

---

## 50. readOnly = true 有什么用？

```text
表示当前事务是只读事务
可以给数据库或 ORM 框架优化提示
适合查询类方法
```

但它不一定能强制阻止所有写操作。

---

# 十三、总结

Spring 事务主要分为编程式事务和声明式事务。编程式事务通过 `TransactionTemplate` 手动控制事务，声明式事务主要通过 `@Transactional` 实现，实际开发中声明式事务使用最多。

Spring 声明式事务底层是基于 AOP 动态代理实现的。Spring 会为事务 Bean 创建代理对象，调用事务方法时会进入 `TransactionInterceptor`，在方法执行前通过 `PlatformTransactionManager` 开启事务，方法正常执行完成后提交事务，如果方法抛出异常，则根据回滚规则决定是否回滚。

`@Transactional` 常用属性包括传播行为、隔离级别、回滚异常、超时时间和只读事务。默认传播行为是 `REQUIRED`，表示有事务就加入，没有事务就新建。常用的 `REQUIRES_NEW` 表示总是新建一个事务，并挂起外层事务。默认隔离级别是 `DEFAULT`，也就是使用数据库默认隔离级别。Spring 默认只对 `RuntimeException` 和 `Error` 回滚，如果希望检查异常也回滚，需要配置 `rollbackFor = Exception.class`。

Spring 事务常见失效场景包括同类内部方法调用、方法不是 public、final 方法、异常被 catch 没有抛出、类没有交给 Spring 管理、抛出检查异常但没有配置 rollbackFor、多线程调用等。本质原因大多是没有经过 Spring 代理对象，或者异常没有被事务拦截器感知。

---

## 51. 一句话总结

> Spring 事务本质是基于 AOP 动态代理，在方法执行前开启事务，执行成功后提交事务，出现异常时根据回滚规则回滚；使用时重点关注传播行为、隔离级别、回滚规则和事务失效场景。
