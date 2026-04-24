# MyBatis 启动流程总结

MyBatis 启动的核心过程是：**读取配置文件和 Mapper 文件，解析成 Configuration 对象，创建 SqlSessionFactory，扫描 Mapper 接口并生成代理对象，最后交给 Spring 容器管理**。

---

## 1. MyBatis 核心启动流程

整体流程：

```text
读取 MyBatis 配置
  ↓
解析 mybatis-config.xml
  ↓
解析 Mapper.xml / 注解 Mapper
  ↓
生成 Configuration 对象
  ↓
创建 SqlSessionFactory
  ↓
创建 SqlSession
  ↓
执行 Mapper 方法
```

MyBatis 官方文档中说明，MyBatis 应用的核心是 `SqlSessionFactory`，它可以通过 `SqlSessionFactoryBuilder` 从 XML 配置或 Configuration 对象构建出来；`SqlSession` 则由 `SqlSessionFactory` 创建，用于执行命令、获取 Mapper 和管理事务。:contentReference[oaicite:0]{index=0}

---

## 2. 解析配置文件

MyBatis 启动时会先解析全局配置文件，例如：

```xml
<configuration>
    <settings/>
    <typeAliases/>
    <plugins/>
    <environments/>
    <mappers/>
</configuration>
```

主要解析内容包括：

- 数据源配置。
- 事务管理器。
- 类型别名。
- 类型处理器。
- 插件。
- Mapper 映射文件。
- 全局配置项。

解析完成后，会封装到 `Configuration` 对象中。

---

## 3. 解析 Mapper 文件

Mapper 可以有两种形式：

```text
XML 方式
注解方式
```

### XML Mapper

例如：

```xml
<select id="selectById" resultType="User">
    SELECT * FROM user WHERE id = #{id}
</select>
```

MyBatis 会把每个 SQL 解析成一个 `MappedStatement`。

`MappedStatement` 中保存：

- SQL ID。
- SQL 语句。
- 参数映射。
- 返回值映射。
- SQL 类型。
- 缓存配置。
- 超时配置。

例如：

```text
namespace + SQL id = com.demo.UserMapper.selectById
```

最终会形成唯一的 `statementId`。

---

## 4. 创建 SqlSessionFactory

配置和 Mapper 解析完成后，会创建 `SqlSessionFactory`。

```text
Configuration
  ↓
SqlSessionFactoryBuilder
  ↓
SqlSessionFactory
```

`SqlSessionFactory` 是 MyBatis 的核心工厂对象，用于创建 `SqlSession`。

---

## 5. 创建 SqlSession

执行 SQL 时，需要通过 `SqlSessionFactory` 创建 `SqlSession`。

```java
SqlSession sqlSession = sqlSessionFactory.openSession();
```

`SqlSession` 的作用：

- 执行 SQL。
- 获取 Mapper 代理对象。
- 管理事务。
- 提交或回滚。
- 管理一级缓存。

---

## 6. Mapper 接口代理对象

MyBatis 中 Mapper 接口本身没有实现类，例如：

```java
public interface UserMapper {
    User selectById(Long id);
}
```

MyBatis 会通过 JDK 动态代理为 Mapper 接口生成代理对象。

调用：

```java
userMapper.selectById(1L);
```

实际执行流程：

```text
调用 Mapper 接口方法
  ↓
进入 MapperProxy
  ↓
根据接口名 + 方法名找到 MappedStatement
  ↓
通过 SqlSession 执行 SQL
  ↓
Executor 执行数据库操作
  ↓
结果映射成 Java 对象
```

---

## 7. Spring Boot 中 MyBatis 启动流程

在 Spring Boot 项目中，一般不会手动创建 `SqlSessionFactory`，而是由 MyBatis Spring Boot Starter 自动配置。

整体流程：

```text
Spring Boot 启动
  ↓
加载 MybatisAutoConfiguration
  ↓
创建 SqlSessionFactory
  ↓
创建 SqlSessionTemplate
  ↓
扫描 Mapper 接口
  ↓
注册 MapperFactoryBean
  ↓
生成 Mapper 代理对象
  ↓
注入到 Spring 容器
```

