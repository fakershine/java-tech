# MyBatis 用了哪些设计模式

## 目录

- [一、总览](#一总览)
- [二、Builder 建造者模式](#二builder-建造者模式)
- [三、Factory 工厂模式](#三factory-工厂模式)
- [四、Proxy 代理模式](#四proxy-代理模式)
- [五、Decorator 装饰器模式](#五decorator-装饰器模式)
- [六、Template Method 模板方法模式](#六template-method-模板方法模式)
- [七、Strategy 策略模式](#七strategy-策略模式)
- [八、Interceptor 拦截器模式](#八interceptor-拦截器模式)
- [九、Chain of Responsibility 责任链模式](#九chain-of-responsibility-责任链模式)
- [十、Adapter 适配器模式](#十adapter-适配器模式)
- [十一、Singleton 单例思想](#十一singleton-单例思想)
- [十二、MyBatis 执行流程中的设计模式](#十二mybatis-执行流程中的设计模式)
- [十三、面试速记版](#十三面试速记版)
- [十四、完整面试回答模板](#十四完整面试回答模板)

---

# 一、总览

MyBatis 中使用了很多设计模式，常见的有：

```text
1. Builder 建造者模式
2. Factory 工厂模式
3. Proxy 代理模式
4. Decorator 装饰器模式
5. Template Method 模板方法模式
6. Strategy 策略模式
7. Interceptor 拦截器模式
8. Chain of Responsibility 责任链模式
9. Adapter 适配器模式
10. Singleton 单例思想
```

这些设计模式主要体现在：

```text
配置解析
SqlSession 创建
Mapper 接口代理
SQL 执行器 Executor
缓存 Cache
插件 Interceptor
参数处理
结果集映射
```

---

# 二、Builder 建造者模式

## 1. 建造者模式是什么？

建造者模式用于创建复杂对象。

它把复杂对象的构建过程拆分出来，让创建过程更加清晰。

---

## 2. MyBatis 中的体现

MyBatis 中很多配置解析类都使用了 Builder 思想。

典型类包括：

```text
SqlSessionFactoryBuilder
XMLConfigBuilder
XMLMapperBuilder
XMLStatementBuilder
CacheBuilder
ResultMapResolver
```

---

## 3. SqlSessionFactoryBuilder

MyBatis 启动时通常这样写：

```java
InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");

SqlSessionFactory sqlSessionFactory =
        new SqlSessionFactoryBuilder().build(inputStream);
```

这里的：

```text
SqlSessionFactoryBuilder
```

就是典型的建造者。

它负责读取配置文件，解析配置，并最终构建出：

```text
SqlSessionFactory
```

---

## 4. 构建流程

大致流程：

```text
1. 读取 mybatis-config.xml
2. 创建 XMLConfigBuilder
3. 解析 environments、settings、typeAliases、plugins、mappers 等配置
4. 构建 Configuration 对象
5. 根据 Configuration 创建 DefaultSqlSessionFactory
```

---

## 5. 面试回答

MyBatis 中使用了建造者模式，比如 `SqlSessionFactoryBuilder`。它负责解析 MyBatis 配置文件，构建 `Configuration` 对象，最终创建 `SqlSessionFactory`。因为 MyBatis 配置项很多，比如数据源、事务管理器、插件、Mapper、别名、缓存等，构建过程比较复杂，所以使用 Builder 模式可以让对象创建过程更加清晰。

---

# 三、Factory 工厂模式

## 1. 工厂模式是什么？

工厂模式用于封装对象创建过程。

调用方不需要关心具体实现类，只需要通过工厂获取对象。

---

## 2. MyBatis 中的体现

MyBatis 中工厂模式非常常见，典型类包括：

```text
SqlSessionFactory
ObjectFactory
MapperProxyFactory
TransactionFactory
DataSourceFactory
LogFactory
Executor 创建逻辑
```

---

## 3. SqlSessionFactory

`SqlSessionFactory` 用于创建 `SqlSession`。

示例：

```java
SqlSession sqlSession = sqlSessionFactory.openSession();
```

调用方不需要关心 `SqlSession` 的具体创建过程。

---

## 4. ObjectFactory

MyBatis 查询结果映射成 Java 对象时，需要创建对象实例。

默认使用：

```text
DefaultObjectFactory
```

它负责创建结果对象。

例如：

```text
查询 User 表
    |
    v
ResultSet
    |
    v
User 对象
```

这个 User 对象就是通过 ObjectFactory 创建出来的。

---

## 5. MapperProxyFactory

Mapper 接口本身没有实现类。

例如：

```java
public interface UserMapper {
    User selectById(Long id);
}
```

但是可以这样使用：

```java
UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
```

底层会通过：

```text
MapperProxyFactory
```

创建 Mapper 接口的代理对象。

---

## 6. 面试回答

MyBatis 中使用了工厂模式，比如 `SqlSessionFactory` 用于创建 `SqlSession`，`ObjectFactory` 用于创建结果对象，`MapperProxyFactory` 用于创建 Mapper 接口代理对象。工厂模式的好处是隐藏对象创建细节，让调用方只依赖抽象，不直接依赖具体实现。

---

# 四、Proxy 代理模式

## 1. 代理模式是什么？

代理模式是指：

> 为目标对象创建一个代理对象，通过代理对象增强或控制对目标对象的访问。

---

## 2. MyBatis 中最典型的代理模式

MyBatis 中最典型的代理模式就是：

```text
Mapper 接口动态代理
```

我们平时只定义 Mapper 接口，不写实现类。

例如：

```java
public interface UserMapper {

    User selectById(Long id);

    int insert(User user);
}
```

但是可以直接调用：

```java
UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

User user = userMapper.selectById(1L);
```

原因就是 MyBatis 为 Mapper 接口生成了代理对象。

---

## 3. Mapper 代理流程

```text
1. 调用 sqlSession.getMapper(UserMapper.class)
2. MapperRegistry 找到对应 MapperProxyFactory
3. MapperProxyFactory 创建 JDK 动态代理
4. 方法调用进入 MapperProxy.invoke()
5. 根据接口方法找到 MappedStatement
6. 执行对应 SQL
7. 返回结果
```

---

## 4. 核心类

```text
MapperProxy
MapperProxyFactory
MapperRegistry
MapperMethod
```

---

## 5. MapperProxy 的作用

`MapperProxy` 实现了 JDK 动态代理中的：

```java
InvocationHandler
```

所有 Mapper 方法调用都会进入：

```java
invoke()
```

然后 MyBatis 根据方法信息找到对应 SQL 并执行。

---

## 6. 面试回答

MyBatis 中最典型的代理模式是 Mapper 接口的动态代理。我们只需要定义 Mapper 接口，不需要写实现类，调用 `sqlSession.getMapper()` 时，MyBatis 会通过 `MapperProxyFactory` 创建 JDK 动态代理对象。调用 Mapper 方法时，会进入 `MapperProxy.invoke()`，再根据方法名找到对应的 `MappedStatement`，最终执行 SQL。

---

# 五、Decorator 装饰器模式

## 1. 装饰器模式是什么？

装饰器模式是在不修改原对象代码的情况下，动态增强对象功能。

它通常通过包装原对象实现功能扩展。

---

## 2. MyBatis 中的体现

MyBatis 缓存模块大量使用装饰器模式。

典型类包括：

```text
Cache
PerpetualCache
LruCache
FifoCache
SoftCache
WeakCache
LoggingCache
SynchronizedCache
SerializedCache
ScheduledCache
BlockingCache
```

---

## 3. Cache 接口

MyBatis 缓存统一抽象为：

```java
public interface Cache {
    String getId();

    void putObject(Object key, Object value);

    Object getObject(Object key);

    Object removeObject(Object key);

    void clear();

    int getSize();
}
```

基础实现是：

```text
PerpetualCache
```

其他缓存功能通过装饰器增强。

---

## 4. 示例：缓存装饰

例如：

```text
PerpetualCache
    |
    v
LruCache
    |
    v
LoggingCache
    |
    v
SynchronizedCache
```

含义：

```text
PerpetualCache：基础缓存
LruCache：增加 LRU 淘汰策略
LoggingCache：增加日志能力
SynchronizedCache：增加同步控制
```

---

## 5. Executor 中的装饰器

MyBatis 的二级缓存执行器也是装饰器模式。

典型类：

```text
Executor
BaseExecutor
SimpleExecutor
ReuseExecutor
BatchExecutor
CachingExecutor
```

`CachingExecutor` 包装真正的 Executor，用于增加二级缓存能力。

```text
CachingExecutor
        |
        v
SimpleExecutor / ReuseExecutor / BatchExecutor
```

---

## 6. 面试回答

MyBatis 的缓存模块使用了装饰器模式。比如基础缓存是 `PerpetualCache`，在它外面可以包装 `LruCache`、`LoggingCache`、`SynchronizedCache` 等装饰器，从而增加 LRU 淘汰、日志记录、同步控制等能力。另外 `CachingExecutor` 也使用了装饰器模式，它包装真正的 Executor，在 SQL 执行前后增加二级缓存逻辑。

---

# 六、Template Method 模板方法模式

## 1. 模板方法模式是什么？

模板方法模式是在父类中定义算法骨架，把某些具体步骤交给子类实现。

---

## 2. MyBatis 中的体现

MyBatis 中的 Executor 使用了模板方法模式。

典型类：

```text
BaseExecutor
SimpleExecutor
ReuseExecutor
BatchExecutor
```

---

## 3. BaseExecutor

`BaseExecutor` 定义了 SQL 执行的通用流程，例如：

```text
一级缓存处理
延迟加载处理
事务处理
查询流程控制
更新流程控制
```

但是具体数据库操作交给子类实现。

例如：

```text
doQuery()
doUpdate()
doFlushStatements()
```

这些方法由子类实现。

---

## 4. 子类职责

| Executor | 作用 |
|---|---|
| SimpleExecutor | 每次执行都创建 Statement |
| ReuseExecutor | 复用 Statement |
| BatchExecutor | 批量执行 SQL |

---

## 5. 面试回答

MyBatis 的 Executor 使用了模板方法模式。`BaseExecutor` 定义了 SQL 执行的公共流程，比如一级缓存、延迟加载、事务相关处理等，而真正执行数据库操作的方法，比如 `doQuery`、`doUpdate`，由 `SimpleExecutor`、`ReuseExecutor`、`BatchExecutor` 这些子类实现。这样可以复用公共逻辑，同时支持不同执行策略。

---

# 七、Strategy 策略模式

## 1. 策略模式是什么？

策略模式是把不同算法或行为封装成不同策略类，运行时根据需要选择不同策略。

---

## 2. MyBatis 中的体现

MyBatis 中很多地方都体现了策略模式。

常见包括：

```text
Executor 执行策略
StatementHandler 处理策略
TypeHandler 类型转换策略
LanguageDriver SQL 解析策略
TransactionFactory 事务策略
DataSourceFactory 数据源策略
```

---

## 3. Executor 策略

MyBatis 支持不同 Executor 类型：

```text
SIMPLE
REUSE
BATCH
```

对应实现：

```text
SimpleExecutor
ReuseExecutor
BatchExecutor
```

配置示例：

```xml
<settings>
    <setting name="defaultExecutorType" value="SIMPLE"/>
</settings>
```

不同策略：

```text
SimpleExecutor：普通执行
ReuseExecutor：复用 Statement
BatchExecutor：批量执行
```

---

## 4. TypeHandler 策略

MyBatis 需要在 Java 类型和 JDBC 类型之间转换。

例如：

```text
String <-> VARCHAR
Integer <-> INTEGER
LocalDateTime <-> TIMESTAMP
Enum <-> VARCHAR / INTEGER
```

不同类型由不同 TypeHandler 处理。

典型类：

```text
StringTypeHandler
IntegerTypeHandler
DateTypeHandler
EnumTypeHandler
LocalDateTimeTypeHandler
```

---

## 5. StatementHandler 策略

StatementHandler 负责处理 JDBC Statement。

常见实现：

```text
SimpleStatementHandler
PreparedStatementHandler
CallableStatementHandler
```

分别对应：

```text
Statement
PreparedStatement
CallableStatement
```

---

## 6. 面试回答

MyBatis 中使用了策略模式，比如 Executor 有 `SimpleExecutor`、`ReuseExecutor`、`BatchExecutor` 三种执行策略；StatementHandler 有普通 Statement、PreparedStatement、CallableStatement 不同处理策略；TypeHandler 负责不同 Java 类型和 JDBC 类型之间的转换。策略模式让 MyBatis 可以根据配置或 SQL 类型选择不同处理方式，扩展性比较好。

---

# 八、Interceptor 拦截器模式

## 1. 拦截器模式是什么？

拦截器模式允许在目标方法执行前后插入自定义逻辑。

常见用途：

```text
日志记录
性能统计
权限校验
SQL 改写
分页处理
数据权限
多租户
```

---

## 2. MyBatis 插件机制

MyBatis 插件机制就是典型的拦截器模式。

核心接口：

```java
public interface Interceptor {

    Object intercept(Invocation invocation) throws Throwable;

    Object plugin(Object target);

    void setProperties(Properties properties);
}
```

---

## 3. MyBatis 可以拦截哪些对象？

MyBatis 插件默认可以拦截四大对象：

```text
Executor
StatementHandler
ParameterHandler
ResultSetHandler
```

---

## 4. 四大对象作用

| 对象 | 作用 |
|---|---|
| Executor | 执行 SQL |
| StatementHandler | 创建和处理 Statement |
| ParameterHandler | 设置 SQL 参数 |
| ResultSetHandler | 处理结果集 |

---

## 5. 插件示例

```java
@Intercepts({
    @Signature(
            type = StatementHandler.class,
            method = "prepare",
            args = {Connection.class, Integer.class}
    )
})
public class SqlLogInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();

        Object result = invocation.proceed();

        long cost = System.currentTimeMillis() - start;
        System.out.println("SQL cost: " + cost + "ms");

        return result;
    }
}
```

---

## 6. 面试回答

MyBatis 的插件机制使用了拦截器模式。MyBatis 允许我们拦截 Executor、StatementHandler、ParameterHandler、ResultSetHandler 这四大对象，在 SQL 执行前后插入自定义逻辑。比如分页插件、数据权限、多租户、SQL 日志、性能监控都可以通过 Interceptor 实现。

---

# 九、Chain of Responsibility 责任链模式

## 1. 责任链模式是什么？

责任链模式是把多个处理器串成一条链，请求沿着链路依次处理。

---

## 2. MyBatis 中的体现

MyBatis 插件机制中也体现了责任链思想。

核心类：

```text
InterceptorChain
```

MyBatis 会把多个插件按顺序包装到目标对象上。

---

## 3. InterceptorChain

当 MyBatis 创建 Executor、StatementHandler 等对象时，会调用：

```java
interceptorChain.pluginAll(target)
```

多个插件会依次对目标对象进行包装。

---

## 4. 插件执行链

假设配置了三个插件：

```text
PluginA
PluginB
PluginC
```

执行时可能形成类似结构：

```text
PluginC
  -> PluginB
      -> PluginA
          -> 原始对象
```

每个插件都可以在执行前后增强逻辑。

---

## 5. 面试回答

MyBatis 的插件机制中也体现了责任链模式。多个 Interceptor 会通过 `InterceptorChain` 依次包装目标对象，方法调用时会按照插件链依次执行。比如同时配置分页插件、数据权限插件、SQL 日志插件时，它们会形成一条处理链，对 SQL 执行过程进行增强。

---

# 十、Adapter 适配器模式

## 1. 适配器模式是什么？

适配器模式用于把一个接口转换成另一个接口，让原本不兼容的类可以协同工作。

---

## 2. MyBatis 中的体现

MyBatis 中日志模块体现了适配器模式。

MyBatis 支持多种日志框架：

```text
SLF4J
Log4j
Log4j2
JDK Logging
Commons Logging
StdOut
NoLogging
```

这些日志框架 API 不同，但 MyBatis 统一抽象为：

```text
Log
```

---

## 3. Log 接口

MyBatis 定义统一日志接口：

```java
public interface Log {

    boolean isDebugEnabled();

    boolean isTraceEnabled();

    void error(String s, Throwable e);

    void error(String s);

    void debug(String s);

    void trace(String s);

    void warn(String s);
}
```

不同日志框架有不同适配器实现。

例如：

```text
Slf4jImpl
Log4jImpl
Log4j2Impl
Jdk14LoggingImpl
StdOutImpl
NoLoggingImpl
```

---

## 4. 面试回答

MyBatis 的日志模块使用了适配器模式。因为 MyBatis 需要兼容 SLF4J、Log4j、Log4j2、JDK Logging 等不同日志框架，而这些日志框架 API 不一样。所以 MyBatis 定义了统一的 `Log` 接口，再通过不同的适配器类适配具体日志框架，这样 MyBatis 内部只依赖统一的日志抽象。

---

# 十一、Singleton 单例思想

## 1. 单例模式是什么？

单例模式保证一个类在系统中只有一个实例，并提供全局访问点。

严格意义上，MyBatis 中不是所有对象都是经典单例模式，但很多对象具有单例使用思想。

---

## 2. MyBatis 中的体现

常见对象：

```text
Configuration
SqlSessionFactory
TypeAliasRegistry
TypeHandlerRegistry
MapperRegistry
LogFactory
```

---

## 3. SqlSessionFactory

通常一个应用只创建一个：

```text
SqlSessionFactory
```

它是线程安全的，可以全局复用。

但是：

```text
SqlSession 不是线程安全的，不能全局共享。
```

---

## 4. Configuration

`Configuration` 是 MyBatis 的核心配置对象。

它保存：

```text
MappedStatement
ResultMap
TypeAlias
TypeHandler
Mapper
Interceptor
Environment
```

通常由 `SqlSessionFactory` 持有并复用。

---

## 5. 面试回答

MyBatis 中也有单例思想，比如 `SqlSessionFactory` 通常在应用中只创建一个并全局复用，`Configuration` 也由 SqlSessionFactory 持有并复用。不过要注意，`SqlSessionFactory` 是线程安全的，但 `SqlSession` 不是线程安全的，不能作为单例共享，每次请求或每次操作应该创建新的 SqlSession，用完后关闭。

---

# 十二、MyBatis 执行流程中的设计模式

## 1. 启动阶段

```text
SqlSessionFactoryBuilder
        |
        | Builder 模式
        v
解析 mybatis-config.xml
        |
        v
XMLConfigBuilder / XMLMapperBuilder
        |
        v
Configuration
        |
        v
DefaultSqlSessionFactory
```

涉及模式：

```text
Builder 建造者模式
Factory 工厂模式
```

---

## 2. 获取 Mapper 阶段

```text
sqlSession.getMapper(UserMapper.class)
        |
        v
MapperRegistry
        |
        v
MapperProxyFactory
        |
        v
MapperProxy
        |
        v
JDK 动态代理对象
```

涉及模式：

```text
Factory 工厂模式
Proxy 代理模式
```

---

## 3. 执行 SQL 阶段

```text
Mapper 方法调用
        |
        v
MapperProxy.invoke()
        |
        v
MapperMethod.execute()
        |
        v
SqlSession.select/update
        |
        v
Executor
        |
        v
StatementHandler
        |
        v
ParameterHandler
        |
        v
ResultSetHandler
```

涉及模式：

```text
Proxy 代理模式
Strategy 策略模式
Template Method 模板方法模式
Interceptor 拦截器模式
```

---

## 4. 缓存阶段

```text
Executor
   |
   v
CachingExecutor
   |
   v
Cache
   |
   v
PerpetualCache + LruCache + LoggingCache + SynchronizedCache
```

涉及模式：

```text
Decorator 装饰器模式
```

---

## 5. 插件阶段

```text
Executor / StatementHandler / ParameterHandler / ResultSetHandler
        |
        v
InterceptorChain
        |
        v
多个 Interceptor 依次增强
```

涉及模式：

```text
Interceptor 拦截器模式
Chain of Responsibility 责任链模式
Proxy 代理模式
```

---

# 十三、面试速记版

## 1. MyBatis 用了哪些设计模式？

```text
Builder 建造者模式
Factory 工厂模式
Proxy 代理模式
Decorator 装饰器模式
Template Method 模板方法模式
Strategy 策略模式
Interceptor 拦截器模式
Chain of Responsibility 责任链模式
Adapter 适配器模式
Singleton 单例思想
```

---

## 2. Builder 建造者模式

```text
SqlSessionFactoryBuilder
XMLConfigBuilder
XMLMapperBuilder
```

用于解析配置并构建复杂对象。

---

## 3. Factory 工厂模式

```text
SqlSessionFactory
ObjectFactory
MapperProxyFactory
TransactionFactory
DataSourceFactory
```

用于封装对象创建过程。

---

## 4. Proxy 代理模式

```text
MapperProxy
MapperProxyFactory
```

Mapper 接口没有实现类，MyBatis 使用 JDK 动态代理生成代理对象。

---

## 5. Decorator 装饰器模式

```text
CachingExecutor
LruCache
LoggingCache
SynchronizedCache
```

用于增强 Executor 和 Cache 功能。

---

## 6. Template Method 模板方法模式

```text
BaseExecutor
SimpleExecutor
ReuseExecutor
BatchExecutor
```

`BaseExecutor` 定义执行骨架，子类实现具体执行逻辑。

---

## 7. Strategy 策略模式

```text
SimpleExecutor / ReuseExecutor / BatchExecutor
TypeHandler
StatementHandler
TransactionFactory
```

根据不同场景选择不同处理策略。

---

## 8. Interceptor 拦截器模式

```text
Interceptor
Invocation
Plugin
```

MyBatis 插件机制可以拦截 Executor、StatementHandler、ParameterHandler、ResultSetHandler。

---

## 9. Chain of Responsibility 责任链模式

```text
InterceptorChain
```

多个插件依次包装和执行。

---

## 10. Adapter 适配器模式

```text
Log
Slf4jImpl
Log4jImpl
Jdk14LoggingImpl
```

统一适配不同日志框架。

---

# 十四、完整面试回答模板

MyBatis 中使用了很多设计模式，比较典型的有建造者模式、工厂模式、代理模式、装饰器模式、模板方法模式、策略模式、拦截器模式、责任链模式和适配器模式。

首先是建造者模式。MyBatis 启动时会通过 `SqlSessionFactoryBuilder` 读取配置文件，解析 `mybatis-config.xml` 和 Mapper XML，构建 `Configuration` 对象，最后创建 `SqlSessionFactory`。因为 MyBatis 配置项比较多，比如数据源、事务管理器、插件、Mapper、别名、缓存等，构建过程比较复杂，所以使用 Builder 模式比较合适。

其次是工厂模式。`SqlSessionFactory` 用于创建 `SqlSession`，`ObjectFactory` 用于创建结果对象，`MapperProxyFactory` 用于创建 Mapper 接口代理对象，`TransactionFactory` 用于创建事务对象。这些都封装了对象创建细节，调用方不需要直接依赖具体实现类。

第三是代理模式。MyBatis 中最典型的代理模式是 Mapper 接口动态代理。我们只需要定义 Mapper 接口，不需要写实现类。调用 `sqlSession.getMapper()` 时，MyBatis 会通过 `MapperProxyFactory` 创建 JDK 动态代理对象。调用 Mapper 方法时，会进入 `MapperProxy.invoke()`，然后根据接口方法找到对应的 `MappedStatement`，最终执行 SQL。

第四是装饰器模式。MyBatis 的缓存模块使用了大量装饰器。基础缓存是 `PerpetualCache`，外层可以包装 `LruCache`、`LoggingCache`、`SynchronizedCache` 等，用来增加 LRU 淘汰、日志记录、同步控制等功能。另外，`CachingExecutor` 也是装饰器模式，它包装真正的 Executor，在执行 SQL 前后增加二级缓存逻辑。

第五是模板方法模式。MyBatis 的 `BaseExecutor` 定义了 SQL 执行的通用流程，比如一级缓存、延迟加载、事务处理等，而具体的数据库操作由 `SimpleExecutor`、`ReuseExecutor`、`BatchExecutor` 实现。这样可以复用公共逻辑，同时支持不同的执行方式。

第六是策略模式。MyBatis 中有很多可替换策略，比如 Executor 有 `SimpleExecutor`、`ReuseExecutor`、`BatchExecutor`；StatementHandler 有 `SimpleStatementHandler`、`PreparedStatementHandler`、`CallableStatementHandler`；TypeHandler 负责不同 Java 类型和 JDBC 类型之间的转换。这些都是根据不同场景选择不同策略。

第七是拦截器模式和责任链模式。MyBatis 插件机制允许我们拦截 `Executor`、`StatementHandler`、`ParameterHandler`、`ResultSetHandler` 四大对象，在 SQL 执行前后增强逻辑，比如分页、SQL 日志、数据权限、多租户等。多个插件会通过 `InterceptorChain` 依次包装目标对象，形成类似责任链的执行结构。

最后是适配器模式。MyBatis 日志模块定义了统一的 `Log` 接口，然后通过 `Slf4jImpl`、`Log4jImpl`、`Jdk14LoggingImpl` 等适配不同日志框架。这样 MyBatis 内部只依赖统一日志接口，不需要关心底层具体使用哪种日志实现。

所以总结来说，MyBatis 中比较核心的设计模式是：`SqlSessionFactoryBuilder` 使用建造者模式，`SqlSessionFactory` 和 `ObjectFactory` 使用工厂模式，Mapper 接口使用代理模式，缓存和 `CachingExecutor` 使用装饰器模式，`BaseExecutor` 使用模板方法模式，Executor、StatementHandler、TypeHandler 使用策略模式，插件机制使用拦截器和责任链模式，日志模块使用适配器模式。