MyBatis Spring Boot 自动配置会向容器贡献 `SqlSessionFactory` 和 `SqlSessionTemplate`；如果使用 `@MapperScan` 或配置了 MyBatis 配置文件，它们会被纳入自动配置流程，否则会尝试基于根自动配置包下的 Mapper 接口进行注册。:contentReference[oaicite:1]{index=1}

---

## 8. Mapper 扫描流程

常见配置：

```java
@MapperScan("com.demo.mapper")
@SpringBootApplication
public class Application {
}
```

或者：

```java
@Mapper
public interface UserMapper {
}
```

启动时会扫描 Mapper 接口，并为每个 Mapper 注册 `MapperFactoryBean`。

流程：

```text
扫描 Mapper 接口
  ↓
封装成 BeanDefinition
  ↓
注册 MapperFactoryBean
  ↓
MapperFactoryBean 创建 Mapper 代理对象
  ↓
代理对象注入到 Service 中
```

所以我们可以直接注入 Mapper：

```java
@Resource
private UserMapper userMapper;
```

本质上注入的是 MyBatis 生成的代理对象。

---

## 9. SqlSessionTemplate 的作用

Spring 环境下通常使用 `SqlSessionTemplate`，而不是直接使用原生 `SqlSession`。

`SqlSessionTemplate` 的作用：

- 线程安全。
- 统一管理 SqlSession。
- 和 Spring 事务集成。
- 自动提交、回滚和关闭 SqlSession。
- 代理执行 Mapper 方法。

在 Spring 项目中：

```text
Mapper 代理对象
  ↓
SqlSessionTemplate
  ↓
SqlSession
  ↓
Executor
  ↓
数据库
```

---

## 10. SQL 执行流程

调用 Mapper 方法后，执行流程如下：

```text
Mapper 代理方法
  ↓
MapperProxy.invoke()
  ↓
根据方法找到 MappedStatement
  ↓
SqlSession 执行查询或更新
  ↓
Executor 执行 SQL
  ↓
StatementHandler 创建 Statement
  ↓
ParameterHandler 设置参数
  ↓
数据库执行 SQL
  ↓
ResultSetHandler 封装结果
  ↓
返回 Java 对象
```

---

## 11. MyBatis 核心对象

| 对象 | 作用 |
|---|---|
| Configuration | 保存 MyBatis 全局配置 |
| SqlSessionFactory | 创建 SqlSession |
| SqlSession | 执行 SQL、管理事务 |
| MapperProxy | Mapper 接口代理对象 |
| MappedStatement | 保存一条 SQL 的完整信息 |
| Executor | SQL 执行器 |
| StatementHandler | 创建和执行 JDBC Statement |
| ParameterHandler | 设置 SQL 参数 |
| ResultSetHandler | 处理结果集 |

---

## 12. 总结

MyBatis 启动时，首先会读取全局配置文件和 Mapper 映射文件，将配置项、SQL、参数映射、结果映射等信息解析成 `Configuration` 对象。

每一条 SQL 会被封装成一个 `MappedStatement`，并以 `namespace + 方法名` 作为唯一标识保存起来。

然后 MyBatis 通过 `SqlSessionFactoryBuilder` 创建 `SqlSessionFactory`，再通过 `SqlSessionFactory` 创建 `SqlSession`。

在 Spring Boot 项目中，MyBatis Starter 会自动配置 `SqlSessionFactory` 和 `SqlSessionTemplate`，并扫描 Mapper 接口，将 Mapper 接口注册成 `MapperFactoryBean`，最终生成 Mapper 代理对象放入 Spring 容器。

当调用 Mapper 方法时，实际调用的是 Mapper 代理对象，代理对象会根据接口名和方法名找到对应的 `MappedStatement`，然后通过 `SqlSession`、`Executor`、`StatementHandler`、`ParameterHandler` 和 `ResultSetHandler` 完成 SQL 执行和结果映射。

一句话总结：

```text
MyBatis 启动流程 = 解析配置和 Mapper -> 创建 Configuration -> 创建 SqlSessionFactory -> 扫描 Mapper -> 生成代理对象 -> 执行 SQL。
```
